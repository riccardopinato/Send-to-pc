# AdMob readiness — Invia al PC

Stato corrente: PREPARED / NOT VERIFIED FOR PRODUCTION

## Runtime già implementato

- Google Mobile Ads Next-Gen SDK.
- UMP.
- `canRequestAds()` gate.
- Adaptive banner fuori dalla sessione attiva.
- Interstitial solo a pausa naturale dopo sessione completata e significativa.
- Frequency cap interno.
- Debug/slim/QA forzano gli ID di test ufficiali.
- Store candidate release fail-closed senza i tre ID production.
- Store candidate rifiuta ID malformati e gli ID demo ufficiali Google.

## Configurazione production da completare

Creare/configurare nell'account AdMob:
- app Android con package `com.riccardopinato.inviaalpc`;
- una Ad Unit banner adaptive;
- una Ad Unit interstitial.

Configurare nel repository GitHub:
- `ADMOB_APP_ID`
- `ADMOB_BANNER_ID`
- `ADMOB_INTERSTITIAL_ID`

Gli ID non sono password, ma restano separati dal codice per evitare che debug/QA usino accidentalmente traffico live.

## app-ads.txt

Per la distribuzione Store:

1. definire un dominio/sito sviluppatore controllato;
2. inserire quel sito nella scheda sviluppatore/app dello Store secondo i requisiti correnti;
3. copiare da AdMob la riga publisher esatta;
4. pubblicarla come `https://<dominio>/app-ads.txt`;
5. attendere la scansione/verifica AdMob;
6. controllare che lo stato risulti verificato.

La propagazione/scansione può richiedere tempo. Non dichiarare il gate chiuso finché AdMob non mostra lo stato corretto.

Usare `store/app-ads.txt.example` solo come struttura. Non pubblicare publisher ID inventati.

## App readiness / eligibility

Per nuove app AdMob, verificare lo stato di readiness/eligibility prima di aspettarsi erogazione pubblicitaria normale.

Registrare:
- stato app in AdMob;
- eventuale review;
- data verifica;
- eventuali limitazioni all'ad serving.

## UMP / privacy

Prima dello Store:
- verificare il consent flow su device reale;
- verificare `canRequestAds`;
- verificare Privacy Options quando richiesto;
- verificare il comportamento in EEA/UK dove pertinente;
- definire esplicitamente audience/age treatment;
- aggiornare Privacy Policy e Data Safety.

## Test traffic

DEBUG / SLIM / QA / TEST MANUALE:
- solo sample test IDs ufficiali, oppure proprio Ad Unit ID su device registrato come test device;
- non cliccare annunci live.

PRODUCTION:
- ID production;
- nessun test-device mode sugli utenti normali.

## Gate

AdMob production è VERIFIED solo dopo:
- production IDs configurati;
- Store candidate build verde;
- UMP verificato fisicamente;
- banner verificato fisicamente;
- interstitial verificato fisicamente;
- no-fill/error path verificato;
- app-ads.txt verificato;
- app readiness nota/approvata;
- privacy/Data Safety coerenti.
