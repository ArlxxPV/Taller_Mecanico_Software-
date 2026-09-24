package com.tallerpro.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OlvidePasswordRequest(
        @NotBlank @Email String email
) {
}
