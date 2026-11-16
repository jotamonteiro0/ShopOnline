# Trabalho Prático — Programação Orientada a Objetos (P2)

Implementação do sistema de comércio eletrônico **ShopOnline**, aplicando os princípios fundamentais da Orientação a Objetos (Abstração, Encapsulamento, Herança e Polimorfismo), persistência em arquivos CSV, tratamento de exceções e boas práticas de arquitetura.

**iCEV — Instituto de Ensino Superior** · Engenharia de Software, 2º Período  
Avaliação P2 · Prof. Samuel Oliveira (`samuel.oliveira@grupocev.com`)  
Apresentações: **23 e 26 de Novembro de 2026**

---

## Equipe e Divisão de Trabalho

| Integrante | Branch | Módulos e Responsabilidades |
| :--- | :--- | :--- |
| **João Guilherme Monteiro** | `dev_joao` | Catálogo de Produtos, Estoque (baixa e alerta < 5 un.), Persistência CSV e Tratamento de Erros. |
| **Maria Vitória Salgado** | `dev_vitoria` | Modelagem de Clientes e VIP (Herança), Sistema de Pontuação e Programa de Cashback. |
| **Lucas Silva** | `dev_lucas` | Carrinho, Pedidos, Cupons de Desconto, Pagamentos (Polimorfismo) e Relatório de Vendas. |

---

## 🚀 Como Executar

O projeto foi construído utilizando **exclusivamente a biblioteca padrão do Java**, sem necessidade de Maven, Gradle ou bibliotecas externas.

### Execução Rápida (Scripts)

```bash
./run.sh          # Linux e macOS
run.bat           # Windows
```

### Execução Manual via Terminal

```bash
# Compilar todos os arquivos Java
mkdir -p out
find src -name "*.java" -print0 | xargs -0 javac -d out

# Executar a aplicação
java -cp out Main
```

**Requisitos:** JDK 17 ou superior instalado (testado e homologado no OpenJDK 21).

---

## Estrutura do Repositório

```
ShopOnline/
├── data/                               # Fonte única de dados persistidos em CSV
│   ├── produtos.csv                    # Inventário e controle de estoque
│   ├── clientes.csv                    # Clientes, pontos e saldos de cashback
│   ├── cupons.csv                      # Cupons cadastrados e controle de uso
│   └── pedidos.csv                     # Histórico consolidado de pedidos
├── docs/                               # Documentação de apoio
│   └── Divisao_Tarefas_P2.pdf          # PDF oficial de distribuição de tarefas
├── relatorio/                          # Documentação acadêmica para a entrega
│   ├── relatorio-decisoes.md           # Registro formal das decisões de arquitetura OO
│   └── relatorio-tecnico.md            # Relatório técnico completo (4+ páginas)
├── src/                                # Código-fonte da aplicação Java
│   ├── Main.java                       # Ponto de entrada, testes e fluxo demonstrativo
│   ├── model/                          # Entidades do domínio
│   │   ├── Cliente.java                # Modelo de cliente com carteira de cashback
│   │   ├── ClienteVip.java             # Herança de Cliente com 10% desconto em itens VIP
│   │   ├── NivelFidelidade.java        # Enum (Bronze, Prata, Ouro) com percentuais
│   │   ├── Produto.java                # Modelo com regra de alerta de reposição (< 5)
│   │   ├── ItemCarrinho.java           # Item associado ao carrinho com subtotal
│   │   ├── Carrinho.java               # Agregação de itens, cupons e totalizadores
│   │   ├── Cupom.java                  # Cupom com validação de expiração e uso único
│   │   ├── TipoCupom.java              # Enum (PERCENTUAL ou VALOR_FIXO)
│   │   ├── StatusPedido.java           # Enum de estados (EM_PROCESSAMENTO, ENVIADO, etc.)
│   │   ├── Pedido.java                 # Registro definitivo e regra de cancelamento seguro
│   │   └── pagamento/                  # Camada de pagamentos polimórficos
│   │       ├── FormaPagamento.java     # Interface de contrato financeiro
│   │       ├── PagamentoPix.java       # Implementação com 5% de desconto
│   │       ├── PagamentoCartao.java    # Implementação em até 10x sem juros
│   │       └── PagamentoBoleto.java    # Implementação via boleto bancário
│   ├── repository/                     # Camada de persistência em arquivos
│   │   ├── ProdutoRepository.java      # Leitura e escrita no produtos.csv
│   │   └── ClienteRepository.java      # Leitura e escrita no clientes.csv
│   └── service/                        # Camada de serviços e regras de negócio
│       ├── EstoqueService.java         # Gestão de baixa e alerta preventivo de reposição
│       ├── FidelidadeService.java      # Cálculo de pontos e acúmulo de saldo
│       └── VendaService.java           # Checkout, validações e emissão de relatórios
├── run.sh                              # Script de compilação e execução para Linux/macOS
├── run.bat                             # Script de compilação e execução para Windows
└── README.md                           # Guia principal do projeto
```

---

## Contrato dos Arquivos de Dados (`data/`)

Os arquivos são armazenados em formato CSV compartilhado, com as seguintes regras de padronização:
* **Codificação:** UTF-8 sem BOM.
* **Separador de campos:** Ponto e vírgula (`;`).
* **Separador decimal:** Ponto (`.`) obrigatório para valores monetários (`4500.00`, nunca vírgula).
* **Cabeçalho:** Fixo na linha 1 de cada arquivo.

| Arquivo | Colunas | Descrição |
| :--- | :--- | :--- |
| `produtos.csv` | `id;nome;descricao;preco;estoque;promocaoVip` | Catálogo de produtos, unidades e itens VIP. |
| `clientes.csv` | `id;nome;email;pontos;saldoCashback;vip` | Clientes, pontuação acumulada e saldo de cashback. |
| `cupons.csv` | `codigo;tipo;valor;dataValidade;utilizado` | Cupons cadastrados e controle de uso único. |
| `pedidos.csv` | `id;clienteId;subtotal;desconto;totalFinal;formaPagamento;status;dataCriacao;cashbackGerado` | Histórico consolidado de pedidos e faturamento. |

---

## 🧱 Princípios de POO Justificados

1. **Abstração:** Isolamento das entidades do mundo real (`Produto`, `Cliente`, `Carrinho`, `Pedido`, `Cupom`) com atributos e métodos focados exclusivamente nas necessidades do e-commerce.
2. **Encapsulamento:** Todos os atributos são privados. Métodos de manipulação contêm validações de negócio (ex.: o estoque nunca fica negativo, o saldo de cashback não pode ser alterado diretamente sem transação).
3. **Herança:** `ClienteVip` herda de `Cliente`, estendendo seu comportamento para calcular automaticamente 10% de desconto em produtos marcados em promoção VIP.
4. **Polimorfismo:** A interface `FormaPagamento` possui implementações concretas (`PagamentoPix`, `PagamentoCartao`, `PagamentoBoleto`). O `VendaService` efetua o cálculo final chamando `formaPagamento.calcularValorFinal(total)` sem precisar de condicionais `if/else` ou conhecer detalhes de cada bandeira.

---

## Regras de Negócio e Casos Especiais

* **Controle de Estoque & Alerta:** Baixa automática a cada venda. Caso o saldo após a venda fique com **menos de 5 unidades**, o sistema emite automaticamente o alerta `[ALERTA DE REPOSIÇÃO]`.
* **Cancelamento Seguro:** O cancelamento de um pedido só é autorizado se o seu status for `EM_PROCESSAMENTO`. Ao cancelar, todos os produtos são estornados imediatamente ao estoque. Pedidos já enviados ou entregues não podem ser cancelados.
* **Programa de Fidelidade e Cashback:**
  * Compras acima de R$ 100,00 geram 1 ponto a cada R$ 10,00.
  * Tiers: **Bronze** (até 499 pts $\rightarrow$ 2% cashback), **Prata** (500 a 1.499 pts $\rightarrow$ 5% cashback), **Ouro** (1.500+ pts $\rightarrow$ 8% cashback).
  * O cashback acumulado fica disponível na carteira do cliente para compras futuras.
* **Cupons:** Suporte a descontos percentuais e em valor fixo, com verificação de data de validade e controle de uso único por cliente.

---

## Git Flow da Equipe

* `main`: Código final homologado, estável para entrega e apresentação.
* `developer`: Branch de integração onde os Pull Requests das branches individuais são testados em conjunto.
* `dev_joao`: Desenvolvimento das features atribuídas ao João.
* `dev_vitoria`: Desenvolvimento das features atribuídas à Vitória.
* `dev_lucas`: Desenvolvimento das features atribuídas ao Lucas.

---

## Documentação Acadêmica

* [Relatório de Decisões de Projeto](file:///home/joaomonteiro/Documents/Engenharia%20de%20Software/2%C2%B0%20Per%C3%ADodo/Programa%20Orientada%20a%20Objetos/ShopOnline/relatorio/relatorio-decisoes.md)
* [Relatório Técnico da P2](file:///home/joaomonteiro/Documents/Engenharia%20de%20Software/2%C2%B0%20Per%C3%ADodo/Programa%20Orientada%20a%20Objetos/ShopOnline/relatorio/relatorio-tecnico.md)
* [PDF de Divisão de Tarefas](file:///home/joaomonteiro/Documents/Engenharia%20de%20Software/2%C2%B0%20Per%C3%ADodo/Programa%20Orientada%20a%20Objetos/ShopOnline/docs/Divisao_Tarefas_P2.pdf)
