package com.phegon.phegonbank.transaction.dtos;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.phegon.phegonbank.account.dtos.AccountDTO;
import com.phegon.phegonbank.account.entity.Account;
import com.phegon.phegonbank.enums.TransactionStatus;
import com.phegon.phegonbank.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
//encountering profilePictureUrl ,which is empty, leave it empty instead of making it null
@JsonIgnoreProperties(ignoreUnknown = true)
//when using UserDTO as an object to receive a new user, it'll omit fields which do not have value
public class TransactionDTO {

    private Long id;

    private BigDecimal amount;

    private TransactionType transactionType;

    private LocalDateTime transactionDate = LocalDateTime.now();

    private String description;

    private TransactionStatus transactionStatus;

    @JsonBackReference
    private AccountDTO account;

    // for transfer
    private String sourceAccount;
    private String destinationAccount;


}
