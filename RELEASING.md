# Reproducible Builds and Releases

## Versioning

Create releases from clean commits with tags in `vMAJOR.MINOR.PATCH` form, such
as `v1.0.0`. Release candidates may use `vMAJOR.MINOR.PATCH-rcN`, such as
`v1.0.0-rc1`. Android `versionName` is the tag without its leading `v`.

The `versionCode` base is `MAJOR * 10,000,000 + MINOR * 100,000 + PATCH *
1,000`. An RC adds its RC number (1-998); a stable release adds 999. `MINOR`
and `PATCH` must each be below 100, and the result must not exceed Android's
2,100,000,000 limit. This gives `1.0.0-rc1` versionCode `10000001` and final
`1.0.0` versionCode `10000999`, so the stable APK updates an installed RC.

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

The existing `v1.0.0-rc1` tag is already pushed. After pushing the workflow
changes on `main`, run the workflow manually to build that existing tag:

1. Create a persistent release keystore and store a secure offline backup.
2. Add the four `ANDROID_RELEASE_*` repository Actions secrets listed above.
3. Commit and push the release workflow changes to `main`.
4. In GitHub, open **Actions > Release > Run workflow**. Select `main` as the
   branch and enter `v1.0.0-rc1` in the tag input.
5. Start the workflow. It checks out the tag, builds and compares two unsigned
   APKs, signs the verified APK, and creates the GitHub prerelease with
   `HabitCounter-1.0.0-rc1.apk` attached.
6. Confirm the workflow succeeds and the APK is listed under **Releases**.

Do not push or move `v1.0.0-rc1` again; it already exists on the remote. Future
tags trigger automatically. To publish the stable release, push a clean
`v1.0.0` tag after the release workflow changes are on `main`; it creates a
non-prerelease titled `HabitCounter 1.0.0` with `HabitCounter-1.0.0.apk`.
No release is published unless all four signing secrets are present.

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
  -PreleaseVersionCode=10000999
```

The resulting APK is unsigned at
`app/build/outputs/apk/release/app-release-unsigned.apk`.
