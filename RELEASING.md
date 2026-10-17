# Reproducible Builds and Releases

## Versioning

Create releases from clean commits with tags in `vMAJOR.MINOR.PATCH` form, such
as `v1.0.0`. The GitHub Actions workflow rejects other tag formats. Android
`versionName` is the tag without its leading `v`. The `versionCode` is
`MAJOR * 1,000,000 + MINOR * 1,000 + PATCH`; `MINOR` and `PATCH` must each be
less than 1000. This produces version code `1000000` for `1.0.0`.

The workflow builds the unsigned release APK twice from the tagged commit and
requires byte-for-byte equality before signing. It then creates a signed APK
and publishes it as a GitHub Release asset. Release signing is separate from
the reproducible APK build; F-Droid builds the unsigned APK from source and
signs it with F-Droid's key.

## GitHub Signing Setup

Create one long-lived Android release keystore and keep a secure offline
backup. Losing or replacing this key prevents direct-download users from
installing future APKs as updates. Never commit the keystore or its passwords.

For example, create a key with:

```sh
keytool -genkeypair -v \
  -keystore habitcounter-release.jks \
  -alias habitcounter \
  -keyalg RSA -keysize 4096 -validity 10000
```

Add these repository Actions secrets in GitHub:

- `ANDROID_RELEASE_KEYSTORE_BASE64`: single-line base64 of the keystore
- `ANDROID_RELEASE_KEYSTORE_PASSWORD`: keystore password
- `ANDROID_RELEASE_KEY_ALIAS`: `habitcounter` (or the alias selected above)
- `ANDROID_RELEASE_KEY_PASSWORD`: private-key password

On macOS, produce the base64 value with
`base64 -i habitcounter-release.jks | tr -d '\n'`. Store it directly in GitHub
Secrets; do not put it in the repository or `.env`.

After pushing a clean commit and its tag, for example `v1.0.0`, the workflow
builds, compares, signs, and attaches `HabitCounter-1.0.0.apk` to the GitHub
Release. No release is published unless all four signing secrets are present.

## F-Droid

F-Droid's official build metadata is maintained in the separate `fdroiddata`
repository. `fdroid/metadata.template.yml` is a starting point for its metadata
file; it is not consumed automatically from this app repository. Before
submitting, replace `OWNER/REPOSITORY` with this public GitHub repository and
replace the commit placeholder with the full commit SHA for the release tag.
Then submit the metadata through the F-Droid inclusion process.

The metadata uses `UpdateCheckMode: Static` because release versions are
provided as Gradle properties, not stored as literal values that F-Droid can
reliably discover. For each release, add a `Builds` entry with the tag's
version name, mapped version code, full commit SHA, and matching
`releaseVersionName` and `releaseVersionCode` Gradle properties. F-Droid builds
from source and signs the resulting APK itself; the GitHub signing key is not
needed for F-Droid.

## Local Verification

Use the Gradle wrapper and a JDK 21 installation. The wrapper distribution
checksum, dependency locks, and SHA-256 dependency verification metadata are
committed to the repository. The Android SDK platform and Build Tools versions
are pinned in the Gradle configuration.

```sh
./gradlew --no-daemon --max-workers=1 clean :app:assembleRelease \
  -PreleaseVersionName=1.0.0 \
  -PreleaseVersionCode=1000000
```

The resulting APK is unsigned at
`app/build/outputs/apk/release/app-release-unsigned.apk`.