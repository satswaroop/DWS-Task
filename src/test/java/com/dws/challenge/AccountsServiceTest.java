package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

    @Autowired
    private AccountsService accountsService;

    @Mock
    private AccountsRepository accountsRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AccountsService accountsServiceObj;

    @BeforeEach
    void setUp() {
        // Reset mocks and setup default behaviors if needed
        reset(accountsRepository, notificationService);
    }

    @Test
    void addAccount() {
        Account account = new Account("Id-123");
        account.setBalance(new BigDecimal(1000));
        this.accountsService.createAccount(account);

        assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
    }

    @Test
    void addAccount_failsOnDuplicateId() {
        String uniqueId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueId);
        this.accountsService.createAccount(account);

        try {
            this.accountsService.createAccount(account);
            fail("Should have failed when adding duplicate account");
        } catch (DuplicateAccountIdException ex) {
            assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
        }
    }

    @Test
    void transferMoney_SuccessfulTransfer() {
        // Mock accounts
        Account accountFrom = new Account("Id-123", new BigDecimal("1000.00"));
        Account accountTo = new Account("Id-456", new BigDecimal("500.00"));

        when(accountsRepository.getAccount("Id-123")).thenReturn(accountFrom);
        when(accountsRepository.getAccount("Id-456")).thenReturn(accountTo);

        // Perform transfer
        accountsServiceObj.transferMoney("Id-123", "Id-456", new BigDecimal("300.00"));

        // Verify balances and notifications
        verify(accountsRepository, times(1)).getAccount("Id-123");
        verify(accountsRepository, times(1)).getAccount("Id-456");
        verify(notificationService, times(1)).notifyAboutTransfer(accountFrom, "Transferred 300.00 to account Id-456");
        verify(notificationService, times(1)).notifyAboutTransfer(accountTo, "Received 300.00 from account Id-123");

        // Assert balances after transfer
        assertThat(accountFrom.getBalance()).isEqualByComparingTo("700.00");
        assertThat(accountTo.getBalance()).isEqualByComparingTo("800.00");
    }

    @Test
    void transferMoney_InsufficientFunds() {
        // Mock accounts
        Account accountFrom = new Account("Id-123", new BigDecimal("100.00"));
        Account accountTo = new Account("Id-456", new BigDecimal("500.00"));

        when(accountsRepository.getAccount("Id-123")).thenReturn(accountFrom);
        when(accountsRepository.getAccount("Id-456")).thenReturn(accountTo);

        // Attempt transfer
        try {
            accountsServiceObj.transferMoney("Id-123", "Id-456", new BigDecimal("200.00"));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Insufficient funds in account Id-123");
        }

        // Verify no balance change
        assertThat(accountFrom.getBalance()).isEqualByComparingTo("100.00");
        assertThat(accountTo.getBalance()).isEqualByComparingTo("500.00");

        // Verify no notifications sent
        verify(notificationService, never()).notifyAboutTransfer(any(), any());
    }

    @Test
    void transferMoney_NegativeAmount() {
        // Mock accounts
        Account accountFrom = new Account("Id-123", new BigDecimal("1000.00"));
        Account accountTo = new Account("Id-456", new BigDecimal("500.00"));

        when(accountsRepository.getAccount("Id-123")).thenReturn(accountFrom);
        when(accountsRepository.getAccount("Id-456")).thenReturn(accountTo);

        // Attempt transfer with negative amount
        try {
            accountsServiceObj.transferMoney("Id-123", "Id-456", new BigDecimal("-200.00"));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Transfer amount must be positive.");
        }

        // Verify no balance change
        assertThat(accountFrom.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(accountTo.getBalance()).isEqualByComparingTo("500.00");

        // Verify no notifications sent
        verify(notificationService, never()).notifyAboutTransfer(any(), any());
    }

    @Test
    void transferMoney_ConcurrentTransfers() throws InterruptedException {
        // Mock accounts
        Account accountFrom = new Account("Id-123", new BigDecimal("1000.00"));
        Account accountTo = new Account("Id-456", new BigDecimal("500.00"));

        when(accountsRepository.getAccount("Id-123")).thenReturn(accountFrom);
        when(accountsRepository.getAccount("Id-456")).thenReturn(accountTo);

        // Create multiple threads to transfer money concurrently
        Runnable transferTask = () -> {
            accountsServiceObj.transferMoney("Id-123", "Id-456", new BigDecimal("10.00"));
        };

        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(transferTask);
            threads[i].start();
        }

        // Wait for all threads to finish
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify final balances and notifications
        assertThat(accountFrom.getBalance()).isEqualByComparingTo("900.00");
        assertThat(accountTo.getBalance()).isEqualByComparingTo("600.00");
        verify(notificationService, times(numThreads)).notifyAboutTransfer(accountFrom, "Transferred 10.00 to account Id-456");
        verify(notificationService, times(numThreads)).notifyAboutTransfer(accountTo, "Received 10.00 from account Id-123");
    }
}
