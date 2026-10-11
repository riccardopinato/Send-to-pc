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

Le finalità indicate dalla documentazione Google includono advertising, analytics e prevenzione delle frodi.

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

## Gate finale

Prima di Store Ready:

[ ] ricontrollare la documentazione Data Safety Google corrente;
[ ] verificare la versione SDK realmente inclusa nell'AAB;
[ ] verificare UMP/consenso su device fisico;
[ ] dichiarare audience/age treatment;
[ ] completare il questionario Play Console in modo coerente;
[ ] verificare Privacy Policy pubblicata e raggiungibile;
[ ] conservare evidence della configurazione finale.

CI GREEN != DATA SAFETY VERIFIED.
