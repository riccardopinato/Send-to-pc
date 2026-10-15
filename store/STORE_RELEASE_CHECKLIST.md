# Store release checklist — Send to PC

Versione target corrente: v1.28.1
Package: `com.riccardopinato.inviaalpc`

## Product / build

[x] feature scope v1.28.1 definito
[ ] unit test PASS sull'head finale
[ ] lint PASS sull'head finale
[ ] release build PASS sull'head finale
[x] versionName/versionCode impostati: 1.28.1 / 45
[x] package invariato
[x] signing fingerprint atteso definito
[ ] APK/AAB firmati v1.28.1 generati
[ ] SHA-256 artifact v1.28.1 registrato
[ ] source SHA finale registrato

## AdMob

[x] app creata/configurata in AdMob
[x] banner production Ad Unit creato
[x] interstitial production Ad Unit creato
[x] `ADMOB_APP_ID` configurato in GitHub Secrets
[x] `ADMOB_BANNER_ID` configurato in GitHub Secrets
[x] `ADMOB_INTERSTITIAL_ID` configurato in GitHub Secrets
[x] banner production verificato fisicamente
[x] UMP European Regulations verificato fisicamente
[x] privacy options/revoca consenso verificate fisicamente
[ ] interstitial production + frequency cap verificati fisicamente
[ ] no-fill/offline/error path verificato fisicamente
[ ] audience/age treatment dichiarato
[ ] release v1.28.1 confermata dal gate `store_candidate`

## app-ads.txt / readiness

[ ] sito/dominio sviluppatore definitivo
[ ] sito sviluppatore collegato alla Store listing
[ ] riga publisher copiata esattamente da AdMob
[ ] `/app-ads.txt` pubblicato sul dominio corretto
[ ] app-ads.txt VERIFIED in AdMob
[ ] stato app readiness/eligibility noto
[ ] eventuale review AdMob completata o stato documentato

## Privacy / Play Console

[ ] TLS 1.2/1.3 LAN verificato sull'esatto candidato v1.28.1
[ ] fingerprint telefono == certificato browser
[ ] nessun endpoint payload HTTP presente nell'artifact finale
[x] Privacy Policy pubblicata e raggiungibile
[ ] Data Safety ricontrollata sull'AAB finale
[x] documentazione distingue contenuti LAN dai dati trattati dagli SDK advertising
[ ] dichiarazioni ads coerenti in Play Console
[ ] content rating completato
[ ] target audience completata
[ ] advertising ID behavior coerente con SDK/configurazione

## Store listing

[x] nome app definitivo
[x] short description preparata
[x] full description preparata
[x] listing copy riallineata a Quick Connect
[ ] icona 512x512 finale
[ ] feature graphic
[ ] screenshot telefono
[ ] screenshot coerenti con la release finale
[ ] email/sito supporto definitivi
[ ] localizzazioni Store inserite/approvate in Play Console

## Physical QA / release reality

[ ] exact APK v1.28.1 installato su device reale
[ ] Quick Connect: telefono Wi-Fi + PC Wi-Fi PASS
[ ] Quick Connect: telefono Wi-Fi + PC Ethernet PASS
[ ] telefono → PC PASS
[ ] PC → telefono PASS
[ ] testo/link bidirezionali PASS
[ ] file grande PASS
[ ] batch multi-file: sessione non diventa terminale dopo il primo file
[ ] resume PASS
[ ] retry ultimo chunk: nessuna doppia entry history/summary
[ ] metadata completed con file cancellato: nuovo upload non viene falsamente considerato già completato
[ ] velocità PC → telefono plausibile e non bloccata a 0 B/s
[ ] screen-off/background PASS
[ ] trusted PC PASS
[ ] dark/font-scale/navigation PASS
[ ] EN/IT/ES/FR/DE/PT smoke PASS
[ ] production interstitial PASS
[ ] update-in-place dalla precedente release firmata PASS
[ ] artifact fisicamente testato = artifact distribuito

## Verdict

Solo dopo i gate pertinenti:

CERTIFIED / NOT CERTIFIED / BLOCKED

Un AAB firmato e CI GREEN non equivalgono da soli a STORE READY.
