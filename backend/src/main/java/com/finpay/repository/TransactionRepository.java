package com.finpay.repository;

import com.finpay.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /**
     * Row lock on a transaction. Used by reversals so two concurrent reversals of the same
     * transaction are serialized (the second one then sees status REVERSED).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdWithLock(@Param("id") String id);

    /**
     * History of a wallet. FAILED attempts are only visible to the wallet that initiated
     * them (the source side): the recipient of a transfer that never happened must not see
     * it, nor the reason it was rejected.
     * (source, created_at) and (destination, created_at) are covered by V6 / V8.
     */
    @Query("SELECT t FROM Transaction t WHERE t.sourceWallet.id = :walletId " +
            "OR (t.destinationWallet.id = :walletId AND t.status <> 'FAILED') " +
            "ORDER BY t.createdAt DESC, t.id DESC")
    Page<Transaction> findByWalletId(@Param("walletId") String walletId, Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.sourceWallet.id = :walletId AND t.type = 'WITHDRAWAL' AND t.status = 'COMPLETED'")
    BigDecimal sumWithdrawalsByWallet(@Param("walletId") String walletId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.destinationWallet.id = :walletId AND t.type = 'DEPOSIT' AND t.status = 'COMPLETED'")
    BigDecimal sumDepositsByWallet(@Param("walletId") String walletId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.sourceWallet.id = :walletId AND t.type = 'TRANSFER' AND t.status = 'COMPLETED'")
    BigDecimal sumTransfersSentByWallet(@Param("walletId") String walletId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.destinationWallet.id = :walletId AND t.type = 'TRANSFER' AND t.status = 'COMPLETED'")
    BigDecimal sumTransfersReceivedByWallet(@Param("walletId") String walletId);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :start AND :end")
    List<Transaction> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
