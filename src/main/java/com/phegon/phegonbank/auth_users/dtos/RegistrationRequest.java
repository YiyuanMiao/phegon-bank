package com.phegon.phegonbank.auth_users.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.List;

@Data
public class RegistrationRequest {
    @NotBlank(message = "FirstName required")
    private String firstName;
    private String lastName;
    private String phoneNumber;
    @NotBlank(message = "Email required")
    @Email
    private String email;
    private List<String> roles;
    @NotBlank(message = "Password required")
    private String password;


}
