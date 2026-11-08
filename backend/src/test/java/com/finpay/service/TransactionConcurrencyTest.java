package com.finpay.service;

import com.finpay.dto.request.DepositRequest;
import com.finpay.dto.request.TransferRequest;
import com.finpay.dto.request.WithdrawRequest;
import com.finpay.dto.response.TransactionResponse;
import com.finpay.entity.Transaction.TransactionStatus;
import com.finpay.entity.Transaction.TransactionType;
import com.finpay.exception.IdempotencyConflictException;
import com.finpay.exception.InsufficientBalanceException;
import com.finpay.exception.ReversalNotAllowedException;
import com.finpay.repository.TransactionRepository;
import com.finpay.repository.UserRepository;
import com.finpay.repository.WalletRepository;
import com.finpay.support.AbstractPostgresIntegrationTest;
import com.finpay.support.ConcurrencyHelper;
import com.finpay.support.ConcurrencyHelper.Result;
import com.finpay.support.TestFixtures;
import com.finpay.support.TestFixtures.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the integrity guarantees the README advertises, against a real PostgreSQL:
 * money is conserved under contention, balances never go negative, concurrent opposite
 * transfers do not deadlock, a retry with the same Idempotency-Key moves money once, and a
 * transaction can only be reversed once.
 * <p>
 * Intentionally NOT @Transactional: the service must commit on its own, like in production.
 */
class TransactionConcurrencyTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;

    private TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures(userRepository, walletRepository);
    }

    // ------------------------------------------------------------------------------------
    // Concurrency
    // ------------------------------------------------------------------------------------

    @Test
    void oppositeConcurrentTransfersConserveMoneyAndDoNotDeadlock() throws Exception {
        Account a = fixtures.createAccount("10000.00");
        Account b = fixtures.createAccount("10000.00");

        List<Callable<TransactionResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> fixtures.callAs(a.user(), () -> transactionService.transfer(transfer(a, b, "10.00"), fixtures.request())));
            tasks.add(() -> fixtures.callAs(b.user(), () -> transactionService.transfer(transfer(b, a, "7.00"), fixtures.request())));
        }

        List<Result<TransactionResponse>> results = ConcurrencyHelper.runConcurrently(tasks);

        assertThat(results).allSatisfy(r -> assertThat(r.error()).isNull());
        // a: 10000 - 20*10 + 20*7 = 9940   b: 10000 + 20*10 - 20*7 = 10060
        assertThat(fixtures.balanceOf(a)).isEqualByComparingTo("9940.00");
        assertThat(fixtures.balanceOf(b)).isEqualByComparingTo("10060.00");
        assertThat(fixtures.balanceOf(a).add(fixtures.balanceOf(b))).isEqualByComparingTo("20000.00");
    }

    @Test
    void concurrentOverdraftAttemptsNeverMakeTheBalanceNegative() throws Exception {
        Account source = fixtures.createAccount("100.00");
        Account target = fixtures.createAccount("0.00");

        List<Callable<TransactionResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            tasks.add(() -> fixtures.callAs(source.user(), () -> transactionService.transfer(transfer(source, target, "10.00"), fixtures.request())));
        }

        List<Result<TransactionResponse>> results = ConcurrencyHelper.runConcurrently(tasks);

        long succeeded = results.stream().filter(Result::succeeded).count();
        long rejected = results.stream()
                .filter(r -> r.error() instanceof InsufficientBalanceException).count();
        assertThat(succeeded).isEqualTo(10);
        assertThat(rejected).isEqualTo(5);

        assertThat(fixtures.balanceOf(source)).isEqualByComparingTo("0.00");
        assertThat(fixtures.balanceOf(target)).isEqualByComparingTo("100.00");

        // The rejected attempts are recorded in the ledger as FAILED, without moving money.
        List<com.finpay.entity.Transaction> history = transactionRepository
                .findByWalletId(source.wallet().getId(), PageRequest.of(0, 100)).getContent();
        assertThat(history).filteredOn(t -> t.getStatus() == TransactionStatus.COMPLETED).hasSize(10);
        assertThat(history).filteredOn(t -> t.getStatus() == TransactionStatus.FAILED).hasSize(5);
    }

    // ------------------------------------------------------------------------------------
    // Idempotency
    // ------------------------------------------------------------------------------------

    @Test
    void retryWithSameIdempotencyKeyMovesMoneyOnlyOnce() throws Exception {
        Account a = fixtures.createAccount("500.00");
        Account b = fixtures.createAccount("0.00");
        String key = UUID.randomUUID().toString();

        TransactionResponse first = fixtures.callAs(a.user(),
                () -> transactionService.transfer(transfer(a, b, "10.00"), key, fixtures.request()));
        TransactionResponse retry = fixtures.callAs(a.user(),
                () -> transactionService.transfer(transfer(a, b, "10.00"), key, fixtures.request()));

        assertThat(retry.getId()).isEqualTo(first.getId());
        assertThat(fixtures.balanceOf(a)).isEqualByComparingTo("490.00");
        assertThat(fixtures.balanceOf(b)).isEqualByComparingTo("10.00");
    }

    @Test
    void sameIdempotencyKeyWithDifferentPayloadIsRejected() throws Exception {
        Account a = fixtures.createAccount("500.00");
        Account b = fixtures.createAccount("0.00");
        String key = UUID.randomUUID().toString();

        fixtures.callAs(a.user(),
                () -> transactionService.transfer(transfer(a, b, "10.00"), key, fixtures.request()));

        assertThatThrownBy(() -> fixtures.callAs(a.user(),
                () -> transactionService.transfer(transfer(a, b, "99.00"), key, fixtures.request())))
                .isInstanceOf(IdempotencyConflictException.class);

        assertThat(fixtures.balanceOf(a)).isEqualByComparingTo("490.00");
    }

    @Test
    void simultaneousRequestsWithSameIdempotencyKeyMoveMoneyOnlyOnce() throws Exception {
        Account a = fixtures.createAccount("500.00");
        Account b = fixtures.createAccount("0.00");
        String key = UUID.randomUUID().toString();

        List<Callable<TransactionResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> fixtures.callAs(a.user(),
                    () -> transactionService.transfer(transfer(a, b, "10.00"), key, fixtures.request())));
        }

        List<Result<TransactionResponse>> results = ConcurrencyHelper.runConcurrently(tasks);

        assertThat(results).allSatisfy(r -> assertThat(r.error()).isNull());
        assertThat(results.stream().map(r -> r.value().getId()).distinct()).hasSize(1);
        assertThat(fixtures.balanceOf(a)).isEqualByComparingTo("490.00");
        assertThat(fixtures.balanceOf(b)).isEqualByComparingTo("10.00");
    }

    @Test
    void sameKeyUsedByDifferentUsersDoesNotCollide() throws Exception {
        Account a = fixtures.createAccount("100.00");
        Account c = fixtures.createAccount("100.00");
        Account sink = fixtures.createAccount("0.00");
        String key = "shared-key-" + UUID.randomUUID();

        TransactionResponse fromA = fixtures.callAs(a.user(),
                () -> transactionService.transfer(transfer(a, sink, "10.00"), key, fixtures.request()));
        TransactionResponse fromC = fixtures.callAs(c.user(),
                () -> transactionService.transfer(transfer(c, sink, "10.00"), key, fixtures.request()));

        assertThat(fromA.getId()).isNotEqualTo(fromC.getId());
        assertThat(fixtures.balanceOf(sink)).isEqualByComparingTo("20.00");
    }

    // ------------------------------------------------------------------------------------
    // Reversal
    // ------------------------------------------------------------------------------------

    @Test
    void concurrentReversalsOfTheSameTransferSucceedExactlyOnce() throws Exception {
        Account a = fixtures.createAccount("500.00");
        Account b = fixtures.createAccount("0.00");
        TransactionResponse transfer = fixtures.callAs(a.user(),
                () -> transactionService.transfer(transfer(a, b, "50.00"), fixtures.request()));

        List<Callable<TransactionResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tasks.add(() -> fixtures.callAs(a.user(),
                    () -> transactionService.reverseTransaction(transfer.getId(), fixtures.request())));
        }

        List<Result<TransactionResponse>> results = ConcurrencyHelper.runConcurrently(tasks);

        assertThat(results.stream().filter(Result::succeeded).count()).isEqualTo(1);
        assertThat(results.stream().filter(r -> r.error() instanceof ReversalNotAllowedException).count()).isEqualTo(5);
        assertThat(fixtures.balanceOf(a)).isEqualByComparingTo("500.00");
        assertThat(fixtures.balanceOf(b)).isEqualByComparingTo("0.00");
    }

    @Test
    void transferCannotBeReversedIfRecipientAlreadySpentTheMoney() throws Exception {
        Account a = fixtures.createAccount("100.00");
        Account b = fixtures.createAccount("0.00");
        Account c = fixtures.createAccount("0.00");
        TransactionResponse transfer = fixtures.callAs(a.user(),
                () -> transactionService.transfer(transfer(a, b, "40.00"), fixtures.request()));
        fixtures.callAs(b.user(), () -> transactionService.transfer(transfer(b, c, "30.00"), fixtures.request()));

        assertThatThrownBy(() -> fixtures.callAs(a.user(),
                () -> transactionService.reverseTransaction(transfer.getId(), fixtures.request())))
                .isInstanceOf(ReversalNotAllowedException.class);

        assertThat(fixtures.balanceOf(a)).isEqualByComparingTo("60.00");
        assertThat(fixtures.balanceOf(b)).isEqualByComparingTo("10.00");
    }

    @Test
    void recipientCannotReverseAnIncomingTransfer() throws Exception {
        Account a = fixtures.createAccount("100.00");
        Account b = fixtures.createAccount("0.00");
        TransactionResponse transfer = fixtures.callAs(a.user(),
                () -> transactionService.transfer(transfer(a, b, "40.00"), fixtures.request()));

        assertThatThrownBy(() -> fixtures.callAs(b.user(),
                () -> transactionService.reverseTransaction(transfer.getId(), fixtures.request())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void onlyAdminsCanReverseDeposits() throws Exception {
        Account owner = fixtures.createAccount("0.00");
        Account admin = fixtures.createAdmin();
        TransactionResponse deposit = fixtures.callAs(owner.user(),
                () -> transactionService.deposit(deposit(owner, "80.00"), fixtures.request()));

        assertThatThrownBy(() -> fixtures.callAs(owner.user(),
                () -> transactionService.reverseTransaction(deposit.getId(), fixtures.request())))
                .isInstanceOf(AccessDeniedException.class);

        fixtures.callAs(admin.user(),
                () -> transactionService.reverseTransaction(deposit.getId(), fixtures.request()));
        assertThat(fixtures.balanceOf(owner)).isEqualByComparingTo("0.00");
    }

    // ------------------------------------------------------------------------------------
    // Withdrawal / authorization
    // ------------------------------------------------------------------------------------

    @Test
    void withdrawalDebitsTheWalletAndCanBeReversedByItsOwner() throws Exception {
        Account a = fixtures.createAccount("100.00");

        TransactionResponse withdrawal = fixtures.callAs(a.user(),
                () -> transactionService.withdraw(withdraw(a, "30.00"), fixtures.request()));

        assertThat(withdrawal.getType()).isEqualTo(TransactionType.WITHDRAWAL.name());
        assertThat(withdrawal.getBalanceBefore()).isEqualByComparingTo("100.00");
        assertThat(withdrawal.getBalanceAfter()).isEqualByComparingTo("70.00");
        assertThat(fixtures.balanceOf(a)).isEqualByComparingTo("70.00");

        fixtures.callAs(a.user(),
                () -> transactionService.reverseTransaction(withdrawal.getId(), fixtures.request()));
        assertThat(fixtures.balanceOf(a)).isEqualByComparingTo("100.00");
    }

    @Test
    void withdrawalBeyondBalanceIsRejectedAndRecordedAsFailed() throws Exception {
        Account a = fixtures.createAccount("20.00");

        assertThatThrownBy(() -> fixtures.callAs(a.user(),
                () -> transactionService.withdraw(withdraw(a, "20.01"), fixtures.request())))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(fixtures.balanceOf(a)).isEqualByComparingTo("20.00");
        List<com.finpay.entity.Transaction> history = transactionRepository
                .findByWalletId(a.wallet().getId(), PageRequest.of(0, 10)).getContent();
        assertThat(history).singleElement().satisfies(t -> {
            assertThat(t.getType()).isEqualTo(TransactionType.WITHDRAWAL);
            assertThat(t.getStatus()).isEqualTo(TransactionStatus.FAILED);
        });
    }

    @Test
    void userCannotMoveMoneyOutOfAnotherUsersWallet() throws Exception {
        Account victim = fixtures.createAccount("100.00");
        Account attacker = fixtures.createAccount("0.00");

        assertThatThrownBy(() -> fixtures.callAs(attacker.user(),
                () -> transactionService.transfer(transfer(victim, attacker, "50.00"), fixtures.request())))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> fixtures.callAs(attacker.user(),
                () -> transactionService.withdraw(withdraw(victim, "50.00"), fixtures.request())))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(fixtures.balanceOf(victim)).isEqualByComparingTo("100.00");
        assertThat(fixtures.balanceOf(attacker)).isEqualByComparingTo("0.00");
    }

    // ------------------------------------------------------------------------------------
    // Request builders. The transfer destination is the wallet NUMBER (what a sender knows).
    // ------------------------------------------------------------------------------------

    private static TransferRequest transfer(Account from, Account to, String amount) {
        return TransferRequest.builder()
                .sourceWalletId(from.wallet().getId())
                .destinationWalletId(to.wallet().getWalletNumber())
                .amount(new BigDecimal(amount))
                .description("integration test")
                .build();
    }

    private static DepositRequest deposit(Account to, String amount) {
        return DepositRequest.builder()
                .walletId(to.wallet().getId())
                .amount(new BigDecimal(amount))
                .description("integration test")
                .build();
    }

    private static WithdrawRequest withdraw(Account from, String amount) {
        return WithdrawRequest.builder()
                .walletId(from.wallet().getId())
                .amount(new BigDecimal(amount))
                .description("integration test")
                .build();
    }
}
