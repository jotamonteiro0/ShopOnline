---
title: "Relatório de Decisões de Projeto e Arquitetura OO"
subtitle: "Sistema de E-Commerce ShopOnline — Avaliação P2"
author:
  - "João Guilherme Monteiro"
  - "Maria Vitória Salgado"
  - "Lucas Silveira"
  - "Gabriel Buhatem"
  - "Lavynne Marianne"
date: "Teresina — PI, Novembro de 2026"
lang: pt-BR
---

**iCEV — Instituto de Ensino Superior** · Engenharia de Software, 2º Período  
Disciplina: Programação Orientada a Objetos · Prof. Samuel Oliveira

| Identificação | Detalhes |
| :--- | :--- |
| **Disciplina** | Programação Orientada a Objetos |
| **Professor** | Samuel Oliveira (`samuel.oliveira@grupocev.com`) |
| **Avaliação** | Trabalho Prático da P2 |
| **Data Limite Fatal** | Sexta-feira, 20 de novembro de 2026 |
| **Data das Apresentações** | 23 e 26 de novembro de 2026 |
| **Equipe (5 Integrantes)** | João Guilherme Monteiro, Maria Vitória Salgado, Lucas Silveira, Gabriel Buhatem, Lavynne Marianne |
| **Repositório GitHub** | [github.com/jotamonteiro0/ShopOnline](https://github.com/jotamonteiro0/ShopOnline) |

---

## 1. Apresentação e Escopo

Este relatório formaliza a arquitetura, as decisões técnicas de modelagem orientada a objetos, a estratégia de persistência de dados e a divisão de trabalho entre os integrantes da equipe para o desenvolvimento do sistema **ShopOnline**.

O sistema atende integralmente ao problema proposto para a loja virtual, que inclui:
1. Catálogo de produtos com controle de estoque e alertas preventivos de reposição.
2. Cadastro de clientes com diferenciação para clientes VIP.
3. Programa progressivo de fidelidade e geração de cashback acumulativo.
4. Carrinho de compras com aplicação dinâmica de descontos e gestão de cupons.
5. Processamento polimórfico de múltiplos meios de pagamento (PIX, Cartão e Boleto).
6. Controle de pedidos e cancelamento restrito com reposição automática de itens.
7. Emissão de relatórios gerenciais consolidados de vendas.

---

## 2. Decisão 1 — Arquitetura e Organização de Pacotes

Para garantir alta coesão, baixo acoplamento e facilitar o desenvolvimento paralelo por múltiplos desenvolvedores, adotamos uma organização em camadas limpa e explícita:

```
src/
├── model/           # Entidades centrais do domínio e regras de negócio encapsuladas
│   └── pagamento/   # Abstrações e implementações polimórficas de pagamento
├── repository/      # Camada de acesso e persistência a arquivos CSV
├── service/         # Serviços de orquestração de regras de negócio
└── view/            # Interfaces e fluxos de interação com o usuário
```

**Justificativa:** O isolamento da camada de modelo (`model`) em relação ao armazenamento (`repository`) e à interface (`view`) impede vazamento de dependências e permite que regras de desconto ou cálculo de pontos sejam testadas e evoluídas de maneira independente.

---

## 3. Decisão 2 — Aplicação Justificada dos Pilares da POO

O edital da P2 exige a aplicação justificada dos quatro pilares fundamentais da Orientação a Objetos. As decisões tomadas foram:

### 3.1 Abstração
Modelagem precisa do mundo real de um e-commerce em entidades de software com responsabilidades bem delimitadas:
* `Produto`: Representa itens comercializáveis, suas propriedades de preço, disponibilidade e condição promocional.
* `Cliente`: Representa os compradores, mantendo o histórico de fidelidade e carteira de cashback.
* `Carrinho` e `ItemCarrinho`: Isola o estado temporário da compra antes da conversão em pedido formal.
* `Pedido`: Registro definitivo da transação, congelando preços, descontos obtidos e status logístico.

### 3.2 Encapsulamento
Todos os atributos das classes de modelo são privados (`private`). O acesso e a mutação são intermediados por métodos com validações de integridade:
* A alteração de estoque é feita exclusivamente por `reduzirEstoque(int)` e `adicionarEstoque(int)`, garantindo que o estoque jamais fique negativo.
* O saldo de cashback não pode ser editado arbitrariamente; apenas métodos de crédito (`creditarCashback`) ou débito autenticado (`debitarCashback`) podem operá-lo.
* O cupom possui controle de uso (`marcarComoUtilizado()`) e verificação de expiração temporal (`isValido()`).

### 3.3 Herança
A relação `ClienteVip extends Cliente` foi aplicada de maneira justificada:
* **Motivo:** Um cliente VIP compartilha todas as características, atributos e comportamentos de um cliente comum (id, nome, email, pontuação, carteira de cashback), mas introduz comportamento especializado: elegibilidade automática para 10% de abatimento em produtos catalogados como promoção VIP.
* O método `calcularDescontoVip(Produto)` é polimórfico na hierarquia de clientes.

### 3.4 Polimorfismo
A interface `FormaPagamento` define o contrato universal de liquidação:
```java
public interface FormaPagamento {
    String getNome();
    double calcularValorFinal(double valorTotal);
    String getDescricao();
}
```
* **Implementações concretas:**
  1. `PagamentoPix`: Aplica o desconto legal de 5% sobre o total.
  2. `PagamentoCartao`: Permite parcelamento em até 10x sem juros mantendo o valor facial.
  3. `PagamentoBoleto`: Emite a cobrança com vencimento bancário.
* **Vantagem:** O `VendaService` não conhece as particularidades financeiras de cada meio; ele simplesmente invoca `formaPagamento.calcularValorFinal(totalIntermediario)`. Novas modalidades (ex.: Criptomoeda, Apple Pay) podem ser adicionadas sem alterar uma única linha do serviço de vendas (princípio Open/Closed).

---

## 4. Decisão 3 — Divisão de Trabalho em Equipe (5 Integrantes)

Para evitar conflitos de merge e maximizar a autonomia, o projeto foi dividido em módulos verticais, com branches individuais no Git Flow:

| Integrante | Branch | Módulos Principais | Responsabilidade & Entregáveis |
| :--- | :--- | :--- | :--- |
| **João Guilherme Monteiro** | `dev_joao` | Produtos, Estoque e Persistência CSV Base | Modelagem de `Produto`, `EstoqueService`, alerta de reposição (< 5 un.), persistência base (`ProdutoRepository`), tratamento robusto de arquivos e arquitetura. |
| **Maria Vitória Salgado** | `dev_vitoria` | Clientes, Especialização VIP e Validações | Modelagem de `Cliente` e `ClienteVip` (herança), validações cadastrais de integridade e persistência de clientes em `clientes.csv`. |
| **Lucas Silveira** | `dev_lucas` | Carrinho, Cupons de Desconto e Promoções | Modelagem de `Carrinho`, `ItemCarrinho`, `Cupom`, `TipoCupom`, persistência de cupons e regra de uso único por cliente. |
| **Gabriel Buhatem** | `dev_gabriel` | Meios de Pagamento, Ciclo de Pedidos e Cancelamento | Implementações polimórficas de `FormaPagamento` (PIX 5%, Cartão até 10x, Boleto), `Pedido`, ciclo de status e cancelamento seguro com estorno em disco. |
| **Lavynne Marianne** | `dev_lavynne` | Fidelidade, Cashback, Relatório de Vendas e Menu | `FidelidadeService`, acúmulo e método de uso do cashback como crédito (`usarCashback`), relatórios gerenciais e organização do `MenuPrincipal`. |
| **Equipe Conjunta** | `developer` e `main` | Integração, Testes End-to-End e Relatório Final | Testes integrados, code review, elaboração das seções individuais do Relatório Acadêmico (PDF 4+ págs) e ensaio da apresentação oral. |

---

## 5. Decisão 4 — Contrato de Persistência em Arquivos

Seguindo a diretriz obrigatória de persistência em arquivos sem dependências pesadas, padronizamos arquivos CSV na pasta `data/`:
* **Codificação:** UTF-8 estrito, sem BOM.
* **Separador de campos:** Ponto e vírgula (`;`) para evitar conflitos com descrições em texto.
* **Ponto flutuante:** Sempre ponto (`.` - ex: `4500.00`), utilizando `Locale.US` em todas as conversões numéricas.
* **Cabeçalhos obrigatórios:** Na primeira linha de cada arquivo.

| Arquivo | Colunas | Finalidade |
| :--- | :--- | :--- |
| `data/produtos.csv` | `id;nome;descricao;preco;estoque;promocaoVip` | Catálogo de produtos e controle de estoque |
| `data/clientes.csv` | `id;nome;email;pontos;saldoCashback;vip` | Base de clientes, fidelidade, saldos e status VIP |
| `data/cupons.csv` | `codigo;tipo;valor;dataValidade;utilizado` | Cupons cadastrados e controle de utilização |
| `data/pedidos.csv` | `id;clienteId;subtotal;desconto;totalFinal;formaPagamento;status;dataCriacao;cashbackGerado` | Histórico consolidado de pedidos |

---

## 6. Decisão 5 — Regras de Negócio e Tratamento de Exceções

O sistema implementa regras defensivas para impedir falhas de execução:
1. **Estoque Negativo:** Verificação prévia no momento de adicionar ao carrinho e na confirmação do pedido (`IllegalStateException` tratada).
2. **Alerta de Reposição:** Caso a baixa reduza o estoque para menos de 5 unidades, o sistema dispara um aviso em console para o gestor.
3. **Cancelamento Seguro:** O cancelamento só é autorizado se o pedido estiver com status `EM_PROCESSAMENTO`. Ao cancelar, todos os itens têm seu estoque devolvido automaticamente. Pedidos com status `ENVIADO` ou `ENTREGUE` têm o cancelamento bloqueado.
4. **Arquivos Ausentes:** Se um arquivo CSV for deletado, a aplicação recria o arquivo e o diretório automaticamente com o cabeçalho padrão, sem quebrar o fluxo.

---

## 7. Cronograma e Prazos

| Etapa | Data Limite | Entregável / Ação |
| :--- | :--- | :--- |
| **Setup & Estrutura Base** | 16/11/2026 | Repositório criado, branches configuradas, arquitetura base e scripts operacionais. |
| **Desenvolvimento Individual (5 Branches)** | 18/11/2026 | Conclusão das features nas branches `dev_joao`, `dev_vitoria`, `dev_lucas`, `dev_gabriel` e `dev_lavynne`. |
| **Integração na Branch `developer`** | 19/11/2026 | Pull Requests abertos, testes integrados de ponta a ponta e ajustes de interface. |
| **Finalização e Entrega (Prazo Fatal)** | 20/11/2026 | Merge final na `main`, fechamento do relatório acadêmico de 4 páginas e checklist de entrega. |
| **Ensaio e Revisão** | 21 e 22/11/2026 | Ensaio da apresentação oral e revisão final antes das bancas. |
| **Defesa Oral perante a Banca** | 23 e 26/11/2026 | Apresentação ao vivo do sistema em funcionamento. |
