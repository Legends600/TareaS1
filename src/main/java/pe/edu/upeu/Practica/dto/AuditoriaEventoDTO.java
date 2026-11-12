package pe.edu.upeu.Practica.dto;

import pe.edu.upeu.Practica.entity.AuditoriaEvento;

import java.time.LocalDateTime;

/** Respuesta de GET /api/v1/auditoria. */
public record AuditoriaEventoDTO(Long id, LocalDateTime fecha, String usuario,
                                 String accion, String entidad, Long entidadId,
                                 String detalle, String requestId) {

    public static AuditoriaEventoDTO de(AuditoriaEvento a) {
        return new AuditoriaEventoDTO(a.getId(), a.getFecha(), a.getUsuario(),
                a.getAccion(), a.getEntidad(), a.getEntidadId(),
                a.getDetalle(), a.getRequestId());
    }
}
