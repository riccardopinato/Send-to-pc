# Privacy — Invia al PC

Invia al PC trasferisce file, foto, testo e link direttamente tra telefono e computer sulla rete locale.

## Dati trasferiti

L'app accede ai contenuti che l'utente seleziona per il trasferimento e ai file, testi o link ricevuti dal computer.

I contenuti trasferiti non vengono inviati a un cloud dell'app: il trasferimento avviene direttamente sulla rete locale tra telefono e browser del PC.

Il protocollo LAN corrente usa HTTP e non cifra il payload in transito. Usare la funzione solo su reti private o fidate. Poiché i contenuti scelti dall'utente possono includere dati personali o sensibili, questo limite resta un gate di sicurezza da chiudere prima di dichiarare la release Store Ready.

## Pubblicità e consenso

L'app integra Google AdMob tramite GMA Next-Gen SDK e Google User Messaging Platform (UMP).

AdMob e UMP possono effettuare connessioni Internet verso i servizi Google indipendenti dal trasferimento locale dei contenuti.

Secondo la documentazione corrente del GMA Next-Gen SDK, il componente pubblicitario raccoglie e condivide automaticamente per advertising, analytics e prevenzione frodi categorie che includono:
- indirizzo IP, utilizzabile anche per stimare una posizione generale;
- interazioni dell'utente con il prodotto;
- informazioni diagnostiche;
- identificatori di dispositivo e account, inclusi quando applicabili advertising ID e app set ID.

Google dichiara TLS per i dati raccolti dal GMA Next-Gen SDK.

L'integrazione dell'app non passa ad AdMob il contenuto dei file, del testo o dei link trasferiti.

Le scelte privacy possono essere riaperte dalle Impostazioni quando UMP indica che il relativo punto di accesso è richiesto.

## Cronologia e PC fidati

La cronologia dei trasferimenti e l'elenco dei PC fidati vengono conservati localmente sul dispositivo. Possono essere cancellati dall'utente dall'app.

## Account

L'app non richiede un account.

## Permessi e rete

Il permesso Internet viene utilizzato sia per la comunicazione sulla rete locale sia per i servizi pubblicitari/privacy quando attivi.

Il servizio in primo piano mantiene attiva una sessione di trasferimento mentre è in uso.

Con targetSdk 36 l'app non dichiara ACCESS_LOCAL_NETWORK. Quando il progetto passerà a targetSdk 37, il relativo permesso runtime dovrà essere introdotto e validato separatamente.

## Google Play Data safety

Il modulo Data safety deve riflettere l'esatto artifact destinato allo Store e le pratiche effettive dell'app e dei relativi SDK.

Finché il protocollo LAN resta HTTP non deve essere dichiarato che tutti i dati dell'app sono cifrati in transito.

Riferimenti:
- https://developers.google.com/admob/android/next-gen/privacy/play-data-disclosure
- https://support.google.com/googleplay/android-developer/answer/18258653
- https://developer.android.com/privacy-and-security/local-network-permission

Ultimo aggiornamento: 12 ottobre 2026 — v1.19 Store hardening.
