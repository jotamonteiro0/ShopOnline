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

A modelagem separa os dados e comportamentos de clientes (`model`), a leitura e gravação em CSV (`repository`) e a interação pelo console (`view`). `MenuClientes` recebe um `ClienteRepository` pelo construtor e reutiliza `Entrada` para ler as opções. A conexão desse submenu ao `MenuPrincipal` pertence à etapa de integração.

```
       +-------------------------------------------------+
       |                     Cliente                     |
       +-------------------------------------------------+
       | - id: String                                    |
       | - nome: String                                  |
       | - email: String                                 |
       | - pontosFidelidade: int                         |
       | - saldoCashback: double                         |
       +-------------------------------------------------+
       | + isVip(): boolean                              |
       | + calcularDescontoVip(produto: Produto): double  |
       +-------------------------------------------------+
                              ^
                              | (Herança)
       +-------------------------------------------------+
       |                   ClienteVip                    |
       +-------------------------------------------------+
       | + isVip(): boolean                              |
       | + calcularDescontoVip(produto: Produto): double  |
       +-------------------------------------------------+

       +------------------------------------+
       |       <<interface>>                |
       |       FormaPagamento               |
       +------------------------------------+
       | + calcularValorFinal(total): double|
       | + getDescricao(): String           |
       +------------------------------------+
              ^            ^           ^
              |            |           | (Polimorfismo)
     +-------------+ +---------------+ +---------------+
     |PagamentoPix | |PagamentoCartao| |PagamentoBoleto|
     +-------------+ +---------------+ +---------------+
```

O diagrama destaca os atributos de `Cliente` e os métodos sobrescritos por `ClienteVip`; construtores, getters, setters e operações de pontos e cashback foram omitidos para facilitar a leitura.

### 2.1 Herança, encapsulamento e polimorfismo de clientes

`ClienteVip extends Cliente` representa uma especialização: um cliente VIP possui os mesmos dados cadastrais, pontos e saldo de um cliente comum, mas tem uma regra adicional de desconto. Essa relação permite reutilizar as validações de ID, nome e e-mail sem duplicá-las na subclasse. Os atributos permanecem privados em `Cliente` e são acessados pelos métodos públicos herdados.

Os construtores de `ClienteVip` chamam `super(...)` para inicializar a parte herdada do objeto. O construtor simples inicia pontos e cashback em zero; o construtor completo permite reconstruir os valores persistidos no CSV.

Os métodos `isVip()` e `calcularDescontoVip(Produto)` usam `@Override`. Uma variável declarada como `Cliente` pode referenciar um `ClienteVip`; a implementação executada depende do objeto em tempo de execução. Em `Cliente`, o desconto é zero. Em `ClienteVip`, ele corresponde a 10% do preço unitário somente quando `produto.isPromocaoVip()` é verdadeiro. Por exemplo, um produto promocional de R$ 199,90 gera R$ 19,99 de desconto por unidade para um VIP. A aplicação desse valor às quantidades do carrinho pertence ao módulo de carrinho.

`ClienteRepository` grava o indicador `vip` no CSV e usa esse campo para reconstruir `Cliente` ou `ClienteVip`. O cadastro rejeita IDs repetidos; atualizações de clientes existentes continuam usando `salvar()`. A leitura identifica registros inválidos sem interromper os registros seguintes e bloqueia regravações até que o arquivo seja corrigido e recarregado, evitando apagar as linhas rejeitadas. A escrita usa UTF-8 e arquivo temporário antes de substituir o destino.

### 2.2 Verificações do módulo de clientes

As classes de verificação cobrem validações cadastrais, duplicidade, persistência comum/VIP, acentos, desconto de 10%, navegação do submenu, registros CSV inválidos e falhas de gravação. Os testes de persistência usam arquivos temporários para preservar `data/clientes.csv`. A integração do submenu ao menu principal e a revisão conjunta na branch `developer` permanecem etapas da equipe.

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
A classe `ClienteVip` herda os comportamentos de `Cliente` e sobrescreve os métodos que identificam a especialização e calculam o desconto. A persistência de ambos os tipos é feita por `ClienteRepository`:
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
