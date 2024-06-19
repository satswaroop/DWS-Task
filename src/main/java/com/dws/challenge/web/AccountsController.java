package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.service.AccountsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;

  @Autowired
  public AccountsController(AccountsService accountsService) {
    this.accountsService = accountsService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
    this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }

  @PostMapping(path = "/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> transferMoney(@RequestBody Map<String, Object> transferDetails) {
    // Validate each parameter
    String accountFromId = (String) transferDetails.get("accountFromId");
    String accountToId = (String) transferDetails.get("accountToId");
    BigDecimal amount;

    // Check for empty accountFromId
    if (accountFromId == null || accountFromId.trim().isEmpty()) {
      return new ResponseEntity<>("AccountFromId cannot be empty or null.", HttpStatus.BAD_REQUEST);
    }

    // Check for empty accountToId
    if (accountToId == null || accountToId.trim().isEmpty()) {
      return new ResponseEntity<>("AccountToId cannot be empty or null.", HttpStatus.BAD_REQUEST);
    }

    // Check for empty amount
    try {
      amount = new BigDecimal(transferDetails.get("amount").toString());
    } catch (NumberFormatException | NullPointerException e) {
      return new ResponseEntity<>("Invalid transfer amount.", HttpStatus.BAD_REQUEST);
    }

    // Check for non-positive amount
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return new ResponseEntity<>("Transfer amount must be positive.", HttpStatus.BAD_REQUEST);
    }

    // If all validations pass, proceed with the transfer
    log.info("Transferring {} from account {} to account {}", amount, accountFromId, accountToId);

    try {
      this.accountsService.transferMoney(accountFromId, accountToId, amount);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.OK);
  }

}
