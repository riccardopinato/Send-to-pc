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

## Identità di firma permanente

È stata generata una keystore persistente dedicata al progetto.

Fingerprint SHA-256 pubblico del certificato previsto:

`248AE3BEDEC1CA6F80B2272F0ACDE5F010987864B4F5754FE2FA499353D77CE4`

La pipeline release rifiuta automaticamente un APK firmato con un certificato diverso da questo fingerprint.

La chiave privata non deve essere committata nel repository. Va conservata in backup sicuri e caricata in CI esclusivamente tramite GitHub Secrets.

Per Google Play questa chiave viene usata come **upload key**. Se Play App Signing è attivo, Google conserva e usa separatamente la Play app signing key per firmare gli APK distribuiti dallo Store. Per distribuzione APK diretta, invece, questa stessa chiave deve rimanere stabile per mantenere la continuità degli aggiornamenti.

## Stato attuale

La keystore e l'identità pubblica sono state create. La release firmata resta **BLOCKED** soltanto finché i quattro GitHub Secrets non vengono configurati nel repository.

## Same-artifact rule

L'APK fisicamente verificato deve essere lo stesso artifact, con lo stesso SHA-256, poi distribuito. Una rebuild produce un artifact diverso e richiede nuova evidence.

## Versione roadmap

- versionName: 1.16.0
- versionCode: 27
- package: com.riccardopinato.inviaalpc
