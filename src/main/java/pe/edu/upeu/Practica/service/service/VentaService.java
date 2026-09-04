package pe.edu.upeu.Practica.service.service;

import pe.edu.upeu.Practica.dto.VentaRequestDTO;
import pe.edu.upeu.Practica.dto.VentaResponseDTO;

import java.util.List;

public interface VentaService {
    VentaResponseDTO registrar(VentaRequestDTO request);
    VentaResponseDTO buscar(Long id);
    List<VentaResponseDTO> listar();
}
