package com.nlt.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    private String userNo;

    @NotBlank
    private String password;

}

