package pe.edu.upeu.Practica.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.Practica.dto.AuditoriaEventoDTO;
import pe.edu.upeu.Practica.dto.PaginaResponseDTO;
import pe.edu.upeu.Practica.service.service.AuditoriaService;

import java.time.LocalDate;

/** Consulta de la bitácora. Solo USUARIOS_ADMINISTRAR (ver SecurityConfig). */
@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponseDTO<AuditoriaEventoDTO>> buscar(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(
                auditoriaService.buscar(usuario, accion, desde, hasta, pagina, tamanio));
    }
}
