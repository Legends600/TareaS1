package pe.edu.upeu.Practica.service.service;

import pe.edu.upeu.Practica.dto.auth.CambioPasswordRequestDTO;
import pe.edu.upeu.Practica.dto.auth.LoginRequestDTO;
import pe.edu.upeu.Practica.dto.auth.LoginResponseDTO;
import pe.edu.upeu.Practica.dto.auth.UsuarioResponseDTO;

public interface AuthService {
    LoginResponseDTO login(LoginRequestDTO request);

    UsuarioResponseDTO usuarioActual(String username);

    void cambiarPassword(String username, CambioPasswordRequestDTO request);
}
