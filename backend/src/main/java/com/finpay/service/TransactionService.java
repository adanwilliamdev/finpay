package com.finpay.service;

import com.finpay.dto.request.DepositRequest;
import com.finpay.dto.request.TransferRequest;
import com.finpay.dto.response.BalanceResponse;
import com.finpay.dto.response.TransactionResponse;
import com.finpay.entity.Transaction;
import com.finpay.entity.Wallet;
import com.finpay.exception.CurrencyMismatchException;
import com.finpay.exception.InsufficientBalanceException;
import com.finpay.exception.WalletNotFoundException;
import com.finpay.repository.TransactionRepository;
import com.finpay.repository.WalletRepository;
import com.finpay.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final AuditService auditService;

    @Transactional
    public TransactionResponse deposit(DepositRequest request, HttpServletRequest httpRequest) {
        log.info("Processing deposit of {} to wallet: {}", request.getAmount(), request.getWalletId());

        Wallet wallet = walletRepository.findByIdWithLock(request.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + request.getWalletId()));

        // A user may only deposit into their own wallet (admins may deposit into any wallet)
        SecurityUtil.checkOwnerOrAdmin(wallet.getUser() != null ? wallet.getUser().getId() : null);

        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.credit(request.getAmount());
        wallet = walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .transactionId(generateTransactionId())
                .amount(request.getAmount())
                .type(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription() != null ? request.getDescription() : "Deposit")
                .currency(wallet.getCurrency())
                .destinationWallet(wallet)
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .ipAddress(getClientIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .build();

        transaction = transactionRepository.save(transaction);

        auditService.logAction("DEPOSIT", "Transaction", transaction.getId(),
                null, transaction.getAmount().toString(), "Deposit completed");

        log.info("Deposit completed successfully. Transaction ID: {}", transaction.getId());

        return convertToResponse(transaction);
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request, HttpServletRequest httpRequest) {
        log.info("Processing transfer of {} from wallet {} to wallet {}",
                request.getAmount(), request.getSourceWalletId(), request.getDestinationWalletId());

        // The client submits the destination as a human-facing wallet NUMBER
        // (e.g. "WPA9BA548D"), not the internal wallet ID/UUID - the sender has no way
        // of knowing the recipient's internal ID. Resolve it to the real wallet ID
        // up front so the locking logic below (which operates on IDs) works correctly.
        String destinationWalletId = walletRepository.findByWalletNumber(request.getDestinationWalletId())
                .map(Wallet::getId)
                .orElseThrow(() -> new WalletNotFoundException(
                        "Destination wallet not found: " + request.getDestinationWalletId()));

        if (request.getSourceWalletId().equals(destinationWalletId)) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }

        // Lock both wallets in a canonical order (by ID) rather than by the order they
        // arrived in the request. If we always locked "source then destination", two
        // concurrent transfers in opposite directions (A->B and B->A) would lock A/B in
        // reverse order from one another and could deadlock. Locking in ID order means
        // every transfer involving A and B always acquires the locks in the same sequence.
        boolean sourceFirst = request.getSourceWalletId().compareTo(destinationWalletId) < 0;
        String firstId = sourceFirst ? request.getSourceWalletId() : destinationWalletId;
        String secondId = sourceFirst ? destinationWalletId : request.getSourceWalletId();

        String firstLabel = sourceFirst ? "Source" : "Destination";
        String secondLabel = sourceFirst ? "Destination" : "Source";

        Wallet firstWallet = walletRepository.findByIdWithLock(firstId)
                .orElseThrow(() -> new WalletNotFoundException(firstLabel + " wallet not found: " + firstId));
        Wallet secondWallet = walletRepository.findByIdWithLock(secondId)
                .orElseThrow(() -> new WalletNotFoundException(secondLabel + " wallet not found: " + secondId));

        Wallet sourceWallet = sourceFirst ? firstWallet : secondWallet;
        Wallet destinationWallet = sourceFirst ? secondWallet : firstWallet;

        if (!sourceWallet.getCurrency().equals(destinationWallet.getCurrency())) {
            throw new CurrencyMismatchException(
                    "Cannot transfer between wallets with different currencies: " +
                            sourceWallet.getCurrency() + " -> " + destinationWallet.getCurrency());
        }

        // Only the owner of the source wallet (or an admin) can move money out of it.
        // No ownership check on the destination wallet: transferring TO another person's
        // wallet is the whole point of a peer-to-peer transfer.
        SecurityUtil.checkOwnerOrAdmin(sourceWallet.getUser() != null ? sourceWallet.getUser().getId() : null);

        if (sourceWallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new IllegalStateException("Source wallet is not active");
        }

        if (destinationWallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new IllegalStateException("Destination wallet is not active");
        }

        BigDecimal sourceBalanceBefore = sourceWallet.getBalance();
        BigDecimal destBalanceBefore = destinationWallet.getBalance();

        // Debit from source
        sourceWallet.debit(request.getAmount());
        sourceWallet = walletRepository.save(sourceWallet);

        // Credit to destination
        destinationWallet.credit(request.getAmount());
        destinationWallet = walletRepository.save(destinationWallet);

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .transactionId(generateTransactionId())
                .amount(request.getAmount())
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription() != null ? request.getDescription() : "Transfer")
                .currency(sourceWallet.getCurrency())
                .sourceWallet(sourceWallet)
                .destinationWallet(destinationWallet)
                .balanceBefore(sourceBalanceBefore)
                .balanceAfter(sourceWallet.getBalance())
                .ipAddress(getClientIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .build();

        transaction = transactionRepository.save(transaction);

        auditService.logAction("TRANSFER", "Transaction", transaction.getId(),
                null, transaction.getAmount().toString(),
                String.format("Transfer from %s to %s", sourceWallet.getId(), destinationWallet.getId()));

        log.info("Transfer completed successfully. Transaction ID: {}", transaction.getId());

        return convertToResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByWallet(String walletId, Pageable pageable) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
        SecurityUtil.checkOwnerOrAdmin(wallet.getUser() != null ? wallet.getUser().getId() : null);

        return transactionRepository.findByWalletId(walletId, pageable)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalanceSummary(String walletId) {
        log.info("Getting balance summary for wallet: {}", walletId);

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        SecurityUtil.checkOwnerOrAdmin(wallet.getUser() != null ? wallet.getUser().getId() : null);

        BigDecimal totalDeposits = transactionRepository.sumDepositsByWallet(walletId);
        BigDecimal totalWithdrawals = transactionRepository.sumWithdrawalsByWallet(walletId);

        if (totalDeposits == null) totalDeposits = BigDecimal.ZERO;
        if (totalWithdrawals == null) totalWithdrawals = BigDecimal.ZERO;

        // Calculate total transfers
        List<Transaction> transfersOut = transactionRepository.findTransfersBySourceWallet(walletId);
        List<Transaction> transfersIn = transactionRepository.findTransfersByDestinationWallet(walletId);

        BigDecimal totalTransfers = BigDecimal.ZERO;
        for (Transaction t : transfersOut) {
            if (t.getStatus() == Transaction.TransactionStatus.COMPLETED) {
                totalTransfers = totalTransfers.add(t.getAmount());
            }
        }
        for (Transaction t : transfersIn) {
            if (t.getStatus() == Transaction.TransactionStatus.COMPLETED) {
                totalTransfers = totalTransfers.add(t.getAmount());
            }
        }

        List<Transaction> recentTransactions = transactionRepository.findByWalletId(walletId, Pageable.ofSize(10))
                .getContent();

        return BalanceResponse.builder()
                .totalBalance(wallet.getBalance())
                .totalDeposits(totalDeposits)
                .totalWithdrawals(totalWithdrawals)
                .totalTransfers(totalTransfers)
                .recentTransactions(recentTransactions.stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList()))
                .totalTransactions((int) transactionRepository.findByWalletId(walletId, Pageable.unpaged()).getTotalElements())
                .build();
    }

    @Transactional
    public TransactionResponse reverseTransaction(String transactionId, HttpServletRequest httpRequest) {
        log.info("Reversing transaction: {}", transactionId);

        Transaction originalTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Only a user who owns one of the wallets involved in the transaction (or an
        // admin) may reverse it - otherwise anyone could reverse anyone else's transfers.
        boolean ownsSource = originalTransaction.getSourceWallet() != null
                && originalTransaction.getSourceWallet().getUser() != null
                && originalTransaction.getSourceWallet().getUser().getId().equals(SecurityUtil.getCurrentUserId());
        boolean ownsDestination = originalTransaction.getDestinationWallet() != null
                && originalTransaction.getDestinationWallet().getUser() != null
                && originalTransaction.getDestinationWallet().getUser().getId().equals(SecurityUtil.getCurrentUserId());

        if (!ownsSource && !ownsDestination && !SecurityUtil.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You don't have permission to reverse this transaction");
        }

        if (originalTransaction.getStatus() == Transaction.TransactionStatus.REVERSED) {
            throw new IllegalStateException("Transaction already reversed");
        }

        if (originalTransaction.getStatus() != Transaction.TransactionStatus.COMPLETED) {
            throw new IllegalStateException("Only completed transactions can be reversed");
        }

        Transaction reversal = null;

        if (originalTransaction.getType() == Transaction.TransactionType.DEPOSIT) {
            // Reverse deposit: withdraw from destination wallet
            Wallet wallet = originalTransaction.getDestinationWallet();
            BigDecimal balanceBefore = wallet.getBalance();
            wallet.debit(originalTransaction.getAmount());
            wallet = walletRepository.save(wallet);

            reversal = createReversalTransaction(originalTransaction, wallet, null,
                    balanceBefore, wallet.getBalance(), httpRequest);
        } else if (originalTransaction.getType() == Transaction.TransactionType.TRANSFER) {
            // Reverse transfer: transfer back
            Wallet sourceWallet = originalTransaction.getSourceWallet();
            Wallet destinationWallet = originalTransaction.getDestinationWallet();

            BigDecimal sourceBalanceBefore = sourceWallet.getBalance();
            BigDecimal destBalanceBefore = destinationWallet.getBalance();

            // Reverse the transfer: debit from destination, credit to source
            destinationWallet.debit(originalTransaction.getAmount());
            sourceWallet.credit(originalTransaction.getAmount());

            walletRepository.save(sourceWallet);
            walletRepository.save(destinationWallet);

            reversal = createReversalTransaction(originalTransaction, sourceWallet, destinationWallet,
                    sourceBalanceBefore, sourceWallet.getBalance(), httpRequest);
        } else if (originalTransaction.getType() == Transaction.TransactionType.WITHDRAWAL) {
            // Reverse withdrawal: deposit back
            Wallet wallet = originalTransaction.getSourceWallet();
            BigDecimal balanceBefore = wallet.getBalance();
            wallet.credit(originalTransaction.getAmount());
            wallet = walletRepository.save(wallet);

            reversal = createReversalTransaction(originalTransaction, null, wallet,
                    balanceBefore, wallet.getBalance(), httpRequest);
        }

        originalTransaction.setStatus(Transaction.TransactionStatus.REVERSED);
        transactionRepository.save(originalTransaction);

        auditService.logAction("REVERSE_TRANSACTION", "Transaction", transactionId,
                originalTransaction.getStatus().name(), "REVERSED", "Transaction reversed");

        return convertToResponse(reversal != null ? reversal : originalTransaction);
    }

    private Transaction createReversalTransaction(Transaction original, Wallet sourceWallet,
                                                  Wallet destinationWallet, BigDecimal balanceBefore,
                                                  BigDecimal balanceAfter, HttpServletRequest httpRequest) {
        Transaction reversal = Transaction.builder()
                .transactionId(generateTransactionId())
                .amount(original.getAmount())
                .type(Transaction.TransactionType.REVERSAL)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description("Reversal of transaction: " + original.getTransactionId())
                .currency(original.getCurrency())
                .sourceWallet(sourceWallet != null ? sourceWallet : original.getDestinationWallet())
                .destinationWallet(destinationWallet != null ? destinationWallet : original.getSourceWallet())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .ipAddress(getClientIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .build();

        return transactionRepository.save(reversal);
    }

    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private TransactionResponse convertToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .amount(transaction.getAmount())
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .description(transaction.getDescription())
                .currency(transaction.getCurrency())
                .sourceWalletId(transaction.getSourceWallet() != null ?
                        transaction.getSourceWallet().getId() : null)
                .destinationWalletId(transaction.getDestinationWallet() != null ?
                        transaction.getDestinationWallet().getId() : null)
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}