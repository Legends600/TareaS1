package pe.edu.upeu.Practica.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.Practica.entity.Categoria;
import pe.edu.upeu.Practica.service.service.CategoriaService;


@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {
    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public Iterable<Categoria> getCategorias(){
        return categoriaService.readAll();
    }
}
