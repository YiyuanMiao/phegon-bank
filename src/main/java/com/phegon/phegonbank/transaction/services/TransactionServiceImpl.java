package com.phegon.phegonbank.transaction.services;

import com.phegon.phegonbank.account.entity.Account;
import com.phegon.phegonbank.account.repo.AccountRepo;
import com.phegon.phegonbank.auth_users.entity.User;
import com.phegon.phegonbank.auth_users.services.UserService;
import com.phegon.phegonbank.enums.TransactionStatus;
import com.phegon.phegonbank.enums.TransactionType;
import com.phegon.phegonbank.exceptions.BadRequestException;
import com.phegon.phegonbank.exceptions.InsufficientBalanceException;
import com.phegon.phegonbank.exceptions.InvalidTransactionException;
import com.phegon.phegonbank.exceptions.NotFoundException;
import com.phegon.phegonbank.notification.dtos.NotificationDTO;
import com.phegon.phegonbank.notification.services.NotificationService;
import com.phegon.phegonbank.res.Response;
import com.phegon.phegonbank.transaction.dtos.TransactionDTO;
import com.phegon.phegonbank.transaction.dtos.TransactionRequest;
import com.phegon.phegonbank.transaction.repo.TransactionRepo;
import com.phegon.phegonbank.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepo transactionRepo;
    private final AccountRepo accountRepo;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public Response<?> createTransaction(TransactionRequest transactionRequest) {
        Transaction transaction = new Transaction();

        transaction.setTransactionType(transactionRequest.getTransactionType());
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setDescription(transactionRequest.getDescription());

        switch (transactionRequest.getTransactionType()) {
            case DEPOSIT ->  handleDeposit(transactionRequest, transaction);
            case WITHDRAW -> handleWithDraw(transactionRequest, transaction);
            case TRANSFER -> handleTransfer(transactionRequest, transaction);
            default -> throw new InvalidTransactionException("Invalid transaction type");
        }

        transaction.setTransactionStatus(TransactionStatus.SUCCESS);
        Transaction savedTxn = transactionRepo.save(transaction);

        sendTransactionNotifications(savedTxn);

        return Response.builder()
                .statusCode(200)
                .message("Transaction successful")
                .build();
    }

    @Override
    @Transactional
    public Response<List<TransactionDTO>> getTransactionsForAnAccount(String accountNumber, int page, int size) {
        User user = userService.getCurrentLoggedUser();

        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Account does not belong to this user");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> txns = transactionRepo.findByAccount_AccountNumber(accountNumber, pageable);
        List<TransactionDTO> transactionDTOS = txns.getContent().stream()
                .map(transaction -> modelMapper.map(transaction, TransactionDTO. class))
                .toList();

        return Response.<List<TransactionDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Transcations retrieved")
                .data(transactionDTOS)
                .meta(Map.of(
                        "currentPage", txns.getNumber(),
                        "totalItems", txns.getTotalElements(),
                        "totalPages", txns.getTotalPages(),
                        "pageSeize", txns.getSize()

                ))

                .build();
    }

    private void handleDeposit(TransactionRequest request, Transaction transaction) {
        Account account = accountRepo.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(()->new NotFoundException("Account not found"));

        account.setBalance(account.getBalance().add(request.getAmount()));
        transaction.setAccount(account);
        accountRepo.save(account);
    }

    private void handleWithDraw(TransactionRequest request, Transaction transaction) {
        Account account = accountRepo.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        transaction.setAccount(account);
        accountRepo.save(account);

    }

    private void handleTransfer(TransactionRequest request, Transaction transaction) {
        Account sourceAccount = accountRepo.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(()->new NotFoundException("Account not found"));
        Account destAccount = accountRepo.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(()->new NotFoundException("Account not found"));

        if(sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
        accountRepo.save(sourceAccount);

        // add to dest
        destAccount.setBalance(destAccount.getBalance().add(request.getAmount()));
        accountRepo.save(destAccount);

        transaction.setAccount(sourceAccount);
        transaction.setSourceAccount(sourceAccount.getAccountNumber());
        transaction.setDestinationAccount(destAccount.getAccountNumber());
    }

    private void sendTransactionNotifications(Transaction txn) {
        User user = txn.getAccount().getUser();
        String subject;
        String template;

        Map<String, Object> templateVariables = new HashMap<>();

        templateVariables.put("name", user.getFirstName());
        templateVariables.put("amount", txn.getAmount());
        templateVariables.put("accountNumber", txn.getAccount().getAccountNumber());
        templateVariables.put("date", txn.getTransactionDate());
        templateVariables.put("balance", txn.getAccount().getBalance());

        if (txn.getTransactionType() == TransactionType.DEPOSIT) {
            subject = "Credit Alert";
            template = "credit-alert";


            NotificationDTO notificationEmailToSendOut = NotificationDTO.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(templateVariables)
                    .build();

            notificationService.sendEmail(notificationEmailToSendOut, user);
        } else if (txn.getTransactionType() == TransactionType.WITHDRAW){
            subject = "Debit Alert";
            template = "debit-alert";

            NotificationDTO notificationEmailToSendOut = NotificationDTO.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(templateVariables)
                    .build();
            notificationService.sendEmail(notificationEmailToSendOut, user);
        } else if (txn.getTransactionType() == TransactionType.TRANSFER){
            subject = "Debit Alert";
            template = "debit-alert";

            NotificationDTO notificationEmailToSendOut = NotificationDTO.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(templateVariables)
                    .build();
            notificationService.sendEmail(notificationEmailToSendOut, user);

            //Receiver credit alert

            Account destAccount = accountRepo.findByAccountNumber(txn.getDestinationAccount())
                    .orElseThrow(()->new NotFoundException("Destination account not found"));

            User receiver = destAccount.getUser();

            Map<String, Object> recvVars = new HashMap<>();

            recvVars.put("name", receiver.getFirstName());
            recvVars.put("amount", txn.getAmount());
            recvVars.put("accountNumber", destAccount.getAccountNumber());
            recvVars.put("date", txn.getTransactionDate());
            recvVars.put("balance", destAccount.getBalance());

            NotificationDTO notificationEmailToSendOutToReceiver = NotificationDTO.builder()
                    .recipient(receiver.getEmail())
                    .subject("Credit Alert")
                    .templateName("credit-alert")
                    .templateVariables(recvVars)
                    .build();
            notificationService.sendEmail(notificationEmailToSendOutToReceiver, receiver);


        }
    }

}
