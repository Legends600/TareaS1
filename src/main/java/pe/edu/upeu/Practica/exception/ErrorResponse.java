package pe.edu.upeu.Practica.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ErrorResponse {
    private LocalDateTime fecha;
    private Integer status;
    private String error;
    private String mensaje;
    private String path;
    private Map<String, String> errores;
}
