# Invia al PC / Send to PC

Utility Android nativa per trasferire file, foto, testo e link tra telefono e computer sulla stessa rete locale.

## Principi del prodotto

- nessun account;
- nessun cloud;
- nessun software obbligatorio sul PC;
- collegamento tramite QR / URL locale + PIN;
- trasferimento bidirezionale nella stessa sessione;
- file, testo e link in entrambe le direzioni telefono ↔ PC;
- progress complessivo per sessione e riepilogo operazione;
- upload resumable con pausa, ripresa e annullamento;
- download HTTP Range;
- PC fidati revocabili;
- cronologia locale raggruppata per sessione;
- apertura/copia/condivisione dei contenuti ricevuti;
- riepilogo operazione;
- tema Sistema / Chiaro / Scuro;
- monetizzazione AdMob separata dal core di trasferimento;
- build Android leggera.

## Stack

- Kotlin;
- Jetpack Compose / Material 3;
- server HTTP LAN nativo tramite ServerSocket;
- MediaStore Downloads per i file ricevuti;
- ZXing per il QR code;
- Google Mobile Ads Next-Gen SDK;
- Google UMP;
- minSdk 29;
- targetSdk 36;
- Java 17.

## Sicurezza

La sessione usa:
- token casuale ad alta entropia nell'URL;
- PIN a 4 cifre con rate limit;
- cookie di sessione HttpOnly + SameSite=Strict;
- credenziale PC fidato HttpOnly, con scadenza a 30 giorni e rotazione dopo ogni riconnessione riuscita;
- CSP, no-store, no-referrer e anti-framing;
- server in ascolto solo sull'indirizzo LAN attivo.

Il traffico locale usa HTTP e **non è cifrato**. Usare l'app solo su Wi-Fi privata o fidata. Questo resta un gate di sicurezza prima di dichiarare la release Store Ready. Vedi [THREAT_MODEL.md](THREAT_MODEL.md) e [store/DATA_SAFETY.md](store/DATA_SAFETY.md).

## Lingue

Lingua dispositivo di default, fallback inglese e override manuale:
- English;
- Italiano;
- Español;
- Français;
- Deutsch;
- Português.

La v1.19 mantiene parità delle chiavi applicative correnti in tutte e sei le lingue; un test automatico impedisce regressioni di copertura. Il fallback inglese resta disponibile solo come meccanismo difensivo per eventuali chiavi future non ancora localizzate.

## Monetizzazione

v1.17 introduce:
- banner adaptive fuori dalla sessione di trasferimento attiva;
- interstitial solo alla chiusura esplicita di una sessione completata e significativa;
- frequency cap interno;
- UMP prima delle richieste pubblicitarie quando applicabile;
- test ads obbligatori per debug/slim/QA.

Un trasferimento breve e piccolo non deve causare automaticamente un interstitial.

Gli ID AdMob production non sono committati. Se non sono configurati, la build release usa gli ID ufficiali di test e NON è pronta alla monetizzazione Store.

## Build

La CI esegue:
- unit test;
- Android Lint;
- build Slim con R8/resource shrinking;
- verifica firma;
- SHA-256 artifact.

`slim` è la lane INTERNAL e usa ancora la firma debug.

La release stabile usa invece una keystore persistente tramite GitHub Secrets e produce APK + AAB. Vedi [RELEASE.md](RELEASE.md).

## QA

La build CI verde non equivale a certificazione runtime. Gli scenari su device reale sono elencati in [QA-CHECKLIST.md](QA-CHECKLIST.md).

## Privacy

I file trasferiti restano sulla rete locale e non vengono caricati su un cloud dell'app. AdMob/UMP effettuano connessioni Internet separate per advertising/privacy. Vedi [PRIVACY.md](PRIVACY.md).

## Store preparation

La cartella [store](store) contiene:
- bozza Data Safety;
- checklist AdMob/app-ads.txt;
- esempio app-ads.txt senza publisher ID reale;
- checklist Store release.

La v1.19 introduce inoltre un gate `store_candidate` nel workflow release: un candidato Store fallisce se gli ID AdMob production non sono configurati.

## Roadmap

Vedi [ROADMAP.md](ROADMAP.md).
