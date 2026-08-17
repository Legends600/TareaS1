package pe.edu.upeu.Practica.service.generic;

public interface CrudService<T, ID> {
    T save(T entity);
    T update(T entity);
    void delete(ID id);
    T read(ID id);
    Iterable<T> readAll();
}
