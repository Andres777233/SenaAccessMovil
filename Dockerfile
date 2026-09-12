# Build reproducible del APK de SenaAccess Móvil.
# Produce el MISMO APK en cualquier máquina o CI sin SDK instalado en el host:
# aquí se instalan JDK 17 + Android SDK (platform, build-tools) y se compila con
# el wrapper de Gradle. La compatibilidad entre dispositivos se resuelve en
# código (minSdk, insets, R8, biometría); este contenedor solo garantiza que el
# binario se genere idéntico siempre.

FROM eclipse-temurin:17-jdk AS build

# Versiones del SDK descargadas en el contenedor (configurables).
ARG ANDROID_CMDLINE_TOOLS=11076708
ARG ANDROID_PLATFORM=android-36
ARG BUILD_TOOLS=36.0.0

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV GRADLE_USER_HOME=/opt/gradle-home

# Herramienta mínima para descomprimir el SDK.
RUN apt-get update && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

# Descarga e instala las command-line tools de Android en su ubicación canónica.
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && curl -fsSL -o /tmp/android-cmdline.zip \
       "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS}_latest.zip" \
    && unzip -q /tmp/android-cmdline.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm -f /tmp/android-cmdline.zip

ENV PATH=${ANDROID_HOME}/cmdline-tools/latest/bin:${PATH}

# Acepta las licencias e instala la plataforma objetivo y las build-tools.
RUN yes | sdkmanager --sdk_root=${ANDROID_HOME} --licenses >/dev/null 2>&1 \
    && sdkmanager --sdk_root=${ANDROID_HOME} \
       "platforms;${ANDROID_PLATFORM}" "build-tools;${BUILD_TOOLS}" platform-tools

WORKDIR /app

# Primero la configuración de Gradle para cachear la descarga de la distribución.
COPY gradlew gradlew.bat settings.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Primera pasada: descarga Gradle + los plugins y dependencias de configuración.
COPY app/build.gradle.kts ./app/
RUN ./gradlew --version >/dev/null

# Ya con las dependencias cacheadas, copia el proyecto completo y compila.
COPY app ./app
RUN ./gradlew :app:assembleDebug :app:assembleRelease --no-daemon

# Punto de montaje donde el compose/CI recoge los APK generados.
RUN mkdir -p /artifacts
CMD ["sh", "-c", "./gradlew :app:assembleDebug :app:assembleRelease --no-daemon && cp -r app/build/outputs/apk /artifacts/"]