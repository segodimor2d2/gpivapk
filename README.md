# GROUPED IMAGEM AND VIDEO ANDROID (gpiv)

Player de vídeo Android baseado em **Kotlin + Jetpack Compose + libmpv + FFmpeg**.

O projeto utiliza o `mpv-android` como fonte das bibliotecas nativas e precisa que as dependências nativas sejam preparadas antes da compilação do `gpivapk`.

## 1. Requisitos

Sistema Linux, recomendado Arch Linux.

Ferramentas básicas:

```bash
git
curl
wget
unzip
tar
pkg-config
make
cmake
```

Java:

```text
JDK 17
```

Android:

```text
Android SDK
Android NDK 28.2.13676358
Android NDK 29.0.14206865
Android SDK Platform 36
Android SDK Build-Tools 36.0.0
Android Command-line Tools
```

O `gpivapk` utiliza o NDK:

```text
28.2.13676358
```

O `mpv-android` utiliza o NDK:

```text
29.0.14206865
```

São necessários os dois.

---

# 2. Diretórios utilizados

Neste exemplo:

```text
/home/usuario/
├── Android/
│   └── Sdk/
│
└── 03android/
    ├── gpivapk/
    └── mpv-android/
```

O `gpivapk` espera encontrar o `mpv-android` como projeto irmão:

```text
03android/
├── gpivapk/
└── mpv-android/
```

---

# 3. Android SDK

Defina:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
```

Para deixar permanente no `zsh`:

```bash
echo 'export ANDROID_HOME="$HOME/Android/Sdk"' >> ~/.zshrc
echo 'export ANDROID_SDK_ROOT="$ANDROID_HOME"' >> ~/.zshrc
source ~/.zshrc
```

Confira:

```bash
echo "$ANDROID_HOME"
```

Deve retornar algo como:

```text
/home/usuario/Android/Sdk
```

---

# 4. Android Command-line Tools

Instale o Android Command-line Tools no SDK:

```text
$ANDROID_HOME/cmdline-tools/latest/
```

O executável deverá existir em:

```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager
```

Teste:

```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --version
```

---

# 5. Instalar componentes do Android SDK

Aceite as licenças:

```bash
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
```

Instale os componentes necessários:

```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
    "platform-tools" \
    "platforms;android-36" \
    "build-tools;36.0.0" \
    "ndk;28.2.13676358" \
    "ndk;29.0.14206865"
```

Confira:

```bash
ls -ld "$ANDROID_HOME/ndk/28.2.13676358"
ls -ld "$ANDROID_HOME/ndk/29.0.14206865"
```

---

# 6. Clonar o gpivapk

```bash
cd ~/03android

git clone https://github.com/segodimor2d2/gpivapk.git
cd gpivapk
```

Usar a branch de desenvolvimento:

```bash
git checkout develop
```

Confira:

```bash
git status
```

---

# 7. Clonar o mpv-android

O `gpivapk` utiliza um checkout específico do `mpv-android`.

Clone:

```bash
cd ~/03android

git clone https://github.com/mpv-android/mpv-android.git
cd mpv-android
```

O projeto deve usar o commit:

```text
474111adc4abe5b67f3f8082c8a307e80d45c174
```

Confira:

```bash
git checkout 474111adc4abe5b67f3f8082c8a307e80d45c174
```

Confirme:

```bash
git rev-parse HEAD
```

Resultado esperado:

```text
474111adc4abe5b67f3f8082c8a307e80d45c174
```

---

# 8. Preparar o NDK r29 para o mpv-android

O `buildall.sh` do `mpv-android` procura o NDK em:

```text
buildscripts/sdk/android-ndk-r29
```

O NDK instalado pelo Android SDK fica em:

```text
$ANDROID_HOME/ndk/29.0.14206865
```

Crie o link simbólico:

```bash
cd ~/03android/mpv-android

mkdir -p buildscripts/sdk

ln -s "$ANDROID_HOME/ndk/29.0.14206865" \
    buildscripts/sdk/android-ndk-r29
```

Confira:

```bash
ls -ld buildscripts/sdk/android-ndk-r29
```

Deve mostrar:

```text
android-ndk-r29 -> /home/usuario/Android/Sdk/ndk/29.0.14206865
```

Confira também o toolchain:

```bash
ls -ld buildscripts/sdk/android-ndk-r29/toolchains/llvm/prebuilt/*
```

Deve existir:

```text
linux-x86_64
```

---

# 9. Baixar as dependências do mpv-android

Entre no diretório `buildscripts`:

```bash
cd ~/03android/mpv-android/buildscripts
```

Execute:

```bash
./include/download-deps.sh
```

Esse script prepara as fontes utilizadas pelo build, incluindo:

```text
deps/mbedtls
deps/dav1d
deps/ffmpeg
deps/freetype2
deps/fribidi
deps/harfbuzz
deps/unibreak
deps/libxml2
deps/fontconfig
deps/libass
deps/lua
deps/libplacebo
deps/curl
deps/mpv
```

Confira pelo menos as dependências do FFmpeg:

```bash
ls -ld \
    deps/mbedtls \
    deps/dav1d \
    deps/ffmpeg \
    deps/libxml2
```

---

# 10. Compilar as dependências do mpv-android

O FFmpeg precisa ser compilado para `arm64`.

Execute:

```bash
cd ~/03android/mpv-android/buildscripts

./buildall.sh --arch arm64 --only-deps mpv
```

Esse comando prepara as dependências necessárias para o `mpv`.

O processo gera, entre outros arquivos:

```text
deps/ffmpeg/_build_arm64/libavutil/avconfig.h
```

Esse arquivo é importante porque o `gpivapk` inclui headers gerados pelo FFmpeg.

Confirme:

```bash
ls -l \
    ~/03android/mpv-android/buildscripts/deps/ffmpeg/_build_arm64/libavutil/avconfig.h
```

Se o arquivo existir, o FFmpeg arm64 foi preparado.

---

# 11. Verificar a estrutura final

A estrutura mínima deverá ser semelhante a:

```text
~/03android/
├── gpivapk/
└── mpv-android/
    └── buildscripts/
        ├── deps/
        │   ├── ffmpeg/
        │   │   └── _build_arm64/
        │   │       └── libavutil/
        │   │           └── avconfig.h
        │   ├── mbedtls/
        │   ├── dav1d/
        │   └── libxml2/
        │
        └── sdk/
            └── android-ndk-r29 -> $ANDROID_HOME/ndk/29.0.14206865
```

---

# 12. Compilar o gpivapk

Entre no projeto:

```bash
cd ~/03android/gpivapk
```

Compile:

```bash
./gradlew assembleDebug
```

Uma compilação correta termina com:

```text
BUILD SUCCESSFUL
```

---

# 13. Instalar no Android

Com um dispositivo Android conectado via ADB:

```bash
adb devices
```

Depois:

```bash
./gradlew installDebug
```

---

# 14. Build completo

O comando normal para testar o projeto é:

```bash
cd ~/03android/gpivapk
./gradlew assembleDebug
```

Para instalar:

```bash
./gradlew installDebug
```

---

# 15. Problema: `libavutil/avconfig.h: No such file or directory`

Erro:

```text
fatal error: 'libavutil/avconfig.h' file not found
```

Esse arquivo não pertence diretamente ao código-fonte original do FFmpeg.

Ele é gerado durante a configuração/compilação do FFmpeg.

Verifique:

```bash
ls -l \
    ~/03android/mpv-android/buildscripts/deps/ffmpeg/_build_arm64/libavutil/avconfig.h
```

Se não existir, volte para:

```bash
cd ~/03android/mpv-android/buildscripts
```

Execute:

```bash
./include/download-deps.sh
```

Depois:

```bash
./buildall.sh --arch arm64 --only-deps mpv
```

E verifique novamente o arquivo.

---

# 16. Problema: `Target mbedtls not found`

Erro:

```text
Target mbedtls not found
```

Isso significa que os fontes das dependências ainda não foram preparados.

Execute:

```bash
cd ~/03android/mpv-android/buildscripts
./include/download-deps.sh
```

Depois:

```bash
./buildall.sh --arch arm64 --only-deps mpv
```

---

# 17. Problema: `Can't find toolchain inside NDK`

Erro:

```text
Can't find toolchain inside NDK
```

Verifique:

```bash
ls -ld \
    ~/03android/mpv-android/buildscripts/sdk/android-ndk-r29/toolchains/llvm/prebuilt/*
```

Deve existir:

```text
linux-x86_64
```

Se o link do NDK não existir:

```bash
cd ~/03android/mpv-android

mkdir -p buildscripts/sdk

ln -s "$ANDROID_HOME/ndk/29.0.14206865" \
    buildscripts/sdk/android-ndk-r29
```

---

# 18. Limpeza

Evite apagar caches ou diretórios inteiros sem necessidade.

Para limpar somente o build do FFmpeg:

```bash
cd ~/03android/mpv-android/buildscripts
./buildall.sh --arch arm64 --only-deps mpv --clean
```

Use `--clean` somente quando realmente for necessário recompilar as dependências.

No `gpivapk`, para uma limpeza normal do Gradle:

```bash
cd ~/03android/gpivapk
./gradlew clean
```

Depois:

```bash
./gradlew assembleDebug
```

---

# 19. Verificação rápida de uma máquina nova

Depois de preparar tudo, estes comandos devem funcionar:

```bash
java -version
```

```bash
ls -ld "$ANDROID_HOME/ndk/28.2.13676358"
```

```bash
ls -ld "$ANDROID_HOME/ndk/29.0.14206865"
```

```bash
ls -ld ~/03android/mpv-android/buildscripts/sdk/android-ndk-r29
```

```bash
ls -l \
    ~/03android/mpv-android/buildscripts/deps/ffmpeg/_build_arm64/libavutil/avconfig.h
```

E finalmente:

```bash
cd ~/03android/gpivapk
./gradlew assembleDebug
```

O resultado esperado é:

```text
BUILD SUCCESSFUL
```

---

# 20. Resumo — instalação do zero

Em uma máquina nova, a sequência principal é:

```bash
# Diretórios
mkdir -p ~/03android
cd ~/03android

# gpivapk
git clone https://github.com/segodimor2d2/gpivapk.git
cd gpivapk
git checkout develop

# mpv-android
cd ~/03android
git clone https://github.com/mpv-android/mpv-android.git
cd mpv-android
git checkout 474111adc4abe5b67f3f8082c8a307e80d45c174

# Link do NDK r29
mkdir -p buildscripts/sdk
ln -s "$ANDROID_HOME/ndk/29.0.14206865" \
    buildscripts/sdk/android-ndk-r29

# Dependências
cd buildscripts
./include/download-deps.sh

# FFmpeg + dependências para arm64
./buildall.sh --arch arm64 --only-deps mpv

# Verificação
ls -l deps/ffmpeg/_build_arm64/libavutil/avconfig.h

# Build do aplicativo
cd ~/03android/gpivapk
./gradlew assembleDebug

# Instalação
./gradlew installDebug
```

## Observação importante

O `gpivapk` e o `mpv-android` são projetos separados.

O `gpivapk` **não deve depender de uma cópia pré-compilada do diretório `_build_arm64`** transferida de outra máquina.

O procedimento correto para uma máquina nova é:

```text
Android SDK
    ↓
NDK 28.2 + NDK 29
    ↓
mpv-android
    ↓
download-deps.sh
    ↓
buildall.sh --arch arm64 --only-deps mpv
    ↓
FFmpeg _build_arm64
    ↓
gpivapk
    ↓
./gradlew assembleDebug
```

Assim os artefatos nativos são gerados localmente na nova máquina.
