# Roadmap — Invia al PC

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

Stato: IMPLEMENTED — CI/QA AUTOMATED PENDING

- PC -> telefono: testo e link dal browser con endpoint autenticato e validazione URL.
- Registrazione batch PC -> telefono con conteggio file e byte attesi.
- Progress complessivo di sessione oltre al progress del file corrente.
- Riepilogo operazione con direzione, volume, durata e velocità media.
- Condivisione Android per file, testo e link ricevuti.
- Copia testo e apertura link dalla cronologia.
- Cronologia raggruppata per sessione con compatibilità per record legacy.
- Metadata di cronologia estesi con sessionId e tipo contenuto.

## v1.19 — Polish & Store Preparation

Stato: IMPLEMENTED — CI PENDING / STORE GATES OPEN

- Parità completa localizzazione EN/IT/ES/FR/DE/PT con test automatico anti-regressione.
- Guida integrata rete locale, VPN, guest Wi-Fi e hotspot del PC.
- Cleanup mirato: guida rete estratta in componente UI autonomo; nessun refactor cosmetico del server già modulare.
- targetSdk 36 allineato: ACCESS_LOCAL_NETWORK non viene dichiarato prematuramente; migrazione rimandata al target 37.
- Privacy / Data Safety / app-ads.txt / AdMob readiness documentati.
- versionName 1.19.0 / versionCode 30.

Gate Store ancora aperti:
- AdMob production IDs: PENDING.
- app-ads.txt + AdMob app readiness: PENDING / external.
- UMP/AdMob physical validation: PENDING.
- LAN transport security: REVIEW REQUIRED; il trasferimento core usa ancora HTTP non cifrato e non va dichiarato Store Ready finché il gate non viene risolto.

## Step successivo — Store security gate

Prima del gate finale:
- risolvere o formalmente chiudere il rischio del trasporto LAN HTTP rispetto alla Google Play User Data policy;
- configurare gli ID AdMob production;
- completare app-ads.txt e app readiness quando la scheda Store/sito sviluppatore sono disponibili.

## Gate finale

Dopo la feature freeze e la chiusura dei blocker Store:

1. nuova release firmata con la signing identity permanente;
2. Physical QA sull'esatto APK candidato;
3. update-in-place dalla precedente release firmata;
4. Evidence Bundle;
5. Final Certification;
6. Store Ready.
