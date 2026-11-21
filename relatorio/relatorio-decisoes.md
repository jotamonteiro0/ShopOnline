# Decisões de projeto - ShopOnline

Revisão: 21/11/2026. Este registro descreve a implementação local e complementa o [relatório técnico](relatorio-tecnico.md). Os requisitos funcionais de referência são os apresentados nas imagens do enunciado do ShopOnline. O documento geral Trabalho P2.pdf exige persistência em arquivos e README. A divisão em camadas, a escolha do formato CSV e o formato dos testes são decisões da equipe.

## 1. Console e biblioteca padrão

O programa é uma aplicação local de console, com JDK 17 ou superior e sem dependências Java externas. Essa escolha permite demonstrar o domínio, os conceitos de POO e a persistência sem exigir servidor ou configuração de banco. Pagamentos são simulados, conforme o enunciado; não há cobrança bancária.

## 2. Separação de responsabilidades

- `model`: Produto, Cliente/ClienteVip, Carrinho/ItemCarrinho, Cupom, Pedido e enumerações.
- `model.pagamento`: interface FormaPagamento e implementações PIX, cartão e boleto.
- `repository`: leitura, gravação e reconstrução de produtos, clientes, cupons e pedidos.
- `service`: coordenação do estoque, fidelidade, checkout e ciclo de pedidos.
- `view`: Entrada, MenuPrincipal e submenus de produtos, clientes, carrinho e pedidos.

O Main instancia as dependências. O menu de carrinho compartilha o repositório de cupons usado pelo VendaService para que a validação de checkout e a interface consultem o mesmo cadastro. Construtores existentes foram preservados para os demais módulos e testes.

## 3. Herança e polimorfismo

ClienteVip especializa Cliente e reutiliza suas validações. `isVip()` e `calcularDescontoVip(Produto)` são sobrescritos; o desconto é de 10% apenas em produto marcado para promoção VIP. Categoria VIP e nível de fidelidade são independentes.

FormaPagamento define o cálculo e a descrição do pagamento. PIX aplica 5% de desconto; cartão mantém o valor com até 10 parcelas sem juros; boleto mantém o valor. O checkout opera com a interface, sem reproduzir as regras das implementações. O desconto PIX é uma regra deste trabalho, não uma obrigação legal.

## 4. Ordem de cálculo e precisão

A ordem adotada, não detalhada no enunciado, é:

1. Somar os subtotais dos itens.
2. Subtrair VIP e um cupom, ambos calculados sobre o subtotal, limitando a base a zero.
3. Aplicar a regra da forma de pagamento.
4. Resgatar opcionalmente o saldo de cashback, limitado ao valor a pagar.
5. Calcular pontos e novo cashback sobre o valor final restante.

Os pontos são a parte inteira de valorFinal/10 apenas quando valorFinal > 100. O percentual de cashback considera o nível antes de creditar os pontos da compra atual. Os valores finais, os créditos e os cupons usam arredondamento para duas casas com HALF_UP. O modelo ainda utiliza double; BigDecimal é usado nos pontos de arredondamento, sem transformar o sistema inteiro em outro modelo monetário.

## 5. Cupons e compatibilidade dos dados

Cupom registra um conjunto de IDs de clientes, mantendo um cupom por carrinho. O checkout verifica validade e uso tanto no objeto aplicado quanto no cadastro persistido. Diferenças de tipo, valor ou validade em relação ao cadastro são rejeitadas. Trocar o cliente ativo remove o cupom aplicado para evitar levar um desconto ao comprador errado.

O cabeçalho novo é `codigo;tipo;valor;dataValidade;clientesUtilizados`, com IDs separados por vírgula. O cabeçalho antigo `codigo;tipo;valor;dataValidade;utilizado` continua legível: false não registra uso; true mantém bloqueio global. Na gravação, o marcador * preserva esse bloqueio no formato novo. TRUE/FALSE no cabeçalho novo são IDs reais, e não indicadores booleanos.

Essa adaptação preserva o que o arquivo antigo consegue informar sem inventar o cliente que o utilizou. Cupons com erros de leitura bloqueiam a escrita até correção e recarga. O valor é normalizado antes de salvar; descontos que virariam zero no CSV são rejeitados.

## 6. Histórico, falhas de escrita e compensação

Pedidos e seus itens são gravados separadamente em pedidos.csv e itens_pedidos.csv. Itens registram quantidade e preço de venda; o modelo fornece cópias para proteger esses dados em memória. O relatório consulta o histórico persistido, incluindo vendas de sessões anteriores, e exclui cancelados dos totais financeiros e das quantidades vendidas.

Produtos, clientes, cupons e pedidos são gravados mediante arquivo temporário e substituição do destino. A movimentação de estoque restaura o estado em memória quando a gravação falha. O checkout tenta desfazer estoque, fidelidade e consumo do cupom se uma etapa falhar. A gravação de pedidos tenta restaurar itens anteriores caso falhe ao atualizar pedidos.csv.

O sistema não oferece transação atômica entre arquivos nem bloqueio para várias instâncias. Compensações podem falhar quando o destino continua indisponível; essas falhas são associadas ao erro original. A solução é proporcional à aplicação local, mas não serve como garantia transacional de um serviço de comércio eletrônico em produção.

## 7. Política de cancelamento e limites do histórico

Somente EM_PROCESSAMENTO pode virar CANCELADO, com devolução de estoque persistida. O fluxo normal é EM_PROCESSAMENTO -> ENVIADO -> ENTREGUE. A repetição do cancelamento é bloqueada.

A versão atual mantém os pontos, o cashback gerado, o saldo resgatado e o cupom utilizado após um cancelamento. O enunciado mostrado não define a reversão dos benefícios; este registro torna explícito o comportamento existente, sem afirmar que há reembolso completo. Uma política diferente exige definir e persistir os dados necessários para o estorno.

O CSV de pedidos preserva o tipo de pagamento, mas não o número de parcelas. Ao recarregar um pagamento por cartão, o repositório o reconstrói com uma parcela. Produtos e clientes referenciados devem continuar no cadastro para reconstruir os pedidos; nomes e descrições dos produtos vêm do catálogo atual.

## 8. Verificação e integração

A revisão compilou a árvore local com JDK 26.0.2.1 e `--release 17`, executou 12 classes de verificação e o run.sh em cópia temporária. O roteiro de console verificou uma compra com VIP, cupom, PIX e resgate, seguida de reinício, relatório e cancelamento. Os dados originais foram preservados por comparação SHA-256. Windows, queda de energia e concorrência não foram testados.

Os testes adicionais reproduzem falhas antes da correção e cobrem descontos acumulados, resgate, reuso de cupom, relatório após reinício, quantidades inválidas, falhas de gravação, compatibilidade do CSV, IDs TRUE/FALSE e precisão do cupom.

O fluxo combinado pela equipe usa branches individuais, PRs para developer e depois integração validada em main. Correções locais e documentação não representam um merge ou publicação. A distribuição de responsabilidades consta no relatório técnico; ela não deve ser confundida com autoria exclusiva de cada linha.
