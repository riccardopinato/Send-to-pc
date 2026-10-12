# AdMob production activation — Send to PC v1.27

Package Android: `com.riccardopinato.inviaalpc`

Stato: **ACCOUNT-SIDE CONFIGURATION REQUIRED**

Il runtime AdMob è già implementato e usa:
- Google Mobile Ads Next-Gen SDK;
- UMP;
- banner adaptive;
- interstitial solo a pausa naturale;
- test IDs obbligatori su debug/slim;
- production IDs solo sulla release;
- Store Candidate fail-closed.

## 1. Crea/collega l'app in AdMob

In AdMob:
1. App -> Aggiungi app.
2. Piattaforma: Android.
3. Nome: Send to PC.
4. Package: `com.riccardopinato.inviaalpc`.
5. Se non è ancora pubblicata su Google Play, configurarla come app non ancora pubblicata e collegarla allo Store quando la listing esiste.

Copia l'**App ID**, formato:
`ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`

Questo valore diventa:
`ADMOB_APP_ID`

## 2. Crea Banner production

Nell'app AdMob:
1. Unità pubblicitarie -> Aggiungi unità pubblicitaria.
2. Formato: Banner.
3. Nome consigliato: `Send to PC - Adaptive Banner`.
4. Mantieni il posizionamento coerente con l'anchored adaptive banner già implementato.

Copia l'Ad Unit ID, formato:
`ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY`

Questo valore diventa:
`ADMOB_BANNER_ID`

## 3. Crea Interstitial production

Nell'app AdMob:
1. Unità pubblicitarie -> Aggiungi unità pubblicitaria.
2. Formato: Interstitial.
3. Nome consigliato: `Send to PC - Session Complete Interstitial`.
4. L'app lo mostra solo dopo una sessione completata significativa, non durante il trasferimento.

Copia l'Ad Unit ID.

Questo valore diventa:
`ADMOB_INTERSTITIAL_ID`

## 4. GitHub Secrets

Repository:
`riccardopinato/Send-to-pc`

Settings -> Secrets and variables -> Actions -> New repository secret.

Creare esattamente:
- `ADMOB_APP_ID`
- `ADMOB_BANNER_ID`
- `ADMOB_INTERSTITIAL_ID`

Non sostituire i signing secrets già esistenti.

## 5. Gate automatico

Il workflow `Android release`, con `store_candidate=true`, rifiuta:
- secret mancanti;
- formato ID non valido;
- ID demo ufficiali Google.

La release build riceve i production IDs via environment e imposta:
`ADMOB_PRODUCTION_CONFIGURED=true`

## 6. app-ads.txt

Per nuove app AdMob, la verifica app-ads.txt è richiesta prima del pieno ad serving.

Passi:
1. avere un developer website HTTPS controllato;
2. collegarlo alla Store listing;
3. copiare da AdMob la publisher line reale;
4. pubblicarla come `/app-ads.txt`;
5. attendere lo stato VERIFIED in AdMob.

Non usare il placeholder del repository come file production.

## 7. UMP / physical QA

La presenza dei production IDs non chiude da sola AdMob.

Sul candidato v1.27 verificare:
- UMP consent flow;
- `canRequestAds()`;
- Privacy Options quando richiesto;
- banner;
- interstitial;
- no-fill;
- offline;
- error recovery;
- nessun annuncio durante trasferimento attivo;
- frequency cap.

## Riferimenti ufficiali

- https://support.google.com/admob/answer/9989980
- https://support.google.com/admob/answer/7356431
- https://support.google.com/admob/answer/7311346
- https://support.google.com/admob/answer/7311435
- https://support.google.com/admob/answer/14538460

## Exit criterion

AdMob production passa da PREPARED a CONFIGURED quando:
- i 3 ID reali esistono;
- i 3 GitHub Secrets sono impostati;
- `Android release` con `store_candidate=true` termina GREEN;
- `RELEASE_IDENTITY.txt` riporta `admob_production_configured=true`.

La piena verifica AdMob resta successiva al Physical QA e alla verifica app-ads.txt/readiness.
