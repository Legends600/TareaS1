package pe.edu.upeu.Practica.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CambioPasswordRequestDTO {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;

    /*
     * Mismas reglas que UsuarioRequestDTO.password. BCrypt solo considera
     * los primeros 72 bytes de la contrasena: por eso el maximo es 72.
     */
    @NotBlank(message = "La contraseña nueva es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "La contraseña debe contener al menos una letra y un número"
    )
    private String passwordNueva;
}
