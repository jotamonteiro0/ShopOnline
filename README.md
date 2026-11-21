# ShopOnline - Trabalho de POO (P2)

Sistema de loja virtual em Java, com interface de console, produtos, clientes comuns e VIP, carrinho, cupons, pagamentos simulados, pedidos, fidelidade, cashback e relatórios de vendas.

iCEV - Engenharia de Software, 2º período. Disciplina: Programação Orientada a Objetos. Professor: Samuel Oliveira.

## Executar

É necessário um **JDK 17 ou superior**, com `java` e `javac` disponíveis no terminal. O programa usa somente a biblioteca padrão do Java. Execute os comandos na raiz do projeto.

```bash
./run.sh
```

No Windows, o script disponível é `run.bat`. A execução nesse sistema operacional não foi validada nesta revisão. Para compilar e executar manualmente em macOS/Linux:

```bash
mkdir -p out
find src -name '*.java' -print0 | xargs -0 javac --release 17 -encoding UTF-8 -d out
java -cp out Main
```

Para executar a demonstração automática depois de compilar:

```bash
java -cp out Main --demo
```

O diretório de trabalho deve ser a raiz `ShopOnline`, inclusive na IDE, pois os caminhos dos dados são relativos. A execução interativa e a demonstração **alteram os CSVs de `data/`**. Guarde uma cópia antes de ensaiar com dados que precise preservar. A demonstração consome uma unidade de P01 e P02 e precisa desses produtos com estoque disponível; o cupom BEMVINDO10 só é aplicado se estiver válido para o cliente.

## Usar o sistema

| Opção | Operações |
| --- | --- |
| 1 - Produtos e estoque | Cadastrar, listar, consultar, repor estoque e consultar alertas abaixo de 5 unidades. |
| 2 - Clientes e VIP | Cadastrar clientes comuns e VIP e listar cadastros. |
| 3 - Carrinho e cupons | Selecionar cliente, adicionar/remover itens, cadastrar/aplicar/remover cupom e finalizar compra. |
| 4 - Pedidos | Listar pedidos, avançar status e cancelar pedidos em processamento. |
| 5 - Fidelidade e relatórios | Consultar pontos, níveis, saldos, produtos vendidos e receitas. |
| 6 - Demonstração | Executar uma compra de exemplo; no modo interativo, permite optar por usar cashback. |
| 0 - Sair | Encerrar o programa. |

No carrinho, confira o cliente ativo antes de comprar. Selecione PIX, cartão com 1 a 10 parcelas ou boleto no checkout. Havendo saldo, o programa pergunta se deseja usar cashback. O ID do pedido aparece na confirmação e pode ser consultado na opção 4.

## Regras implementadas

- A venda valida a disponibilidade e baixa o estoque. O alerta ocorre com **menos de 5 unidades**.
- Cliente VIP recebe **10% de desconto nos produtos marcados como promoção VIP**. Ser VIP e possuir nível Ouro são características independentes.
- Um pedido pode ter **um cupom**, percentual ou fixo, válido até a data informada inclusive. O mesmo código pode ser usado uma vez por cliente, com histórico persistido.
- Os descontos VIP e de cupom incidem sobre o subtotal. O resultado é limitado a zero; em seguida aplica-se o pagamento: PIX reduz 5%, cartão e boleto mantêm o valor.
- Se o cliente optar pelo resgate, o cashback existente abate o valor após os descontos e o pagamento, limitado ao total a pagar.
- Pontos e novo cashback são calculados sobre o valor final após o resgate. Compras **acima de R$ 100** geram a parte inteira de `valorFinal / 10`; compras de R$ 100 não geram pontos.
- O percentual de cashback considera o nível **antes dos pontos da compra atual**: Bronze até 499 pontos (2%), Prata de 500 a 1.499 (5%) e Ouro a partir de 1.500 (8%). A geração de cashback não depende do limite de R$ 100 exigido para pontos.
- Os valores finais e o cashback são arredondados para duas casas, com `HALF_UP`. Cupons também são normalizados para duas casas e descontos que arredondariam para zero são rejeitados.
- O fluxo de status é `EM_PROCESSAMENTO -> ENVIADO -> ENTREGUE`. Somente pedidos em processamento podem ser cancelados, com reposição dos itens no estoque persistido. O relatório exclui cancelados das receitas e quantidades vendidas.

As regras de ordem de cálculo e arredondamento são decisões da implementação para os pontos que o enunciado não detalha.

## Organização do código

```text
src/
  Main.java                 Inicialização e demonstração
  model/                    Entidades, enumerações, carrinho e cupom
    pagamento/              Interface FormaPagamento e implementações
  repository/               Persistência de produtos, clientes, cupons e pedidos
  service/                  Estoque, fidelidade, checkout e ciclo de pedidos
  view/                     Entrada e menus do console
  teste/                    12 classes de verificação sem frameworks externos
data/                       CSVs usados pela aplicação
relatorio/                  Relatório técnico editável, PDF e decisões de projeto
docs/                       Planejamento original da equipe e checklist de produtos
```

## Persistência

Os CSVs usam UTF-8, `;` como separador e ponto nos valores decimais. Os arquivos não são um banco de dados e não possuem controle de acesso concorrente.

| Arquivo | Cabeçalho |
| --- | --- |
| `produtos.csv` | `id;nome;descricao;preco;estoque;promocaoVip` |
| `clientes.csv` | `id;nome;email;pontos;saldoCashback;vip` |
| `cupons.csv` | `codigo;tipo;valor;dataValidade;clientesUtilizados` |
| `pedidos.csv` | `id;clienteId;subtotal;desconto;totalFinal;formaPagamento;status;dataCriacao;cashbackGerado` |
| `itens_pedidos.csv` | `pedidoId;produtoId;quantidade;precoUnitario` |

Na coluna `clientesUtilizados`, IDs são separados por vírgula. O leitor também aceita o cabeçalho antigo `codigo;tipo;valor;dataValidade;utilizado`: `false` não registra uso; `true` mantém o bloqueio global, pois o formato antigo não identifica o cliente. Na próxima gravação, o arquivo passa ao formato novo; o marcador `*` preserva esse bloqueio. Não há como recuperar usuários históricos apenas a partir de um booleano.

Produtos, pedidos e cupons criam arquivos ausentes; clientes são persistidos ao cadastrar ou salvar. Se o catálogo ou a lista de clientes estiver vazia, o `Main` insere exemplos. Clientes e cupons com erros de leitura bloqueiam regravações até correção e recarga. O leitor de produtos avisa e ignora linhas inválidas; portanto, corrija o arquivo antes de salvar para evitar perder registros rejeitados.

## Testes reproduzíveis sem alterar os dados reais

Execute em macOS/Linux, a partir da raiz. Os comandos criam uma cópia temporária do código e dos dados e deixam os resultados nessa cópia.

```bash
pasta_teste=$(mktemp -d)
cp -R src data "$pasta_teste/"
(
  cd "$pasta_teste" || exit 1
  mkdir -p out
  find src -name '*.java' -print0 | xargs -0 javac --release 17 -encoding UTF-8 -d out || exit 1
  for arquivo in src/teste/Verificacao*.java; do
    classe=${arquivo##*/}
    classe=${classe%.java}
    java -ea -cp out "teste.$classe" || exit 1
  done
)
```

A compilação e as **12 classes de verificação passaram em 21/11/2026**, com JDK 26.0.2.1 e compilação `--release 17`. Isso verifica compatibilidade de compilação com Java 17; não representa execução em uma JVM 17. `run.sh` também foi executado com sucesso.

As verificações incluem 30 casos do módulo de carrinho/cupons, 26 do módulo de pedidos, 27 de integração de pedidos, 8 de produtos e 12 regressões das correções, além de cadastro, persistência, VIP e navegação. O teste do console executa o `Main` em processos separados e confirma compra com VIP, cupom, PIX e cashback, consulta após reinício e cancelamento com devolução ao estoque.

## Limites conhecidos

- O uso previsto é local, com uma instância do programa por vez. Escritas por arquivo temporário e compensações tratam falhas detectadas, mas os CSVs não formam uma transação atômica contra queda de energia ou gravação concorrente.
- O cancelamento devolve estoque e muda o status. Na versão atual, não desfaz pontos, cashback gerado ou saldo resgatado; o cupom permanece utilizado. Essa política não foi detalhada nas imagens do enunciado e deve ser considerada ao demonstrar cancelamentos com benefícios.
- A quantidade de parcelas do cartão não é persistida no histórico: ao reabrir, o pagamento é reconstruído com uma parcela; valor e tipo de pagamento são preservados.
- O histórico dos itens guarda quantidade e preço de venda, mas depende do catálogo atual para nome/descrição e para reconstruir produtos. Não remova produtos ou clientes referenciados por pedidos.
- Os pagamentos são simulados. Não há cobrança bancária, autenticação ou integração externa.

## Equipe e fluxo de integração

| Integrante | Frente acordada |
| --- | --- |
| João Guilherme Monteiro | Produtos, estoque, persistência base e arquitetura. |
| Maria Vitória Salgado | Clientes, especialização VIP, validações e persistência de clientes. |
| Lucas Silveira | Carrinho, cupons, promoções e persistência de cupons. |
| Gabriel Buhatem | Pagamentos, pedidos, ciclo de status e cancelamento. |
| Lavynne Marianne | Fidelidade, cashback, relatórios e organização do menu. |

O fluxo combinado é branch individual, Pull Request para `developer` e integração validada em `main`. Esta documentação descreve os arquivos locais revisados; não afirma que as correções já foram publicadas ou integradas às branches remotas.

## Documentação

- [Relatório técnico editável](relatorio/relatorio-tecnico.md)
- [Relatório técnico em PDF](relatorio/relatorio-tecnico.pdf)
- [Decisões de projeto](relatorio/relatorio-decisoes.md)
- [Guia original da equipe](docs/Guia_Geral_Equipe_ShopOnline.pdf)
- [Divisão original de tarefas](docs/Divisao_Tarefas_P2.pdf)
- [Checklist do módulo de produtos](docs/CHECKLIST_DEV_JOAO.md)

O documento geral do professor, Trabalho P2.pdf, exige código-fonte no Git e relatório PDF com pelo menos quatro páginas no dia anterior à apresentação designada. O relatório deve incluir contribuição e experiência individual, funcionalidades implementadas e não concluídas com motivos, bibliotecas justificadas, referências e trecho ou screenshot de uma classe importante. O relato pessoal dos integrantes ainda depende das respostas de cada um antes do envio acadêmico. A recomendação de iText e o desafio de salvar dados no Google Drive não foram tratados como requisitos funcionais do ShopOnline.

Os PDFs de planejamento registram a organização anterior da equipe e podem conter pendências ou cronogramas históricos. Para executar e consultar o comportamento desta versão, use este README e o relatório técnico atualizado.
