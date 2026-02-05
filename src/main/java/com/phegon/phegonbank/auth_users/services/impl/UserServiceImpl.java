package com.phegon.phegonbank.auth_users.services.impl;

import com.phegon.phegonbank.account.dtos.AccountDTO;
import com.phegon.phegonbank.auth_users.dtos.UpdatePasswordRequest;
import com.phegon.phegonbank.auth_users.dtos.UserDTO;
import com.phegon.phegonbank.auth_users.entity.User;
import com.phegon.phegonbank.auth_users.repo.UserRepo;
import com.phegon.phegonbank.auth_users.services.UserService;
import com.phegon.phegonbank.aws.S3Service;
import com.phegon.phegonbank.exceptions.BadRequestException;
import com.phegon.phegonbank.exceptions.NotFoundException;
import com.phegon.phegonbank.notification.dtos.NotificationDTO;
import com.phegon.phegonbank.notification.services.NotificationService;
import com.phegon.phegonbank.res.Response;
import com.phegon.phegonbank.transaction.dtos.TransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final S3Service s3Service;

    // will save img to backend root folder
    // private final String uploadDir = "uploads/profile-pictures/";

    // will save img to front-end public folder for easy frontend access
    private final String uploadDir = "/Users/miaoyiyuan/Desktop/试验田/phegon-bank-react/profile-picture";

    @Override
    public User getCurrentLoggedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationServiceException("Authentication is required");
        }
        String email = authentication.getName();

        return userRepo.findByEmail(email).orElseThrow(() -> new NotFoundException("user not found"));

    }

    @Override
    public Response<UserDTO> getMyProfile() {
        User user = getCurrentLoggedUser();
        //UserDTO userDTO = modelMapper.map(user, UserDTO.class);

         UserDTO userDTO = UserDTO.builder()
                 .id(user.getId())
                 .firstName(user.getFirstName())
                 .lastName(user.getLastName())
                 .email(user.getEmail())
                 .phoneNumber(user.getPhoneNumber())
                 .active(user.isActive())
                 .accounts(user.getAccounts().stream().map(account -> {
                      return AccountDTO.builder()
                      .id(account.getId())
                      .balance(account.getBalance())
                      .accountStatus(account.getAccountStatus())
                      .accountNumber(account.getAccountNumber())
                      .createdAt(account.getCreatedAt())
                      .transactions(account.getTransactions().stream().map(transaction -> {
                           return TransactionDTO.builder()
                                   .amount(transaction.getAmount())
                                   .transactionType(transaction.getTransactionType())
                                   .transactionDate(transaction.getTransactionDate())
                                   .description(transaction.getDescription())
                                   .build();
                            }).toList())
                      .build();
                    }).collect(Collectors.toList()))
                 .profilePictureUrl(user.getProfilePictureUrl())
                 .build();


        return Response.<UserDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User retrieved")
                .data(userDTO)
                .build();
    }

    @Override
    public Response<Page<UserDTO>> getAllUsers(int page, int size) {
        Page<User> users = userRepo.findAll(PageRequest.of(page, size));
        Page<UserDTO> userDTOS = users.map(user -> modelMapper.map(user, UserDTO.class));

        return Response.<Page<UserDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User retrieved")
                .data(userDTOS)
                .build();
    }

    @Override
    public Response<?> updatePassword(UpdatePasswordRequest updatePasswordRequest) {
        User user = getCurrentLoggedUser();

        String oldPassword = updatePasswordRequest.getOldPassword();
        String newPassword = updatePasswordRequest.getNewPassword();

        if (oldPassword == null || newPassword == null) {
            throw new BadRequestException("Old Password or New Password Required");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadRequestException("Old Password is Incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        userRepo.save(user);

        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", user.getFirstName());
        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Your PassWord Was Successfully Changed")
                .templateVariables(templateVariables)
                .templateName("password-change")
                .build();

        notificationService.sendEmail(notificationDTO, user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value()).message("Password Changed Successfully").build();
    }

    @Override
    public Response<?> uploadProfilePicture(MultipartFile file) {
        User user = getCurrentLoggedUser();
        try {
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
                Path oldFile = Paths.get(user.getProfilePictureUrl());
                if (Files.exists(oldFile)) {
                    Files.delete(oldFile);
                }
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID() + "." + fileExtension;
            Path filePath = uploadPath.resolve(newFileName);

            Files.copy(file.getInputStream(), filePath);

            // String fileUrl = uploadDir + newFileName; // for backend
            String fileUrl = "/profile-picture/" + newFileName;// a relative path for frontend

            user.setProfilePictureUrl(fileUrl);
            userRepo.save(user);

            return Response.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Profile Picture Uploaded Successfully")
                    .data(fileUrl)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

    }

    @Override
    public Response<?> uploadProfilePictureToS3(MultipartFile file) {
        User user = getCurrentLoggedUser();

        try {
            if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
                s3Service.deleteFile(user.getProfilePictureUrl());
            }
            String s3Url = s3Service.uploadFile(file, "profile-pictures");
            user.setProfilePictureUrl(s3Url);
            userRepo.save(user);

            return Response.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Profile pic uploaded successfully.")
                    .data(s3Url)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
