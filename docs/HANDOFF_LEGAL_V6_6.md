# Handoff legal v6.6 — o que o app cliente precisa acompanhar

Documento escrito em 13/09/2026 a partir do repositório web
(`rennervdprog/ifood-style-landing`, commits `4f86b42c` e `6538bf63`).
Serve para manter os três clientes alinhados: web, app cliente e app entregador
compartilham o mesmo Postgres (`qkjhguziuchqsbxzruea`) e as mesmas tabelas de
documentos legais.

O que este documento **não** é: um pedido para refazer a tela de aceite. Ela já
existe e está correta na maior parte. Abaixo está só o delta.

---

## 1. O que mudou no modelo de negócio

Os Termos e a Política foram reescritos para a v6.6 e agora dizem, com todas as
letras, o que o sistema de fato faz:

> O ItaSuper é o software onde lojista e motoboy se encontram e seguem o fluxo
> do pedido. O pagamento da entrega é negociado e pago **direto entre eles**.
> A plataforma não sabe os valores, não intermedia e não tem vínculo
> empregatício com motoboy.

Consequência para a UI do app cliente: **não prometer "entregador ItaSuper"**,
"nosso entregador" ou qualquer texto que sugira que quem entrega é funcionário
ou preposto da plataforma. Quem entrega é o motoboy da loja. Se houver string
nesse sentido em tela de acompanhamento de pedido, ela contradiz a cláusula 10
dos Termos e precisa mudar.

A taxa de R$ 0,99 por entrega passou a ter cláusula própria (Termos §6.3): pode
mudar de valor no futuro, mas só com **30 dias corridos de aviso prévio**, sem
retroatividade, com direito de sair sem multa, e **o total de um pedido já
confirmado nunca muda**. Essa última parte é contrato: se alguma tela do app
recalcular a taxa de um pedido já confirmado, isso agora é descumprimento
expresso, não só um bug de UX.

---

## 2. O que mudou no banco (duas migrations)

Ainda **não aplicadas** — o Supabase está suspenso. Quando voltarem, nesta ordem:

1. `20260913120000_publish_legal_v6_6.sql` — publica os documentos v6.6
   (`version_num 660`) e as 12 linhas de `legal_document_changes`.
2. `20260913150000_legal_notice_period_and_audience.sql` — o resto.

### `get_pending_legal_changes` ganhou uma sobrecarga

A assinatura de dois argumentos **continua existindo** e delega para a nova com
`'all'`. O app cliente, do jeito que está hoje, não quebra quando a migration
rodar. Não há urgência de deploy casado.

A nova assinatura é de três argumentos:

```
get_pending_legal_changes(_terms_accepted text, _privacy_accepted text, _audience text)
```

`_audience` aceita `all`, `cliente`, `lojista`, `motoboy` e filtra as mudanças
pela nova coluna `legal_document_changes.audience text[]`. O app cliente deve
passar `"cliente"` — sem isso, o usuário vê tabela de planos e taxa de PIX do
lojista no meio do diff, e a mudança que de fato o afeta fica soterrada.

### Campos novos no retorno

```json
{
  "mode": "binding" | "notice",
  "terms_effective_date": "2026-10-13T00:00:00-03:00",
  "privacy_effective_date": "...",
  "days_until_effective": 30
}
```

`binding` = a vigência chegou, bloqueia até aceitar (comportamento atual).
`notice` = publicado com vigência futura, é **aviso prévio e não pode bloquear**.

### Colunas novas em `terms_acceptance`

`terms_document_id uuid`, `privacy_document_id uuid`, `acceptance_mode text NOT
NULL DEFAULT 'binding'`. Todas com default ou nuláveis — insert que não as
mencione continua funcionando.

---

## 3. Delta concreto no app cliente

Arquivos relevantes, verificados em `main` nesta data:

- `app/src/main/java/com/example/data/model/LegalDocuments.kt`
- `app/src/main/java/com/example/data/remote/SupabaseClient.kt` (linhas ~506 e ~547)
- `app/src/main/java/com/example/ui/legal/LegalConsentViewModel.kt`
- `app/src/main/java/com/example/ui/legal/LegalConsentGate.kt`

### 3.1. Aviso prévio — o item que importa

`LegalConsentGate` hoje é sempre bloqueante: `BackHandler(enabled = true) { }`,
`dismissOnBackPress = false`, `dismissOnClickOutside = false`, e não há caminho
de saída. Isso está certo para `binding` e **errado para `notice`**: os Termos
prometem 30 dias de aviso em que nada muda para o usuário, e um app que bloqueia
a tela inteira nesse período faz o software contradizer o contrato.

O que falta:

- `PendingLegalChanges` precisa dos campos `mode`, `termsEffectiveDate`,
  `privacyEffectiveDate`, `daysUntilEffective`. Se `mode` vier ausente ou nulo
  (banco antes da migration), tratar como `binding` — nunca como `notice`, ou o
  aceite obrigatório vira opcional por acidente.
- Em `notice`: permitir fechar, mostrar a data de vigência e os dias restantes,
  e adiar. O web guarda o adiamento por versão (`"6.6/6.6"`) em memória de
  sessão, então o aviso reaparece no próximo acesso e some sozinho quando vira
  `binding`. Um `SharedPreferences` com a mesma chave resolve o equivalente.
- Em `notice`, o botão principal vira "Aceitar desde já" — aceitar antes da
  vigência é permitido e conta como aceite válido (`acceptance_mode = 'notice'`).

### 3.2. Público-alvo

Em `SupabaseClient.fetchPendingLegalChanges`, acrescentar ao corpo:

```kotlin
put("_audience", "cliente")
```

Se o banco ainda não tiver a sobrecarga, o PostgREST devolve erro de função
inexistente (`42883`) — nesse caso repetir a chamada sem o campo. É o mesmo
fallback que o web faz. Enquanto a migration não roda, o comportamento degrada
para "mostra tudo", que é o de hoje.

### 3.3. Recusa explícita

Não existe caminho de recusa na tela. Consentimento sob bloqueio total e sem
saída não é consentimento livre (LGPD art. 8º, §4º). O web ganhou um "Não
concordo com as mudanças" que abre um painel com: exportar dados / excluir
conta, e falar com o suporte (`https://wa.me/5522992796291`). O app cliente já
tem tela de perfil com exclusão de conta — basta linkar para lá.

### 3.4. O que **já está certo** e não deve ser mexido

`SupabaseClient.recordLegalAcceptance` grava em `terms_acceptance` **antes** de
atualizar `profiles`, e aborta se o primeiro falhar. Está correto e é o oposto
do que o web fazia até este commit. O web tinha a ordem invertida com o erro
ignorado, então um usuário podia constar como tendo aceito um texto sem nenhum
registro que sustentasse isso. Corrigido lá; aqui nunca esteve errado.

Se um dia adicionarem `acceptance_mode` ao insert, prever o caso da coluna não
existir (`42703` / `PGRST204`) e repetir sem ela. A prova do aceite não pode
falhar por causa de um campo acessório.

---

## 4. Comparação de versão

Se em algum ponto o app comparar versões de documento como string
(`"6.6" >= "4.2"`), trocar por comparação numérica componente a componente.
`"10.0" >= "4.2"` é `false` em ordem lexicográfica, e isso reabre o gate para
quem já aceitou. Foi um bug real no web (`ProductTour.tsx`), corrigido com
`compareLegalVersions` e coberto por teste.

---

## 5. Resumo do que fazer e quando

| | item | depende da migration? |
|---|---|---|
| 1 | remover promessa de "entregador ItaSuper" da UI | não |
| 2 | recusa explícita na tela de aceite | não |
| 3 | comparação numérica de versão, se houver | não |
| 4 | `_audience = "cliente"` com fallback 42883 | não (degrada sozinho) |
| 5 | modo `notice` não bloqueante | campos chegam nulos até lá; implementar com default `binding` é seguro |

Nada aqui exige deploy casado com o banco. Os cinco itens podem ir antes.
