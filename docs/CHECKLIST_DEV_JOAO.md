# Checklist do módulo de produtos e estoque

Revisão: 21/11/2026. Frente acordada: João Guilherme Monteiro. Este documento complementa o [README](../README.md), que contém os comandos de execução e teste da versão integrada.

## Componentes e operações

| Componente | Responsabilidade |
| --- | --- |
| `model/Produto.java` | ID, nome, descrição, preço, estoque e promoção VIP; comparação de produtos por ID. |
| `service/EstoqueService.java` | Cadastro sem ID duplicado, baixa, reposição, consulta e resumo de estoque. |
| `repository/ProdutoRepository.java` | Leitura e gravação UTF-8 em `data/produtos.csv`. |
| `view/MenuProdutos.java` | Cadastro, listagem, busca por ID, reposição e alertas pelo console. |
| `view/Entrada.java` | Leitura de texto, IDs, inteiros, valores finitos e confirmações. |

O alerta é ativado quando restam **menos de 5 unidades**; exatamente 5 unidades não ativa o alerta. A baixa é feita na confirmação da venda. Um pedido cancelado em processamento devolve as quantidades ao estoque persistido.

## Verificações executadas

`teste.VerificacaoModuloJoao` passou nas oito verificações:

1. Cadastro e persistência de produto.
2. Baixa de estoque.
3. Alerta abaixo de cinco unidades.
4. Bloqueio de baixa acima do disponível.
5. Reposição de estoque.
6. Rejeição de ID duplicado.
7. Leitura dos dados persistidos.
8. Criação de arquivo ausente.

`teste.VerificacaoCorrecoesEntrega` também verifica falha de gravação do estoque e compensação de checkout. A bateria completa passou em cópia temporária. Siga o procedimento do README para evitar que testes antigos criem arquivos dentro de `data/` do projeto.

Na IDE, configure o JDK e use a raiz ShopOnline como diretório de trabalho. Execute o `main` da classe de teste em uma cópia de teste do projeto; não é necessário que exista uma configuração de execução previamente cadastrada.

## Persistência e limites

O cabeçalho é `id;nome;descricao;preco;estoque;promocaoVip`, com ponto decimal. A gravação usa arquivo temporário e comunica falhas ao serviço. Baixa e reposição restauram o estoque em memória quando a escrita falha.

A leitura de produtos avisa e ignora linhas inválidas. Corrija essas linhas antes de uma nova gravação para evitar perder registros rejeitados. Os CSVs não oferecem transação conjunta contra queda de energia nem proteção para várias instâncias simultâneas.

## Pontos para a defesa oral

- Por que os atributos são privados e como métodos controlam mudanças no estoque.
- Diferença entre validar a quantidade no carrinho e revalidá-la no checkout.
- Condição exata do alerta (`estoque < 5`).
- Diferença entre mudar o estoque em memória e gravá-lo no CSV.
- Uso de UTF-8, `Locale.US`, arquivos temporários e propagação de erros de gravação.
