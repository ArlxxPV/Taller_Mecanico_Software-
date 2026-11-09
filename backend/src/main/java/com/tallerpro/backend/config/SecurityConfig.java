package com.tallerpro.backend.config;

import com.tallerpro.backend.security.AppUserDetailsService;
import com.tallerpro.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 7 (Spring Boot 4): DSL de lambdas obligatorio, sin
 * WebSecurityConfigurerAdapter (removido) y sin .and() (removido).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AppUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, AppUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API sin estado consumida por un SPA con Bearer token: no hay cookies de
                // sesion que un tercero pueda "montar", asi que el riesgo que CSRF mitiga
                // no aplica aqui y lo desactivamos (si en el futuro se usan cookies de
                // sesion, esto se debe revertir).
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Mas especifico primero: cualquier usuario autenticado puede ver su propio perfil.
                        .requestMatchers("/api/usuarios/yo").authenticated()
                        // Catalogo geografico publico (sin dato sensible), usado tanto por el
                        // formulario de cliente como el de taller: cualquier sesion vale.
                        .requestMatchers("/api/clientes/direcciones/**").authenticated()
                        // Quien da de alta personal necesita leer el catalogo de roles para
                        // el select, aunque no administre roles; crear/editar roles si es exclusivo.
                        .requestMatchers(HttpMethod.GET, "/api/roles/**").hasAnyAuthority("PERM_ROLES_ADMINISTRAR", "PERM_USUARIOS_CREAR")
                        .requestMatchers("/api/roles/**").hasAuthority("PERM_ROLES_ADMINISTRAR")
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**").hasAnyAuthority("PERM_USUARIOS_VER", "PERM_USUARIOS_CREAR")
                        .requestMatchers("/api/usuarios/**").hasAuthority("PERM_USUARIOS_CREAR")
                        // El listado de talleres lo necesitan tambien quienes trabajan con
                        // clientes (select de taller del formulario) y quien da de alta personal.
                        .requestMatchers(HttpMethod.GET, "/api/talleres/**").hasAnyAuthority(
                                "PERM_TALLERES_VER", "PERM_CLIENTES_VER", "PERM_CLIENTES_CREAR", "PERM_CLIENTES_EDITAR", "PERM_USUARIOS_CREAR")
                        .requestMatchers("/api/talleres/**").hasAnyAuthority("PERM_TALLERES_CREAR", "PERM_TALLERES_EDITAR")
                        // El catalogo de estatus lo administra solo ADMIN, pero su lectura la
                        // necesita cualquiera que trabaje con clientes (select de filtros/formularios).
                        .requestMatchers(HttpMethod.GET, "/api/estatus/**").hasAnyAuthority(
                                "PERM_ESTATUS_ADMINISTRAR", "PERM_CLIENTES_VER", "PERM_CLIENTES_CREAR", "PERM_CLIENTES_EDITAR")
                        .requestMatchers("/api/estatus/**").hasAuthority("PERM_ESTATUS_ADMINISTRAR")
                        // Suspender/reactivar un cliente es un permiso aparte del resto de /api/clientes/**.
                        .requestMatchers(HttpMethod.PATCH, "/api/clientes/*/estatus").hasAuthority("PERM_CLIENTES_SUSPENDER")
                        .requestMatchers(HttpMethod.PATCH, "/api/clientes/*/suspender").hasAuthority("PERM_CLIENTES_SUSPENDER")
                        .requestMatchers(HttpMethod.PATCH, "/api/clientes/*/reactivar").hasAuthority("PERM_CLIENTES_SUSPENDER")
                        // Eliminar (baja logica), restaurar y ver eliminados: solo ADMIN.
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/*").hasAuthority("PERM_CLIENTES_ELIMINAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/clientes/*/restaurar").hasAuthority("PERM_CLIENTES_ELIMINAR")
                        // Asociar vehiculo: igual que registrar/editar clientes (ver maqueta), no solo ver.
                        .requestMatchers(HttpMethod.POST, "/api/clientes/*/vehiculos").hasAnyAuthority("PERM_CLIENTES_CREAR", "PERM_CLIENTES_EDITAR")
                        // Editar un vehiculo o cambiar su cliente (UC-CV-12): igual permiso que asociarlo (ver arriba), no solo verlo.
                        .requestMatchers(HttpMethod.PUT, "/api/vehiculos/*").hasAnyAuthority("PERM_CLIENTES_CREAR", "PERM_CLIENTES_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/vehiculos/*/cliente").hasAnyAuthority("PERM_CLIENTES_CREAR", "PERM_CLIENTES_EDITAR")
                        // Activar/desactivar un vehiculo: mismo permiso que editarlo, no solo verlo.
                        .requestMatchers(HttpMethod.PATCH, "/api/vehiculos/*/activar").hasAnyAuthority("PERM_CLIENTES_CREAR", "PERM_CLIENTES_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/vehiculos/*/desactivar").hasAnyAuthority("PERM_CLIENTES_CREAR", "PERM_CLIENTES_EDITAR")
                        // Pantalla "Vehiculos" (consulta y edicion): mismo aislamiento por taller que clientes.
                        .requestMatchers("/api/vehiculos/**").hasAnyAuthority("PERM_CLIENTES_VER", "PERM_CLIENTES_CREAR", "PERM_CLIENTES_EDITAR")
                        // Indicadores y "ultimos movimientos" del panel: solo quien ve clientes (el panel
                        // simple de un CLIENTE autoservicio no llama a estos endpoints).
                        .requestMatchers("/api/panel/**").hasAnyAuthority("PERM_CLIENTES_VER", "PERM_CLIENTES_CREAR", "PERM_CLIENTES_EDITAR")
                        // Dar de alta el acceso (contrasena) de un cliente registrado por
                        // personal es, en el fondo, crear una cuenta: mismo permiso que USUARIOS_CREAR.
                        .requestMatchers(HttpMethod.GET, "/api/clientes/sin-acceso").hasAuthority("PERM_USUARIOS_CREAR")
                        .requestMatchers(HttpMethod.POST, "/api/clientes/*/acceso").hasAuthority("PERM_USUARIOS_CREAR")
                        .requestMatchers("/api/clientes/**").hasAnyAuthority("PERM_CLIENTES_VER", "PERM_CLIENTES_CREAR", "PERM_CLIENTES_EDITAR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Origen del servidor de desarrollo de Vite. Agrega aqui el dominio real
        // cuando despliegues el frontend a produccion.
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Argon2id es la recomendacion actual de Spring Security 7 (mas resistente a
     * ataques por GPU/ASIC que BCrypt). Requiere BouncyCastle en el classpath
     * (ver pom.xml), si no, esto lanza IllegalStateException al arrancar.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
