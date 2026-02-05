package com.phegon.phegonbank.auth_users.controller;

import com.phegon.phegonbank.auth_users.dtos.RegistrationRequest;
import com.phegon.phegonbank.auth_users.dtos.UpdatePasswordRequest;
import com.phegon.phegonbank.auth_users.dtos.UserDTO;
import com.phegon.phegonbank.auth_users.services.UserService;
import com.phegon.phegonbank.res.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/register")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<Page<UserDTO>>> getAllUsers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(userService.getAllUsers(page, size));
    }

    @GetMapping("/me")
    public ResponseEntity<Response<UserDTO>> getCurrentUser() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @PutMapping("/update-password")
    public ResponseEntity<Response<?>> updatePassword(@RequestBody @Valid UpdatePasswordRequest updatePasswordRequest) {
        return ResponseEntity.ok(userService.updatePassword(updatePasswordRequest));
    }

    @PutMapping("/profile-picture")
    public ResponseEntity<Response<?>> uploadProfilePictureToS3(@RequestParam("file") MultipartFile file) {
        System.out.println(">>> uploadProfilePicture HIT");
        return ResponseEntity.ok(userService.uploadProfilePictureToS3(file));
    }

}
