# Roadmap — Send to PC

Baseline precedente: v1.16.0 production-signing verified.

## v1.17 — Monetization Foundation

Stato: IMPLEMENTED — CI GREEN (test ads; production AdMob IDs pending)

- Google Mobile Ads SDK `play-services-ads:25.5.0` (legacy API line).
- UMP consent flow.
- Anchored adaptive banner fuori dalla sessione di trasferimento attiva.
- Interstitial solo al termine esplicito di una sessione completata e significativa.
- Frequency cap interno: massimo una volta per sessione e almeno 5 minuti tra interstitial.
- Nessun interstitial per trasferimenti piccoli/rapidi salvo batch multi-file.
- Transfer operation summary.
- Settings: lingua, tema, privacy advertising.
- Test ads obbligatori per debug/slim/QA.
- Production AdMob IDs configurabili solo tramite environment/GitHub Secrets.
- Privacy/docs aggiornate.

Gate:
- CI GREEN.
- AdMob production IDs: PENDING.
- UMP/AdMob physical validation: PENDING.

## v1.18 — Transfer Experience

Stato: IMPLEMENTED — CI GREEN

- PC -> telefono: testo e link dal browser con endpoint autenticato e validazione URL.
- Registrazione batch PC -> telefono con conteggio file e byte attesi.
- Progress complessivo di sessione oltre al progress del file corrente.
- Riepilogo operazione con direzione, volume, durata e velocità media.
- Condivisione Android per file, testo e link ricevuti.
- Copia testo e apertura link dalla cronologia.
- Cronologia raggruppata per sessione con compatibilità per record legacy.
- Metadata di cronologia estesi con sessionId e tipo contenuto.

## v1.19 — Polish & Store Preparation

Stato: IMPLEMENTED — CI GREEN

- Parità completa delle chiavi localizzate EN/IT/ES/FR/DE/PT, protetta da test automatico.
- Guida rete locale / VPN / guest isolation / hotspot nelle Impostazioni.
- App shell alleggerita estraendo Cronologia e Impostazioni in file dedicati.
- Il server locale resta intenzionalmente non rifattorizzato in questa fase per evitare regressioni prima del Physical QA.
- Workflow release version-aware.
- Nuovo gate Store Candidate fail-closed: richiede gli ID AdMob production.
- Preparazione documentale Data Safety.
- Checklist app-ads.txt / AdMob readiness.
- Checklist Store release.
- Store release candidate predisposta a livello di build/configurazione, ma non ancora certificata fisicamente.
- Hardening 12/10/2026: ACCESS_LOCAL_NETWORK rimosso finché targetSdk resta 36; guida hotspot allineata al percorso supportabile (hotspot del PC).
- Gate sicurezza Store aperto: il trasporto LAN core usa HTTP non cifrato.

## v1.20 — LAN Transport Security

Stato: IMPLEMENTED — CI GREEN / PHYSICAL QA PENDING

- HTTPS locale al posto del trasporto HTTP.
- TLS 1.2 / TLS 1.3.
- Identity RSA persistente nell'Android Keystore.
- Certificato self-signed e fingerprint SHA-256 mostrato sul telefono.
- Cookie sid/trusted_pc Secure + HttpOnly + SameSite=Strict.
- Nessun cloud relay e nessun software PC obbligatorio.
- Trust model e recovery documentati.

Gate residuo:
- validazione fisica warning certificato/fingerprint sui browser target;
- verifica sniffing LAN;
- policy review finale sull'esatto artifact.

## v1.21 — Transfer Reliability Final

Stato: IMPLEMENTED — CI GREEN / PHYSICAL QA PENDING

- policy centralizzata per limiti file/batch/chunk;
- arithmetic chunk overflow-safe;
- storage headroom overflow-safe;
- boundary tests su 8 GiB, 100 file e Long overflow;
- resume/Range/partial cleanup già presenti e mantenuti;
- Wi-Fi recovery 12 s già coperta da test policy;
- background/FGS/wake lock restano da verificare fisicamente.

## v1.22 — Security Hardening

Stato: IMPLEMENTED — CI GREEN / PHYSICAL QA PENDING

- Host header vincolato a IP/porta sessione;
- cross-site POST bloccati tramite Fetch Metadata;
- header duplicati rifiutati;
- Transfer-Encoding non supportato e rifiutato;
- rate-limit PIN bounded + lockout test;
- input/filename/link/CSP/security headers riesaminati;
- audit evidence in RELIABILITY-SECURITY-AUDIT.md.

## v1.23 — UX / Physical QA Polish

Stato: PREPARED — PHYSICAL QA PENDING

- fingerprint TLS reso selezionabile e monospace;
- audit stringhe UI: nessun nuovo testo user-facing non localizzato;
- matrice device/browser/rete/accessibilità in PHYSICAL-QA.md;
- EN/IT/ES/FR/DE/PT già protette da parity test;
- physical evidence non simulata.

## v1.24 — AdMob Production

Stato: PRODUCTION CONFIGURED — BANNER + UMP PHYSICAL PASS / INTERSTITIAL PHYSICAL PENDING

- Store Candidate fail-closed su ID mancanti;
- ID production validati per formato;
- ID demo Google esplicitamente rifiutati;
- production IDs reali: CONFIGURED e verificati dal Store Candidate workflow;
- banner production su device reale: PASS;
- UMP europeo + privacy options su device reale: PASS;
- interstitial production / frequency cap nel natural break: PENDING;
- no-fill/offline/error physical path: PENDING.

## v1.25 — Store Infrastructure

Stato: REPOSITORY PACK PREPARED — EXTERNAL ACTIVATION PENDING

- privacy page statica pronta in docs/privacy.html;
- developer landing page pronta;
- Play Console pack preparato;
- Data Safety engineering evidence aggiornata;
- app-ads.txt template presente;
- hosting HTTPS / publisher line / AdMob readiness / Play forms: PENDING.

## v1.26 — Store Assets

Stato: COPY/SPECS PREPARED — FINAL BINARY ASSETS PENDING

- listing copy EN/IT/ES/FR/DE/PT preparata;
- specifiche Play icon/feature graphic/screenshots documentate;
- screenshot reali devono provenire dall'esatto candidato dopo Physical QA;
- support email/sito definitivo: PENDING.

## v1.27 — Production Release Candidate

Stato: NOT CERTIFIED — REAL-DEVICE STARTUP CRASH

- versionName 1.27.0 / versionCode 33;
- feature freeze confermata;
- production AdMob configurato e gate fail-closed superato;
- canonical Store Candidate run: 38247324555;
- source SHA: 79efe53c229e919d220ddc070a93a254b500888f;
- APK SHA-256: 78dde1a90def7cfb594b377ae5906869d745c186363a9d5094a0e3d47972619f;
- AAB SHA-256: e524c19033ce838f2cf4120f22f2ab7ae856510a3683503fd417decfb1265a09;
- signing certificate MATCH / VERIFIED;
- evidence: store/V1_27_RC_EVIDENCE.md;
- Physical QA ha rilevato crash immediato all'avvio sulla v1.27.0 production;
- la v1.27.0 è quindi esclusa come candidato finale.

## v1.27.1 — Production startup hotfix

Stato: RC GENERATED — CI GREEN / PHYSICAL QA REQUIRED

- versionName 1.27.1 / versionCode 34;
- package invariato: com.riccardopinato.inviaalpc;
- signing identity invariata e verificata;
- UMP/GMA/banner/interstitial resi fail-safe rispetto al core app;
- production AdMob configurato;
- Store Candidate run: 38254139820;
- source SHA: 46748a9a2acadb27d7dd2650ef7b1b40b22025ee;
- APK SHA-256: 406b671396ac329e2035d7cd3b5931b09725d9a467cb4050e562c5256d3a5512;
- AAB SHA-256: 9553344d98937881b5c1c6ae0fd8f9e9bffe7dcc374a23b7fdbbb333436992dd;
- deve aggiornare direttamente la v1.27.0 già installata;
- Physical QA riparte dall'esatto APK v1.27.1.

## v1.27.4 — Legacy GMA startup diagnostic

Stato: NOT CERTIFIED — APPLAB STARTUP CRASH REPRODUCED

- versionName 1.27.4 / versionCode 41;
- Google Mobile Ads riportato alla linea legacy `play-services-ads:25.5.0`;
- APK release esatto riprodotto in AppLab;
- crash prima di `Application.onCreate`, dentro `androidx.startup.InitializationProvider`;
- errore deterministico: impossibile creare `androidx.work.impl.WorkDatabase`;
- la rimozione del solo init applicativo Mobile Ads non eliminava il crash;
- la rimozione dello stack ads/UMP eliminava il crash, indicando una dipendenza transitiva;
- il candidato v1.27.4 resta escluso dal merge/release finale.

## v1.27.5 — Room / R8 startup fix

Stato: STARTUP FIX VERIFIED — ADMOB RUNTIME BUG FOUND

- versionName 1.27.5 / versionCode 42;
- package e signing identity invariati;
- mantiene `play-services-ads:25.5.0` + UMP 4.0.0;
- aggiunta regola R8 per preservare i costruttori delle subclass di `RoomDatabase`;
- la stessa correzione, applicata al diagnostico v1.27.4, ha prodotto AppLab `PASS`;
- cold start diagnostico: 2463 ms;
- nessun `FATAL EXCEPTION`, `am_crash` o `WorkDatabase` nel logcat diagnostico;
- exact APK v1.27.5: AppLab PASS;
- avvio su device reale: PASS;
- Physical QA ha rilevato assenza totale AdMob;
- diagnostica successiva: `InterstitialAd.load()` veniva invocato da Dispatchers.IO e generava `#008 Must be called on the main UI thread`;
- l'eccezione attivava il fail-safe globale, disabilitando anche il banner.

## v1.27.6 — AdMob main-thread hotfix

Stato: VERIFIED HOTFIX — UMP CONFIGURED / PRODUCTION INTERSTITIAL PHYSICAL PENDING

- versionName 1.27.6 / versionCode 43;
- `MobileAds.initialize()` resta in background;
- `InterstitialAd.load()` viene marshalled sul main thread;
- failure di banner/interstitial isolate per formato: non disabilitano più l'intero stack ads;
- diagnostica con App ID production + unità test ufficiali Google: banner PASS, interstitial PASS;
- il precedente blocker UMP `no form(s) configured` è stato chiuso pubblicando/configurando il messaggio European regulations;
- banner production + UMP/privacy options: PASS su device reale;
- resta da verificare l'interstitial production nel natural break e i relativi failure path fisici.

## v1.28.0 — PC Quick Connect

Stato: IMPLEMENTATION / QA

- versionName 1.28.0 / versionCode 44;
- flusso principale PC-first: indirizzo locale breve + codice connessione;
- codice manuale 8 caratteri da alfabeto human-safe (~40 bit);
- rate-limit separato dal PIN QR;
- il root URL non espone il token di sessione prima dell'autenticazione;
- PC fidati possono riutilizzare la credenziale dal root URL;
- link completo condivisibile e QR restano disponibili come fallback;
- nessun cloud, nessun software PC obbligatorio, nessun nuovo permesso Android;
- TLS/fingerprint invariati;
- unit test + AppLab + Physical QA richiesti.

## v1.28.1 — Truth & Reliability Fix

Stato: IMPLEMENTATION / CI + PHYSICAL QA REQUIRED

- copy TLS/HTTPS allineato in EN/IT/ES/FR/DE/PT;
- concetto rete corretto: stessa LAN, PC consentito anche via Ethernet;
- Quick Connect copy riallineato in ES/FR/DE/PT;
- nuovi file PC -> telefono salvati in `Download/Send to PC/`, con cleanup compatibile anche con la cartella legacy `Download/Invia al PC/`;
- stato sessione separato dal completamento del singolo file: dopo un file completato la sessione resta CONNECTED;
- dedupe idempotente del completamento resumable tramite completion key per upload;
- metadata completed invalidato se il relativo content URI non è più leggibile;
- velocità PC -> telefono calcolata sul chunk reale;
- LAN discovery non limitata alla default network: vengono valutate anche Wi-Fi/Ethernet locali non-default, escludendo VPN;
- regression test aggiunti per product truth localization e velocità upload;
- Data Safety, Store checklist, Play listing, QA e roadmap riallineati.

Gate:
- unit test + lint + release build;
- AppLab sull'esatto APK candidato;
- test fisico Quick Connect + trasferimento bidirezionale;
- rete PC Ethernet + telefono Wi-Fi;
- resume retry/idempotency;
- production interstitial.

## v1.28.2 — Physical Certification

Stato: BLOCKED BY DEVICE TEST

- exact-artifact validation;
- update-in-place;
- Quick Connect manuale da Windows/macOS/Linux;
- link condiviso + QR fallback;
- Evidence Bundle;
- CERTIFIED / NOT CERTIFIED / BLOCKED.

## v1.29 — UX Consolidation

Stato: PLANNED

- Connect più compatta;
- Selection Review dedicata per liste file;
- niente liste lunghe nella Home;
- active session e dettagli TLS più gerarchici;
- refactor bounded dei monoliti UI/server senza riscrittura del core.

## v1.30 / 1.0 Store — Google Play

Stato: BLOCKED BY PLAY CONSOLE / FINAL CERTIFICATION

## Gate finale

Dopo la feature freeze e la chiusura dei blocker Store:

1. nuova release firmata con la signing identity permanente;
2. Physical QA sull'esatto APK candidato;
3. update-in-place dalla precedente release firmata;
4. Evidence Bundle;
5. Final Certification;
6. Store Ready.
