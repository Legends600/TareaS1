package pe.edu.upeu.Practica.service.service;

import pe.edu.upeu.Practica.dto.auth.EmpleadoResponseDTO;
import pe.edu.upeu.Practica.dto.auth.RolResponseDTO;
import pe.edu.upeu.Practica.dto.auth.UsuarioRequestDTO;
import pe.edu.upeu.Practica.dto.auth.UsuarioResponseDTO;

import java.util.List;

public interface UsuarioService {
    UsuarioResponseDTO crear(UsuarioRequestDTO request);

    List<UsuarioResponseDTO> listar();

    List<EmpleadoResponseDTO> listarEmpleados();

    List<RolResponseDTO> listarRoles();
}
