# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projeto

App **cliente** do ItaSuper — Android nativo, Kotlin + Jetpack Compose + Material 3.
`applicationId` `app.itasuper.cliente`, `namespace` `com.example`, `compileSdk 36`, `minSdk 24`.

**Três repositórios, um banco.** Este repo é o app do cliente. Os outros dois são
`rennervdprog/ifood-style-landing` (web — painel do lojista, PDV, site) e
`rennervdprog/Itasuper-entregador-` (app do motoboy). Os três falam com o **mesmo Postgres**:
mudar assinatura de RPC ou coluna de `orders` compila aqui e quebra os outros em silêncio.
Não há tipos gerados nem teste de contrato entre eles. Tratar o schema como contrato público.

## Backend

`SUPABASE_URL` e `SUPABASE_ANON_KEY` estão **hardcoded** em
`app/src/main/java/com/example/data/remote/SupabaseClient.kt` (projeto `qkjhguziuchqsbxzruea`).
É o banco real de negócio — o mesmo que o web usa. Não apontar para outro projeto.

O acesso é **REST puro via OkHttp + JSONObject**, sem SDK do Supabase e sem serialização
automática: cada chamada monta a URL `/rest/v1/...` ou `/rest/v1/rpc/...` na mão, com headers
`apikey` + `Authorization: Bearer`. O token é o `accessToken` da sessão quando existe, senão
o anon. `SupabaseClient.kt` é um objeto único e grande (~4k linhas) — ao adicionar chamada,
seguir o padrão vizinho em vez de introduzir Retrofit/kotlinx-serialization pontualmente.

RLS está ativo em todas as tabelas. Se uma consulta volta vazia sem erro, suspeitar de RLS
antes de suspeitar do código.

## Comandos

```bash
./gradlew assembleDebug                 # build debug (é o que o CI roda em build-apk.yml)
./gradlew :app:testDebugUnitTest        # testes unitários (JVM + Robolectric)
./gradlew :app:lint                     # Android Lint

# release local (exige assinatura privada fora do Git)
./gradlew :app:assembleRelease :app:bundleRelease \
  -PAPP_VERSION_CODE=12 -PAPP_VERSION_NAME=1.0.11
```

## Versionamento — diferente do repo web

`versionCode` e `versionName` **não são editados em arquivo**: vêm das propriedades Gradle
`APP_VERSION_CODE` / `APP_VERSION_NAME`, passadas pelo workflow `android-release.yml`
(`workflow_dispatch`, com dois campos de entrada). Não hardcodar versão no `build.gradle.kts`.
`version_code` sempre aumenta e nunca repete. Detalhes em `RELEASE_VERSIONING.md`.

## Estrutura

```
app/src/main/java/com/example/
  MainActivity.kt              única Activity; Compose daqui pra baixo
  data/model/                  data classes puras (Order, Store, Product, LegalDocuments, ...)
  data/remote/SupabaseClient.kt  TODO o acesso a rede
  data/repository/             sessão, carrinho, pedidos, busca, storage local
  ui/<feature>/                Screen + ViewModel por feature (auth, home, search, store,
                               orders, notifications, profile, legal, permissions)
  ui/theme/                    cores, tipografia, shapes
  location/  notifications/  core/
```

Padrão: `Screen` composable + `ViewModel` com `MutableStateFlow<UiState>` exposto como
`StateFlow`, coletado com `collectAsState()`. Estado de UI em `data class ...UiState` com
propriedades derivadas (`canAccept`, `requiresAcceptance`) em vez de lógica na composable.

## Testes

`app/src/test/` roda em JVM com Robolectric e inclui testes de screenshot
(`HomeScreenScreenshotTest`, `GreetingScreenshotTest`) e de regra de negócio
(`CheckoutLocationPolicyTest`, `CatalogVisibilityRuleTest`). `app/src/androidTest/` tem
instrumentado. Ao mexer em regra de checkout ou visibilidade de catálogo, rodar os testes
correspondentes — são eles que protegem essas regras.

## Documentos legais — leia antes de mexer em aceite, entrega ou taxa

`docs/HANDOFF_LEGAL_V6_6.md` é a referência atual e traz o delta pendente neste app.
Dois pontos que valem como regra permanente:

- **Não chamar quem entrega de "entregador ItaSuper"**, "nosso entregador" ou equivalente.
  O ItaSuper é só o software onde lojista e motoboy se encontram; o pagamento da entrega é
  negociado direto entre eles e não há vínculo com a plataforma. Texto em contrário contradiz
  a cláusula 10 dos Termos.
- **O total de um pedido já confirmado nunca muda.** É cláusula expressa (Termos §6.3), não
  só boa prática de UX.

A tela de aceite (`ui/legal/`) já existe e funciona. `SupabaseClient.recordLegalAcceptance`
grava `terms_acceptance` **antes** de `profiles` e aborta se o primeiro falhar — isso está
certo e é proposital: `terms_acceptance` é a prova do consentimento e `profiles` é só cache.
Não inverter.

## Migrations e Edge Functions

`supabase/migrations/` e `supabase/functions/` existem neste repo para o que é específico do
cliente (notificações, push). Toda mudança de banco vai como migration versionada — nunca SQL
manual em produção. O banco pode estar suspenso; nesse caso escrever a migration mesmo assim
e tratar a ausência do schema novo com degradação graciosa no app (ex.: erro `42883` de função
inexistente → repetir a chamada na assinatura antiga).

## Convenções

- Comentários e mensagens de commit em **português**, seguindo o histórico.
- Commits isolados por assunto. **Não commitar nem dar push sem autorização explícita.**
- **Nunca versionar** chave, senha, keystore, Base64 de assinatura, token, dado pessoal ou
  conta de teste. A chave de release só entra por Secrets do GitHub.
- Não mexer no app do entregador nem no web a partir daqui.

## Outros documentos

`MANUS_HANDOFF.md` (continuidade e regras de trabalho), `IMPLEMENTATION_CONTRACTS.md`
(contratos confirmados com o banco e equivalência com o web), `AUDITORIA_TECNICA.md` e
`auditoria_*.md` (o que é lógica real e o que ainda é mock por tela),
`docs/GOOGLE_PLAY_RELEASE.md`, `docs/GITHUB_ACTIONS_ANDROID_RELEASE.md`.
