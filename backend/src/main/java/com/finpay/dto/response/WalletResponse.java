package com.finpay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private String id;
    private String walletNumber;
    private BigDecimal balance;
    private String currency;
    private String status;
    private String userId;
    private String userFullName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}