package com.phegon.phegonbank.account.services;

import com.phegon.phegonbank.account.dtos.AccountDTO;
import com.phegon.phegonbank.account.entity.Account;
import com.phegon.phegonbank.account.repo.AccountRepo;
import com.phegon.phegonbank.auth_users.entity.User;
import com.phegon.phegonbank.auth_users.services.UserService;
import com.phegon.phegonbank.enums.AccountStatus;
import com.phegon.phegonbank.enums.AccountType;
import com.phegon.phegonbank.enums.Currency;
import com.phegon.phegonbank.exceptions.BadRequestException;
import com.phegon.phegonbank.exceptions.NotFoundException;
import com.phegon.phegonbank.res.Response;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AccountServiceImpl implements AccountService {
    private final AccountRepo accountRepo;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final Random random =  new Random();


    @Override
    public Account createAccount(AccountType accountType, User user) {
        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountType(accountType)
                .currency(Currency.USD)
                .balance(BigDecimal.ZERO)
                .accountStatus(AccountStatus.ACTIVE)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        return accountRepo.save(account);
    }

    @Override
    public Response<List<AccountDTO>> getMyAccounts() {
        User user = userService.getCurrentLoggedUser();

        List<AccountDTO> accounts = accountRepo.findByUserId(user.getId())
                .stream()
                .map(account->modelMapper.map(account, AccountDTO.class))
                .toList();

        return Response.<List<AccountDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User accounts fetched successfully")
                .data(accounts)
                .build();
    }

    @Override
    public Response<?> closeAccount(String accountNumber) {
        User user = userService.getCurrentLoggedUser();
        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(()->new NotFoundException("Account not found"));

        if(!user.getAccounts().contains(account)) {
            throw new NotFoundException("Account does not belong to you");
        }

        if(account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Account balance must be 0 before closing");
        }
        account.setAccountStatus(AccountStatus.CLOSED);
        account.setClosedAt(LocalDateTime.now());
        accountRepo.save(account);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Account closed successfully")
                .build();

    }
    private String generateAccountNumber() {
        String accountNumber;

        do{
            accountNumber = "66" + (random.nextInt(90000000) + 10000000);
        } while (accountRepo.findByAccountNumber(accountNumber).isPresent());
        log.info("Generated account number: {}", accountRepo);//track possible failure
        return accountNumber;
    }

}




