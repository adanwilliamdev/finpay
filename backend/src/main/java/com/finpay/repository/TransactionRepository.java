package com.finpay.repository;

import com.finpay.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Optional<Transaction> findByTransactionId(String transactionId);

    @Query("SELECT t FROM Transaction t WHERE t.sourceWallet.id = :walletId OR t.destinationWallet.id = :walletId ORDER BY t.createdAt DESC")
    Page<Transaction> findByWalletId(@Param("walletId") String walletId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.sourceWallet.id = :walletId AND t.type = 'TRANSFER'")
    List<Transaction> findTransfersBySourceWallet(@Param("walletId") String walletId);

    @Query("SELECT t FROM Transaction t WHERE t.destinationWallet.id = :walletId AND t.type = 'TRANSFER'")
    List<Transaction> findTransfersByDestinationWallet(@Param("walletId") String walletId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.sourceWallet.id = :walletId AND t.type = 'WITHDRAWAL' AND t.status = 'COMPLETED'")
    BigDecimal sumWithdrawalsByWallet(@Param("walletId") String walletId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.destinationWallet.id = :walletId AND t.type = 'DEPOSIT' AND t.status = 'COMPLETED'")
    BigDecimal sumDepositsByWallet(@Param("walletId") String walletId);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :start AND :end")
    List<Transaction> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}