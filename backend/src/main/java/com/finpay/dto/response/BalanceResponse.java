package com.finpay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {
    private BigDecimal totalBalance;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    /** Sum of completed transfers sent FROM this wallet (money out). */
    private BigDecimal totalTransfersOut;
    /** Sum of completed transfers received BY this wallet (money in). */
    private BigDecimal totalTransfersIn;
    private List<TransactionResponse> recentTransactions;
    private long totalTransactions;
}
