package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        // Reset the existing accounts before each test.
        accountsService.getAccountsRepository().clearAccounts();
    }

    @Test
    void createAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        Account account = accountsService.getAccount("Id-123");
        assertThat(account.getAccountId()).isEqualTo("Id-123");
        assertThat(account.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void createDuplicateAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoBody() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNegativeBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountEmptyAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void getAccount() throws Exception {
        String uniqueAccountId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
        this.accountsService.createAccount(account);
        this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
                .andExpect(status().isOk())
                .andExpect(
                        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
    }

    @Test
    void transferMoney_SuccessfulTransfer() throws Exception {
        // Create accounts
        Account account1 = new Account("Id-123", new BigDecimal("1000.00"));
        Account account2 = new Account("Id-456", new BigDecimal("500.00"));
        accountsService.createAccount(account1);
        accountsService.createAccount(account2);

        // Perform transfer request
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-456\",\"amount\":300.00}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify balances after transfer
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/accounts/Id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("700.0"));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/accounts/Id-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("800.0"));
    }

    @Test
    void transferMoney_InsufficientFunds() throws Exception {
        // Create accounts
        Account account1 = new Account("Id-123", new BigDecimal("100.00"));
        Account account2 = new Account("Id-456", new BigDecimal("500.00"));
        accountsService.createAccount(account1);
        accountsService.createAccount(account2);

        // Perform transfer request
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-456\",\"amount\":200.00}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Insufficient funds")));

        // Verify balances after failed transfer
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/accounts/Id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("100.0"));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/accounts/Id-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("500.0"));
    }

    @Test
    void transferMoney_InvalidAmountFormat() throws Exception {
        // Perform transfer request with invalid amount format
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-456\",\"amount\":\"testing\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid transfer amount.")));
    }

    @Test
    void transferMoney_NegativeAmount() throws Exception {
        // Perform transfer request with negative amount
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-456\",\"amount\":-100.00}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Transfer amount must be positive.")));
    }

    @Test
    void transferMoney_AccountNotFound() throws Exception {
        // Perform transfer request with non-existent account
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-456\",\"amount\":100.00}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Both accounts must exist.")));
    }

    @Test
    void transferMoney_EmptyRequestBody() throws Exception {
        // Perform transfer request with empty request body
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferMoney_AccountFromEmpty() throws Exception {
        // Perform transfer request with missing accountFromId
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountToId\":\"Id-456\",\"amount\":100.00}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("AccountFromId cannot be empty or null.")));
    }

    @Test
    void transferMoney_AccountToEmpty() throws Exception {
        // Perform transfer request with missing accountToId
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\":\"Id-123\",\"amount\":100.00}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("AccountToId cannot be empty or null.")));
    }

    @Test
    void transferMoney_AmountEmpty() throws Exception {
        // Perform transfer request with missing amount
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-456\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid transfer amount.")));
    }

    @Test
    void transferMoney_ConcurrentTransfers() throws Exception {
        // Create accounts
        Account account1 = new Account("Id-123", new BigDecimal("1000.00"));
        Account account2 = new Account("Id-456", new BigDecimal("500.00"));
        accountsService.createAccount(account1);
        accountsService.createAccount(account2);

        // Create multiple concurrent transfer requests
        Runnable transferTask = () -> {
            try {
                mockMvc.perform(MockMvcRequestBuilders.post("/v1/accounts/transfer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-456\",\"amount\":10.00}")
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        // Verify final balances after concurrent transfers
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/accounts/Id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("900.0"));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/accounts/Id-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("600.0"));
    }
}
