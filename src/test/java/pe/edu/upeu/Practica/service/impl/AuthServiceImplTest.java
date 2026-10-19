package pe.edu.upeu.Practica.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.upeu.Practica.dto.auth.CambioPasswordRequestDTO;
import pe.edu.upeu.Practica.entity.Usuario;
import pe.edu.upeu.Practica.exception.ReglaNegocioException;
import pe.edu.upeu.Practica.mapper.UsuarioMapper;
import pe.edu.upeu.Practica.repository.UsuarioRepository;
import pe.edu.upeu.Practica.security.JwtService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioMapper usuarioMapper;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthServiceImpl service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        service = new AuthServiceImpl(authenticationManager, jwtService,
                usuarioRepository, usuarioMapper, passwordEncoder);
        usuario = new Usuario();
        usuario.setUsername("vendedor");
        usuario.setPassword("HASH_ACTUAL");
        when(usuarioRepository.findByUsernameIgnoreCase("vendedor"))
                .thenReturn(Optional.of(usuario));
    }

    @Test
    @DisplayName("Cambio correcto: se guarda el hash de la nueva contraseña")
    void cambiaPassword() {
        when(passwordEncoder.matches("Vendedor2026*", "HASH_ACTUAL")).thenReturn(true);
        when(passwordEncoder.encode("NuevaClave2026")).thenReturn("HASH_NUEVO");

        service.cambiarPassword("vendedor",
                new CambioPasswordRequestDTO("Vendedor2026*", "NuevaClave2026"));

        assertThat(usuario.getPassword()).isEqualTo("HASH_NUEVO");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Contraseña actual incorrecta: 409 y no se guarda nada")
    void actualIncorrecta() {
        when(passwordEncoder.matches("mala", "HASH_ACTUAL")).thenReturn(false);

        assertThatThrownBy(() -> service.cambiarPassword("vendedor",
                new CambioPasswordRequestDTO("mala", "NuevaClave2026")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La contraseña actual no es correcta");

        assertThat(usuario.getPassword()).isEqualTo("HASH_ACTUAL");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nueva igual a la actual: 409 y no se guarda nada")
    void nuevaIgualALaActual() {
        when(passwordEncoder.matches("Vendedor2026*", "HASH_ACTUAL")).thenReturn(true);

        assertThatThrownBy(() -> service.cambiarPassword("vendedor",
                new CambioPasswordRequestDTO("Vendedor2026*", "Vendedor2026*")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La nueva contraseña debe ser distinta de la actual");

        assertThat(usuario.getPassword()).isEqualTo("HASH_ACTUAL");
        verify(usuarioRepository, never()).save(any());
    }
}
