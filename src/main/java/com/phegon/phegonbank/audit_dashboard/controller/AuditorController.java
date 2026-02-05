package com.phegon.phegonbank.audit_dashboard.controller;

import com.phegon.phegonbank.account.dtos.AccountDTO;
import com.phegon.phegonbank.audit_dashboard.services.AuditorService;
import com.phegon.phegonbank.auth_users.dtos.UserDTO;
import com.phegon.phegonbank.transaction.dtos.TransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ADMIN') or hasAuthority('AUDITOR')")
public class AuditorController {
    private final AuditorService auditorService;

    @GetMapping("/totals")
    public ResponseEntity<Map<String, Long>> getSystemTotals() {
        return  ResponseEntity.ok(auditorService.getSystemTotals());
    }

    @GetMapping("/users")
    public ResponseEntity<UserDTO> findUserByEmail(@RequestParam String email) {
        Optional<UserDTO> userDTO = auditorService.findUserByEmail(email);
        return userDTO.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('AUDITOR') or hasAuthority('CUSTOMER')")
    @GetMapping("/accounts")
    public ResponseEntity<AccountDTO> findAccountDetailsByAccountNumber(@RequestParam String accountNumber) {
        Optional<AccountDTO> accountDTO = auditorService.findAccountDetailsByAccountNumber(accountNumber);
        return accountDTO.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/transactions/by-account")
    public ResponseEntity<List<TransactionDTO>> findTransactionsByAccountNumber(@RequestParam String accountNumber) {
        List<TransactionDTO> transactionDTOs = auditorService.findTransactionsByAccountNumber(accountNumber);
        if (transactionDTOs.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(transactionDTOs);
    }

    @GetMapping("/transactions/by-id")
    public ResponseEntity<TransactionDTO> findTransactionById(@RequestParam Long transactionId) {
        Optional<TransactionDTO> transactionDTO = auditorService.findTransactionById(transactionId);

        return transactionDTO.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());

    }



}
