package com.finpay.support;

import com.finpay.entity.User;
import com.finpay.entity.Wallet;
import com.finpay.repository.UserRepository;
import com.finpay.repository.WalletRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;

/** Helpers to create accounts and to run service calls "as" a given user. */
public class TestFixtures {

    public record Account(User user, Wallet wallet) {
    }

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public TestFixtures(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    public Account createAccount(String initialBalance) {
        return createAccount(new BigDecimal(initialBalance), User.UserRole.USER);
    }

    public Account createAdmin() {
        return createAccount(BigDecimal.ZERO, User.UserRole.ADMIN);
    }

    public Account createAccount(BigDecimal initialBalance, User.UserRole role) {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        User user = userRepository.save(User.builder()
                .email("it-" + suffix + "@example.com")
                .password("not-used-in-these-tests")
                .fullName("Integration Test User")
                .role(role)
                .build());
        Wallet wallet = walletRepository.save(Wallet.builder()
                .walletNumber("WT" + suffix.substring(0, 8).toUpperCase())
                .balance(initialBalance)
                .currency("BRL")
                .status(Wallet.WalletStatus.ACTIVE)
                .user(user)
                .build());
        return new Account(user, wallet);
    }

    public BigDecimal balanceOf(Account account) {
        return walletRepository.findById(account.wallet().getId()).orElseThrow().getBalance();
    }

    /** Runs the action with {@code user} as the authenticated principal of the current thread. */
    public <T> T callAs(User user, Callable<T> action) throws Exception {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(context);
        try {
            return action.call();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "integration-test");
        return request;
    }
}
