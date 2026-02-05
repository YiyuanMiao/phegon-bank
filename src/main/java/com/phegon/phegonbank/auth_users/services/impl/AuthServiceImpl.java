package com.phegon.phegonbank.auth_users.services.impl;

import com.phegon.phegonbank.account.entity.Account;
import com.phegon.phegonbank.account.services.AccountService;
import com.phegon.phegonbank.auth_users.dtos.LoginRequest;
import com.phegon.phegonbank.auth_users.dtos.LoginResponse;
import com.phegon.phegonbank.auth_users.dtos.RegistrationRequest;
import com.phegon.phegonbank.auth_users.dtos.ResetPasswordRequest;
import com.phegon.phegonbank.auth_users.entity.PassWordResetCode;
import com.phegon.phegonbank.auth_users.entity.User;
import com.phegon.phegonbank.auth_users.repo.PassWordResetCodeRepo;
import com.phegon.phegonbank.auth_users.repo.UserRepo;
import com.phegon.phegonbank.auth_users.services.AuthService;
import com.phegon.phegonbank.auth_users.services.CodeGenerator;
import com.phegon.phegonbank.enums.AccountType;
import com.phegon.phegonbank.enums.Currency;
import com.phegon.phegonbank.exceptions.BadRequestException;
import com.phegon.phegonbank.exceptions.NotFoundException;
import com.phegon.phegonbank.notification.dtos.NotificationDTO;
import com.phegon.phegonbank.notification.services.NotificationService;
import com.phegon.phegonbank.res.Response;
import com.phegon.phegonbank.role.entity.Role;
import com.phegon.phegonbank.role.repo.RoleRepo;
import com.phegon.phegonbank.security.TokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final NotificationService notificationService;
    private final AccountService accountService;

    private final CodeGenerator codeGenerator;
    private final PassWordResetCodeRepo passWordResetCodeRepo;


    @Value("${password.reset.link}")
    private String resetLink;


    @Override
    public Response<String> register(RegistrationRequest request) {
        List<Role> roles;

        if(request.getRoles() == null || request.getRoles().isEmpty()) {
            //Default to customer
            Role defaultRole = roleRepo.findByName("CUSTOMER")
                    .orElseThrow(()->new NotFoundException("Customer Role Not Found"));
            roles = Collections.singletonList(defaultRole);
        } else {
            roles = request.getRoles().stream()
                    .map(roleName -> roleRepo.findByName(roleName)
                            .orElseThrow(() -> new NotFoundException("ROLE NOT FOUND" + roleName)))
                    .collect(Collectors.toList());
        }

        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email Already Exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .active(true)
                .build();

        User savedUser = userRepo.save(user);

        //TODO AUTO GENERATE AN ACCOUNT NUMBER FOR THE USER
       Account savedAccount = accountService.createAccount(AccountType.SAVINGS, savedUser);

        //TODO SEND A WELCOME EMAIL OF THE USER AND ACCOUNT DETAILS TO THE USERS EMAIL
        //send welcome email
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", savedUser.getFirstName());

        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(savedUser.getEmail())
                .subject("Welcome to Phegon Bank")
                .templateName("welcome")
                .templateVariables(vars)
                .build();

        notificationService.sendEmail(notificationDTO, savedUser);

        //send account creation email
        Map<String, Object> accountVars = new HashMap<>();
        accountVars.put("name", savedUser.getFirstName());
        accountVars.put("accountNumber", savedAccount.getAccountNumber());
        accountVars.put("accountType", AccountType.SAVINGS.name());
        accountVars.put("currency", Currency.USD);

        NotificationDTO accountedCreatedEmail = NotificationDTO.builder()
                .recipient(savedUser.getEmail())
                .subject("Your New Bank Account Has Been Created ✅")
                .templateName("account-created")
                .templateVariables(accountVars)
                .build();

        notificationService.sendEmail(accountedCreatedEmail, savedUser);

        return Response.<String>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Your account has been created successfully")
                .data("Email of your account details has been sent to you. Your account number is: " + savedAccount.getAccountNumber())
                .build();
    }

    @Override
    public Response<LoginResponse> login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        User user = userRepo.findByEmail(email).orElseThrow(()->new NotFoundException("Email Not Found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Password Do Not Match");
        }

        String token = tokenService.generateToken(user.getEmail());
        LoginResponse loginResponse = LoginResponse.builder()
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .token(token)
                .build();

        return Response.<LoginResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Login Successful")
                .data(loginResponse)
                .build();
    }

    @Override
    @Transactional
    public Response<?> forgetPassword(String email) {
        User user = userRepo.findByEmail(email).orElseThrow(()->new NotFoundException("User Not Found"));
        passWordResetCodeRepo.deleteByUserId(user.getId());

        String code = codeGenerator.generateUniqueCode();

        PassWordResetCode resetCode = PassWordResetCode.builder()
                .user(user)
                .code(code)
                .expiryDate(calculateExpiryDate())
                .used(false)
                .build();

        passWordResetCodeRepo.save(resetCode);

        //send email reset link out
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", user.getFirstName());
        templateVariables.put("resetLink", resetLink + code);

        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Password Reset Code")
                .templateName("password-reset")
                .templateVariables(templateVariables)
                .build();

        notificationService.sendEmail(notificationDTO, user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Password rest code sent to your email")
                .build();

    }

    @Override
    @Transactional // roll back if failed
    public Response<?> updatePasswordViaResetCode(ResetPasswordRequest resetPassWordRequest) {
        String code = resetPassWordRequest.getCode();
        String newPassword = resetPassWordRequest.getNewPassword();

        // Find and validate code (within 5h)
        PassWordResetCode resetCode = passWordResetCodeRepo.findByCode(code).orElseThrow(()->new NotFoundException("Invalid reset code"));

        // check expiration first
        if (resetCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            passWordResetCodeRepo.delete(resetCode);
            throw new BadRequestException("Password Reset Code has expired");
        }

        // update password
        User user = resetCode.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // delete the code after use
        passWordResetCodeRepo.delete(resetCode);

        // Send confirmation email
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", user.getFirstName());

        NotificationDTO confirmationEmail = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Password Updated Successfully")
                .templateName("password-update-confirmation")
                .templateVariables(templateVariables)
                .build();

        notificationService.sendEmail(confirmationEmail, user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Password updated successfully")
                .build();
    }

    private LocalDateTime calculateExpiryDate() {
        return LocalDateTime.now().plusHours(5);
    }






}
