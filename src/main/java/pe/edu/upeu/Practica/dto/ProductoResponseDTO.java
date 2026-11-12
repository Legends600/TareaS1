package pe.edu.upeu.Practica.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProductoResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;
    private Double precio;
    private Integer stock;
    private Boolean estado;
    private Long categoriaId;
    private String categoriaNombre;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private String creadoPor;
    private String modificadoPor;
}
