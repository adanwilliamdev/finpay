package com.finpay.config;

import com.finpay.entity.Transaction;
import com.finpay.entity.User;
import com.finpay.entity.Wallet;
import com.finpay.repository.TransactionRepository;
import com.finpay.repository.UserRepository;
import com.finpay.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Creates the demo accounts advertised in the README (admin@finpay.com / user@finpay.com).
 * <p>
 * Deliberately a runtime seeder instead of a Flyway migration: a migration runs in EVERY
 * environment, so production would get an admin account with a publicly documented password.
 * This bean only exists when {@code finpay.seed.demo-users=true} (env var SEED_DEMO_USERS),
 * which is enabled by the {@code dev} profile and by docker-compose, and is off by default.
 * Idempotent: accounts that already exist are left untouched.
 */
@Component
@ConditionalOnProperty(name = "finpay.seed.demo-users", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@finpay.com";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String USER_EMAIL = "user@finpay.com";
    private static final String USER_PASSWORD = "User@123";
    private static final BigDecimal USER_INITIAL_BALANCE = new BigDecimal("1000.00");

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.warn("Demo data seeding is ENABLED (finpay.seed.demo-users=true). Never enable it in production.");

        createUserIfMissing(ADMIN_EMAIL, ADMIN_PASSWORD, "FinPay Admin", "00000000191",
                User.UserRole.ADMIN, "WPADMIN01", BigDecimal.ZERO);
        createUserIfMissing(USER_EMAIL, USER_PASSWORD, "Demo User", "52998224725",
                User.UserRole.USER, "WPUSER001", USER_INITIAL_BALANCE);
    }

    private void createUserIfMissing(String email, String rawPassword, String fullName, String document,
                                     User.UserRole role, String walletNumber, BigDecimal initialBalance) {
        if (userRepository.existsByEmail(email) || userRepository.existsByDocumentNumber(document)) {
            log.info("Demo user {} already exists - skipping", email);
            return;
        }

        // NOTE: the entities use Lombok @Builder without @Builder.Default, so every field
        // must be set explicitly - field initializers (role = USER, balance = ZERO, ...) are
        // ignored by the builder.
        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .documentNumber(document)
                .role(role)
                .enabled(true)
                .accountNonLocked(true)
                .build());

        Wallet wallet = walletRepository.save(Wallet.builder()
                .walletNumber(walletNumber)
                .balance(initialBalance)
                .currency("BRL")
                .status(Wallet.WalletStatus.ACTIVE)
                .user(user)
                .build());

        if (initialBalance.signum() > 0) {
            // Keep the ledger consistent with the wallet: the opening balance is a real deposit.
            transactionRepository.save(Transaction.builder()
                    .transactionId("TXN-SEED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .amount(initialBalance)
                    .type(Transaction.TransactionType.DEPOSIT)
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .description("Demo opening balance")
                    .currency("BRL")
                    .destinationWallet(wallet)
                    .balanceBefore(BigDecimal.ZERO)
                    .balanceAfter(initialBalance)
                    .ipAddress("127.0.0.1")
                    .userAgent("demo-seeder")
                    .build());
        }

        log.info("Created demo user {} ({}), wallet {}", email, role, walletNumber);
    }
}
