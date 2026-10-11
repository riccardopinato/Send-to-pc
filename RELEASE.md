# Release engineering

## Lane INTERNAL

Build type: `slim`

Scopo:
- sviluppo;
- QA;
- installazione manuale interna.

Caratteristiche:
- R8;
- resource shrinking;
- APK leggero;
- firma debug.

Una build INTERNAL non è una release production con identità di firma permanente.

## Lane RELEASE

Workflow:
`.github/workflows/android-release.yml`

Richiede i GitHub Secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

La keystore non deve essere committata.

Output:
- APK release firmato;
- AAB;
- fingerprint certificato;
- SHA256SUMS;
- RELEASE_IDENTITY con source SHA e workflow run.

## Stato attuale

La pipeline release è implementata, ma la release firmata resta **BLOCKED** finché i quattro secret non sono configurati con una keystore persistente posseduta e salvata dall'utente.

## Same-artifact rule

L'APK fisicamente verificato deve essere lo stesso artifact, con lo stesso SHA-256, poi distribuito. Una rebuild produce un artifact diverso e richiede nuova evidence.

## Versione roadmap

- versionName: 1.16.0
- versionCode: 27
- package: com.riccardopinato.inviaalpc
