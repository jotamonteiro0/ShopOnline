package repository;

import java.util.List;

/**
 * Interface generica para a camada de persistencia de dados.
 * Define o contrato para operacoes de consulta e armazenamento em arquivos.
 *
 * @param <T>  Tipo da entidade gerenciada
 * @param <ID> Tipo do identificador da entidade
 */
public interface Repositorio<T, ID> {
    List<T> listarTodos();
    T buscarPorId(ID id);
    void salvar(T entidade);
    boolean removerPorId(ID id);
    boolean existePorId(ID id);
    void carregarDoArquivo();
    void salvarNoArquivo();
}
