# ItaSuper Cliente Android — Handoff de Continuidade para Manus

**Repositório:** `rennervdprog/Itasuper-APP-NATIVO`  
**Branch de referência:** `main`  
**Último commit no momento deste handoff:** `ca10e55` — `ci(client): parameterize Android release versions`  
**Pacote Android:** `app.itasuper.cliente`  
**Escopo deste repositório:** aplicativo Android nativo do cliente, em Kotlin e Jetpack Compose.  
**Fora de escopo:** app Android de entregador; não alterar código, workflow ou telas do entregador neste repositório ou neste chat.

> Este documento é um estado operacional para uma nova sessão de trabalho. Leia antes de alterar autenticação, pedidos, pagamentos, reembolsos, disponibilidade de entrega, Farmácias ou configuração de releases.

## Regras obrigatórias de trabalho

| Regra | Aplicação prática |
|---|---|
| Commits isolados | Um commit por funcionalidade; nunca incluir APKs, AABs, diretórios `build`, relatórios temporários, logs, chaves ou arquivos de ambiente. |
| Publicação | Não criar commit ou `push` sem autorização explícita do usuário para publicar. |
| Segurança | Nunca registrar ou exibir keystore, senha, Base64, token, `.env`, `keystore.properties`, credenciais ou dados pessoais. |
| Banco | O app consome Supabase. Mudanças estruturais de banco pertencem ao repositório web e precisam de migration versionada. |
| Regulação | Farmácia no app mostra dados objetivos cadastrados pela loja; não implementar dispensação, upload/retenção de receita, orientação médica ou alegação de conformidade regulatória sem projeto formal. |
| App entregador | Não alterar nem publicar o aplicativo de entregador por este repositório. |

## Arquitetura e referências principais

| Área | Referências principais |
|---|---|
| Modelo de produto | `app/src/main/java/com/example/data/model/Product.kt` |
| Catálogo Supabase | `app/src/main/java/com/example/data/remote/SupabaseClient.kt` |
| Carrinho | `app/src/main/java/com/example/data/repository/CartRepository.kt`, `CartStorage.kt` |
| Criação de pedido | `app/src/main/java/com/example/data/repository/OrderRepository.kt` |
| Busca e categorias | `app/src/main/java/com/example/data/repository/StoreRepository.kt`, `ui/search/SearchScreen.kt` |
| Página de loja | `app/src/main/java/com/example/ui/store/StoreDetailScreen.kt`, `StoreDetailViewModel.kt` |
| Início | `app/src/main/java/com/example/ui/home/HomeScreen.kt` |
| Build e versão | `app/build.gradle.kts`, `.github/workflows/android-release.yml`, `RELEASE_VERSIONING.md` |

## Estado funcional já publicado

### Autenticação e conta

O commit `fabc619` publicou o redesign de autenticação. O fluxo de login/cadastro foi alinhado para **e-mail e senha**, porque e-mail é necessário para recuperação de conta e evita a criação confusa de e-mails sintéticos a partir de WhatsApp.

O app contém suporte de biometria para login rápido. A experiência deve continuar oferecendo biometria depois que o usuário fizer login manual e puder ativá-la, sem fazer a solicitação sumir imediatamente. Ao alterar autenticação, preservar os formatos de CPF e WhatsApp já corrigidos e não reintroduzir login por WhatsApp isolado quando o backend espera e-mail.

### Disponibilidade de entregador e realtime

A regra operacional vigente é **13 minutos** de indisponibilidade/ausência de resposta. Lojas sem entregador operacional devem permanecer visíveis ao cliente, mas com aviso de entrega indisponível e finalização de pedido bloqueada. Quando o entregador se torna disponível novamente, a interface deve atualizar por realtime sem exigir fechar e abrir o app.

Não fazer a loja desaparecer da Home ou da busca apenas por indisponibilidade de entregador. A decisão canônica precisa permanecer alinhada ao servidor e ao web.

### Pedido, PIX e reembolso

| Regra | Estado esperado |
|---|---|
| PIX | Pedido só entra em preparo depois de confirmação financeira canônica no servidor/webhook. |
| Reembolso | Somente PIX Direto confirmado, pedido concluído e solicitação dentro de 24 horas. |
| Pagamento físico | Dinheiro, cartão/maquininha, PIX de PDV e outros fluxos físicos não devem ter reembolso financeiro automático pela plataforma. |
| Presença de entregador | Manter a regra de 13 minutos e bloquear checkout quando a entrega estiver indisponível. |

Ao alterar `OrderRepository`, nunca liberar preparação de pedido com base somente em uma atualização visual local de pagamento.

### Farmácias

O commit `2e41298` publicou a experiência de Farmácias no app cliente.

| Entrega | Estado atual |
|---|---|
| Descoberta | Categoria `farmacias` aparece em Home/Descobrir com ícone próprio. |
| Página de loja | Farmácias têm banner e explicação visual específicos, sem mudar as páginas de outras categorias. |
| Metadados | Produto suporta receita, controle, modalidade de venda, tipo farmacêutico, princípio ativo, dosagem, forma, fabricante, embalagem e genérico. |
| Catálogo | Produtos restritos aparecem para consulta, com selos e detalhes objetivos. |
| Carrinho | `CartRepository` recusa item com receita, controle ou modalidade diferente de `platform_checkout`. |
| Pedido | `OrderRepository` repete a proteção final, inclusive para carrinho restaurado. |

O contrato farmacêutico usa `products.metadata` vindo do Supabase.

| Campo | Valores ou uso |
|---|---|
| `pharma_type` | `medicine`, `personal_care`, `baby`, `vitamin_supplement`, `convenience`, `other` |
| `sale_mode` | `platform_checkout`, `pharmacy_validation`, `not_available_app` |
| `requires_prescription` e `controlled` | Bloqueiam o checkout comum. |
| `active_ingredient`, `dosage`, `pharma_form` | Apresentação objetiva do produto. |
| `manufacturer`, `pack_quantity`, `is_generic` | Informações complementares. |

A regra de segurança é intencionalmente conservadora: se o item tiver receita, controle ou `sale_mode` não permitido, ele fica fora da sacola e do pedido comum. A validação de servidor está no Supabase e não deve ser removida pelo app.

## Releases Android assinadas

O commit `ca10e55` configurou o versionamento explícito de APK e AAB.

| Item | Estado atual |
|---|---|
| Versão padrão atual | `versionCode 12` e `versionName 1.0.11`. |
| Arquivo de configuração | `app/build.gradle.kts` aceita `APP_VERSION_CODE` e `APP_VERSION_NAME` por propriedade Gradle ou ambiente. |
| Workflow oficial | `.github/workflows/android-release.yml` gera **APK release assinado** e **AAB release assinado** na mesma execução. |
| Campos no GitHub | O workflow pede `version_code` e `version_name` antes de começar. |
| Próxima atualização | O `version_code` deve ser sempre maior que o instalado/publicado: depois de 12, usar 13, 14 e assim por diante. |

Para gerar release no GitHub:

1. Abrir o repositório e entrar em **Actions**.
2. Escolher **Android Release AAB and APK**.
3. Clicar em **Run workflow** no branch `main`.
4. Informar, por exemplo, `version_code: 12` e `version_name: 1.0.11`.
5. Baixar os dois artefatos ao término.

O APK serve para instalação direta e testes. O AAB é para envio ao Google Play Console; ele não é instalado diretamente no telefone.

> Para atualizar um APK já instalado, três condições precisam ser verdadeiras: mesmo `applicationId`, mesma chave de assinatura e `versionCode` maior. Mudar somente o texto de `versionName` não basta.

Leia `RELEASE_VERSIONING.md` para o guia completo. Os nomes dos artefatos do workflow incluem versão e código para evitar confusão.

### Segurança de assinatura

O workflow restaura o material de assinatura temporariamente no executor e o remove no final. Os secrets já esperados pelo workflow são configurados no GitHub e **nunca** devem ser copiados para Git, documentos, chat, logs ou arquivos de teste.

Não gerar nova chave de upload a cada atualização. A mesma chave deve continuar sendo usada para permitir atualização sobre a versão anterior. O certificado público pode ser usado para integrações quando necessário, mas chaves privadas e senhas nunca devem ser expostas.

## Comandos de validação

| Finalidade | Comando |
|---|---|
| Testes unitários | `ANDROID_HOME=/home/ubuntu/.android-sdk ANDROID_SDK_ROOT=/home/ubuntu/.android-sdk ./gradlew --no-daemon :app:testDebugUnitTest` |
| APK debug | `ANDROID_HOME=/home/ubuntu/.android-sdk ANDROID_SDK_ROOT=/home/ubuntu/.android-sdk ./gradlew --no-daemon :app:assembleDebug` |
| Validar versão específica | adicionar `-PAPP_VERSION_CODE=12 -PAPP_VERSION_NAME=1.0.11` aos comandos acima. |

Na última validação, `:app:testDebugUnitTest` e `:app:assembleDebug` foram aprovados com `versionCode 12` e `versionName 1.0.11`. O metadado do APK debug confirmou esses valores. Warnings de depreciação existentes não bloquearam o build.

O build de release exige os materiais de assinatura privados. Não tentar contornar essa exigência nem alterar o workflow para gravar segredos no repositório.

## Histórico resumido de commits relevantes

| Commit | Assunto |
|---|---|
| `ca10e55` | Versionamento parametrizado de APK/AAB e guia de release. |
| `2e41298` | Descoberta e proteções de Farmácias. |
| `fabc619` | Redesign e alinhamento da autenticação. |
| `f84b2e6` | Workflow de APK e AAB assinados. |
| `3995916` | Retorno de Descobrir e regra de reembolso de 24 horas. |
| `957644d` | Atualização de disponibilidade de entregador sem bloquear a interface principal. |

## Alterações locais não publicadas — GPS e checkout

A correção local do fluxo de localização do cliente foi concluída e validada em 25/08/2026. `HomeViewModel` e `SearchViewModel` usam `CurrentLocationProvider`, baseado em `FusedLocationProviderClient.getCurrentLocation()`, com prioridade alta quando há permissão precisa e rejeição de posições com precisão pior que 250 metros. O reverse geocoding ocorre em `Dispatchers.IO`, e o CEP retornado pelo geocoder é persistido junto à posição.

O checkout não reutiliza mais o CEP do endereço salvo ao selecionar a localização atual. A seleção GPS monta um endereço exclusivamente com os campos GPS; se o CEP não estiver disponível, o usuário é orientado a preenchê-lo antes da cotação. Endereços salvos não usam cidade/UF da localização ativa como fallback. A localização ativa possui timestamp persistido e só é considerada atual por cinco minutos, evitando confirmação baseada em posição antiga. A política pura está em `CheckoutLocationPolicy.kt` e tem testes unitários para GPS completo, CEP ausente, endereço incompleto e expiração. A cotação agora recebe também cidade e UF preenchidas pelo CEP, além de rua, número, bairro e CEP, para melhorar a geocodificação estruturada.

O topbar da Home agora mostra rua/número e cidade do endereço ativo. Ao tocar no endereço, o seletor apresenta o endereço cadastrado, a ação para usar localização atual e o acesso para editar/cadastrar endereço. Quando a permissão já existe, a ação inicia o GPS diretamente; sem permissão, abre o fluxo nativo de autorização. Selecionar o endereço cadastrado limpa coordenadas GPS antigas do carrinho e da sessão.

O checkout agora sintetiza o endereço da aba Perfil como item selecionável em `savedAddresses`, com o rótulo `Endereço do perfil`, junto aos registros da tabela remota. O item do perfil é priorizado na seleção inicial, não é duplicado quando já existe endereço remoto equivalente e abre o editor automaticamente quando rua, número, bairro ou CEP estão incompletos. Alterar qualquer campo remove a seleção visual anterior e passa a cotar exatamente os dados editados. A busca por CEP também normaliza o CEP retornado pelo ViaCEP.

Quando o checkout usa endereço obtido pelo GPS, o editor informa que rua, CEP, bairro, cidade, UF e complemento são dados automáticos e deixa somente o número editável. A busca por CEP e a edição dos demais campos continuam disponíveis para endereços cadastrados ou manuais; o bloqueio é exclusivo do estado `usingGpsAddress`. Também é exibido um cartão de orientação e um botão explícito `Confirmar localização e usar neste pedido`. O botão valida o número e a cotação, remove a seleção visual de endereço salvo, fecha o editor GPS e deixa o cliente seguir para a confirmação final do pedido.

A confirmação do pedido não exibe mais o nome do backend, banco de dados ou UUID completo. Durante o envio, o texto é `Enviando pedido para [nome da loja]...`; após sucesso, a mensagem informa que o pedido foi enviado para a loja e orienta o acompanhamento em Meus Pedidos. O identificador público usa `OrderPresentation.customerOrderCode()`, exibindo somente um código curto, enquanto o UUID permanece no modelo para operações internas.

A vitrine do app cliente agora exige pelo menos cinco produtos cadastrados e visíveis por loja. A contagem é feita de forma leve e paginada por loja, ignorando itens sem nome, vendidos por peso, ocultos ou exclusivos de PDV. Lojas com quatro ou menos itens visíveis ficam fora da lista global; categorias, busca, Home, Descobrir e lojas recentes consomem essa lista filtrada. Se a consulta de contagem falhar, a loja é preservada para evitar que uma falha transitória de rede oculte todo o catálogo.

A validação local aprovou `:app:testDebugUnitTest` e `:app:assembleDebug`. O APK de teste local foi gerado com `versionCode 25` e `versionName 1.0.24`. Essas alterações ainda não foram commitadas nem publicadas.

## Pendências e cuidados para a próxima sessão

1. Antes de qualquer alteração, executar `git status --short` e ler este arquivo.
2. Não publicar automaticamente. Solicitar autorização explícita antes de `commit`/`push`, exceto quando a autorização de publicação daquela mudança já tiver sido concedida de forma inequívoca.
3. Se for gerar uma atualização distribuível, escolher `versionCode` maior que o último código instalado/publicado e gerar APK/AAB pelo workflow assinado.
4. Não gerar uma release distribuível reutilizando o mesmo `versionCode` apenas porque houve mudança de código.
5. Manter os bloqueios de Farmácia em `CartRepository`, `OrderRepository` e na UI. Eles complementam a proteção do banco.
6. Preservar as regras de PIX, reembolso, disponibilidade de entrega e atualização realtime.
7. Se o usuário pedir app iOS, usar este repositório apenas como referência funcional; não misturar código iOS aqui.
8. Atualizar este handoff após alterações estruturais, novas migrations que afetem o app, mudanças de versionamento ou decisões de negócio relevantes.

## Dados que nunca devem ser registrados neste arquivo

Nunca adicionar keystore, senhas, aliases secretos, Base64, certificados privados, tokens, variáveis `.env`, `keystore.properties`, credenciais de contas, documentos pessoais ou contatos reais.


## Alterações locais desta sessão — alinhamento com as gravações Android

Em 26/08/2026, após confirmar que as gravações comparadas eram dos apps Android, foram aplicadas localmente no app cliente as seguintes melhorias, ainda sem commit, push ou APK: a aba Descobrir passou a persistir na sessão a localização GPS válida retornada pelo `CurrentLocationProvider`, mantendo Home, Busca e Checkout na mesma cidade/endereço ativo; o filtro `Retirada` passou a incluir lojas com `deliveryMode` `pickup` e `both`; o cabeçalho da Busca passou a permitir atualizar GPS pelo diálogo de permissão já existente; a Home recebeu skeleton de lojas no carregamento e os cards exibem distância apenas quando calculada; a Descoberta recebeu skeleton durante a consulta de destaques.

O checkout e a página de loja foram auditados e não foram reescritos porque já possuem separação entre endereço salvo/GPS, confirmação explícita da localização, cotação oficial, retirada, personalização, bloqueios de Farmácia, textos amigáveis e código curto do pedido. O app entregador não foi alterado. A validação `:app:testDebugUnitTest` foi aprovada após essas mudanças, com warnings de APIs Java depreciadas já existentes; não foi gerado APK nesta sessão.
