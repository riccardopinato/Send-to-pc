# Signing identity — Invia al PC

Questa pagina contiene solo informazioni pubbliche dell'identità di firma. Non contiene la chiave privata né password.

## Upload / direct APK key

- package: `com.riccardopinato.inviaalpc`
- key algorithm: RSA 4096
- signature algorithm: SHA256withRSA
- keystore format: JKS
- alias: `inviaalpc-upload`
- certificate SHA-256:

`248AE3BEDEC1CA6F80B2272F0ACDE5F010987864B4F5754FE2FA499353D77CE4`

## Uso

- APK distribuiti direttamente: devono restare firmati con questa chiave per gli aggiornamenti in-place.
- AAB Google Play: questa chiave è l'upload key.
- Con Play App Signing, la chiave che firma gli APK distribuiti dal Play Store è gestita separatamente da Google.

## CI invariant

Il workflow `.github/workflows/android-release.yml` confronta il certificato dell'APK costruito con il fingerprint sopra e fallisce se non coincide.

## Segreti richiesti

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

La keystore e le password non devono mai essere committate.
