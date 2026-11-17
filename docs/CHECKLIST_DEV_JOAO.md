# Checklist de Verificacao - Modulo Joao Guilherme (dev_joao)

Este guia documenta o modulo desenvolvido pelo Joao Guilherme Monteiro para o sistema **ShopOnline**.

---

## 1. Modulos sob Responsabilidade do Joao
1. **Modelagem de Produtos** (`src/model/Produto.java`):
   - Atributos privados: `id`, `nome`, `descricao`, `preco`, `quantidadeEstoque`, `promocaoVip`.
   - Validacoes defensivas no construtor e setters (preco >= 0, estoque >= 0, nome e id nao vazios).
   - Metodo `precisaReposicao()`: retorna verdadeiro quando estoque < 5 unidades.
   - Metodo `reduzirEstoque(quantidade)`: efetua baixa impedindo estoque negativo.
   - Metodo `adicionarEstoque(quantidade)`: adiciona unidades ao inventario.
   - Metodos `equals()` e `hashCode()` baseados no ID.
2. **Servico de Estoque** (`src/service/EstoqueService.java`):
   - `cadastrarProduto(...)`: valida duplicidade de ID e persiste.
   - `darBaixaEstoque(id, quantidade)`: baixa estoque e dispara alerta de reposicao no console quando < 5 unidades.
   - `reporEstoque(id, quantidade)`: adiciona unidades e atualiza o arquivo CSV.
   - `listarProdutosComAlertaReposicao()`: filtra produtos em estado critico.
   - `temEstoqueSuficiente(id, quantidade)`.
   - `calcularTotalUnidadesEmEstoque()` e `calcularValorTotalInventario()`.
3. **Persistencia CSV de Produtos** (`src/repository/ProdutoRepository.java`):
   - Implementa `Repositorio<Produto, String>`.
   - Formato padronizado: `id;nome;descricao;preco;estoque;promocaoVip` com `Locale.US` e `UTF-8`.
   - Tolerancia a falhas: se o arquivo nao existir, cria a pasta `data/` e o arquivo com cabecalho automaticamente.
   - Se uma linha estiver corrompida, ignora a linha com aviso e carrega as demais sem travar.
4. **Tratamento de Entradas Seguras** (`src/view/Entrada.java`):
   - Metodos utilitarios: `lerTexto`, `lerId`, `lerInteiro`, `lerDouble`, `lerConfirmacao`.
   - Blindagem com try/catch: impede que o sistema aborte se o usuario digitar letras em campos numericos.
5. **Menu de Produtos e Menu Principal** (`src/view/MenuProdutos.java` e `src/view/MenuPrincipal.java`):
   - Navegacao interativa completa com cadastro, listagem, consulta por ID, reposicao manual e relatorio de estoque baixo.

---

## 2. Como Executar os Testes no IntelliJ IDEA
1. Abra o projeto `ShopOnline` no IntelliJ IDEA.
2. No canto superior direito, selecione a Run Configuration **VerificacaoModuloJoao** e clique no botao **Run** (Play verde).
   * Ou pelo terminal:
     ```bash
     javac -d out $(find src -name "*.java")
     java -cp out teste.VerificacaoModuloJoao
     ```
3. Resultado esperado: **8 de 8 testes aprovados com status PASSOU**.

---

## 3. Como Executar o Sistema Interativo
* No IntelliJ IDEA: selecione a Run Configuration **Main** e clique em **Run**.
* Ou pelo terminal:
  ```bash
  ./run.sh          # Linux e macOS
  run.bat           # Windows
  ```
* Para rodar a demonstracao automatizada sem interacao:
  ```bash
  java -cp out Main --demo
  ```

---

## 4. O que Explicar na Defesa Oral
* **Encapsulamento:** Por que os atributos de `Produto` sao privados e a baixa de estoque passa pelo metodo `reduzirEstoque`, que retorna `boolean` e nao deixa o saldo ficar negativo.
* **Persistencia em CSV:** Como o `BufferedReader` e `BufferedWriter` leem e gravam em `data/produtos.csv`, e por que o `Locale.US` e indispensavel para evitar falhas silenciosas de ponto flutuante.
* **Resiliencia:** Como a classe `Entrada` e os repositórios garantem que o software nao quebre com entradas invalidas ou arquivos ausentes.
