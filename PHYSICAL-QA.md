# Physical QA — Invia al PC

Target di lavoro: v1.26 candidate preparation.
Stato iniziale: NOT VERIFIED.

Questo documento non trasforma scenari fisici in PASS sulla base della CI.

## Artifact rule

Per ogni campagna:
- registrare source SHA;
- registrare workflow run;
- registrare SHA-256 dell'APK;
- installare esattamente quell'APK;
- non ricostruire prima della distribuzione/certificazione.

## Device matrix minima

Android:
- Android 10 / API 29 o device equivalente minSdk;
- Android 13;
- Android 16 / API 36;
- Android 17 / API 37 solo quando il progetto passerà a targetSdk 37.

PC/browser:
- Chrome stable;
- Edge stable;
- Firefox stable.

Reti:
- Wi-Fi privata con router;
- hotspot mobile del PC con telefono client;
- rete guest/client isolation per verificare recovery esplicita;
- VPN attiva per verificare recovery esplicita.

## TLS / first connection

Per ogni browser:
1. creare una nuova sessione sul telefono;
2. verificare URL https://;
3. aprire QR/URL;
4. osservare il warning del certificato self-signed;
5. visualizzare i dettagli del certificato nel browser;
6. confrontare SHA-256 col fingerprint mostrato sul telefono;
7. procedere solo con fingerprint identico;
8. autenticare con PIN;
9. verificare che il browser mostri una connessione TLS stabilita dopo l'eccezione locale;
10. ripetere dopo riavvio app: fingerprint deve restare stabile;
11. documentare comportamento dopo reinstallazione/clear data: un nuovo fingerprint è atteso.

Verdetto separato:
- TLS handshake: PASS / FAIL / BLOCKED;
- fingerprint continuity: PASS / FAIL / BLOCKED;
- UX del warning: PASS / FAIL / BLOCKED.

## Phone -> PC

- file 1 KB;
- foto;
- testo;
- link;
- 100 file;
- file >1 GB;
- Range resume;
- download interrotto e ripreso;
- nomi Unicode;
- nomi molto lunghi;
- file senza MIME noto.

## PC -> phone

- drag & drop singolo;
- batch;
- testo;
- link;
- pause/resume;
- cancel;
- resume stesso file;
- hash mismatch;
- offset mismatch;
- file duplicato;
- spazio insufficiente;
- cleanup partial.

## Reliability

- schermo spento;
- app in background;
- micro perdita Wi-Fi <12 s;
- perdita >12 s;
- cambio IP;
- kill service/process osservato;
- notifica FGS;
- session expiry;
- wake lock durante file grande.

## Security smoke

- PIN errato ripetuto;
- trusted PC;
- rotazione trusted token;
- revoca singola;
- revoca totale;
- Host header non atteso -> reject;
- POST cross-site -> reject;
- duplicate Content-Length -> reject;
- Transfer-Encoding request -> reject;
- URL scheme file:/javascript:/ftp: -> reject per contenuti link;
- nomi file con CR/LF/path traversal;
- sniffing LAN: nessun file/testo/link in chiaro.

## UX / accessibility

- 360 dp;
- large phone;
- portrait;
- landscape;
- font scale 1.3x / 2.0x;
- dark mode;
- TalkBack smoke;
- gesture navigation;
- 3-button navigation;
- tastiera aperta nelle form;
- onboarding scroll;
- QR leggibile;
- fingerprint selezionabile/leggibile.

## Localization smoke

Per EN / IT / ES / FR / DE / PT:
- onboarding;
- Home;
- session;
- errori rete;
- history;
- settings;
- TLS warning;
- pagina browser;
- notification;
- summary;
- AdMob/UMP privacy options.

## Evidence record

Per ogni scenario registrare:
- artifact SHA-256;
- device + Android;
- browser + versione;
- rete;
- lingua;
- risultato PASS / FAIL / BLOCKED / NOT VERIFIED;
- screenshot/video/log essenziale;
- issue/fix SHA in caso di FAIL.
