# Publicação do ItaSuper Cliente no Google Play

**Pacote Android:** `app.itasuper.cliente`

**Tipo de artefato:** Android App Bundle (`.aab`)

**Assinatura recomendada:** Play App Signing com **upload key** separada

**Versão inicial configurada:** `versionCode 1`, `versionName 1.0`

> O Google Play exige que o bundle seja assinado com uma chave de upload antes do envio. Com Play App Signing, o Google guarda a chave de assinatura de distribuição e a equipe do ItaSuper conserva apenas a upload key para assinar novos envios. A upload key precisa ser mantida em local seguro, fora do GitHub.[1] [2]

## 1. O que já está pronto

| Item | Estado |
|---|---|
| Identificador único do app | Configurado como `app.itasuper.cliente`. |
| SDK mínimo / alvo | `minSdk 24` e `targetSdk 36`. |
| Firebase Cloud Messaging | Integrado; o arquivo de configuração do Firebase já pertence ao projeto. |
| Configuração Gradle de release | Preparada para ler somente `keystore.properties` local ou variáveis de ambiente. |
| Proteção de segredo | Arquivos `.jks`, `.keystore`, certificados e `keystore.properties` estão ignorados pelo Git. |
| Artefato exigido pela Play Store | O projeto gera `.aab` com a tarefa `bundleRelease`. |

## 2. Criar e guardar a upload key

A primeira chave deve ser criada e guardada pelo proprietário da conta Play Console. Use uma senha forte e exclusiva. Guarde em **dois locais seguros e privados**, por exemplo, um gerenciador de senhas e um cofre criptografado. Não envie a chave, senha, certificado privado ou arquivo `keystore.properties` por e-mail, WhatsApp ou GitHub.

No computador de publicação, copie `keystore.properties.example` para `keystore.properties` e preencha as informações locais. A chave pode ser gerada pelo Android Studio ou pelo comando abaixo. O padrão usa RSA 4096 e validade de 50 anos:

```bash
keytool -genkeypair \
  -keystore /caminho/seguro/itasuper-upload-key.jks \
  -alias itasuper_upload \
  -keyalg RSA -keysize 4096 -validity 18250
```

Depois, configure `keystore.properties`:

```properties
storeFile=/caminho/seguro/itasuper-upload-key.jks
storePassword=SENHA_DO_COFRRE
keyAlias=itasuper_upload
keyPassword=SENHA_DA_CHAVE
```

Opcionalmente, é possível usar variáveis de ambiente em vez do arquivo local: `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD`.

## 3. Gerar o Android App Bundle assinado

Com o Java 21 e o Android SDK configurados, execute:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
ANDROID_HOME=/home/ubuntu/.android-sdk \
./gradlew --no-daemon bundleRelease
```

O arquivo será criado em:

```text
app/build/outputs/bundle/release/app-release.aab
```

A configuração interrompe intencionalmente a compilação de release se a assinatura não estiver configurada. Isso evita o envio acidental de um AAB assinado com certificado de depuração.

Antes de cada atualização, aumente o `versionCode` no `app/build.gradle.kts`. O `versionCode` precisa sempre ser maior que o publicado anteriormente. Atualize também `versionName` com a versão mostrada ao público, por exemplo, `1.1` ou `1.0.1`.

## 4. Primeiro envio no Play Console

1. Crie a conta de desenvolvedor e habilite verificação em duas etapas para todos os administradores.
2. Crie o aplicativo com o pacote **`app.itasuper.cliente`**. Esse identificador não poderá ser reutilizado por outro app no Google Play.
3. Na primeira publicação, escolha **Play App Signing** e permita que o Google gere a **app signing key**. A chave local criada acima ficará como **upload key**.
4. Crie uma faixa de **teste interno** e envie o `app-release.aab` assinado.
5. Convide os testadores, valide login, localização, pedidos, checkout, notificações e reaceite de Termos/Política.
6. Depois da validação, conclua a ficha da loja, a declaração de segurança de dados, classificação de conteúdo, público-alvo e informações de acesso ao app antes de enviar para revisão de produção.

## 5. Materiais que ainda devem ser preparados no Play Console

| Entrega | Responsável sugerido | Observação |
|---|---|---|
| Nome público, descrição curta e descrição completa | ItaSuper | Não prometer cupom, prazo ou benefício que o app não ofereça. |
| Ícone de alta resolução e imagem de destaque | ItaSuper | Usar a identidade visual final. |
| Capturas de tela reais | ItaSuper | Capturar Home, Loja, Carrinho/Checkout, Pedidos e Perfil; sem dados pessoais reais. |
| E-mail de suporte e site | ItaSuper | Confirmar que os canais respondem ao usuário. |
| URL da Política de Privacidade | ItaSuper | Usar a página v6.1 publicada no site do ItaSuper. |
| Declaração de Segurança de Dados | ItaSuper + revisão jurídica | Declarar os dados realmente tratados: cadastro, endereço, pedido, localização quando permitida, tokens de notificação, dados de suporte e dados técnicos. |
| Classificação de conteúdo e público-alvo | ItaSuper | Responder com precisão à operação do app. |
| Acesso de revisão | ItaSuper | Criar conta de teste ou fornecer credenciais de demonstração que permitam revisar o fluxo. |

## 6. Verificações antes de produção

Antes de liberar para todos os usuários, faça ao menos um teste interno completo: cadastro, login, recuperação de senha, aceite de Termos/Política, endereço, localização, busca, carrinho, checkout, pedido, notificações, histórico de avisos e exclusão de conta. Confirme também que o comportamento offline exibe mensagem clara e não mantém carregamento infinito.

Para APIs que usem a impressão digital de assinatura — por exemplo, Google Maps, OAuth ou App Links — obtenha a impressão digital da **app signing key** no Play Console após o primeiro envio. A impressão da upload key local não é a que assina o APK distribuído pelo Google Play.[2]

## 7. Recuperação e segurança

Com Play App Signing, a perda da upload key pode ser resolvida por solicitação de redefinição no Play Console. Ainda assim, trate a upload key como segredo. Nunca descarte a cópia de backup até confirmar que a chave foi cadastrada e que uma atualização assinada por ela foi aceita pela faixa de teste.

## Referências

[1]: https://developer.android.com/studio/publish/app-signing "Android Developers — Sign your app"
[2]: https://support.google.com/googleplay/android-developer/answer/9842756?hl=en "Google Play Console Help — Use Play App Signing"
