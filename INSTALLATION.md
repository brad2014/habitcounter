1. Install Android Studio

Download and install the latest stable Android Studio for macOS from https://developer.android.com/studio. Complete the setup wizard and accept the Android SDK licenses.

2. Install Android SDK Command-line Tools

In Android Studio, open **Android Studio > Settings > Languages & Frameworks > Android SDK > SDK Tools**. Select **Android SDK Command-line Tools (latest)** and apply the changes. Also ensure **Android SDK Platform-Tools** and **Android SDK Build-Tools 36.0.0** are installed. These tools use the SDK location shown in that settings window, which should be `$HOME/Library/Android/sdk` for this setup.

3. Select the Gradle JDK

Open **Android Studio > Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK** and select Android Studio's bundled JDK (JBR). AGP requires JDK 17 or newer.

4. Configure the local environment

The repository ignores `.env` because its paths are machine-specific. At the repository root, create or update it with:

```sh
ANDROID_HOME="$HOME/Library/Android/sdk"
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
PATH="${PATH}:${JAVA_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator:${ANDROID_HOME}/build-tools/36.0.0"
```

Android Studio and Gradle do not automatically load `.env`. In a terminal, export its values before using the CLI or building:

```sh
set -a && source .env && set +a
```

5. Install the required SDK platform and build

Use Android CLI to install the latest stable API 37 platform. Accept any SDK license prompts when they appear:

```sh
android sdk install platforms/android-37.2
```

The project compiles and targets SDK 37. The Gradle wrapper downloads Gradle 9.8.1; Gradle resolves AGP 9.4.0, Kotlin 2.4.21, and the Compose BOM from the project configuration. No global Gradle or Kotlin installation is needed.

Build the debug APK with:

```sh
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`. To run the app in an emulator, install an API 37 emulator image from **Android Studio > Settings > Languages & Frameworks > Android SDK > SDK Platforms** and create an AVD in Device Manager.
