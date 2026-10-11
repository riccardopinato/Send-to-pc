# Data Safety draft — Invia al PC

Versione di lavoro: v1.19.0
Stato: DRAFT — da verificare nuovamente al momento della pubblicazione su Google Play.

## Confine del prodotto

Il trasferimento di file, foto, testo e link avviene direttamente tra telefono e browser del PC sulla rete locale.

L'app non dispone di un cloud proprietario per i contenuti trasferiti e il codice di trasferimento non invia ad AdMob il contenuto di file, testo o link.

La pubblicità e il consenso privacy costituiscono un canale di rete separato dal trasferimento locale.

## SDK Google Advertising / Privacy

L'app integra:
- Google Mobile Ads Next-Gen SDK;
- Google User Messaging Platform (UMP).

Secondo la documentazione Data Safety corrente di Google per il Mobile Ads SDK, il componente pubblicitario può raccogliere/condividere automaticamente categorie di dati che includono:
- indirizzo IP, utilizzabile anche per stime di localizzazione generale;
- interazioni dell'utente con il prodotto e con gli annunci;
- informazioni diagnostiche;
- identificatori del dispositivo/account.

Le finalità indicate dalla documentazione Google includono advertising, analytics e prevenzione delle frodi. Google dichiara che i dati raccolti dal GMA Next-Gen SDK sono cifrati in transito tramite TLS. Google dichiara che i dati raccolti dal GMA Next-Gen SDK sono cifrati in transito tramite TLS.

Questa pagina NON sostituisce il questionario Data Safety della Play Console. Prima della submission finale bisogna verificare:
- versione effettiva degli SDK;
- configurazione AdMob effettiva;
- eventuali servizi aggiunti dopo questa bozza;
- dichiarazioni correnti richieste dalla Play Console;
- comportamento UMP e audience/age treatment scelto per l'app.

## Dati applicativi locali

Sono salvati localmente:
- cronologia dei trasferimenti;
- metadati per resume dei caricamenti quando necessari;
- elenco/hash delle credenziali dei PC fidati;
- preferenze lingua/tema;
- stato di monetizzazione/frequency cap non sensibile.

L'utente può cancellare cronologia e PC fidati dall'app.

## Dati che non devono essere dichiarati come cloud dell'app

Non attribuire al backend dell'app:
- file trasferiti;
- testo trasferito;
- link trasferiti;
- foto trasferite.

Restano comunque da distinguere le connessioni esterne degli SDK Google.

## Sicurezza del trasporto core

Il trasferimento telefono ↔ browser usa attualmente HTTP sulla LAN.

Conseguenze:
- il payload applicativo non è cifrato in transito;
- non è corretto dichiarare che tutti i dati dell'app sono cifrati in transito;
- file, foto, testo e link scelti dall'utente possono includere dati personali o sensibili;
- la sola raccomandazione di usare una rete fidata non chiude automaticamente la Google Play User Data policy, che richiede gestione sicura dei dati personali/sensibili e crittografia moderna durante la trasmissione.

Stato: **BLOCKED FOR STORE REVIEW** finché il rischio non viene risolto tecnicamente o chiuso con evidence/policy interpretation adeguata.

Riferimento:
https://support.google.com/googleplay/android-developer/answer/18258653

## Sicurezza del trasporto core

Il trasferimento telefono ↔ browser usa attualmente HTTP sulla LAN.

Conseguenze:
- il payload applicativo non è cifrato in transito;
- non è corretto dichiarare che tutti i dati dell'app sono cifrati in transito;
- la sola raccomandazione di usare una rete fidata non chiude automaticamente il requisito Store.

Stato: **BLOCKED FOR STORE REVIEW** finché il rischio non viene risolto tecnicamente o chiuso con evidence/policy interpretation adeguata.

Riferimento:
https://support.google.com/googleplay/android-developer/answer/18258653

## Gate finale

Prima di Store Ready:

[ ] ricontrollare la documentazione Data Safety Google corrente;
[ ] verificare la versione SDK realmente inclusa nell'AAB;
[ ] verificare UMP/consenso su device fisico;
[ ] dichiarare audience/age treatment;
[ ] completare il questionario Play Console in modo coerente;
[ ] verificare Privacy Policy pubblicata e raggiungibile;
[ ] conservare evidence della configurazione finale;
[ ] chiudere il gate LAN transport security;
[ ] non selezionare "all data encrypted in transit" se l'artifact mantiene HTTP LAN.

CI GREEN != DATA SAFETY VERIFIED.
