# Trabalho Pratico - Programacao Orientada a Objetos (P2)

Implementacao do sistema de comercio eletronico ShopOnline, aplicando os principios fundamentais da Orientacao a Objetos (Abstracao, Encapsulamento, Heranca e Polimorfismo), persistencia em arquivos CSV, tratamento de excecoes e boas praticas de arquitetura.

iCEV - Instituto de Ensino Superior - Engenharia de Software, 2o Periodo  
Avaliacao P2 - Prof. Samuel Oliveira (samuel.oliveira@grupocev.com)  
Meta de Conclusao da Equipe: Sexta-feira, 20 de Novembro de 2026  
Apresentacoes: 23 e 26 de Novembro de 2026

---

## 1. Equipe e Divisao de Trabalho

| Integrante | Branch | Modulos e Responsabilidades |
| :--- | :--- | :--- |
| **Joao Guilherme Monteiro** | `dev_joao` | Catalogo de Produtos, Estoque (baixa automatica e alerta < 5 un.), Persistencia CSV e Tratamento de Erros. |
| **Maria Vitoria Salgado** | `dev_vitoria` | Modelagem de Clientes e VIP (Heranca), Sistema de Pontuacao e Programa de Cashback. |
| **Lucas Silva** | `dev_lucas` | Carrinho, Pedidos, Cupons de Desconto, Pagamentos (Polimorfismo) e Relatorio de Vendas. |

---

## 2. Como Executar

O projeto foi construido utilizando exclusivamente a biblioteca padrao do Java, sem necessidade de Maven, Gradle ou bibliotecas externas.

### Execucao Rapida (Scripts)

```bash
./run.sh          # Linux e macOS
run.bat           # Windows
```

### Execucao Manual via Terminal

```bash
# Compilar todos os arquivos Java
mkdir -p out
find src -name "*.java" -print0 | xargs -0 javac -d out

# Executar a aplicacao
java -cp out Main
```

**Requisitos:** JDK 17 ou superior instalado (testado e homologado no OpenJDK 21 e OpenJDK 26).

---

## 3. Estrutura do Repositorio

```
ShopOnline/
├── .idea/                              # Configuracoes do projeto no IntelliJ IDEA
├── ShopOnline.iml                      # Modulo Java do IntelliJ IDEA
├── data/                               # Fonte unica de dados persistidos em CSV
│   ├── produtos.csv                    # Inventario e controle de estoque
│   ├── clientes.csv                    # Clientes, pontos e saldos de cashback
│   ├── cupons.csv                      # Cupons cadastrados e controle de uso
│   └── pedidos.csv                     # Historico consolidado de pedidos
├── docs/                               # Documentacao de apoio
│   ├── Divisao_Tarefas_P2.pdf          # PDF oficial de distribuicao de tarefas
│   └── CHECKLIST_DEV_JOAO.md           # Guia de verificacao do modulo do Joao
├── relatorio/                          # Documentacao academica para a entrega
│   ├── relatorio-decisoes.md           # Registro formal das decisoes de arquitetura OO
│   └── relatorio-tecnico.md            # Relatorio tecnico completo (4+ paginas)
├── src/                                # Codigo-fonte da aplicacao Java
│   ├── Main.java                       # Ponto de entrada, testes e fluxo demonstrativo
│   ├── model/                          # Entidades do dominio
│   │   ├── Cliente.java                # Modelo de cliente com carteira de cashback
│   │   ├── ClienteVip.java             # Heranca de Cliente com 10% desconto em itens VIP
│   │   ├── NivelFidelidade.java        # Enum (Bronze, Prata, Ouro) com percentuais
│   │   ├── Produto.java                # Modelo com regra de alerta de reposicao (< 5)
│   │   ├── ItemCarrinho.java           # Item associado ao carrinho com subtotal
│   │   ├── Carrinho.java               # Agregacao de itens, cupons e totalizadores
│   │   ├── Cupom.java                  # Cupom com validacao de expiracao e uso unico
│   │   ├── TipoCupom.java              # Enum (PERCENTUAL ou VALOR_FIXO)
│   │   ├── StatusPedido.java           # Enum de estados (EM_PROCESSAMENTO, ENVIADO, etc.)
│   │   ├── Pedido.java                 # Registro definitivo e regra de cancelamento seguro
│   │   └── pagamento/                  # Camada de pagamentos polimorficos
│   │       ├── FormaPagamento.java     # Interface de contrato financeiro
│   │       ├── PagamentoPix.java       # Implementacao com 5% de desconto
│   │       ├── PagamentoCartao.java    # Implementacao em ate 10x sem juros
│   │       └── PagamentoBoleto.java    # Implementacao via boleto bancario
│   ├── repository/                     # Camada de persistencia em arquivos
│   │   ├── ProdutoRepository.java      # Leitura e escrita no produtos.csv
│   │   └── ClienteRepository.java      # Leitura e escrita no clientes.csv
│   ├── service/                        # Camada de servicos e regras de negocio
│   │   ├── EstoqueService.java         # Gestao de baixa e alerta preventivo de reposicao
│   │   ├── FidelidadeService.java      # Calculo de pontos e acumulo de saldo
│   │   └── VendaService.java           # Checkout, validacoes e emissao de relatorios
│   └── teste/                          # Suites de verificacao de modulos
│       └── VerificacaoModuloJoao.java   # Suite de testes e checklist do Joao
├── run.sh                              # Script de compilacao e execucao para Linux/macOS
├── run.bat                             # Script de compilacao e execucao para Windows
└── README.md                           # Guia principal do projeto
```

---

## 4. Contrato dos Arquivos de Dados (data/)

Os arquivos sao armazenados em formato CSV compartilhado, com as seguintes regras de padronizacao:
* **Codificacao:** UTF-8 sem BOM.
* **Separador de campos:** Ponto e virgula (`;`).
* **Separador decimal:** Ponto (`.`) obrigatorio para valores monetarios (`4500.00`, nunca virgula).
* **Cabecalho:** Fixo na linha 1 de cada arquivo.

| Arquivo | Colunas | Descricao |
| :--- | :--- | :--- |
| `produtos.csv` | `id;nome;descricao;preco;estoque;promocaoVip` | Catalogo de produtos, unidades e itens VIP. |
| `clientes.csv` | `id;nome;email;pontos;saldoCashback;vip` | Clientes, pontuacao acumulada e saldo de cashback. |
| `cupons.csv` | `codigo;tipo;valor;dataValidade;utilizado` | Cupons cadastrados e controle de uso unico. |
| `pedidos.csv` | `id;clienteId;subtotal;desconto;totalFinal;formaPagamento;status;dataCriacao;cashbackGerado` | Historico consolidado de pedidos e faturamento. |

---

## 5. Principios de POO Justificados

1. **Abstracao:** Isolamento das entidades do mundo real (`Produto`, `Cliente`, `Carrinho`, `Pedido`, `Cupom`) com atributos e metodos focados exclusivamente nas necessidades do e-commerce.
2. **Encapsulamento:** Todos os atributos sao privados. Metodos de manipulacao contem validacoes de negocio (ex.: o estoque nunca fica negativo, o saldo de cashback nao pode ser alterado diretamente sem transacao).
3. **Heranca:** `ClienteVip` herda de `Cliente`, estendendo seu comportamento para calcular automaticamente 10% de desconto em produtos marcados em promocao VIP.
4. **Polimorfismo:** A interface `FormaPagamento` possui implementacoes concretas (`PagamentoPix`, `PagamentoCartao`, `PagamentoBoleto`). O `VendaService` efetua o calculo final chamando `formaPagamento.calcularValorFinal(total)` sem condicionais `if/else` ou dependencias de regras especificas de cada meio.

---

## 6. Regras de Negocio e Casos Especiais

* **Controle de Estoque e Alerta:** Baixa automatica a cada venda. Caso o saldo apos a venda fique com **menos de 5 unidades**, o sistema emite automaticamente o alerta `[ALERTA DE REPOSICAO]`.
* **Cancelamento Seguro:** O cancelamento de um pedido so e autorizado se o seu status for `EM_PROCESSAMENTO`. Ao cancelar, todos os produtos sao estornados imediatamente ao estoque. Pedidos ja enviados ou entregues nao podem ser cancelados.
* **Programa de Fidelidade e Cashback:**
  * Compras acima de R$ 100,00 geram 1 ponto a cada R$ 10,00.
  * Faixas: **Bronze** (ate 499 pts -> 2% cashback), **Prata** (500 a 1.499 pts -> 5% cashback), **Ouro** (1.500+ pts -> 8% cashback).
  * O cashback acumulado fica disponivel na carteira do cliente para compras futuras.
* **Cupons:** Suporte a descontos percentuais e em valor fixo, com verificacao de data de validade e controle de uso unico por cliente.

---

## 7. Git Flow da Equipe e Cronograma (Meta: Sexta-feira, 20/11)

* `main`: Codigo final homologado, estavel para entrega e apresentacao.
* `developer`: Branch de integracao onde os Pull Requests das branches individuais sao testados em conjunto.
* `dev_joao`: Desenvolvimento das features atribuidas ao Joao.
* `dev_vitoria`: Desenvolvimento das features atribuidas a Vitoria.
* `dev_lucas`: Desenvolvimento das features atribuidas ao Lucas.

### Cronograma Fatal:
* **Ate Quarta (18/11):** Conclusao das implementacoes individuais em cada branch.
* **Ate Quinta (19/11):** Merges integrados na branch `developer` e realizacao de testes conjuntos.
* **Sexta-feira (20/11):** **Finalizacao obrigatoria de todo o projeto**, merge final na `main`, fechamento do relatorio tecnico (PDF 4+ paginas) e checklist de entrega.
* **23 e 26/11:** Apresentacoes e defesas orais.

---

## 8. Documentacao Academica

* [Relatorio de Decisoes de Projeto](file:///home/joaomonteiro/Documents/Engenharia%20de%20Software/2%C2%B0%20Per%C3%ADodo/Programa%20Orientada%20a%20Objetos/ShopOnline/relatorio/relatorio-decisoes.md)
* [Relatorio Tecnico da P2](file:///home/joaomonteiro/Documents/Engenharia%20de%20Software/2%C2%B0%20Per%C3%ADodo/Programa%20Orientada%20a%20Objetos/ShopOnline/relatorio/relatorio-tecnico.md)
* [PDF de Divisao de Tarefas](file:///home/joaomonteiro/Documents/Engenharia%20de%20Software/2%C2%B0%20Per%C3%ADodo/Programa%20Orientada%20a%20Objetos/ShopOnline/docs/Divisao_Tarefas_P2.pdf)
