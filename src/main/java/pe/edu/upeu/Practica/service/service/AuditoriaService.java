package pe.edu.upeu.Practica.service.service;

import pe.edu.upeu.Practica.auditoria.EventoAuditoria;
import pe.edu.upeu.Practica.dto.AuditoriaEventoDTO;
import pe.edu.upeu.Practica.dto.PaginaResponseDTO;

import java.time.LocalDate;

public interface AuditoriaService {

    /** Guarda el evento en su propia transacción. */
    void registrar(EventoAuditoria evento);

    PaginaResponseDTO<AuditoriaEventoDTO> buscar(String usuario, String accion,
                                                 LocalDate desde, LocalDate hasta,
                                                 int pagina, int tamanio);
}
