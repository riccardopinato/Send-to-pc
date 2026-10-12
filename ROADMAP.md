# Roadmap — Send to PC

Baseline precedente: v1.16.0 production-signing verified.

## v1.17 — Monetization Foundation

Stato: IMPLEMENTED — CI GREEN (test ads; production AdMob IDs pending)

- GMA Next-Gen SDK.
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

Stato: PRODUCTION CONFIGURED — PHYSICAL QA PENDING

- Store Candidate fail-closed su ID mancanti;
- ID production validati per formato;
- ID demo Google esplicitamente rifiutati;
- production IDs reali: CONFIGURED e verificati dal Store Candidate workflow;
- UMP/banner/interstitial/no-fill physical validation: PENDING.

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

## v1.28 — Physical Certification

Stato: BLOCKED BY DEVICE TEST

- exact-artifact validation;
- update-in-place;
- Evidence Bundle;
- CERTIFIED / NOT CERTIFIED / BLOCKED.

## v1.29 / 1.0 Store — Google Play

Stato: BLOCKED BY PLAY CONSOLE / FINAL CERTIFICATION

## Gate finale

Dopo la feature freeze e la chiusura dei blocker Store:

1. nuova release firmata con la signing identity permanente;
2. Physical QA sull'esatto APK candidato;
3. update-in-place dalla precedente release firmata;
4. Evidence Bundle;
5. Final Certification;
6. Store Ready.
