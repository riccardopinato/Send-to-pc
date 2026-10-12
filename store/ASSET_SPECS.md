# Google Play asset specification — Send to PC

Validate again against Play Console at submission time.

## App icon

Required Store asset:
- 512 × 512 px;
- 32-bit PNG;
- alpha allowed;
- max 1 MB.

Use the existing app identity as the source, but export a dedicated Play Store PNG rather than relying on the adaptive launcher XML.

## Feature graphic

Required target:
- 1024 × 500 px;
- JPEG or 24-bit PNG;
- no alpha.

Creative direction:
- app logo / phone on the left;
- browser/PC on the right;
- local encrypted connection motif between them;
- avoid tiny text;
- do not show a claim such as “100% secure”;
- suitable copy if used: “Phone ↔ PC. Local. Direct.”

## Phone screenshots

Prepare screenshots only from the exact final candidate after Physical QA.

Minimum preparation set:
1. Home / Connect;
2. HTTPS QR + PIN session;
3. PC browser transfer page;
4. transfer progress;
5. completed summary;
6. Recent transfers;
7. Trusted PCs;
8. Settings / language / privacy.

Capture:
- portrait first;
- one coherent device frame/style;
- no debug/test ad identifiers visible;
- no personal filenames, IPs, PINs or fingerprints that belong to a real user session;
- screenshots must match the shipped UI.

## Localization

Preferred initial Store image strategy:
- English master set;
- Italian master set;
- reuse language-neutral visuals for ES/FR/DE/PT only if listing quality remains high.

## Release gate

Assets are VERIFIED only when:
- generated from final UI;
- visual QA completed;
- no obsolete screens;
- no unsupported feature claims;
- Play Console accepts dimensions/format.
