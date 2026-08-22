# Geração de APK e AAB com versão atualizável

O workflow **Android Release AAB and APK** gera, na mesma execução, um **APK release assinado** e um **AAB release assinado**. Os dois usam exatamente o mesmo `versionCode` e `versionName`.

> Para instalar uma atualização sobre o APK já instalado no celular, mantenha a mesma chave de assinatura e use um `versionCode` maior que o da versão instalada.

## Como gerar uma nova versão no GitHub

1. Abra o repositório **Itasuper-APP-NATIVO** no GitHub e entre na aba **Actions**.
2. Selecione o workflow **Android Release AAB and APK**.
3. Clique em **Run workflow** e mantenha o branch `main`.
4. Preencha os dois campos de versão.
5. Clique em **Run workflow** e aguarde a conclusão.
6. Abra a execução concluída e baixe os dois artefatos gerados.

| Campo | Para que serve | Exemplo da próxima atualização |
|---|---|---|
| `version_code` | Número técnico usado pelo Android e pela Play Store. Deve sempre aumentar e nunca pode ser repetido. | `12` |
| `version_name` | Número que aparece para você e para o cliente. Pode seguir o formato comercial de versão. | `1.0.11` |

Para o APK que já foi distribuído com `versionCode 11`, use na próxima geração pelo menos:

```text
version_code: 12
version_name: 1.0.11
```

Na atualização seguinte, use `13` e `1.0.12`, e assim sucessivamente. Mesmo que você queira manter o mesmo texto em `version_name`, o `version_code` ainda precisa aumentar.

## Qual arquivo usar

| Artefato | Uso |
|---|---|
| `itasuper-cliente-apk-...` | Instalação direta no celular e testes internos. |
| `itasuper-cliente-aab-...` | Envio para o Google Play Console. O AAB não é instalado diretamente no celular. |

Os artefatos já incluem a versão e o código no nome para evitar confusão entre builds. O workflow recebe a chave de assinatura somente por Secrets do GitHub; nunca coloque a chave, Base64, senhas ou `keystore.properties` no Git.

## Geração local opcional

Se estiver usando Android Studio ou terminal, você pode escolher a versão sem editar arquivos:

```bash
./gradlew :app:assembleRelease :app:bundleRelease \
  -PAPP_VERSION_CODE=12 \
  -PAPP_VERSION_NAME=1.0.11
```

A geração local de release continua exigindo uma configuração de assinatura privada fora do Git.
