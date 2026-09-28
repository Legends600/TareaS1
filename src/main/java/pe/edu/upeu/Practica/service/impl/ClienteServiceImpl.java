package pe.edu.upeu.Practica.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.Practica.dto.ClienteRequestDTO;
import pe.edu.upeu.Practica.dto.ClienteResponseDTO;
import pe.edu.upeu.Practica.dto.PaginaResponseDTO;
import pe.edu.upeu.Practica.entity.Cliente;
import pe.edu.upeu.Practica.exception.RecursosNoEncontradoException;
import pe.edu.upeu.Practica.exception.ReglaNegocioException;
import pe.edu.upeu.Practica.repository.ClienteRepository;
import pe.edu.upeu.Practica.service.service.ClienteService;

import java.util.Set;

@Service
public class ClienteServiceImpl implements ClienteService {
    private static final Logger log =
            LoggerFactory.getLogger(ClienteServiceImpl.class);

    private static final Set<String> CAMPOS_ORDEN =
            Set.of("id", "dni", "nombres", "apellidos", "email");
    private static final int TAMANIO_MAXIMO = 100;

    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(
            ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional
    public ClienteResponseDTO create(ClienteRequestDTO t) {
        log.info(
                "Registrando cliente con DNI={}",
                t.getDni()
        );

        String dni = t.getDni().trim();
        String email = t.getEmail()
                .trim()
                .toLowerCase();

        // Regla de negocio 1
        if (clienteRepository.existsByDni(dni)) {
            throw new ReglaNegocioException(
                    "Ya existe un cliente con el DNI: " + dni
            );
        }

        // Regla de negocio 2
        if (clienteRepository.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException(
                    "Ya existe un cliente con el correo: " + email
            );
        }

        Cliente cliente = new Cliente();

        cliente.setDni(dni);
        cliente.setNombres(
                t.getNombres().trim()
        );
        cliente.setApellidos(
                t.getApellidos().trim()
        );
        cliente.setEmail(email);
        cliente.setTelefono(
                normalizar(t.getTelefono())
        );
        cliente.setDireccion(
                normalizar(t.getDireccion())
        );
        cliente.setEstado(t.getEstado());

        Cliente guardado =
                clienteRepository.save(cliente);

        log.info(
                "Cliente registrado correctamente id={}",
                guardado.getId()
        );
        return convertirResponse(guardado);
    }

    @Override
    @Transactional
    public ClienteResponseDTO update(Long aLong, ClienteRequestDTO t) {
        Cliente cliente =
                clienteRepository.findById(aLong)
                        .orElseThrow(() ->
                                new RecursosNoEncontradoException(
                                        "Cliente no encontrado con id: " + aLong
                                )
                        );

        String dni = t.getDni().trim();
        String email = t.getEmail()
                .trim()
                .toLowerCase();

        // DNI de otro cliente
        if (clienteRepository
                .existsByDniAndIdNot(dni, aLong)) {

            throw new ReglaNegocioException(
                    "Ya existe otro cliente con el DNI: "
                            + dni
            );
        }

        // Email de otro cliente
        if (clienteRepository
                .existsByEmailIgnoreCaseAndIdNot(
                        email,
                        aLong)) {

            throw new ReglaNegocioException(
                    "Ya existe otro cliente con el correo: "
                            + email
            );
        }

        cliente.setDni(dni);
        cliente.setNombres(
                t.getNombres().trim()
        );
        cliente.setApellidos(
                t.getApellidos().trim()
        );
        cliente.setEmail(email);
        cliente.setTelefono(
                normalizar(t.getTelefono())
        );
        cliente.setDireccion(
                normalizar(t.getDireccion())
        );
        cliente.setEstado(t.getEstado());

        Cliente actualizado =
                clienteRepository.save(cliente);

        log.info(
                "Cliente id={} actualizado correctamente",
                aLong
        );

        return convertirResponse(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO read(Long aLong) {
        log.info("Buscando cliente id={}", aLong);

        Cliente cliente =
                clienteRepository.findById(aLong)
                        .orElseThrow(() ->
                                new RecursosNoEncontradoException(
                                        "Cliente no encontrado con id: " + aLong
                                )
                        );

        return convertirResponse(cliente);
    }

    @Override
    @Transactional
    public void delete(Long aLong) {
        Cliente cliente =
                clienteRepository.findById(aLong)
                        .orElseThrow(() ->
                                new RecursosNoEncontradoException(
                                        "Cliente no encontrado con id: " + aLong
                                )
                        );

        // Baja lógica: el cliente puede estar referenciado en ventas
        if (!Boolean.TRUE.equals(cliente.getEstado())) {
            throw new ReglaNegocioException(
                    "El cliente con id " + aLong + " ya se encuentra inactivo"
            );
        }

        cliente.setEstado(false);
        clienteRepository.save(cliente);

        log.info(
                "Cliente id={} dado de baja correctamente",
                aLong
        );
    }

    private ClienteResponseDTO convertirResponse(
            Cliente cliente) {

        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getDni(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                cliente.getEstado(),
                cliente.getFechaCreacion(),
                cliente.getFechaModificacion()
        );
    }

    private String normalizar(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public Iterable<ClienteResponseDTO> readAll() {
        log.info("Listando clientes");

        return clienteRepository.findAll()
                .stream()
                .map(this::convertirResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ClienteResponseDTO> listarPaginado(
            int pagina, int tamanio, String ordenarPor, String direccion) {
        log.info(
                "Listando clientes pagina={} tamanio={} ordenarPor={} direccion={}",
                pagina, tamanio, ordenarPor, direccion
        );

        if (!CAMPOS_ORDEN.contains(ordenarPor)) {
            throw new ReglaNegocioException(
                    "No se puede ordenar por '" + ordenarPor
                            + "'. Valores permitidos: id, dni, nombres, apellidos, email"
            );
        }
        if (pagina < 0 || tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new ReglaNegocioException(
                    "La página debe ser mayor o igual a 0 y el tamaño estar entre 1 y "
                            + TAMANIO_MAXIMO
            );
        }
        Sort.Direction sentido = Sort.Direction.fromOptionalString(direccion)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La dirección debe ser 'asc' o 'desc'"
                ));

        // id como segundo criterio para que el orden entre páginas sea estable
        Sort orden = Sort.by(sentido, ordenarPor).and(Sort.by("id"));

        return PaginaResponseDTO.de(
                clienteRepository
                        .findAll(PageRequest.of(pagina, tamanio, orden))
                        .map(this::convertirResponse)
        );
    }
}
