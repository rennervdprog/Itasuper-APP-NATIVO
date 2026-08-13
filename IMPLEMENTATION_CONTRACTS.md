# Contratos externos confirmados — equivalência do cliente

## Capacitor/Web

A função `src/lib/deliveryFee.ts` calcula taxa de entrega própria por modalidade fixa ou por quilômetro. Em modo `km`, usa coordenadas do GPS/endereço, arredonda a distância de cobrança para cima com mínimo de 1 km, aplica `delivery_fee_base` até `delivery_base_km`, acrescenta `delivery_fee_per_km` aos quilômetros excedentes e soma a parcela operacional da plataforma. A fonte de distância prioriza rota via `resolveDistance`, com fallback Haversine.

O checkout web persiste no pedido `client_lat`, `client_lng`, `wallet_discount`, `scheduled_for`, `release_at` e campos de fidelidade. Depois de criar o pedido, chama `redeem_loyalty_points` para resgatar pontos, grava itens e chama `apply_wallet_discount` para debitar carteira. O cupom é registrado pela RPC `use_coupon` em fluxo não bloqueante.

A página de reembolso cria `refund_requests` com `order_id`, `store_id`, `requester_id`, `reason`, `description`, `refund_type = wallet_credit` e `requested_amount`. A avaliação é persistida em `order_ratings`.

## Supabase ItaSuper confirmado

Tabelas existentes: `saved_addresses`, `user_wallet`, `loyalty_config`, `loyalty_points`, `order_ratings`, `refund_requests`, `coupon_uses` e `driver_locations`.

A tabela `saved_addresses` contém `label`, endereço, `is_default`, `cep`, `latitude`, `longitude` e `pin_confirmed`. A tabela `orders` contém `client_lat`, `client_lng`, `wallet_discount`, `loyalty_points_used`, `loyalty_discount`, `scheduled_for`, `release_at`, campos de PIX e cancelamento/reembolso.

RPCs confirmadas e assinaturas: `apply_wallet_discount(_order_id uuid, _user_id uuid, _discount_amount numeric)`, `redeem_loyalty_points(_order_id uuid, _store_id uuid, _points_to_use integer)`, `apply_cancellation_policy(_order_id uuid, _reason text default 'Cancelado pelo cliente')` e `client_confirm_delivery(_order_id uuid)`.

## Referências

- `/home/ubuntu/itasuper_web_reference/src/lib/deliveryFee.ts`
- `/home/ubuntu/itasuper_web_reference/src/pages/CheckoutPage.tsx`
- `/home/ubuntu/itasuper_web_reference/src/components/RefundRequestModal.tsx`
- Supabase project `qkjhguziuchqsbxzruea`, consulta somente leitura de esquema e rotinas em 2026-08-13.
