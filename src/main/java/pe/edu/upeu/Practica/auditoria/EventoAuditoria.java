package pe.edu.upeu.Practica.auditoria;

import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.edu.upeu.Practica.config.RequestIdFilter;

/*
 * Evento que viaja desde los servicios hasta la bitácora. Lleva el usuario
 * y el requestId de la petición en curso, que se leen en el hilo de la
 * petición (antes del commit).
 */
public record EventoAuditoria(String usuario, String accion,
                              String entidad, Long entidadId,
                              String detalle, String requestId) {

    /** Evento realizado por el usuario autenticado. */
    public static EventoAuditoria de(String accion, String entidad,
                                     Long entidadId, String detalle) {
        return new EventoAuditoria(usuarioActual(), accion, entidad, entidadId,
                recortar(detalle, 500), MDC.get(RequestIdFilter.CLAVE_MDC));
    }

    /** Evento de login: todavía no hay usuario autenticado. */
    public static EventoAuditoria login(String accion, String username) {
        return new EventoAuditoria(recortar(username, 50), accion, "Usuario",
                null, null, MDC.get(RequestIdFilter.CLAVE_MDC));
    }

    private static String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()
                || a instanceof AnonymousAuthenticationToken) {
            return "anonimo";
        }
        return a.getName();
    }

    private static String recortar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo
                ? texto : texto.substring(0, maximo);
    }
}
