package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  @Getter
  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void transferMoney(String accountFromId, String accountToId, BigDecimal amount) {

    // Check for non-positive amount
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Transfer amount must be positive.");
    }

    Account accountFrom = accountsRepository.getAccount(accountFromId);
    Account accountTo = accountsRepository.getAccount(accountToId);

    if (accountFrom == null || accountTo == null) {
      throw new IllegalArgumentException("Both accounts must exist.");
    }

    // Lock ordering to prevent deadlock
    Object lock1 = accountFromId.compareTo(accountToId) < 0 ? accountFrom : accountTo;
    Object lock2 = accountFromId.compareTo(accountToId) < 0 ? accountTo : accountFrom;

    synchronized (lock1) {
      synchronized (lock2) {
        if (accountFrom.getBalance().compareTo(amount) < 0) {
          throw new IllegalArgumentException("Insufficient funds in account " + accountFromId);
        }

        // Perform the transfer
        accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
        accountTo.setBalance(accountTo.getBalance().add(amount));

        // Notify about the transfer
        notificationService.notifyAboutTransfer(accountFrom, "Transferred " + amount + " to account " + accountToId);
        notificationService.notifyAboutTransfer(accountTo, "Received " + amount + " from account " + accountFromId);
      }
    }
  }

}
