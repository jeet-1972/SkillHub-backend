package com.skillhub.lms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100)
    private String password;
}
