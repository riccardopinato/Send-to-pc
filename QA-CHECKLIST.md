# QA checklist — Invia al PC v1.16.0

## CI gate

- [ ] unit test PASS
- [ ] Android Lint PASS
- [ ] Slim/R8 build PASS
- [ ] apksigner verify PASS
- [ ] SHA-256 registrato
- [ ] nessun secret nei log

## Telefono → PC

- [ ] file singolo piccolo
- [ ] foto multiple
- [ ] file > 1 GB
- [ ] 100 file
- [ ] testo
- [ ] link
- [ ] Range resume dopo interruzione con client compatibile

## PC → telefono

- [ ] drag & drop singolo
- [ ] drag & drop multiplo
- [ ] pausa / riprendi / annulla
- [ ] resume selezionando lo stesso file
- [ ] file duplicato senza sovrascrittura
- [ ] salvataggio in Download/Invia al PC
- [ ] file parziale vecchio eliminato dal cleanup

## Reliability

- [ ] micro-interruzione Wi-Fi < 12 s: sessione resta attiva se torna lo stesso IP
- [ ] perdita Wi-Fi > 12 s: sessione termina con reason corretta
- [ ] cambio IP: messaggio recovery corretto
- [ ] schermo spento durante file grande
- [ ] ritorno dal background
- [ ] termina da notifica
- [ ] timeout FGS
- [ ] process/service kill osservato e classificato

## Sicurezza

- [ ] PIN errato e rate limit
- [ ] PC fidato senza PIN
- [ ] token trusted ruotato dopo reconnect
- [ ] revoca singolo PC
- [ ] revoca tutti
- [ ] cookie trusted non leggibile da JavaScript
- [ ] HTML escaping su nomi file/testo/link
- [ ] warning Wi-Fi non fidata visibile

## UX / visual

- [ ] 360dp small phone
- [ ] large phone
- [ ] portrait
- [ ] landscape
- [ ] dark mode
- [ ] font scale large
- [ ] gesture navigation
- [ ] 3-button navigation
- [ ] onboarding scrollabile
- [ ] adaptive icon launcher
- [ ] monochrome/themed icon

## Lingue

Per EN / IT / ES / FR / DE / PT:
- [ ] primo avvio
- [ ] override manuale
- [ ] ritorno a Sistema
- [ ] navigazione principale
- [ ] sessione QR/PIN
- [ ] pagina browser
- [ ] notifiche
- [ ] stringhe lunghe senza overflow

## Android 17 / API 37

Quando disponibile:
- [ ] prompt ACCESS_LOCAL_NETWORK just-in-time
- [ ] grant -> sessione parte
- [ ] deny -> recovery chiara
- [ ] revoca permission -> comportamento fail-safe

## Release identity

- [ ] keystore persistente configurata
- [ ] certificate SHA-256 registrato
- [ ] APK release firmato
- [ ] AAB generato
- [ ] update-in-place con stessa signing identity
- [ ] artifact fisicamente testato = artifact distribuito

## Evidence semantics

Usare solo:
- PASS
- FAIL
- BLOCKED
- NOT VERIFIED

CI GREEN non rende automaticamente PASS gli scenari fisici.
