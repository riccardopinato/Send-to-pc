# Store Readiness — Invia al PC v1.19.0

Data baseline: 12 ottobre 2026.

Questo documento separa IMPLEMENTED, VERIFIED e BLOCKED. Una build verde non equivale a Store Ready.

## Stato sintetico

- Product feature freeze v1.19.0: IMPLEMENTED.
- versionCode: 30.
- targetSdk: 36.
- signing identity permanente: VERIFIED sulla v1.16.0; nuova v1.19 firmata non ancora prodotta.
- localizzazione EN/IT/ES/FR/DE/PT: parità catalogo implementata + gate automatico.
- GMA Next-Gen / UMP: integrati; physical validation ancora PENDING.
- AdMob production IDs: PENDING.
- app-ads.txt / AdMob app readiness: PENDING / external.
- LAN transport security: BLOCKED FOR STORE REVIEW.

## Target Android

La build corrente usa targetSdk 36.

ACCESS_LOCAL_NETWORK non viene dichiarato con targetSdk 36. La migrazione al modello runtime di Android 17 deve avvenire solo quando anche il progetto passerà a targetSdk 37, con QA specifico.

Riferimento:
https://developer.android.com/privacy-and-security/local-network-permission

## Advertising

La build integra:
- GMA Next-Gen SDK;
- UMP;
- adaptive banner;
- interstitial con natural-break policy e frequency cap;
- test ads obbligatori per debug/slim/QA.

Per una release monetizzata servono i tre valori production:
- ADMOB_APP_ID
- ADMOB_BANNER_ID
- ADMOB_INTERSTITIAL_ID

Se mancano, la release resta tecnicamente buildabile con ID demo ma NON è la release monetizzata Store.

## app-ads.txt / AdMob readiness

Per le nuove app configurate in AdMob la verifica app-ads.txt è parte del percorso di monetizzazione.

Gate esterno:
1. app pubblicata/collegabile a uno store supportato;
2. sito sviluppatore presente nella scheda Store;
3. app-ads.txt pubblicato alla root del sito sviluppatore;
4. snippet AdMob reale con publisher ID corretto;
5. verifica app-ads.txt completata in AdMob;
6. app readiness review approvata.

Questi passaggi non possono essere certificati dal repository da solo.

Riferimenti:
https://support.google.com/admob/answer/14538460
https://support.google.com/admob/answer/9363762

## Data Safety — advertising SDK

Secondo la documentazione Google del GMA Next-Gen SDK, il SDK raccoglie e condivide automaticamente per advertising, analytics e prevenzione frodi:
- indirizzo IP, potenzialmente usato per posizione generale;
- user product interactions;
- diagnostic information;
- device/account identifiers, inclusi quando applicabili advertising ID e app set ID.

Google dichiara TLS per i dati raccolti dal GMA Next-Gen SDK.

La dichiarazione Play Console finale deve essere riesaminata sull'esatto artifact e sulla versione SDK realmente distribuita.

Riferimento:
https://developers.google.com/admob/android/next-gen/privacy/play-data-disclosure

## P0 — LAN transport security

Il core del prodotto oggi usa HTTP sulla rete locale tra telefono e browser del PC.

Questo significa:
- i payload applicativi non sono cifrati in transito;
- non possiamo dichiarare che tutti i dati dell'app sono cifrati in transito;
- la sola indicazione "usa una Wi-Fi fidata" non chiude automaticamente il requisito Store;
- file, foto, testo e link possono contenere dati personali o sensibili.

La Google Play User Data policy richiede la gestione sicura dei dati personali e sensibili, inclusa la trasmissione tramite crittografia moderna.

Verdetto v1.19:
**STORE READY = BLOCKED** finché questo rischio non viene risolto tecnicamente o chiuso con evidence/policy interpretation sufficientemente forte.

Riferimento:
https://support.google.com/googleplay/android-developer/answer/18258653

## Gate per il prossimo step

Prima di produrre la release Store candidate definitiva:
- chiudere LAN transport security;
- configurare AdMob production IDs;
- completare app-ads.txt / AdMob readiness quando possibile;
- rieseguire release workflow con signing identity permanente;
- Physical QA dell'esatto APK;
- update-in-place;
- Evidence Bundle;
- Final Certification.
