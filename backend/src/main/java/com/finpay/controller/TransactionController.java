package com.finpay.controller;

import com.finpay.dto.request.DepositRequest;
import com.finpay.dto.request.TransferRequest;
import com.finpay.dto.response.BalanceResponse;
import com.finpay.dto.response.TransactionResponse;
import com.finpay.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request,
                                                       HttpServletRequest httpRequest) {
        return ResponseEntity.ok(transactionService.deposit(request, httpRequest));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.ok(transactionService.transfer(request, httpRequest));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<Page<TransactionResponse>> getTransactionsByWallet(
            @PathVariable String walletId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionsByWallet(walletId, pageable));
    }

    @GetMapping("/wallet/{walletId}/balance")
    public ResponseEntity<BalanceResponse> getBalanceSummary(@PathVariable String walletId) {
        return ResponseEntity.ok(transactionService.getBalanceSummary(walletId));
    }

    @PostMapping("/{transactionId}/reverse")
    public ResponseEntity<TransactionResponse> reverseTransaction(
            @PathVariable String transactionId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(transactionService.reverseTransaction(transactionId, httpRequest));
    }
}