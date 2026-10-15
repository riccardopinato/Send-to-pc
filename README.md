# Send to PC

Utility Android nativa per trasferire file, foto, testo e link tra telefono e computer sulla stessa rete locale.

## Principi del prodotto

- nessun account;
- nessun cloud;
- nessun software obbligatorio sul PC;
- collegamento PC-first tramite indirizzo locale breve + codice connessione; QR / link completo + PIN come alternative;
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
- server HTTPS LAN nativo tramite TLS + ServerSocket;
- MediaStore Downloads per i file ricevuti;
- ZXing per il QR code;
- Google Mobile Ads SDK (`play-services-ads` 25.5.0, legacy API line);
- Google UMP;
- minSdk 29;
- targetSdk 36;
- Java 17.

## Sicurezza

La sessione usa:
- token casuale ad alta entropia nell'URL;
- PIN a 4 cifre con rate limit;
- cookie di sessione Secure + HttpOnly + SameSite=Strict;
- credenziale PC fidato Secure + HttpOnly, con scadenza a 30 giorni e rotazione dopo ogni riconnessione riuscita;
- CSP, no-store, no-referrer e anti-framing;
- server in ascolto solo sull'indirizzo LAN attivo.

Dalla v1.20 il traffico locale usa TLS 1.2/1.3 con identity persistente nell'Android Keystore. Il certificato è self-signed: al primo collegamento il browser può mostrare un warning e il fingerprint SHA-256 va confrontato con quello mostrato sul telefono. Vedi [TLS-TRANSPORT.md](TLS-TRANSPORT.md) e [THREAT_MODEL.md](THREAT_MODEL.md).

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

La v1.27.5 ha corretto il crash R8/Room `WorkDatabase` ed è stata verificata sia in AppLab sia con avvio fisico. La v1.27.6 corregge inoltre il caricamento interstitial sul main thread: un errore di un singolo formato pubblicitario non disabilita più l'intero stack AdMob. Banner production, messaggio UMP europeo e privacy options sono stati verificati su device reale; resta da validare fisicamente l'interstitial production nel natural break previsto dalla policy.

## Privacy

I file trasferiti restano sulla rete locale e non vengono caricati su un cloud dell'app. AdMob/UMP effettuano connessioni Internet separate per advertising/privacy. Vedi [PRIVACY.md](PRIVACY.md).

## Store preparation

La cartella [store](store) contiene Data Safety, AdMob/app-ads.txt, Play Console pack, listing localizzata, specifiche asset e checklist Store. La cartella [docs](docs) contiene una Privacy Policy statica pronta per un hosting HTTPS.

La v1.19 introduce inoltre un gate `store_candidate` nel workflow release: un candidato Store fallisce se gli ID AdMob production non sono configurati.

## PC Quick Connect

Dalla v1.28 il flusso principale non richiede più di scansionare un QR dal PC: il telefono mostra un indirizzo locale breve `https://IP:porta` e un codice connessione monouso di 8 caratteri. Il server accetta il codice solo sulla rete locale, applica un rate-limit dedicato e crea una sessione autenticata senza esporre il token lungo prima dell'autenticazione. Il link completo e il QR restano disponibili come alternative. Telefono e PC devono trovarsi sulla stessa rete locale; il PC può essere collegato via Wi-Fi oppure Ethernet.

## Trasporto cifrato

La v1.20 usa HTTPS locale e cookie Secure. La validazione fisica del warning certificato/fingerprint resta separata dal CI.

## Roadmap

Vedi [ROADMAP.md](ROADMAP.md).
