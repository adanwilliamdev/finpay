package com.finpay.service;

import com.finpay.dto.request.DepositRequest;
import com.finpay.dto.request.TransferRequest;
import com.finpay.dto.request.WithdrawRequest;
import com.finpay.dto.response.BalanceResponse;
import com.finpay.dto.response.TransactionResponse;
import com.finpay.entity.Transaction;
import com.finpay.entity.Transaction.TransactionStatus;
import com.finpay.entity.Transaction.TransactionType;
import com.finpay.entity.Wallet;
import com.finpay.exception.CurrencyMismatchException;
import com.finpay.exception.InsufficientBalanceException;
import com.finpay.exception.ReversalNotAllowedException;
import com.finpay.exception.TransactionNotFoundException;
import com.finpay.exception.WalletNotFoundException;
import com.finpay.repository.TransactionRepository;
import com.finpay.repository.WalletRepository;
import com.finpay.security.SecurityUtil;
import com.finpay.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final TransactionTemplate transactionTemplate;

    /** How long after a transfer/withdrawal its sender may still reverse it without an admin. */
    @Value("${finpay.reversal.user-window-minutes:30}")
    private long reversalUserWindowMinutes;

    // ------------------------------------------------------------------------------------
    // Deposit / withdrawal / transfer
    //
    // These methods are intentionally NOT @Transactional. The money movement runs inside
    // a programmatic transaction (TransactionTemplate); when a business rule rejects the
    // operation that transaction rolls back and ONLY THEN is a FAILED record written, in a
    // fresh transaction. Writing it inside the failing transaction would be rolled back with
    // it, and writing it with REQUIRES_NEW from inside would self-deadlock: the new
    // connection's FK check needs a KEY SHARE lock on wallet rows that the outer transaction
    // holds FOR UPDATE.
    // Consequence: callers must not wrap these methods in their own transaction.
    // ------------------------------------------------------------------------------------

    public TransactionResponse deposit(DepositRequest request, HttpServletRequest httpRequest) {
        return deposit(request, null, httpRequest);
    }

    public TransactionResponse deposit(DepositRequest request, String idempotencyKey, HttpServletRequest httpRequest) {
        log.info("Processing deposit of {} to wallet: {}", request.getAmount(), request.getWalletId());
        String hash = IdempotencyService.hash("DEPOSIT", request.getWalletId(),
                canonical(request.getAmount()), nullToEmpty(request.getDescription()));
        return executeMoneyMovement(idempotencyService.normalize(idempotencyKey), hash, httpRequest,
                () -> doDeposit(request, httpRequest));
    }

    public TransactionResponse withdraw(WithdrawRequest request, HttpServletRequest httpRequest) {
        return withdraw(request, null, httpRequest);
    }

    public TransactionResponse withdraw(WithdrawRequest request, String idempotencyKey, HttpServletRequest httpRequest) {
        log.info("Processing withdrawal of {} from wallet: {}", request.getAmount(), request.getWalletId());
        String hash = IdempotencyService.hash("WITHDRAWAL", request.getWalletId(),
                canonical(request.getAmount()), nullToEmpty(request.getDescription()));
        return executeMoneyMovement(idempotencyService.normalize(idempotencyKey), hash, httpRequest,
                () -> doWithdraw(request, httpRequest));
    }

    public TransactionResponse transfer(TransferRequest request, HttpServletRequest httpRequest) {
        return transfer(request, null, httpRequest);
    }

    public TransactionResponse transfer(TransferRequest request, String idempotencyKey, HttpServletRequest httpRequest) {
        log.info("Processing transfer of {} from wallet {} to wallet {}",
                request.getAmount(), request.getSourceWalletId(), request.getDestinationWalletId());
        String hash = IdempotencyService.hash("TRANSFER", request.getSourceWalletId(),
                request.getDestinationWalletId(), canonical(request.getAmount()),
                nullToEmpty(request.getDescription()));
        return executeMoneyMovement(idempotencyService.normalize(idempotencyKey), hash, httpRequest,
                () -> doTransfer(request, httpRequest));
    }

    /**
     * Runs a money movement in its own database transaction and, when the client sent an
     * Idempotency-Key, makes the movement exactly-once:
     * <ul>
     *   <li>a known key + same payload returns the stored result without moving anything;</li>
     *   <li>the key is stored INSIDE the money-moving transaction, so "moved" and "remembered"
     *       commit or roll back together;</li>
     *   <li>two simultaneous requests with the same key serialize on the wallet lock, the loser
     *       then hits the unique constraint, rolls back, and returns the winner's result.</li>
     * </ul>
     */
    private TransactionResponse executeMoneyMovement(String key, String requestHash,
                                                     HttpServletRequest httpRequest,
                                                     Supplier<TransactionResponse> operation) {
        if (key != null) {
            Optional<TransactionResponse> replay = findReplay(key, requestHash);
            if (replay.isPresent()) {
                log.info("Idempotent replay for key {}: returning stored result", key);
                return replay.get();
            }
        }

        try {
            return transactionTemplate.execute(status -> {
                TransactionResponse response = operation.get();
                if (key != null) {
                    idempotencyService.remember(key, requestHash, response.getId());
                }
                return response;
            });
        } catch (RejectedOperationException rejected) {
            // A concurrent duplicate may have succeeded first and changed the balance under us:
            // the client must then see that success, not an "insufficient balance" error.
            if (key != null) {
                Optional<TransactionResponse> replay = findReplay(key, requestHash);
                if (replay.isPresent()) {
                    return replay.get();
                }
            }
            recordFailedAttempt(rejected, httpRequest);
            throw rejected.getReason();
        } catch (DataIntegrityViolationException duplicate) {
            // Lost the race on the idempotency unique constraint: our movement was rolled back.
            if (key != null) {
                Optional<TransactionResponse> replay = findReplay(key, requestHash);
                if (replay.isPresent()) {
                    return replay.get();
                }
            }
            throw duplicate;
        }
    }

    private Optional<TransactionResponse> findReplay(String key, String requestHash) {
        return transactionTemplate.execute(status ->
                idempotencyService.findCompletedTransactionId(key, requestHash)
                        .flatMap(transactionRepository::findById)
                        .map(this::convertToResponse));
    }

    private static String canonical(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private TransactionResponse doDeposit(DepositRequest request, HttpServletRequest httpRequest) {
        Wallet wallet = walletRepository.findByIdWithLock(request.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + request.getWalletId()));

        // A user may only deposit into their own wallet (admins may deposit into any wallet)
        SecurityUtil.checkOwnerOrAdmin(ownerId(wallet));

        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new RejectedOperationException(new IllegalStateException("Wallet is not active"),
                    TransactionType.DEPOSIT, request.getAmount(), request.getDescription(),
                    null, wallet.getId());
        }

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.credit(request.getAmount());
        wallet = walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .transactionId(generateTransactionId())
                .amount(request.getAmount())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription() != null ? request.getDescription() : "Deposit")
                .currency(wallet.getCurrency())
                .destinationWallet(wallet)
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .ipAddress(RequestUtil.getClientIp(httpRequest))
                .userAgent(RequestUtil.getUserAgent(httpRequest))
                .build();

        transaction = transactionRepository.save(transaction);

        auditService.logAction("DEPOSIT", "Transaction", transaction.getId(),
                null, transaction.getAmount().toString(), "Deposit completed");

        log.info("Deposit completed successfully. Transaction ID: {}", transaction.getId());

        return convertToResponse(transaction);
    }

    private TransactionResponse doTransfer(TransferRequest request, HttpServletRequest httpRequest) {
        // The client submits the destination as a human-facing wallet NUMBER
        // (e.g. "WPA9BA548D"), not the internal wallet ID/UUID - the sender has no way
        // of knowing the recipient's internal ID. Resolve it to the real wallet ID
        // up front so the locking logic below (which operates on IDs) works correctly.
        String sourceId = request.getSourceWalletId();
        String destinationId = walletRepository.findByWalletNumber(request.getDestinationWalletId())
                .map(Wallet::getId)
                .orElseThrow(() -> new WalletNotFoundException(
                        "Destination wallet not found: " + request.getDestinationWalletId()));

        if (sourceId.equals(destinationId)) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }

        Map<String, Wallet> locked = lockWallets(List.of(sourceId, destinationId));
        Wallet sourceWallet = locked.get(sourceId);
        Wallet destinationWallet = locked.get(destinationId);

        // Only the owner of the source wallet (or an admin) can move money out of it.
        // No ownership check on the destination wallet: transferring TO another person's
        // wallet is the whole point of a peer-to-peer transfer.
        // This runs BEFORE the business checks below on purpose: a rejected attempt is
        // recorded on the source wallet, so only its owner may be able to trigger that.
        SecurityUtil.checkOwnerOrAdmin(ownerId(sourceWallet));

        BigDecimal sourceBalanceBefore = sourceWallet.getBalance();

        try {
            requireActive(sourceWallet, "Source wallet is not active");
            requireActive(destinationWallet, "Destination wallet is not active");
            if (!sourceWallet.getCurrency().equals(destinationWallet.getCurrency())) {
                throw new CurrencyMismatchException(
                        "Cannot transfer between wallets with different currencies: " +
                                sourceWallet.getCurrency() + " -> " + destinationWallet.getCurrency());
            }
            sourceWallet.debit(request.getAmount());
        } catch (InsufficientBalanceException | CurrencyMismatchException | IllegalStateException e) {
            throw new RejectedOperationException(e, TransactionType.TRANSFER, request.getAmount(),
                    request.getDescription(), sourceId, destinationId);
        }

        sourceWallet = walletRepository.save(sourceWallet);

        destinationWallet.credit(request.getAmount());
        destinationWallet = walletRepository.save(destinationWallet);

        Transaction transaction = Transaction.builder()
                .transactionId(generateTransactionId())
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription() != null ? request.getDescription() : "Transfer")
                .currency(sourceWallet.getCurrency())
                .sourceWallet(sourceWallet)
                .destinationWallet(destinationWallet)
                .balanceBefore(sourceBalanceBefore)
                .balanceAfter(sourceWallet.getBalance())
                .ipAddress(RequestUtil.getClientIp(httpRequest))
                .userAgent(RequestUtil.getUserAgent(httpRequest))
                .build();

        transaction = transactionRepository.save(transaction);

        auditService.logAction("TRANSFER", "Transaction", transaction.getId(),
                null, transaction.getAmount().toString(),
                String.format("Transfer from %s to %s", sourceWallet.getId(), destinationWallet.getId()));

        log.info("Transfer completed successfully. Transaction ID: {}", transaction.getId());

        return convertToResponse(transaction);
    }

    private TransactionResponse doWithdraw(WithdrawRequest request, HttpServletRequest httpRequest) {
        Wallet wallet = walletRepository.findByIdWithLock(request.getWalletId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + request.getWalletId()));

        // Only the owner (or an admin) can take money out of a wallet.
        SecurityUtil.checkOwnerOrAdmin(ownerId(wallet));

        BigDecimal balanceBefore = wallet.getBalance();

        try {
            requireActive(wallet, "Wallet is not active");
            wallet.debit(request.getAmount());
        } catch (InsufficientBalanceException | IllegalStateException e) {
            throw new RejectedOperationException(e, TransactionType.WITHDRAWAL, request.getAmount(),
                    request.getDescription(), wallet.getId(), null);
        }

        wallet = walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .transactionId(generateTransactionId())
                .amount(request.getAmount())
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription() != null ? request.getDescription() : "Withdrawal")
                .currency(wallet.getCurrency())
                .sourceWallet(wallet)
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .ipAddress(RequestUtil.getClientIp(httpRequest))
                .userAgent(RequestUtil.getUserAgent(httpRequest))
                .build();

        transaction = transactionRepository.save(transaction);

        auditService.logAction("WITHDRAWAL", "Transaction", transaction.getId(),
                null, transaction.getAmount().toString(), "Withdrawal completed");

        log.info("Withdrawal completed successfully. Transaction ID: {}", transaction.getId());

        return convertToResponse(transaction);
    }

    /**
     * Persists a FAILED ledger entry (plus an audit entry) for an attempt rejected by a business
     * rule. Runs after the failed transaction has rolled back. Never throws: the caller is about
     * to rethrow the real business exception and that one must not be masked.
     */
    private void recordFailedAttempt(RejectedOperationException rejected, HttpServletRequest httpRequest) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Wallet source = rejected.sourceWalletId != null
                        ? walletRepository.findById(rejected.sourceWalletId).orElse(null) : null;
                Wallet destination = rejected.destinationWalletId != null
                        ? walletRepository.findById(rejected.destinationWalletId).orElse(null) : null;
                Wallet reference = source != null ? source : destination;
                if (reference == null) {
                    return;
                }

                String reason = rejected.getReason().getMessage();
                Transaction failed = transactionRepository.save(Transaction.builder()
                        .transactionId(generateTransactionId())
                        .amount(rejected.amount)
                        .type(rejected.type)
                        .status(TransactionStatus.FAILED)
                        .description(failureDescription(reason, rejected.description))
                        .currency(reference.getCurrency())
                        .sourceWallet(source)
                        .destinationWallet(destination)
                        // nothing moved: before == after
                        .balanceBefore(reference.getBalance())
                        .balanceAfter(reference.getBalance())
                        .ipAddress(RequestUtil.getClientIp(httpRequest))
                        .userAgent(RequestUtil.getUserAgent(httpRequest))
                        .build());

                auditService.logAction(rejected.type.name() + "_FAILED", "Transaction", failed.getId(),
                        null, String.format("amount=%s; reason=%s", rejected.amount, reason), reason);
            });
        } catch (RuntimeException e) {
            log.error("Could not record failed {} attempt (original failure: {})",
                    rejected.type, rejected.getReason().getMessage(), e);
        }
    }

    private static String failureDescription(String reason, String description) {
        String text = "Failed: " + reason
                + (description != null && !description.isBlank() ? " - " + description : "");
        return text.length() > 500 ? text.substring(0, 500) : text;
    }

    // ------------------------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByWallet(String walletId, Pageable pageable) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
        SecurityUtil.checkOwnerOrAdmin(ownerId(wallet));

        return transactionRepository.findByWalletId(walletId, pageable)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalanceSummary(String walletId) {
        log.info("Getting balance summary for wallet: {}", walletId);

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));

        SecurityUtil.checkOwnerOrAdmin(ownerId(wallet));

        // Aggregates are computed by the database (SUM) instead of loading every
        // transaction into memory. Only COMPLETED rows are summed, so reversed and failed
        // operations are excluded and: balance = deposits - withdrawals - out + in.
        BigDecimal totalDeposits = orZero(transactionRepository.sumDepositsByWallet(walletId));
        BigDecimal totalWithdrawals = orZero(transactionRepository.sumWithdrawalsByWallet(walletId));
        BigDecimal transfersOut = orZero(transactionRepository.sumTransfersSentByWallet(walletId));
        BigDecimal transfersIn = orZero(transactionRepository.sumTransfersReceivedByWallet(walletId));

        // One paged query yields both the 10 latest entries and the total count
        // (Spring Data issues the COUNT itself only when the page is full).
        Page<Transaction> recent = transactionRepository.findByWalletId(walletId, PageRequest.of(0, 10));

        return BalanceResponse.builder()
                .totalBalance(wallet.getBalance())
                .totalDeposits(totalDeposits)
                .totalWithdrawals(totalWithdrawals)
                .totalTransfersOut(transfersOut)
                .totalTransfersIn(transfersIn)
                .recentTransactions(recent.getContent().stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList()))
                .totalTransactions(recent.getTotalElements())
                .build();
    }

    // ------------------------------------------------------------------------------------
    // Reversal
    // ------------------------------------------------------------------------------------

    /**
     * Reverses a COMPLETED deposit / transfer / withdrawal by moving the money back.
     * <p>
     * Who may reverse:
     * <ul>
     *   <li>ADMIN: any reversible transaction;</li>
     *   <li>the SENDER (owner of the source wallet) of a transfer or withdrawal, within
     *       {@code finpay.reversal.user-window-minutes} of its creation;</li>
     *   <li>nobody else - in particular not the recipient of a transfer, and not the owner of
     *       a deposit (deposits are admin-only).</li>
     * </ul>
     * Concurrency: the transaction row and every wallet involved are locked before anything is
     * read or changed. Wallets are locked in ID order (same as transfers) so reversals and
     * transfers can never deadlock against each other, and two simultaneous reversals of the
     * same transaction are serialized - the second sees REVERSED and is rejected.
     */
    @Transactional
    public TransactionResponse reverseTransaction(String transactionId, HttpServletRequest httpRequest) {
        log.info("Reversing transaction: {}", transactionId);

        Transaction original = transactionRepository.findByIdWithLock(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId));

        // getId() on a lazy proxy does not load the wallet, so wallet state is only ever read
        // AFTER the row lock below has been taken.
        String sourceId = original.getSourceWallet() != null ? original.getSourceWallet().getId() : null;
        String destinationId = original.getDestinationWallet() != null ? original.getDestinationWallet().getId() : null;

        Map<String, Wallet> locked = lockWallets(
                Stream.of(sourceId, destinationId).filter(Objects::nonNull).toList());
        Wallet source = sourceId != null ? locked.get(sourceId) : null;
        Wallet destination = destinationId != null ? locked.get(destinationId) : null;

        authorizeReversal(original, source);

        if (original.getType() == TransactionType.REVERSAL) {
            throw new ReversalNotAllowedException("A reversal transaction cannot itself be reversed");
        }
        if (original.getStatus() == TransactionStatus.REVERSED) {
            throw new ReversalNotAllowedException("Transaction already reversed");
        }
        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw new ReversalNotAllowedException("Only completed transactions can be reversed");
        }

        Transaction reversal = switch (original.getType()) {
            case DEPOSIT -> reverseDeposit(original, destination, httpRequest);
            case TRANSFER -> reverseTransfer(original, source, destination, httpRequest);
            case WITHDRAWAL -> reverseWithdrawal(original, source, httpRequest);
            default -> throw new ReversalNotAllowedException(
                    "Transactions of type " + original.getType() + " cannot be reversed");
        };

        // Capture the status BEFORE changing it: the audit entry used to record the new status
        // as the "old" value.
        TransactionStatus previousStatus = original.getStatus();
        original.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(original);

        auditService.logAction("REVERSE_TRANSACTION", "Transaction", original.getId(),
                previousStatus.name(), TransactionStatus.REVERSED.name(),
                "Reversed by " + reversal.getTransactionId());

        return convertToResponse(reversal);
    }

    private void authorizeReversal(Transaction original, Wallet source) {
        if (SecurityUtil.isAdmin()) {
            return;
        }

        boolean isSender = source != null
                && source.getUser() != null
                && source.getUser().getId().equals(SecurityUtil.getCurrentUserId());

        if (original.getType() == TransactionType.DEPOSIT || !isSender) {
            throw new AccessDeniedException("You don't have permission to reverse this transaction");
        }

        LocalDateTime deadline = original.getCreatedAt().plusMinutes(reversalUserWindowMinutes);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new ReversalNotAllowedException(String.format(
                    "The %d-minute window to reverse this transaction has expired. Please contact an administrator.",
                    reversalUserWindowMinutes));
        }
    }

    private Transaction reverseDeposit(Transaction original, Wallet wallet, HttpServletRequest httpRequest) {
        requireNotClosed(wallet);
        requireFundsForReversal(wallet, original.getAmount(),
                "The wallet no longer holds enough balance to reverse this deposit");

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.debit(original.getAmount());
        walletRepository.save(wallet);

        return saveReversal(original, wallet, null, balanceBefore, wallet.getBalance(), httpRequest);
    }

    private Transaction reverseTransfer(Transaction original, Wallet source, Wallet destination,
                                        HttpServletRequest httpRequest) {
        requireNotClosed(source);
        requireNotClosed(destination);
        // Business rule: if the recipient already spent the money the transfer cannot be undone.
        requireFundsForReversal(destination, original.getAmount(),
                "The recipient no longer has enough balance to reverse this transfer");

        BigDecimal debitedBalanceBefore = destination.getBalance();
        destination.debit(original.getAmount());
        source.credit(original.getAmount());
        walletRepository.save(destination);
        walletRepository.save(source);

        // Money flows destination -> source, so the reversal's source is the original recipient.
        return saveReversal(original, destination, source,
                debitedBalanceBefore, destination.getBalance(), httpRequest);
    }

    private Transaction reverseWithdrawal(Transaction original, Wallet wallet, HttpServletRequest httpRequest) {
        requireNotClosed(wallet);

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.credit(original.getAmount());
        walletRepository.save(wallet);

        return saveReversal(original, null, wallet, balanceBefore, wallet.getBalance(), httpRequest);
    }

    /**
     * Writes the REVERSAL ledger entry. Same convention as transfers: source = wallet that was
     * debited, destination = wallet that was credited, and balanceBefore/After describe the
     * DEBITED wallet (or the credited one when nothing was debited, i.e. a withdrawal reversal).
     */
    private Transaction saveReversal(Transaction original, Wallet debited, Wallet credited,
                                     BigDecimal balanceBefore, BigDecimal balanceAfter,
                                     HttpServletRequest httpRequest) {
        return transactionRepository.save(Transaction.builder()
                .transactionId(generateTransactionId())
                .amount(original.getAmount())
                .type(TransactionType.REVERSAL)
                .status(TransactionStatus.COMPLETED)
                .description("Reversal of transaction: " + original.getTransactionId())
                .currency(original.getCurrency())
                .sourceWallet(debited)
                .destinationWallet(credited)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .ipAddress(RequestUtil.getClientIp(httpRequest))
                .userAgent(RequestUtil.getUserAgent(httpRequest))
                .build());
    }

    // ------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------

    /**
     * Locks the given wallets (SELECT ... FOR UPDATE) in ascending ID order. Every code path that
     * locks more than one wallet must go through here: a single global lock order is what rules
     * out deadlocks between concurrent A->B / B->A operations.
     */
    private Map<String, Wallet> lockWallets(List<String> walletIds) {
        Map<String, Wallet> locked = new LinkedHashMap<>();
        for (String id : new TreeSet<>(walletIds)) {
            locked.put(id, walletRepository.findByIdWithLock(id)
                    .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + id)));
        }
        return locked;
    }

    private static String ownerId(Wallet wallet) {
        return wallet.getUser() != null ? wallet.getUser().getId() : null;
    }

    private static void requireActive(Wallet wallet, String message) {
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new IllegalStateException(message);
        }
    }

    private static void requireNotClosed(Wallet wallet) {
        if (wallet.getStatus() == Wallet.WalletStatus.CLOSED) {
            throw new ReversalNotAllowedException("Wallet " + wallet.getWalletNumber() + " is closed");
        }
    }

    private static void requireFundsForReversal(Wallet wallet, BigDecimal amount, String message) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new ReversalNotAllowedException(message);
        }
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
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

    /**
     * Internal signal: a deposit/withdrawal/transfer was rejected by a business rule AFTER ownership was
     * verified. It rolls back the money-movement transaction and carries what is needed to
     * write the FAILED ledger entry; the public methods unwrap it and rethrow the original
     * exception, so it never reaches the exception handler.
     */
    private static final class RejectedOperationException extends RuntimeException {
        private final transient RuntimeException reason;
        private final TransactionType type;
        private final BigDecimal amount;
        private final String description;
        private final String sourceWalletId;
        private final String destinationWalletId;

        RejectedOperationException(RuntimeException reason, TransactionType type, BigDecimal amount,
                                   String description, String sourceWalletId, String destinationWalletId) {
            super(reason.getMessage(), reason, false, false);
            this.reason = reason;
            this.type = type;
            this.amount = amount;
            this.description = description;
            this.sourceWalletId = sourceWalletId;
            this.destinationWalletId = destinationWalletId;
        }

        RuntimeException getReason() {
            return reason;
        }
    }
}
