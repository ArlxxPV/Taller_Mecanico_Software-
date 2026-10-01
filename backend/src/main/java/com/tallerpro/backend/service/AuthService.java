package com.tallerpro.backend.service;

import com.tallerpro.backend.config.SeguridadProperties;
import com.tallerpro.backend.dto.*;
import com.tallerpro.backend.exception.ApiException;
import com.tallerpro.backend.model.*;
import com.tallerpro.backend.repository.ClienteRepository;
import com.tallerpro.backend.repository.LoginAuditRepository;
import com.tallerpro.backend.repository.RolRepository;
import com.tallerpro.backend.repository.UsuarioRepository;
import com.tallerpro.backend.security.AppUserDetailsService;
import com.tallerpro.backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ClienteRepository clienteRepository;
    private final LoginAuditRepository loginAuditRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final SeguridadProperties seguridadProperties;

    public AuthService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            ClienteRepository clienteRepository,
            LoginAuditRepository loginAuditRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AppUserDetailsService userDetailsService,
            SeguridadProperties seguridadProperties
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.clienteRepository = clienteRepository;
        this.loginAuditRepository = loginAuditRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.seguridadProperties = seguridadProperties;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest req) {
        if (usuarioRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ese correo ya esta registrado. Inicia sesion o usa otro correo.");
        }

        Rol rolCliente = rolRepository.findByNombre(NombreRol.CLIENTE)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "El rol CLIENTE no existe. Verifica las migraciones de Flyway."));

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(req.nombreCompleto());
        usuario.setEmail(req.email().toLowerCase());
        usuario.setTelefono(req.telefono());
        usuario.setPasswordHash(passwordEncoder.encode(req.password()));
        usuario.setRoles(Set.of(rolCliente));
        usuario = usuarioRepository.save(usuario);

        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setNombreCompleto(usuario.getNombreCompleto());
        cliente.setEmail(usuario.getEmail());
        clienteRepository.save(cliente);

        return UsuarioResponse.desde(usuario);
    }

    @Transactional
    public AuthResponse login(LoginRequest req, String ip, String userAgent) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(req.email()).orElse(null);

        if (usuario == null) {
            loginAuditRepository.save(LoginAudit.fallido(req.email(), "USUARIO_NO_EXISTE", ip, userAgent));
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas. Verifica tu correo y contrasena.");
        }

        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            loginAuditRepository.save(LoginAudit.fallido(req.email(), "CUENTA_INACTIVA", ip, userAgent));
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta cuenta esta inactiva. Contacta al administrador del taller.");
        }

        if (usuario.estaBloqueado()) {
            loginAuditRepository.save(LoginAudit.fallido(req.email(), "CUENTA_BLOQUEADA", ip, userAgent));
            long minutosRestantes = java.time.Duration.between(LocalDateTime.now(), usuario.getBloqueadoHasta()).toMinutes() + 1;
            throw new ApiException(HttpStatus.LOCKED,
                    "Tu cuenta esta bloqueada temporalmente por multiples intentos fallidos. Intenta de nuevo en " + minutosRestantes + " minuto(s).");
        }

        if (!passwordEncoder.matches(req.password(), usuario.getPasswordHash())) {
            registrarIntentoFallido(usuario);
            loginAuditRepository.save(LoginAudit.fallido(req.email(), "PASSWORD_INCORRECTA", ip, userAgent));
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas. Verifica tu correo y contrasena.");
        }

        // Login exitoso: se resetean los intentos fallidos y se registra la sesion.
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimoLoginEn(LocalDateTime.now());
        usuario.setUltimoLoginIp(ip);
        usuarioRepository.save(usuario);
        loginAuditRepository.save(LoginAudit.exitoso(usuario, ip, userAgent));

        var userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
        String token = jwtService.generarToken(userDetails);

        return AuthResponse.de(token, jwtService.expiracionEnSegundos(), UsuarioResponse.desde(usuario));
    }

    private void registrarIntentoFallido(Usuario usuario) {
        usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
        if (usuario.getIntentosFallidos() >= seguridadProperties.maxIntentosFallidos()) {
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(seguridadProperties.minutosBloqueo()));
        }
        usuarioRepository.save(usuario);
    }
}
