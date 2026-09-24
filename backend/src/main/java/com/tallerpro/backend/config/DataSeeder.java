package com.tallerpro.backend.config;

import com.tallerpro.backend.model.NombreRol;
import com.tallerpro.backend.model.Usuario;
import com.tallerpro.backend.repository.RolRepository;
import com.tallerpro.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Crea un usuario ADMIN de arranque si todavia no existe ninguno.
 * Se hace en codigo (no en el SQL de Flyway) para usar siempre el mismo
 * PasswordEncoder que el resto de la app: asi el hash siempre es valido,
 * sin riesgo de que un hash "a mano" en SQL no coincida con los parametros
 * de Argon2 configurados.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String ADMIN_EMAIL = "admin@tallerpro.mx";
    private static final String ADMIN_PASSWORD_INICIAL = "Admin123!";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            return;
        }

        var rolAdmin = rolRepository.findByNombre(NombreRol.ADMIN)
                .orElseThrow(() -> new IllegalStateException("El rol ADMIN no existe: revisa que Flyway haya corrido V2."));

        Usuario admin = new Usuario();
        admin.setNombreCompleto("Administrador TallerPro");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD_INICIAL));
        admin.setRoles(Set.of(rolAdmin));
        usuarioRepository.save(admin);

        log.warn("""

                ================= USUARIO ADMIN CREADO =================
                Correo:      {}
                Contrasena:  {}
                Cambia esta contrasena en cuanto inicies sesion la primera vez.
                ==========================================================
                """, ADMIN_EMAIL, ADMIN_PASSWORD_INICIAL);
    }
}
