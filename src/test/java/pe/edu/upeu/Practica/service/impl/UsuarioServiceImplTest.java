package pe.edu.upeu.Practica.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.upeu.Practica.dto.auth.AsignarRolesRequestDTO;
import pe.edu.upeu.Practica.dto.auth.CambioEstadoRequestDTO;
import pe.edu.upeu.Practica.entity.Rol;
import pe.edu.upeu.Practica.entity.Usuario;
import pe.edu.upeu.Practica.exception.RecursosNoEncontradoException;
import pe.edu.upeu.Practica.exception.ReglaNegocioException;
import pe.edu.upeu.Practica.mapper.UsuarioMapper;
import pe.edu.upeu.Practica.repository.EmpleadoRepository;
import pe.edu.upeu.Practica.repository.RolRepository;
import pe.edu.upeu.Practica.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UsuarioMapper usuarioMapper;

    private UsuarioServiceImpl service;
    private Usuario vendedor;
    private Usuario admin;
    private Rol rolAdmin;
    private Rol rolVendedor;
    private Rol rolAlmacenero;

    @BeforeEach
    void preparar() {
        service = new UsuarioServiceImpl(usuarioRepository, empleadoRepository,
                rolRepository, passwordEncoder, usuarioMapper);
        vendedor = new Usuario();
        vendedor.setUsername("vendedor");
        vendedor.setEstado(true);
        admin = new Usuario();
        admin.setUsername("admin");
        admin.setEstado(true);
        rolAdmin = rol(1L, "ADMIN");
        rolVendedor = rol(2L, "VENDEDOR");
        rolAlmacenero = rol(3L, "ALMACENERO");
    }

    private Rol rol(Long id, String nombre) {
        Rol r = new Rol();
        r.setId(id);
        r.setNombre(nombre);
        return r;
    }

    // ---------- asignarRoles ----------

    @Test
    @DisplayName("Asignar roles: usuario inexistente da 404")
    void rolesUsuarioInexistente() {
        when(usuarioRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignarRoles(9999L,
                new AsignarRolesRequestDTO(Set.of(2L)), "admin"))
                .isInstanceOf(RecursosNoEncontradoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Asignar roles: rol inexistente da 404")
    void rolesRolInexistente() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(vendedor));
        when(rolRepository.findAllById(Set.of(2L, 99L))).thenReturn(List.of(rolVendedor));

        assertThatThrownBy(() -> service.asignarRoles(5L,
                new AsignarRolesRequestDTO(Set.of(2L, 99L)), "admin"))
                .isInstanceOf(RecursosNoEncontradoException.class)
                .hasMessageContaining("99");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Asignar roles: un admin no puede quitarse el rol ADMIN (409)")
    void adminSeQuitaAdmin() {
        admin.getRoles().add(rolAdmin);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(rolRepository.findAllById(Set.of(2L))).thenReturn(List.of(rolVendedor));

        assertThatThrownBy(() -> service.asignarRoles(1L,
                new AsignarRolesRequestDTO(Set.of(2L)), "admin"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(usuarioRepository, never()).save(any());
        assertThat(admin.getRoles()).containsExactly(rolAdmin);
    }

    @Test
    @DisplayName("Asignar roles: un admin puede conservar ADMIN y agregar otros")
    void adminConservaAdmin() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(rolRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(rolAdmin, rolVendedor));
        when(usuarioRepository.save(admin)).thenReturn(admin);

        service.asignarRoles(1L, new AsignarRolesRequestDTO(Set.of(1L, 2L)), "admin");

        assertThat(admin.getRoles()).containsExactlyInAnyOrder(rolAdmin, rolVendedor);
    }

    @Test
    @DisplayName("Asignar roles: reemplaza los roles del usuario")
    void asignaRoles() {
        vendedor.getRoles().add(rolVendedor);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(vendedor));
        when(rolRepository.findAllById(Set.of(2L, 3L)))
                .thenReturn(List.of(rolVendedor, rolAlmacenero));
        when(usuarioRepository.save(vendedor)).thenReturn(vendedor);

        service.asignarRoles(5L, new AsignarRolesRequestDTO(Set.of(2L, 3L)), "admin");

        assertThat(vendedor.getRoles()).containsExactlyInAnyOrder(rolVendedor, rolAlmacenero);
        verify(usuarioRepository).save(vendedor);
    }

    // ---------- cambiarEstado ----------

    @Test
    @DisplayName("Cambiar estado: usuario inexistente da 404")
    void estadoUsuarioInexistente() {
        when(usuarioRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(9999L,
                new CambioEstadoRequestDTO(false), "admin"))
                .isInstanceOf(RecursosNoEncontradoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cambiar estado: un admin no puede desactivarse a si mismo (409)")
    void adminSeDesactiva() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.cambiarEstado(1L,
                new CambioEstadoRequestDTO(false), "admin"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(usuarioRepository, never()).save(any());
        assertThat(admin.getEstado()).isTrue();
    }

    @Test
    @DisplayName("Cambiar estado: desactiva a otro usuario")
    void desactivaOtro() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(vendedor));
        when(usuarioRepository.save(vendedor)).thenReturn(vendedor);

        service.cambiarEstado(5L, new CambioEstadoRequestDTO(false), "admin");

        assertThat(vendedor.getEstado()).isFalse();
        verify(usuarioRepository).save(vendedor);
    }

    @Test
    @DisplayName("Cambiar estado: reactiva a un usuario")
    void reactiva() {
        vendedor.setEstado(false);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(vendedor));
        when(usuarioRepository.save(vendedor)).thenReturn(vendedor);

        service.cambiarEstado(5L, new CambioEstadoRequestDTO(true), "admin");

        assertThat(vendedor.getEstado()).isTrue();
    }
}
