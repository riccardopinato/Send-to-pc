# Local TLS transport — Invia al PC v1.20

## Obiettivo

Cifrare il traffico telefono ↔ browser PC senza introdurre:
- account;
- cloud relay;
- software desktop obbligatorio;
- chiavi private esportate dal telefono.

## Architettura

La v1.20 sostituisce la sessione HTTP LAN con HTTPS locale.

Il telefono genera e conserva una identity RSA 2048 nell'Android Keystore:
- alias stabile: `send_to_pc_local_tls_v1`;
- private key non esportabile;
- certificato X.509 self-signed persistente;
- validità lunga;
- SHA-256 fingerprint mostrato sul telefono.

Il server abilita solo TLS 1.2 / TLS 1.3 quando disponibili.

La sessione usa:
`https://<LAN-IP>:<PORT>/s/<TOKEN>`

I cookie `sid` e `trusted_pc` sono:
- Secure;
- HttpOnly;
- SameSite=Strict.

## Trust model

Il certificato non è firmato da una CA pubblica perché il server vive su un IP privato dinamico e il prodotto non richiede installazione di una CA o di software sul PC.

Conseguenza:
- il browser può mostrare un warning sul certificato locale;
- l'utente deve confrontare il fingerprint SHA-256 del certificato con quello mostrato sul telefono;
- una volta verificato il fingerprint, TLS protegge il traffico da lettura/modifica passiva sulla LAN.

## Rischio residuo

Se l'utente ignora il fingerprint e accetta un certificato diverso, un attaccante attivo sulla LAN può impersonare il telefono.

Quindi:
- PASS crittografico != Physical UX VERIFIED;
- il gate Store resta aperto finché Chrome/Edge/Firefox target non sono verificati su device reale;
- non abilitare HSTS su questa origin locale self-signed: potrebbe rendere il recovery del certificato inutilmente fragile.

## Recovery identity

Se la chiave Android Keystore diventa inutilizzabile o il certificato scade, l'app rigenera l'identity.

Una nuova identity implica un nuovo fingerprint e deve essere trattata dall'utente come nuovo pairing TLS.

## Evidence richiesta

Per chiudere il gate:
- TLS 1.2/1.3 handshake reale;
- nessun endpoint HTTP di trasferimento;
- cookie Secure presenti;
- fingerprint telefono == certificato browser;
- file piccolo/grande, testo e link trasferiti;
- resume;
- browser target;
- rete privata + hotspot PC;
- test di sniffing che non mostri payload applicativo in chiaro.
