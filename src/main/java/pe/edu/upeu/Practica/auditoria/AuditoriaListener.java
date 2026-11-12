package pe.edu.upeu.Practica.auditoria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.Practica.service.service.AuditoriaService;

/*
 * Registra los eventos de negocio SOLO después del commit: si la venta
 * hace rollback, no queda un evento falso en la bitácora. Un error al
 * auditar se escribe en el log, pero no convierte en error una operación
 * que ya se guardó.
 */
@Component
public class AuditoriaListener {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaListener.class);

    private final AuditoriaService auditoriaService;

    public AuditoriaListener(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmar(EventoAuditoria evento) {
        try {
            auditoriaService.registrar(evento);
        } catch (RuntimeException ex) {
            log.error("No se pudo auditar {} {}", evento.accion(), evento.entidadId(), ex);
        }
    }
}
