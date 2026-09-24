package com.tallerpro.backend.dto;

public record AuthResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        UsuarioResponse usuario
) {
    public static AuthResponse de(String token, long expiraEnSegundos, UsuarioResponse usuario) {
        return new AuthResponse(token, "Bearer", expiraEnSegundos, usuario);
    }
}
