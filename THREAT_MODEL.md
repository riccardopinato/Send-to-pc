# Threat model — Invia al PC

## Scope

Il prodotto trasferisce contenuti direttamente tra Android e un browser PC sulla stessa LAN.

## Asset da proteggere

- file e testo trasferiti;
- token di sessione;
- PIN;
- cookie di sessione;
- credenziale PC fidato;
- cronologia locale.

## Assunzione operativa

La rete Wi-Fi deve essere privata o fidata.

Dalla v1.20 il protocollo applicativo usa TLS 1.2/1.3 locale. La private key resta nell'Android Keystore. Il certificato è self-signed e richiede verifica del fingerprint SHA-256 mostrato sul telefono.

## Minacce e mitigazioni

### Accesso casuale non autorizzato

Mitigazioni:
- URL con token casuale ad alta entropia;
- PIN separato;
- rate limit sui tentativi PIN;
- sessioni temporanee;
- revoca dei PC fidati.

### Furto della credenziale PC fidato tramite JavaScript

Mitigazioni:
- il segreto non è salvato in localStorage;
- cookie HttpOnly;
- SameSite=Strict;
- rotazione del token dopo ogni autenticazione trusted riuscita;
- scadenza a 30 giorni;
- sul telefono viene salvato solo l'hash SHA-256.

Rischio residuo:
- se l'utente accetta un certificato diverso senza confrontare il fingerprint, un attaccante attivo sulla LAN può tentare impersonation/MITM.
- i cookie sono ora Secure + HttpOnly + SameSite=Strict.

### XSS / contenuti utente

Mitigazioni:
- escaping HTML dei nomi file, testo e link;
- Content-Security-Policy;
- X-Content-Type-Options;
- X-Frame-Options;
- Referrer-Policy no-referrer.

### Esposizione token sessione

Il token è contenuto nell'URL locale per consentire il QR/browser flow.

Mitigazioni:
- token casuale;
- PIN obbligatorio prima del contenuto;
- Cache-Control no-store;
- Referrer-Policy no-referrer;
- durata limitata della sessione.

### Denial of service locale

Mitigazioni:
- limite header;
- timeout socket;
- limite upload singolo;
- massimo 12 client contemporanei;
- massimo 100 file per coda;
- controllo spazio disponibile;
- cleanup dei file parziali.

## Non dichiarare

Finché non esiste Physical QA del trust flow:
- non dichiarare protezione MITM completa senza verifica fingerprint;
- non dichiarare compatibilità browser universale;
- non suggerire l'uso su Wi-Fi pubbliche/non fidate.

## Store security gate

Per build INTERNAL/QA il rischio è esplicitamente visibile e può essere testato su rete privata.

Per una release Google Play, invece, il trasferimento arbitrario di file/testo/link può includere dati personali o sensibili. La User Data policy richiede protezione con crittografia moderna durante la trasmissione. Di conseguenza il trasporto HTTP LAN è un blocker da risolvere o chiudere con evidence/policy interpretation adeguata prima di STORE READY.

Riferimento:
https://support.google.com/googleplay/android-developer/answer/18258653

## Possibile hardening futuro

TLS locale con certificato/pairing, un trasporto P2P cifrato o un'altra soluzione end-to-end compatibile con il requisito browser-only richiedono una valutazione separata di UX, compatibilità browser, footprint e gestione certificati.


## v1.28 PC Quick Connect

Il percorso manuale PC-first espone soltanto l'indirizzo locale `https://IP:porta`. La richiesta `GET /` non rivela il token lungo della sessione.

L'autenticazione manuale usa un codice casuale di 8 caratteri generato con `SecureRandom` su un alfabeto di 32 simboli non ambigui, pari a circa 40 bit di spazio. Il codice:
- è valido solo per la sessione locale corrente;
- è verificato in tempo costante;
- usa un rate-limit separato dal PIN tradizionale;
- dopo autenticazione crea il cookie di sessione Secure/HttpOnly e reindirizza al percorso con token;
- non sostituisce il QR/link completo, che conservano token ad alta entropia + PIN.

Il browser continua a dover verificare/accettare il certificato locale self-signed e il fingerprint SHA-256 mostrato sul telefono.
