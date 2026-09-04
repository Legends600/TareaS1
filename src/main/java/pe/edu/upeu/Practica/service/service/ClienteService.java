package pe.edu.upeu.Practica.service.service;

import pe.edu.upeu.Practica.dto.ClienteRequestDTO;
import pe.edu.upeu.Practica.dto.ClienteResponseDTO;
import pe.edu.upeu.Practica.service.generic.CrudService;

public interface ClienteService extends CrudService<ClienteRequestDTO, ClienteResponseDTO, Long> {
}
