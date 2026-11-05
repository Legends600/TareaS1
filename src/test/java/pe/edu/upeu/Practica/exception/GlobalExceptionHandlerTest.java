package pe.edu.upeu.Practica.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import pe.edu.upeu.Practica.config.RequestIdFilter;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request =
            new MockHttpServletRequest("POST", "/api/v1/clientes");

    @BeforeEach
    void ponerIdentificador() {
        MDC.put(RequestIdFilter.CLAVE_MDC, "a1b2c3d4");
    }

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    @Test
    void reglaDeNegocioLlevaElRequestId() {
        ResponseEntity<ErrorResponseDTO> r = handler.handleBusinessRule(
                new ReglaNegocioException("Ya existe un cliente con el DNI: 77001122"), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().getRequestId()).isEqualTo("a1b2c3d4");
    }

    @Test
    void recursoNoEncontradoLlevaElRequestId() {
        ResponseEntity<ErrorResponseDTO> r = handler.handleNotFound(
                new RecursosNoEncontradoException("Producto no encontrado"), request);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody().getRequestId()).isEqualTo("a1b2c3d4");
    }
}
