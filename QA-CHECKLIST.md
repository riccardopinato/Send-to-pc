# QA checklist — Invia al PC v1.19.0

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

- [ ] test automatico: parità 202/202 chiavi EN / IT / ES / FR / DE / PT

Per EN / IT / ES / FR / DE / PT:
- [ ] primo avvio
- [ ] override manuale
- [ ] ritorno a Sistema
- [ ] navigazione principale
- [ ] sessione QR/PIN
- [ ] pagina browser
- [ ] notifiche
- [ ] stringhe lunghe senza overflow

## Monetizzazione / AdMob

- [ ] debug/slim usano esclusivamente Test Ads
- [ ] UMP requestConsentInfoUpdate ad ogni avvio
- [ ] nessuna ad request prima di canRequestAds
- [ ] Privacy Options visibile quando UMP la richiede
- [ ] banner adaptive senza overlap con CTA/navigation
- [ ] banner assente durante sessione di trasferimento attiva
- [ ] no-fill/network error non blocca la UI
- [ ] trasferimento breve/piccolo: nessun interstitial
- [ ] trasferimento significativo: interstitial solo dopo tap Fine
- [ ] massimo un interstitial per sessione
- [ ] cooldown minimo 5 minuti rispettato
- [ ] ad non pronto: nessuna attesa per l'utente
- [ ] portrait / landscape
- [ ] dark mode / font scale
- [ ] production IDs verificati prima dello Store
- [ ] app-ads.txt pubblicato alla root del sito sviluppatore collegato alla scheda Store
- [ ] app-ads.txt verificato in AdMob
- [ ] AdMob app readiness APPROVED prima dello Store

## Rete / guida recovery

- [ ] guida: stessa Wi-Fi privata
- [ ] guida: VPN disattivata
- [ ] guida: evitare rete guest con client isolation
- [ ] guida: hotspot mobile del PC come alternativa
- [ ] dopo cambio rete: nuovo QR/sessione

## Android 17 / API 37

Solo quando il progetto passerà anche a targetSdk 37:
- [ ] manifest dichiara ACCESS_LOCAL_NETWORK
- [ ] prompt ACCESS_LOCAL_NETWORK just-in-time
- [ ] grant -> sessione parte
- [ ] deny -> recovery chiara
- [ ] revoca permission -> comportamento fail-safe

## Store / Data Safety

- [ ] target API conforme al requisito Play vigente
- [ ] Data safety include le pratiche effettive del GMA Next-Gen SDK
- [ ] nessuna dichiarazione falsa "all data encrypted in transit"
- [ ] protocollo LAN HTTP riesaminato rispetto alla User Data policy
- [ ] STORE-READINESS.md senza blocker P0 aperti

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
