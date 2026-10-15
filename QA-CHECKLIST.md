# QA checklist — Send to PC v1.28.1

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
- [ ] testo dal browser → telefono
- [ ] link http/https dal browser → telefono
- [ ] link con schema non consentito rifiutato
- [ ] copia testo ricevuto
- [ ] apri link ricevuto
- [ ] share sheet per file/testo/link ricevuti
- [ ] progress complessivo coerente per batch multi-file
- [ ] velocità PC → telefono plausibile e non ferma a 0 B/s
- [ ] dopo il completamento di un file la sessione resta CONNECTED per i file successivi
- [ ] riepilogo sessione con conteggio/direzione/byte/durata
- [ ] pausa / riprendi / annulla
- [ ] resume selezionando lo stesso file
- [ ] retry dell'ultimo chunk non duplica history/summary
- [ ] record completed con content URI cancellato viene invalidato e non blocca un nuovo upload
- [ ] file duplicato senza sovrascrittura
- [ ] nuovi file salvati in Download/Send to PC
- [ ] file legacy in Download/Invia al PC restano accessibili e il cleanup non li corrompe
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
- [ ] URL sessione usa esclusivamente https://
- [ ] TLS 1.2/1.3 handshake su browser target
- [ ] fingerprint telefono == fingerprint certificato browser
- [ ] cookie sid/trusted_pc hanno flag Secure
- [ ] sniffing LAN non espone payload file/testo/link
- [ ] warning self-signed/recovery comprensibile

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

- [ ] test automatico: zero chiavi mancanti rispetto a EN per IT/ES/FR/DE/PT

Per EN / IT / ES / FR / DE / PT:
- [ ] primo avvio
- [ ] override manuale
- [ ] ritorno a Sistema
- [ ] navigazione principale
- [ ] sessione QR/PIN
- [ ] pagina browser
- [ ] notifiche
- [ ] stringhe lunghe senza overflow

## Cronologia

- [ ] trasferimenti v1.19 raggruppati correttamente per sessione
- [ ] vecchie entry prive di sessionId/kind restano leggibili
- [ ] gruppi con file + testo + link mantengono azioni corrette
- [ ] cancellazione cronologia non elimina file ricevuti

## Network help

- [ ] IP/rete locale mostrati correttamente in Impostazioni
- [ ] guida stessa rete locale comprensibile e coerente con PC via Ethernet
- [ ] telefono Wi-Fi + PC Ethernet sulla stessa LAN viene rilevato correttamente
- [ ] Wi-Fi LAN non-default resta rilevabile quando Android usa un'altra default network
- [ ] indicazioni VPN / guest-client isolation visibili
- [ ] hotspot del PC → telefono collegato come client

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
- [ ] workflow `store_candidate=true` fallisce senza i 3 ID production
- [ ] workflow `store_candidate=true` usa versione/artifact identity corrente
- [ ] app-ads.txt / AdMob readiness verificati prima dello Store

## Android 17 / API 37

Solo quando il progetto passerà anche a targetSdk 37:
- [ ] manifest dichiara ACCESS_LOCAL_NETWORK
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


## PC Quick Connect — v1.28 / v1.28.1

- [ ] Sul PC digitare soltanto `https://IP:porta` mostrato dal telefono.
- [ ] Verificare il warning TLS/fingerprint come nel flusso esistente.
- [ ] Inserire il codice connessione a 8 caratteri anche nel formato `XXXX-XXXX`.
- [ ] Confermare l'apertura della dashboard senza dover digitare il token lungo.
- [ ] Inserire codice errato ripetutamente e verificare il rate-limit.
- [ ] Verificare che un PC fidato possa riaprire il root URL senza nuovo codice finché la credenziale è valida.
- [ ] Verificare "Condividi link completo" verso un secondo dispositivo/PC.
- [ ] Espandere "Mostra QR code" e verificare il flusso QR + PIN esistente.
- [ ] Verificare che il root URL non mostri il token lungo nel sorgente HTML prima dell'autenticazione.
- [ ] Ripetere su rete Wi-Fi normale e hotspot del PC.
- [ ] Ripetere con telefono su Wi-Fi e PC collegato alla stessa LAN via Ethernet.
