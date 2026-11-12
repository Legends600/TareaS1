package pe.edu.upeu.Practica.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.Practica.auditoria.EventoAuditoria;
import pe.edu.upeu.Practica.dto.AuditoriaEventoDTO;
import pe.edu.upeu.Practica.dto.PaginaResponseDTO;
import pe.edu.upeu.Practica.entity.AuditoriaEvento;
import pe.edu.upeu.Practica.exception.ReglaNegocioException;
import pe.edu.upeu.Practica.repository.AuditoriaEventoRepository;
import pe.edu.upeu.Practica.service.service.AuditoriaService;
import pe.edu.upeu.Practica.util.PaginacionUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaServiceImpl.class);
    private static final Set<String> CAMPOS_ORDENABLES = Set.of("fecha", "usuario", "accion");

    private final AuditoriaEventoRepository repository;

    public AuditoriaServiceImpl(AuditoriaEventoRepository repository) {
        this.repository = repository;
    }

    /*
     * REQUIRES_NEW: el evento se guarda en su propia transacción. Así el
     * login fallido queda registrado aunque la operación termine en error,
     * y un evento de negocio no depende de la transacción que ya terminó.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(EventoAuditoria evento) {
        AuditoriaEvento a = new AuditoriaEvento();
        a.setFecha(LocalDateTime.now());
        a.setUsuario(evento.usuario());
        a.setAccion(evento.accion());
        a.setEntidad(evento.entidad());
        a.setEntidadId(evento.entidadId());
        a.setDetalle(evento.detalle());
        a.setRequestId(evento.requestId());
        repository.save(a);
        log.info("Auditoría {} | {} {} | usuario={}", evento.accion(),
                evento.entidad(), evento.entidadId(), evento.usuario());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<AuditoriaEventoDTO> buscar(
            String usuario, String accion, LocalDate desde, LocalDate hasta,
            int pagina, int tamanio) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaNegocioException("El rango de fechas es inválido: 'desde' ("
                    + desde + ") es posterior a 'hasta' (" + hasta + ")");
        }
        Pageable pageable = PaginacionUtil.construir(pagina, tamanio,
                "fecha", "desc", CAMPOS_ORDENABLES, "fecha");
        return PaginaResponseDTO.de(repository.buscar(
                vacioANulo(usuario), vacioANulo(accion),
                desde == null ? null : desde.atStartOfDay(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay(),
                pageable).map(AuditoriaEventoDTO::de));
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
