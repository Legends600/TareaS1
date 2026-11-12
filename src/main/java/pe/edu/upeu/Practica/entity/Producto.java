package pe.edu.upeu.Practica.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "producto")
@EntityListeners(AuditingEntityListener.class)
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Boolean estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion", nullable = false)
    private LocalDateTime fechaModificacion;

    @PrePersist
    public void prePersist(){
        this.fechaCreacion = LocalDateTime.now();
        if(estado == null){
            estado = true;
        }
    }
    @PreUpdate
    public void preUpdate(){
        this.fechaModificacion = LocalDateTime.now();
    }

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    /** Usuario del token que creó la fila (lo llena Spring Data JPA). */
    @CreatedBy
    @Column(name = "creado_por", length = 50, updatable = false)
    private String creadoPor;

    /** Usuario del token que hizo la última modificación. */
    @LastModifiedBy
    @Column(name = "modificado_por", length = 50)
    private String modificadoPor;
}
