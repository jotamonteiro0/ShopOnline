package repository;

import java.util.List;

public interface Repositorio<T, ID> {
    List<T> listarTodos();
    T buscarPorId(ID id);
    void salvar(T entidade);
    boolean removerPorId(ID id);
    boolean existePorId(ID id);
    void carregarDoArquivo();
    void salvarNoArquivo();
}
