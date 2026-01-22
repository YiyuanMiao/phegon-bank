package com.phegon.phegonbank.auth_users.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.phegon.phegonbank.account.dtos.AccountDTO;
import com.phegon.phegonbank.role.entity.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
//encountering profilePictureUrl ,which is empty, leave it empty instead of making it null
@JsonIgnoreProperties(ignoreUnknown = true)
//when using UserDTO as an object to receive a new user, it'll omit fields which do not have value
public class UserDTO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // generate a new id automatically
    private Long id;

    private String firstName;
    private String lastName;
    private String phoneNumber;

    private String email;

    @JsonIgnore
    private String password;// not gonna show password to user when returning data

    private String profilePictureUrl;
    private boolean active;


    private List<Role> roles;

    @JsonManagedReference
    private List<AccountDTO> accounts;
    // when returning a user obj, return the user's accounts and all the fields of that account,
    // except the "user" in the account's field
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;


}
