# Data Safety draft — Send to PC

Versione di lavoro: v1.28.1
Stato: DRAFT — da verificare nuovamente sull'esatto AAB al momento della pubblicazione su Google Play.

## Confine del prodotto

Il trasferimento di file, foto, testo e link avviene direttamente tra telefono e browser del PC sulla stessa rete locale.

Il PC può essere collegato alla stessa LAN via Wi-Fi oppure Ethernet. L'app non dispone di un cloud proprietario per i contenuti trasferiti e il codice di trasferimento non invia ad AdMob il contenuto di file, testo o link.

La pubblicità e il consenso privacy costituiscono un canale di rete separato dal trasferimento locale.

## SDK Google Advertising / Privacy

L'artifact corrente integra:
- Google Mobile Ads SDK `com.google.android.gms:play-services-ads:25.5.0` (legacy API line);
- Google User Messaging Platform `com.google.android.ump:user-messaging-platform:4.0.0`.

Il questionario Data Safety finale deve essere verificato contro la documentazione Google corrente e l'esatto AAB distribuito. In particolare vanno riesaminate le categorie trattate automaticamente dagli SDK advertising/privacy, incluse quando applicabili:
- indirizzo IP / localizzazione generale derivata;
- interazioni con prodotto e annunci;
- diagnostica;
- identificatori del dispositivo/account.

Le finalità e le dichiarazioni effettive dipendono dalla configurazione finale AdMob/UMP e dalla versione degli SDK inclusa nell'AAB.

## Dati applicativi locali

Sono salvati localmente:
- cronologia dei trasferimenti;
- metadati per resume dei caricamenti quando necessari;
- hash delle credenziali dei PC fidati;
- preferenze lingua/tema;
- stato di monetizzazione/frequency cap non sensibile.

L'utente può cancellare cronologia e PC fidati dall'app.

I file ricevuti dal PC vengono salvati tramite MediaStore in:
- `Download/Send to PC/` per i nuovi trasferimenti;
- la precedente cartella `Download/Invia al PC/` resta soltanto una compatibilità legacy per file/metadati già esistenti.

## Dati che non devono essere descritti come cloud dell'app

Non attribuire a un backend Send to PC:
- file trasferiti;
- testo trasferito;
- link trasferiti;
- foto trasferite.

Restano separate le connessioni esterne effettuate dagli SDK Google Advertising / Privacy.

## Sicurezza del trasporto core

Dalla v1.20 il trasferimento telefono ↔ browser usa TLS 1.2/1.3 sulla LAN.

- chiave privata persistente nell'Android Keystore;
- certificato self-signed;
- fingerprint SHA-256 visibile sul telefono;
- cookie session/trusted con Secure + HttpOnly + SameSite=Strict;
- server legato all'indirizzo LAN selezionato;
- nessun endpoint intenzionale di trasferimento payload via HTTP.

Stato engineering: **IMPLEMENTED**.
Stato release reality: **PHYSICAL VALIDATION DEL TRUST FLOW ANCORA RICHIESTA SULL'ESATTO CANDIDATO**.

La vecchia nota repository che descriveva il core come HTTP non cifrato era precedente alla v1.20 ed è rimossa perché non rappresenta più l'implementazione corrente.

## Stato privacy / AdMob osservato

- Privacy Policy HTTPS: pubblicata e raggiungibile;
- banner production: verificato su device reale;
- UMP European Regulations + privacy options: verificati su device reale;
- interstitial production nel natural break: ancora da verificare fisicamente;
- audience/age treatment e questionario Play Console: ancora da chiudere nella console.

## Gate finale

Prima di Store Ready:

[ ] ricontrollare la documentazione Data Safety Google corrente;
[ ] verificare le versioni SDK realmente incluse nell'AAB finale;
[x] verificare UMP/consenso e privacy options su device fisico;
[ ] dichiarare audience/age treatment;
[ ] completare il questionario Data Safety in Play Console;
[x] Privacy Policy pubblicata e raggiungibile;
[ ] conservare evidence della configurazione finale;
[ ] validare fisicamente TLS/fingerprint sui browser target sull'esatto artifact;
[ ] confermare sull'artifact finale che nessun payload di trasferimento usa HTTP LAN;
[ ] verificare interstitial production e failure path pertinenti.

CI GREEN != DATA SAFETY VERIFIED.
