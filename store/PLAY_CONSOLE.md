# Google Play Console pack — Invia al PC

Target preparation: v1.26.
Package: `com.riccardopinato.inviaalpc`
Current targetSdk: 36.

## App access

- No account required.
- No login credentials required for reviewer.
- Core transfer requires phone and PC/browser on the same local network.
- PC authenticates with the PIN shown on the phone.

## Ads

- Contains ads: YES for the monetized Store build.
- Google AdMob + UMP.
- Store Candidate workflow fails closed unless production AdMob IDs are configured and are not Google demo IDs.

## Data safety

Use `store/DATA_SAFETY.md` as engineering evidence, then complete the Play Console form against the exact final AAB.

Important boundaries:
- transferred files/photos/text/links stay on the local LAN and are not uploaded to an app-owned cloud;
- LAN transfer is TLS 1.2/1.3 from v1.20;
- GMA/UMP are separate external SDK network channels;
- review Google's current Mobile Ads disclosure at submission time.

## Privacy Policy

Publish `docs/privacy.html` on a stable HTTPS developer URL before submission.

Candidate GitHub Pages path if Pages is enabled from `/docs`:
`https://riccardopinato.github.io/Send-to-pc/privacy.html`

Do not put this candidate URL in Play Console until it is actually reachable.

## Target audience

Decision required before submission:
- choose the intended age groups in Play Console;
- configure UMP/AdMob age treatment consistently;
- do not claim child-directed treatment unless the product/configuration is intentionally designed for it.

## Content rating

Complete the questionnaire from the final product behavior. The transfer utility itself does not supply a content catalog; users choose their own local files/text/links.

## Permissions / foreground service

Document:
- INTERNET / network connectivity;
- foreground service dataSync for active local transfer;
- notification permission on supported Android versions;
- ACCESS_LOCAL_NETWORK only when/if targetSdk reaches 37 and the runtime permission is actually introduced.

## Release evidence

Before production:
- final versionName/versionCode;
- final source SHA;
- signed AAB;
- signer fingerprint;
- AAB SHA-256;
- Store Candidate workflow run;
- production AdMob configured=true;
- Physical QA;
- update-in-place;
- app-ads.txt VERIFIED;
- AdMob readiness known;
- Data Safety final review.

## External gates

The repository cannot close these by itself:
- Play Console app creation/submission;
- AdMob app/ad units;
- GitHub Pages or another public website activation if not already enabled;
- publisher-specific app-ads.txt line;
- Play content rating / target audience submission;
- final rollout.
