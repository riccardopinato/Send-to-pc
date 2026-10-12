# Store release checklist — Send to PC

Versione target corrente: v1.26.0
Package: `com.riccardopinato.inviaalpc`

## Product / build

[ ] feature freeze confermata
[ ] unit test PASS
[ ] lint PASS
[ ] release build PASS
[ ] versione/versionCode definitivi
[ ] package invariato
[ ] signing fingerprint atteso verificato
[ ] AAB firmato generato
[ ] SHA-256 artifact registrato
[ ] source SHA registrato

## AdMob

[ ] app creata/configurata in AdMob
[ ] banner production Ad Unit creato
[ ] interstitial production Ad Unit creato
[ ] `ADMOB_APP_ID` configurato in GitHub Secrets
[ ] `ADMOB_BANNER_ID` configurato in GitHub Secrets
[ ] `ADMOB_INTERSTITIAL_ID` configurato in GitHub Secrets
[ ] `Android release` eseguito con `store_candidate=true`
[ ] artifact evidence riporta `admob_production_configured=true`
[ ] UMP verificato fisicamente
[ ] banner adaptive verificato fisicamente
[ ] interstitial policy/frequency cap verificati fisicamente
[ ] no-fill/offline/error path non blocca l'app
[ ] audience/age treatment dichiarato

## app-ads.txt / readiness

[ ] sito/dominio sviluppatore definito
[ ] sito sviluppatore collegato alla Store listing
[ ] riga publisher copiata esattamente da AdMob
[ ] `/app-ads.txt` pubblicato sul dominio corretto
[ ] app-ads.txt VERIFIED in AdMob
[ ] stato app readiness/eligibility noto
[ ] eventuale review AdMob completata o stato documentato

## Privacy / Play Console

[ ] TLS 1.2/1.3 LAN verificato sull'esatto candidato
[ ] fingerprint telefono == certificato browser
[ ] nessun endpoint di trasferimento HTTP presente nell'artifact finale
[ ] Privacy Policy pubblicata e raggiungibile
[ ] Data Safety ricontrollata sulla build finale
[ ] contenuti LAN distinti chiaramente dai dati trattati dagli SDK advertising
[ ] dichiarazioni ads coerenti
[ ] content rating completato
[ ] target audience completata
[ ] eventuale advertising ID behavior coerente con SDK/configurazione

## Store listing

[ ] nome app definitivo
[ ] short description
[ ] full description
[ ] icona 512x512
[ ] feature graphic
[ ] screenshot telefono
[ ] screenshot coerenti con la release finale
[ ] email/sito supporto
[ ] localizzazioni Store decise

## Physical QA / release reality

[ ] exact APK candidato installato su device reale
[ ] telefono → PC PASS
[ ] PC → telefono PASS
[ ] testo/link bidirezionali PASS
[ ] file grande PASS
[ ] resume PASS
[ ] screen-off/background PASS
[ ] trusted PC PASS
[ ] dark/font-scale/navigation PASS
[ ] EN/IT/ES/FR/DE/PT smoke PASS
[ ] AdMob/UMP PASS
[ ] update-in-place dalla precedente release firmata PASS
[ ] artifact fisicamente testato = artifact distribuito

## Verdict

Solo dopo i gate pertinenti:

CERTIFIED / NOT CERTIFIED / BLOCKED

Un AAB firmato e CI GREEN non equivalgono da soli a STORE READY.

La cifratura TLS è implementata; il verdetto Store resta **BLOCKED** finché trust flow, AdMob/UMP, Physical QA e update-in-place non sono verificati.
