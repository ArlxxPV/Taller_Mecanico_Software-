package com.tallerpro.backend.service;

import com.tallerpro.backend.config.ResetPasswordProperties;
import com.tallerpro.backend.dto.OlvidePasswordRequest;
import com.tallerpro.backend.dto.RestablecerPasswordRequest;
import com.tallerpro.backend.exception.ApiException;
import com.tallerpro.backend.model.PasswordResetToken;
import com.tallerpro.backend.model.Usuario;
import com.tallerpro.backend.repository.PasswordResetTokenRepository;
import com.tallerpro.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final ResetPasswordProperties resetProperties;

    public PasswordResetService(
            UsuarioRepository usuarioRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            MailService mailService,
            ResetPasswordProperties resetProperties
    ) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.resetProperties = resetProperties;
    }

    @Transactional
    public void solicitarRecuperacion(OlvidePasswordRequest req) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(req.email()).orElse(null);

        // Nunca revelamos si el correo existe o no: siempre respondemos "ok" al
        // controlador. Si el usuario existe, ademas generamos el token y el enlace.
        if (usuario == null) {
            log.info("Se solicito recuperacion para un correo no registrado: {}", req.email());
            return;
        }

        String tokenPlano = generarTokenAleatorio();
        String tokenHash = sha256(tokenPlano);

        PasswordResetToken token = new PasswordResetToken(
                usuario,
                tokenHash,
                LocalDateTime.now().plusMinutes(resetProperties.minutosExpiracion())
        );
        tokenRepository.save(token);

        String enlace = resetProperties.urlFrontend() + "?token=" + tokenPlano;
        mailService.enviarCorreoRecuperacion(usuario.getEmail(), usuario.getNombreCompleto(), enlace);
    }

    @Transactional
    public void restablecerPassword(RestablecerPasswordRequest req) {
        String tokenHash = sha256(req.token());

        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El enlace de recuperacion no es valido."));

        if (!token.esValido()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El enlace de recuperacion ya expiro o ya fue usado. Solicita uno nuevo.");
        }

        Usuario usuario = token.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(req.nuevaPassword()));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        token.setUsado(true);
        tokenRepository.save(token);
    }

    private String generarTokenAleatorio() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
