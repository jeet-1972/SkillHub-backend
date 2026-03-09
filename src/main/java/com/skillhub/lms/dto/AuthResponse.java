package com.skillhub.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String message;
    /** Short-lived access token for Authorization: Bearer header. */
    private String accessToken;
    private UserResponse user;
}
