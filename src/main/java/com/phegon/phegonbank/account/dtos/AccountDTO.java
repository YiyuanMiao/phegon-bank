package com.phegon.phegonbank.account.dtos;

import com.fasterxml.jackson.annotation.*;
import com.phegon.phegonbank.auth_users.dtos.UserDTO;
import com.phegon.phegonbank.auth_users.entity.User;
import com.phegon.phegonbank.enums.AccountStatus;
import com.phegon.phegonbank.enums.AccountType;
import com.phegon.phegonbank.enums.Currency;
import com.phegon.phegonbank.transaction.dtos.TransactionDTO;
import jakarta.persistence.*;
import jakarta.transaction.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTO {

    private Long id;

    private String accountNumber;

    private BigDecimal balance;

    private AccountType accountType;

    @JsonBackReference("user-accounts")
    //@JsonIgnore
    private UserDTO user;

    private Currency currency;

    private AccountStatus accountStatus;

    @JsonManagedReference("account-transactions")
    private List<TransactionDTO> transactions;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
