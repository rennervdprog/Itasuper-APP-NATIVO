# Builds assinadas no GitHub Actions

O workflow `.github/workflows/android-release.yml` gera um Android App Bundle assinado somente quando for iniciado manualmente por alguém com acesso ao repositório. A upload key não é versionada: ela entra no executor temporário somente por meio de **GitHub Actions Secrets**, é usada para o build e é removida ao final.

> **Segurança:** GitHub Actions Secrets são criptografados no repositório. Ainda assim, acesso de administrador ao repositório e permissão para executar workflows são sensíveis. Restrinja essas permissões, exija autenticação de dois fatores e não aceite alterações no workflow de release de pessoas não confiáveis. Nunca coloque segredos em issues, pull requests, arquivos do projeto, logs ou variáveis comuns do GitHub.

## Secrets obrigatórios

| Nome do Secret | Conteúdo | Observação |
|---|---|---|
| `ANDROID_UPLOAD_KEYSTORE_BASE64` | Conteúdo Base64 do arquivo `itasuper-upload-key.jks`. | Não inserir quebras de linha desnecessárias. |
| `ANDROID_KEYSTORE_PASSWORD` | Senha do keystore. | Deve permanecer confidencial. |
| `ANDROID_KEY_ALIAS` | `itasuper_upload` | Não é segredo, mas é mantido como Secret para simplificar o fluxo. |
| `ANDROID_KEY_PASSWORD` | Senha da chave. | Deve permanecer confidencial. |

## Configuração pela interface do GitHub

No repositório `rennervdprog/Itasuper-APP-NATIVO`, abra **Settings → Secrets and variables → Actions → New repository secret**. Cadastre os quatro nomes acima. Para obter o conteúdo Base64 do arquivo `.jks` em Linux/macOS, execute:

```bash
base64 -w 0 itasuper-upload-key.jks
```

No Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\caminho\itasuper-upload-key.jks"))
```

Cole a saída inteira no secret `ANDROID_UPLOAD_KEYSTORE_BASE64`. Em seguida, cadastre as senhas e o alias. Depois de salvar, o GitHub não permite ler o valor novamente; mantenha a cópia privada da chave e da senha em backup separado.

## Como gerar uma atualização

1. Altere o `versionCode` e o `versionName` em `app/build.gradle.kts`. O `versionCode` deve ser maior que o valor já aceito pelo Google Play.
2. Publique o código aprovado na branch `main`.
3. Abra **Actions → Android Release Bundle → Run workflow**.
4. Informe uma identificação interna, por exemplo `1.0.1`, e execute na branch `main`.
5. Aguarde o job concluir e baixe o artefato AAB gerado. O artefato fica disponível por 14 dias.
6. Envie o `.aab` para a faixa de teste interno no Play Console. Não envie diretamente para produção sem validar o fluxo completo.

## O que não fazer

Não adicione `keystore.properties`, `*.jks`, `*.keystore`, `*.pem`, senhas ou o conteúdo Base64 da chave ao GitHub. Não use esses dados em pull requests de terceiros. Não registre a upload key em provedor público de build. Se houver suspeita de vazamento, interrompa novas builds e solicite redefinição da upload key no Play Console após o primeiro cadastro.

## Separação de responsabilidades

A **upload key** identifica quem pode enviar bundles ao Google Play. A **app signing key** será gerenciada pelo Google ao habilitar Play App Signing. Para APIs que exigem SHA-1/SHA-256 — como App Links, OAuth ou mapas — use a impressão digital da **app signing key** exibida pelo Play Console após o primeiro upload, e não apenas a impressão da upload key local.
