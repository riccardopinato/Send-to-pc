# Open-source transfer research — v1.20

Scopo: confrontare architetture già usate da progetti reali prima di fissare il trasporto sicuro.

Nessun codice dei progetti sotto è stato copiato. Il riuso è soltanto architetturale/conoscitivo; una verifica licenza resta obbligatoria prima di qualsiasi futuro code reuse.

## 1. LocalSend

Repo/protocollo:
- https://github.com/localsend/localsend
- https://github.com/localsend/protocol

Pattern rilevante:
- trasferimento HTTPS;
- certificato TLS self-signed;
- SHA-256 del certificato usato come fingerprint;
- persistenza delle credenziali TLS per evitare identity che cambia a ogni avvio.

Decisione:
**ADOPT ARCHITECTURAL PATTERN.**

È il donor più vicino a Invia al PC per modello LAN/local-first. La v1.20 usa una identity persistente in Android Keystore e fingerprint SHA-256.

## 2. PairDrop

Repo:
- https://github.com/schlagmichdoch/PairDrop

Pattern rilevante:
- WebRTC;
- DTLS gestito dal browser;
- signaling via server/WebSocket;
- possibili STUN/TURN.

Decisione:
**DO NOT ADOPT FOR CORE v1.20.**

Il trasporto è valido ma aggiungerebbe signaling/infrastruttura esterna e complicherebbe il requisito local-only/no-cloud.

## 3. Flying Carpet

Repo:
- https://github.com/spieglt/FlyingCarpet

Pattern rilevante:
- Noise protocol;
- PSK derivata dalla password;
- X25519 + ChaCha20-Poly1305;
- forward secrecy;
- client dedicati sui due endpoint.

Decisione:
**REFERENCE FOR FUTURE HARDENING, NOT CURRENT CORE.**

È più forte contro MITM ma non conserva il requisito browser-only senza introdurre un runtime crittografico browser affidabile e autenticato.

## 4. LANDrop

Repo:
- https://github.com/LANDrop/LANDrop

Pattern rilevante:
- LAN peer-to-peer;
- crittografia tramite librerie native;
- applicazioni dedicate sui peer.

Decisione:
**NOT ADOPTED.**

Richiede client desktop e non corrisponde al prodotto zero-install sul PC.

## Conclusione

Per la v1.20:
- LocalSend è il reference donor principale;
- HTTPS locale + identity persistente è coerente con una soluzione LAN reale già diffusa;
- il nostro differenziale resta il browser PC senza installazione;
- proprio questo vincolo rende obbligatoria la Physical QA del warning self-signed/fingerprint.
