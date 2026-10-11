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

Stato: PLANNED

- PC -> telefono: testo e link.
- Progress complessivo per batch PC -> telefono.
- Riepilogo operazione più ricco.
- Condividi file ricevuto tramite Android share sheet.
- Migliore gestione/aggregazione della cronologia.

## v1.19 — Polish & Store Preparation

Stato: PLANNED

- Parità localizzazione EN/IT/ES/FR/DE/PT.
- Guida hotspot/rete locale.
- Pulizia dei monoliti UI/server dove riduce rischio reale.
- Preparazione Data Safety / app-ads.txt / AdMob app readiness.
- Store release candidate.

## Gate finale

Dopo la feature freeze:

1. nuova release firmata con la signing identity permanente;
2. Physical QA sull'esatto APK candidato;
3. update-in-place dalla precedente release firmata;
4. Evidence Bundle;
5. Final Certification;
6. Store Ready.
