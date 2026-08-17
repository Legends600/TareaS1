package pe.edu.upeu.Practica.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.Practica.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria,Long> {

}
