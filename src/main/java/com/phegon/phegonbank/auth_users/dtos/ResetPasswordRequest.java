package com.phegon.phegonbank.auth_users.dtos;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)

public class ResetPasswordRequest {
    // request for forgot password
    private String email;

    // set new password
    private String code;
    private String newPassword;
}
