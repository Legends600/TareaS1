package pe.edu.upeu.Practica.service.impl;

import org.springframework.stereotype.Service;
import pe.edu.upeu.Practica.entity.Categoria;
import pe.edu.upeu.Practica.repository.CategoriaRepository;
import pe.edu.upeu.Practica.service.service.CategoriaService;

@Service
public class CategoriaServiceImpl implements CategoriaService {
    private final CategoriaRepository categoriaRepository;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public Categoria save(Categoria entity) {
        return categoriaRepository.save(entity);
    }

    @Override
    public Categoria update(Categoria entity) {
        return categoriaRepository.save(entity);
    }

    @Override
    public void delete(Long aLong) {
        categoriaRepository.deleteById(aLong);
    }

    @Override
    public Categoria read(Long aLong) {
        return categoriaRepository.findById(aLong).orElse(null);
    }

    @Override
    public Iterable<Categoria> readAll() {
        return categoriaRepository.findAll();
    }
}
