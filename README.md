# ShopOnline - Sistema de E-Commerce (POO - P2)

Sistema de e-commerce desenvolvido em Java para a avaliação da segunda etapa (P2) da disciplina de **Programação Orientada a Objetos** do curso de Engenharia de Software (iCEV).

## 👥 Equipe
* **João Guilherme Monteiro** (`dev_joao`)
* **Maria Vitória Salgado** (`dev_vitoria`)
* **Lucas Silva** (`dev_lucas`)

---

## 🛠️ Princípios de Orientação a Objetos Aplicados

1. **Abstração**: Modelagem dos domínios centrais do e-commerce: `Produto`, `Cliente`, `Carrinho`, `Pedido`, `Cupom` e `FormaPagamento`.
2. **Encapsulamento**: Atributos privados protegidos com métodos de acesso e mutação controlados, além de regras de negócio validadas dentro das próprias entidades (ex.: integridade de estoque, saldo de cashback e validação de cupons).
3. **Herança**: A classe `ClienteVip` herda de `Cliente`, estendendo suas capacidades para descontos exclusivos em itens promocionais.
4. **Polimorfismo**: A interface `FormaPagamento` possui diferentes implementações concretas (`PagamentoPix`, `PagamentoCartao`, `PagamentoBoleto`), cada uma calculando os valores finais e regras financeiras de forma dinâmica.

---

## 📌 Funcionalidades Principais

- **Catálogo de Produtos e Estoque:** Baixa automática a cada venda efetuada e disparo de alerta de reposição quando o estoque for inferior a 5 unidades.
- **Carrinho e Gestão de Pedidos:** Aplicação automática de cupons e descontos VIP. Cancelamento de pedidos permitido apenas em status `EM_PROCESSAMENTO` com estorno automático ao estoque.
- **Fidelidade & Cashback:**
  - Acúmulo de 1 ponto a cada R$ 10,00 em compras acima de R$ 100,00.
  - Níveis de fidelidade: **Bronze** (2%), **Prata** (5%) e **Ouro** (8%).
  - Crédito de cashback acumulado para compras futuras.
- **Formas de Pagamento:**
  - PIX (5% de desconto automático).
  - Cartão de Crédito (até 10x sem juros).
  - Boleto Bancário.
- **Persistência de Dados:** Arquivos CSV em `data/` para produtos e clientes.

---

## 🚀 Como Executar

### Pré-requisitos
* Java JDK 17 ou superior (testado com Java 21).

### Compilação e Execução
Na raiz do projeto:

```bash
# Compilar os arquivos Java
javac -d out $(find src -name "*.java")

# Executar a aplicação
java -cp out Main
```

---

## 🌿 Estrutura de Branches no Git

* `main`: Código de produção consolidado e estável para a entrega e apresentação.
* `developer`: Branch de integração contínua do time.
* `dev_joao`: Desenvolvimento do módulo de Produtos, Estoque e Persistência CSV.
* `dev_vitoria`: Desenvolvimento do módulo de Clientes, VIP, Fidelidade e Cashback.
* `dev_lucas`: Desenvolvimento do módulo de Carrinho, Pedidos, Cupons e Pagamentos.
