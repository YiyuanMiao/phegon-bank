package com.phegon.phegonbank.auth_users.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePasswordRequest {

    @NotBlank(message = "Old Password required")
    private String oldPassword;
    @NotBlank(message = "New Password required")
    private String newPassword;

}
