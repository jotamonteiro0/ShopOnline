---
title: "Relatório Técnico — Sistema de E-Commerce ShopOnline"
subtitle: "Documentação de Engenharia e POO — Avaliação P2"
author:
  - "João Guilherme Monteiro"
  - "Maria Vitória Salgado"
  - "Lucas Silveira"
  - "Gabriel Buhatem"
  - "Lavynne Marianne"
date: "Teresina — PI, Novembro de 2026"
lang: pt-BR
---

# Relatório Técnico — Sistema ShopOnline

**iCEV — Instituto de Ensino Superior**  
Curso: Bacharelado em Engenharia de Software — 2º Período  
Disciplina: Programação Orientada a Objetos (POO)  
Professor: Samuel Oliveira (`samuel.oliveira@grupocev.com`)

---

## 1. Introdução e Visão Geral do Sistema

O **ShopOnline** é uma plataforma de comércio eletrônico desenvolvida em Java orientada a objetos que resolve a necessidade de gestão de vendas, estoques e fidelização de clientes de uma loja virtual contemporânea.

### 1.1 Problema Resolvido
O sistema integra os seguintes processos críticos de negócio:
1. **Gestão de Catálogo e Estoque Ativo:** Monitoramento contínuo de unidades em tempo real, prevenção de vendas sem estoque e sinalização antecipada de reposição (< 5 unidades).
2. **Ciclo de Compra e Descontos Cumulativos:** O carrinho de compras calcula automaticamente abatimentos para clientes VIP em itens participantes somados a cupons promocionais válidos.
3. **Engajamento e Cashback:** Incentivo a compras recorrentes transformando valor transacionado em pontos e cashback acumulado na carteira virtual do usuário.
4. **Múltiplos Meios de Pagamento:** Simulação de fluxos com vantagens competitivas (ex.: 5% de desconto no PIX e parcelamento no Cartão de Crédito).
5. **Auditoria de Vendas:** Rastreamento do ciclo logístico dos pedidos (`EM_PROCESSAMENTO` $\rightarrow$ `ENVIADO` $\rightarrow$ `ENTREGUE`), autorizando cancelamento com restituição imediata ao estoque apenas enquanto não despachado.

---

## 2. Modelagem Orientada a Objetos e Diagrama de Classes

A arquitetura do projeto foi desenhada visando aderência aos padrões de responsabilidade única e desacoplamento.

```
       +-----------------------+
       |        Cliente        |
       +-----------------------+
       | - id: String          |
       | - nome: String        |
       | - email: String       |
       | - pontos: int         |
       | - saldoCashback: dbl  |
       +-----------------------+
                   ^
                   | (Herança)
       +-----------------------+
       |      ClienteVip       |
       +-----------------------+
       | + calcularDescVip()   |
       +-----------------------+

       +------------------------------------+
       |       <<interface>>                |
       |       FormaPagamento               |
       +------------------------------------+
       | + calcularValorFinal(total): double|
       | + getDescricao(): String           |
       +------------------------------------+
              ^            ^           ^
              |            |           | (Polimorfismo)
     +-------------+ +-------------+ +---------------+
     |PagamentoPix | |PagamentoCard| |PagamentoBoleto|
     +-------------+ +-------------+ +---------------+
```

---

## 3. Implementação dos Princípios de POO

### 3.1 Abstração
As entidades do domínio contêm apenas as propriedades e métodos estritamente necessários para o funcionamento das regras de e-commerce:
* `Produto`: Contém identificador, nome, descrição, preço unitário, contagem em estoque e sinalização de participação em promoção VIP.
* `Cupom`: Encapsula código, modalidade (percentual ou fixo), valor de corte, data limite de expiração e estado de consumo.

### 3.2 Encapsulamento
A integridade dos estados internos é garantida por invariantes validadas dentro da própria classe:
```java
public boolean reduzirEstoque(int quantidade) {
    if (quantidade > 0 && this.quantidadeEstoque >= quantidade) {
        this.quantidadeEstoque -= quantidade;
        return true;
    }
    return false;
}
```

### 3.3 Herança
A classe `ClienteVip` reutiliza 100% da lógica e persistência de `Cliente`, redefinindo apenas os métodos pertinentes ao tratamento especial de descontos:
```java
public class ClienteVip extends Cliente {
    private static final double DESCONTO_PROMOCAO_VIP = 0.10; // 10%

    public ClienteVip(String id, String nome, String email, int pontos, double saldo) {
        super(id, nome, email, pontos, saldo);
    }

    @Override
    public boolean isVip() {
        return true;
    }

    @Override
    public double calcularDescontoVip(Produto produto) {
        if (produto != null && produto.isPromocaoVip()) {
            return produto.getPreco() * DESCONTO_PROMOCAO_VIP;
        }
        return 0.0;
    }
}
```

### 3.4 Polimorfismo
O checkout manipula pagamentos de forma totalmente polimórfica:
```java
double valorFinal = formaPagamento.calcularValorFinal(totalIntermediario);
```
O serviço desconhece se o pagamento é PIX, Boleto ou Cartão; cada implementação calcula seu respectivo abatimento ou acréscimo de forma independente.

---

## 4. Persistência de Dados e Tratamento de Exceções

### 4.1 Persistência em Arquivos CSV
O armazenamento foi implementado na camada `repository` sem necessidade de bibliotecas de terceiros (usando `BufferedReader` e `BufferedWriter` da biblioteca padrão):
* `data/produtos.csv`: Tabela de inventário.
* `data/clientes.csv`: Base de clientes e carteira de cashback.
* `data/cupons.csv`: Tabela de cupons e validade.
* `data/pedidos.csv`: Histórico consolidado de vendas.

### 4.2 Robustez e Tolerância a Falhas
* Diretórios e arquivos ausentes são gerados automaticamente na inicialização com seus respectivos cabeçalhos.
* Parsing numérico configurado com `Locale.US` para evitar falhas com separadores decimais.
* Operações no carrinho validam disponibilidade de estoque previamente.

---

## 5. Divisão de Atividades da Equipe (5 Integrantes)

| Integrante | Módulos e Funcionalidades Desenvolvidas |
| :--- | :--- |
| **João Guilherme Monteiro** | • Estrutura de pacotes, setup base e arquitetura do repositório.<br>• Modelagem de `Produto`, `EstoqueService` e alertas preventivos (< 5 un.).<br>• Camada de persistência CSV em arquivos com sanitização de entradas.<br>• Scripts de compilação multiplataforma (`run.sh` e `run.bat`). |
| **Maria Vitória Salgado** | • Hierarquia de clientes: `Cliente` e `ClienteVip` (herança).<br>• Validações cadastrais de integridade e persistência de clientes.<br>• Implementação de desconto exclusivo de 10% para clientes VIP em itens promocionais.<br>• Submenu do console para gestão de clientes. |
| **Lucas Silveira** | • Modelagem do `Carrinho` e `ItemCarrinho`.<br>• Implementação de `Cupom` (% e valor fixo, expiração e uso único por cliente).<br>• Persistência de cupons em `data/cupons.csv`.<br>• Totalização de compras com múltiplos descontos aplicados. |
| **Gabriel Buhatem** | • Polimorfismo nos pagamentos: `PagamentoPix` (5% desc.), `PagamentoCartao` (até 10x s/ juros) e `PagamentoBoleto`.<br>• Modelagem de `Pedido` e ciclo de vida (`EM_PROCESSAMENTO` -> `ENVIADO` -> `ENTREGUE`).<br>• Regra de cancelamento seguro com estorno de estoque em memória e em disco.<br>• Persistência do histórico de pedidos em `data/pedidos.csv`. |
| **Lavynne Marianne** | • Regra de negócio de fidelidade (1 ponto a cada R$ 10 em compras > R$ 100).<br>• Tiers de fidelidade: Bronze (2%), Prata (5%) e Ouro (8%).<br>• Gestão da carteira de crédito e método `usarCashback(cliente, valor)` em compras futuras.<br>• Emissão de relatório analítico de vendas e orquestração do `MenuPrincipal`. |

---

## 6. Bibliotecas Utilizadas e Justificativa

* **Java Standard Edition (JDK 17+)**: Escolha de utilizar exclusivamente a biblioteca nativa (`java.util`, `java.io`, `java.time`) para assegurar máxima portabilidade, facilidade de execução em qualquer máquina e eliminação de dependências de gerenciadores pesados.
* **Python Cairo / ReportLab (Docs)**: Utilizado para a confecção e automação dos documentos e relatórios formais da disciplina em PDF.

---

## 7. Referências Consultadas

1. DEITEL, Paul; DEITEL, Harvey. *Java: Como Programar*. 10ª Edição. Pearson, 2016.
2. ORACLE. *The Java™ Tutorials: Classes and Objects & Interfaces*. Disponível em: `https://docs.oracle.com/javase/tutorial/`.
3. BLOCH, Joshua. *Effective Java*. 3ª Edição. Addison-Wesley Professional, 2018.
4. GAMMA, Erich et al. *Padrões de Projeto: Soluções Reutilizáveis de Software Orientado a Objetos*. Bookman, 2000.
