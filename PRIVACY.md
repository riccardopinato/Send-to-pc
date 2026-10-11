# Privacy — Invia al PC

Invia al PC trasferisce file, foto, testo e link direttamente tra telefono e computer sulla rete locale.

## Dati trasferiti

L'app accede ai contenuti che l'utente seleziona per il trasferimento e ai file, testi o link ricevuti dal computer.

I contenuti trasferiti non vengono inviati a un cloud dell'app: il trasferimento avviene direttamente sulla rete locale tra telefono e browser del PC.

Il protocollo di trasferimento locale corrente usa HTTP e non cifra il contenuto in transito. Per questo motivo la funzione va usata solo su reti private o fidate. Questo limite è dichiarato e resta un gate di sicurezza prima della pubblicazione Store.

## Pubblicità e consenso

L'app integra Google AdMob tramite GMA Next-Gen SDK e Google User Messaging Platform (UMP) per gestire pubblicità e scelte privacy quando richieste.

AdMob e UMP possono effettuare connessioni Internet verso i servizi Google indipendenti dal trasferimento locale dei contenuti.

Secondo la documentazione Google del GMA Next-Gen SDK, il SDK può raccogliere e condividere automaticamente per pubblicità, analytics e prevenzione frodi:
- indirizzo IP, che può essere usato per stimare una posizione generale;
- interazioni dell'utente con il prodotto;
- informazioni diagnostiche;
- identificatori di dispositivo e account, inclusi quando applicabili advertising ID e app set ID.

Google dichiara che i dati raccolti dal GMA Next-Gen SDK sono cifrati in transito tramite TLS.

L'integrazione dell'app non passa ad AdMob il contenuto dei file, del testo o dei link trasferiti.

Le scelte privacy possono essere riaperte dalle Impostazioni quando UMP indica che il relativo punto di accesso è richiesto.

## Cronologia e PC fidati

La cronologia dei trasferimenti e l'elenco dei PC fidati vengono conservati localmente sul dispositivo. Possono essere cancellati dall'utente dall'app.

La cronologia registra metadati di trasferimento e, per testo/link, il contenuto necessario alle azioni locali di cronologia. Non costituisce un backup cloud.

## Account

L'app non richiede un account.

## Permessi e rete

Il permesso Internet viene utilizzato sia per la comunicazione sulla rete locale sia per i servizi pubblicitari/privacy quando attivi.

Il servizio in primo piano mantiene attiva una sessione di trasferimento mentre è in uso.

Con targetSdk 36 l'app non dichiara ACCESS_LOCAL_NETWORK. L'eventuale migrazione ad Android 17 / targetSdk 37 deve introdurre il relativo permesso runtime just-in-time e una nuova validazione fisica.

## Google Play Data safety

Il modulo Data safety deve essere compilato sulla base dell'esatto artifact destinato allo Store e delle pratiche effettive dell'app e dei relativi SDK.

Non dichiarare che tutti i dati dell'app sono cifrati in transito finché il protocollo locale di trasferimento resta HTTP.

Riferimenti:
- https://developers.google.com/admob/android/next-gen/privacy/play-data-disclosure
- https://support.google.com/googleplay/android-developer/answer/18258653

Ultimo aggiornamento: 12 ottobre 2026.
