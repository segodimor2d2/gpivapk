# GROUPED IMAGEM AND VIDEO ANDROID (gpiv)

Sim. E agora temos uma situação muito boa: **o projeto passou pelo teste de clone limpo**, então podemos montar um procedimento reproduzível para um Arch Linux completamente novo.

A ideia é separar em:

1. instalar ferramentas do sistema;
2. instalar Android SDK/NDK/CMake;
3. clonar o `gpivapk`;
4. obter exatamente as versões das dependências externas;
5. criar `local.properties`;
6. compilar;
7. conectar o Android;
8. instalar e executar.

---

# Procedimento completo — Arch Linux do zero

## 0. Pré-requisitos

Computador:

```text
Arch Linux 64-bit
Internet
Git
Um telefone Android compatível
```

Não é necessário ter Android Studio instalado para compilar pelo terminal.

---

# 1. Atualizar o Arch

Em uma instalação nova:

```bash
sudo pacman -Syu
```

Reinicie se o sistema solicitar.

---

# 2. Instalar ferramentas básicas

```bash
sudo pacman -S --needed \
    git \
    base-devel \
    curl \
    wget \
    unzip \
    zip \
    tar \
    cmake \
    ninja \
    pkgconf \
    python \
    jdk17-openjdk
```

Confirme:

```bash
git --version
cmake --version
java -version
python --version
```

O Java precisa ser **JDK 17**.

---

# 3. Configurar Java 17

Confira:

```bash
archlinux-java status
```

Se houver outra versão selecionada:

```bash
sudo archlinux-java set java-17-openjdk
```

Depois:

```bash
java -version
```

---

# 4. Instalar Android SDK

No Arch, uma maneira simples é instalar as ferramentas do Android pelo `pacman`/AUR conforme o ambiente disponível.

O que precisamos no final é uma instalação semelhante a:

```text
~/Android/Sdk/
```

com:

```text
platform-tools
build-tools
platforms
ndk
cmake
cmdline-tools
```

### Componentes necessários para este projeto

Precisamos especificamente de:

```text
Android SDK Platform 36
Android SDK Build Tools
Android SDK Platform Tools
Android NDK 28.2.13676358
CMake 3.22.1
```

**Importante:** não substitua o NDK 28.2 por uma versão mais nova só porque ela está disponível.

O projeto atualmente está fixado em:

```kotlin
ndkVersion = "28.2.13676358"
```

---

# 5. Verificar o Android SDK

Depois de instalar/configurar:

```bash
ls ~/Android/Sdk
```

Deve existir algo parecido com:

```text
build-tools
cmake
cmdline-tools
ndk
platform-tools
platforms
```

Confira o NDK:

```bash
ls ~/Android/Sdk/ndk
```

Precisamos encontrar:

```text
28.2.13676358
```

Confira o CMake:

```bash
ls ~/Android/Sdk/cmake
```

Precisamos de:

```text
3.22.1
```

---

# 6. Criar a pasta dos projetos

No computador novo:

```bash
mkdir -p ~/03android
cd ~/03android
```

---

# 7. Clonar o projeto GPIV

Se o projeto estiver em um repositório Git remoto:

```bash
git clone <URL_DO_REPOSITORIO> gpivapk
```

Depois:

```bash
cd ~/03android/gpivapk
```

Se você estiver transferindo o repositório de outra máquina, também pode simplesmente copiar/clonar o repositório Git.

---

# 8. Conferir o estado do projeto

```bash
git status
```

Deve aparecer algo semelhante a:

```text
On branch main
nothing to commit, working tree clean
```

---

# 9. Obter as dependências externas

Aqui existe uma característica importante do nosso projeto:

**o Git do `gpivapk` não contém o código-fonte completo do MPV, mpv-android e FFmpeg.**

Eles são dependências externas.

Precisamos recriar:

```text
~/03android/
├── gpivapk/
├── mpv/
└── mpv-android/
```

---

# 10. Clonar MPV

```bash
cd ~/03android

git clone https://github.com/mpv-player/mpv.git
cd mpv
```

Agora devemos usar **exatamente o commit que estamos utilizando**:

```text
f5bcfb195412e0ca733eac2e850879cd3b1ded18
```

Execute:

```bash
git checkout f5bcfb195412e0ca733eac2e850879cd3b1ded18
```

Confirme:

```bash
git rev-parse HEAD
```

Deve retornar:

```text
f5bcfb195412e0ca733eac2e850879cd3b1ded18
```

---

# 11. Clonar mpv-android

```bash
cd ~/03android

git clone https://github.com/mpv-android/mpv-android.git
cd mpv-android
```

Fixar o commit:

```bash
git checkout 725e3d675cc6cb385ebbf7b16fc258401d422ee8
```

Confirmar:

```bash
git rev-parse HEAD
```

Deve retornar:

```text
725e3d675cc6cb385ebbf7b16fc258401d422ee8
```

---

# 12. Verificar FFmpeg

O FFmpeg utilizado pelo nosso projeto fica dentro do `mpv-android`:

```bash
~/03android/mpv-android/buildscripts/deps/ffmpeg
```

Confira:

```bash
cd ~/03android/mpv-android/buildscripts/deps/ffmpeg
git rev-parse HEAD
```

Precisamos de:

```text
c9e36046a338638279782cba4fba3299bf65f46b
```

Se o checkout do `mpv-android` já deixar essa árvore no estado correto, ótimo.

---

# 13. Voltar ao GPIV

```bash
cd ~/03android/gpivapk
```

---

# 14. Criar `local.properties`

Este arquivo **não entra no Git** porque contém caminhos específicos daquela máquina.

Crie:

```bash
cat > local.properties <<'EOF'
sdk.dir=/home/SEU_USUARIO/Android/Sdk

GPIV_MPV_ROOT=/home/SEU_USUARIO/03android/mpv
GPIV_MPV_ANDROID_ROOT=/home/SEU_USUARIO/03android/mpv-android
GPIV_FFMPEG_ROOT=/home/SEU_USUARIO/03android/mpv-android/buildscripts/deps/ffmpeg
EOF
```

Substitua `SEU_USUARIO` pelo usuário daquela máquina.

Por exemplo, se o usuário for `joao`:

```properties
sdk.dir=/home/joao/Android/Sdk

GPIV_MPV_ROOT=/home/joao/03android/mpv
GPIV_MPV_ANDROID_ROOT=/home/joao/03android/mpv-android
GPIV_FFMPEG_ROOT=/home/joao/03android/mpv-android/buildscripts/deps/ffmpeg
```

---

# 15. Conferir o `local.properties`

```bash
cat local.properties
```

E:

```bash
git status --short
```

**`local.properties` não deve aparecer como arquivo modificado.**

Isso é importante: cada computador terá seu próprio `local.properties`.

---

# 16. Conferir o NDK

No projeto:

```bash
grep -n "ndkVersion" app/build.gradle.kts
```

Precisamos ver:

```text
ndkVersion = "28.2.13676358"
```

---

# 17. Primeiro teste — Clean

Agora:

```bash
./gradlew clean
```

Esperamos:

```text
BUILD SUCCESSFUL
```

---

# 18. Segundo teste — Build

```bash
./gradlew assembleDebug -Pandroid.injected.build.abi=arm64-v8a
```

Esperamos:

```text
BUILD SUCCESSFUL
```

O APK será gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

# 19. Conectar o Android por USB

No telefone:

```text
Configurações
→ Sobre o telefone
→ Número da versão
```

Ative as opções de desenvolvedor e:

```text
Depuração USB
```

No computador:

```bash
adb devices -l
```

Se o `adb` ainda não estiver disponível no PATH, use o `platform-tools` do SDK.

O telefone deve aparecer como:

```text
device
```

---

# 20. Instalar

```bash
./gradlew installDebug -Pandroid.injected.build.abi=arm64-v8a
```

Se aparecer:

```text
BUILD SUCCESSFUL
```

a instalação terminou.

---

# 21. Abrir o aplicativo

Podemos abrir manualmente pelo telefone ou pelo ADB.

Por exemplo:

```bash
adb shell monkey -p com.rec.gpiv 1
```

---

# 22. Teste funcional

No telefone:

```text
[ ] GPIV abre
[ ] Tela inicial aparece
[ ] Nenhum vídeo de teste inicia automaticamente
[ ] Picker abre
[ ] É possível escolher um vídeo
[ ] Vídeo aparece
[ ] Play funciona
[ ] Pause funciona
[ ] Troca de vídeo funciona
[ ] Aplicativo não fecha
```

---

# 23. Verificar logs, se necessário

```bash
adb logcat -c
```

Abra o aplicativo e depois:

```bash
adb logcat -v threadtime | grep -E \
'GPIV_NATIVE|MpvNative|MpvPlayer|PlayerScreen|FATAL EXCEPTION'
```

---

# 24. Estrutura final esperada

Em uma máquina nova, queremos:

```text
~/03android/
│
├── gpivapk/
│   ├── app/
│   ├── gradle/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradlew
│   └── local.properties       ← NÃO versionado
│
├── mpv/
│   └── ...                     ← commit fixado
│
└── mpv-android/
    ├── buildscripts/
    │   └── deps/
    │       └── ffmpeg/
    └── ...                     ← commit fixado
```

---

# Versões que precisamos preservar

Esta é a parte mais importante para o futuro:

| Componente      | Versão/commit                              |
| --------------- | ------------------------------------------ |
| **GPIV**        | Git commit do projeto                      |
| **MPV**         | `f5bcfb195412e0ca733eac2e850879cd3b1ded18` |
| **mpv-android** | `725e3d675cc6cb385ebbf7b16fc258401d422ee8` |
| **FFmpeg**      | `c9e36046a338638279782cba4fba3299bf65f46b` |
| **Android NDK** | `28.2.13676358`                            |
| **CMake**       | `3.22.1`                                   |
| **Compile SDK** | `36.1`                                     |
| **Gradle**      | `9.4.1`                                    |
| **AGP**         | `9.2.1`                                    |
| **Kotlin**      | `2.2.10`                                   |
| **Java**        | `17`                                       |
| **ABI atual**   | `arm64-v8a`                                |

---

## Um detalhe importante

Há uma diferença entre **"reproduzir o ambiente de desenvolvimento"** e **"reproduzir 100% o build original do MPV"**.

Nós já resolvemos a parte do projeto Android: o `gpivapk` agora declara o NDK e recebe os caminhos externos por `local.properties`.

Mas os `.so` do MPV/FFmpeg **já estão dentro do Git do GPIV**:

```text
app/src/main/jniLibs/arm64-v8a/
```

Então o computador novo **não precisa recompilar MPV/FFmpeg para compilar o GPIV**. Ele precisa dos fontes externos principalmente porque o CMake usa os headers deles.

Isso é uma grande vantagem do estado atual.

### Portanto, o procedimento mínimo depois que tudo estiver instalado é:

```bash
cd ~/03android

git clone <URL> gpivapk
git clone https://github.com/mpv-player/mpv.git
git clone https://github.com/mpv-android/mpv-android.git

cd mpv
git checkout f5bcfb195412e0ca733eac2e850879cd3b1ded18

cd ../mpv-android
git checkout 725e3d675cc6cb385ebbf7b16fc258401d422ee8

cd ../gpivapk
# criar local.properties

./gradlew clean
./gradlew assembleDebug -Pandroid.injected.build.abi=arm64-v8a
./gradlew installDebug -Pandroid.injected.build.abi=arm64-v8a
```

**Eu considero o próximo passo ideal agora criar um `README.md` de instalação completo no próprio projeto**, contendo esse procedimento, para que daqui a alguns meses você não dependa desta conversa para montar outro computador.
