package pe.edu.upeu.Practica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.Practica.entity.Empleado;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
}
