package pe.edu.upeu.Practica.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import pe.edu.upeu.Practica.exception.ReglaNegocioException;

import java.util.Set;

/*
 * Valida los parámetros de una consulta paginada y arma el Pageable.
 * Un tamaño fuera de rango o un campo fuera de la lista blanca responde
 * 409 (ReglaNegocioException), igual que en productos y ventas.
 */
public final class PaginacionUtil {

    public static final int TAMANIO_MAXIMO = 100;

    private PaginacionUtil() {
    }

    public static Pageable construir(int pagina, int tamanio,
                                     String ordenarPor, String direccion,
                                     Set<String> camposOrdenables,
                                     String campoPorDefecto) {
        if (pagina < 0 || tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new ReglaNegocioException(
                    "La página debe ser mayor o igual a 0 y el tamaño estar entre 1 y "
                            + TAMANIO_MAXIMO);
        }
        String campo = ordenarPor == null || ordenarPor.isBlank()
                ? campoPorDefecto : ordenarPor.trim();
        if (!camposOrdenables.contains(campo)) {
            throw new ReglaNegocioException("No se puede ordenar por '" + campo
                    + "'. Valores permitidos: " + String.join(", ", camposOrdenables));
        }
        Sort.Direction sentido = Sort.Direction.fromOptionalString(direccion)
                .orElseThrow(() -> new ReglaNegocioException("La dirección debe ser 'asc' o 'desc'"));
        // id como segundo criterio: el orden entre páginas queda estable
        return PageRequest.of(pagina, tamanio,
                Sort.by(sentido, campo).and(Sort.by(sentido, "id")));
    }
}
