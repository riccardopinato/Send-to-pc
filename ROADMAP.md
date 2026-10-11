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

## Step successivo — Store security gate

Prima del gate finale va risolto o formalmente chiuso il rischio del trasporto LAN HTTP rispetto alla Google Play User Data policy.

Restano inoltre esterni al codice:
- AdMob production IDs;
- app-ads.txt;
- AdMob app readiness;
- physical validation UMP/ads.

## Gate finale

Dopo la feature freeze e la chiusura dei blocker Store:

1. nuova release firmata con la signing identity permanente;
2. Physical QA sull'esatto APK candidato;
3. update-in-place dalla precedente release firmata;
4. Evidence Bundle;
5. Final Certification;
6. Store Ready.
