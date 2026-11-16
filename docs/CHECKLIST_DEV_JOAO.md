# Checklist de Verificacao - Modulo Joao Guilherme (dev_joao)

Este guia serve para voce verificar seu modulo no IntelliJ IDEA antes de fazer commits e merges.

---

## Modulos sob sua responsabilidade
1. **Entidade Produto** (`src/model/Produto.java`):
   - Atributos privados e getters/setters.
   - Metodo `precisaReposicao()` que retorna verdadeiro se estoque < 5.
   - Metodo `reduzirEstoque(quantidade)` impedindo estoque negativo.
   - Metodo `adicionarEstoque(quantidade)` para reposicao/estorno.
2. **Servico de Estoque** (`src/service/EstoqueService.java`):
   - `darBaixaEstoque(produtoId, quantidade)` com emissao de alerta de estoque baixo.
   - `reporEstoque(produtoId, quantidade)`.
   - `listarProdutosComAlertaReposicao()`.
3. **Persistencia CSV de Produtos** (`src/repository/ProdutoRepository.java`):
   - Leitura e gravacao com `Locale.US` e delimitador `;`.
   - Tratamento de arquivos inexistentes (criacao automatica).

---

## Como Rodar a Verificacao no IntelliJ IDEA
1. Abra a pasta `ShopOnline` no seu IntelliJ IDEA.
2. No canto superior direito, selecione a Run Configuration **VerificacaoModuloJoao** e clique no botao **Run** (Play verde).
   * Ou pelo terminal: `./run.sh` ou execute `java -cp out teste.VerificacaoModuloJoao`.
3. Certifique-se de que todos os 6 testes estao com status **PASSOU**.

---

## Como Subir suas Alteracoes ao Git
Apos testar e validar:

```bash
# Conferir status dos arquivos alterados
git status

# Adicionar arquivos
git add .

# Criar commit semantico
git commit -m "feat(joao): implementa e valida modulo de estoque, produtos e persistencia CSV"

# Enviar para a sua branch remota
git push origin dev_joao
```

---

## Prazo Fatal da Equipe
* **Conclusao individual:** Quarta-feira, 18/11/2026.
* **Integracao na developer:** Quinta-feira, 19/11/2026.
* **Finalizacao obrigatoria de todo o projeto:** Sexta-feira, 20/11/2026.
