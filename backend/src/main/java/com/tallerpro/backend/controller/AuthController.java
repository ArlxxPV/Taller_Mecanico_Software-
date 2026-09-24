package com.tallerpro.backend.controller;

import com.tallerpro.backend.dto.*;
import com.tallerpro.backend.service.AuthService;
import com.tallerpro.backend.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        String ip = obtenerIp(request);
        String userAgent = request.getHeader("User-Agent");
        return ResponseEntity.ok(authService.login(req, ip, userAgent));
    }

    @PostMapping("/olvide-password")
    public ResponseEntity<Map<String, String>> olvidePassword(@Valid @RequestBody OlvidePasswordRequest req) {
        passwordResetService.solicitarRecuperacion(req);
        // Respuesta identica exista o no el correo, para no filtrar que emails estan registrados.
        return ResponseEntity.ok(Map.of("mensaje", "Si el correo esta registrado, recibiras un enlace de recuperacion."));
    }

    @PostMapping("/restablecer-password")
    public ResponseEntity<Map<String, String>> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest req) {
        passwordResetService.restablecerPassword(req);
        return ResponseEntity.ok(Map.of("mensaje", "Tu contrasena se actualizo correctamente. Ya puedes iniciar sesion."));
    }

    private String obtenerIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
