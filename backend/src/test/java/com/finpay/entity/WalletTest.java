package com.finpay.entity;

import com.finpay.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTest {

    private Wallet walletWith(String balance) {
        return Wallet.builder().walletNumber("WT00000001").balance(new BigDecimal(balance)).build();
    }

    @Test
    void creditIncreasesBalance() {
        Wallet wallet = walletWith("10.00");
        wallet.credit(new BigDecimal("5.25"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("15.25");
    }

    @Test
    void debitDecreasesBalance() {
        Wallet wallet = walletWith("10.00");
        wallet.debit(new BigDecimal("4.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("6.00");
    }

    @Test
    void debitCanEmptyTheWalletExactly() {
        Wallet wallet = walletWith("10.00");
        wallet.debit(new BigDecimal("10.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void debitBeyondBalanceIsRejectedAndLeavesBalanceUntouched() {
        Wallet wallet = walletWith("10.00");
        assertThatThrownBy(() -> wallet.debit(new BigDecimal("10.01")))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(wallet.getBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void nonPositiveAmountsAreRejected() {
        Wallet wallet = walletWith("10.00");
        assertThatThrownBy(() -> wallet.credit(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.credit(new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.debit(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.debit(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void builderUsesFieldDefaults() {
        Wallet wallet = Wallet.builder().walletNumber("WT00000002").build();
        assertThat(wallet.getBalance()).isEqualByComparingTo("0");
        assertThat(wallet.getCurrency()).isEqualTo("BRL");
        assertThat(wallet.getStatus()).isEqualTo(Wallet.WalletStatus.ACTIVE);
    }

    @Test
    void userAndWalletCanReferenceEachOtherWithoutRecursingInEqualsOrHashCode() {
        User user = User.builder().id("u1").email("a@b.com").fullName("A B").password("x").build();
        Wallet wallet = Wallet.builder().id("w1").walletNumber("WT00000003").user(user).build();
        user.setWallet(wallet);

        // With @Data on both sides these calls overflowed the stack.
        assertThat(wallet.hashCode()).isEqualTo(wallet.hashCode());
        assertThat(user.hashCode()).isEqualTo(user.hashCode());
        assertThat(wallet).isEqualTo(Wallet.builder().id("w1").walletNumber("OTHER").build());
        assertThat(wallet).isNotEqualTo(Wallet.builder().id("w2").walletNumber("WT00000003").build());
    }
}
