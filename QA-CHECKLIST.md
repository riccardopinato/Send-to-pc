# Final QA checklist — Invia al PC v1.10.0

## Telefono → PC
- File singolo piccolo.
- Foto multiple.
- File > 1 GB.
- Download interrotto e ripreso con client compatibile HTTP Range.
- Testo e link.
- 100 file selezionati.

## PC → telefono
- Drag & drop singolo e multiplo.
- Pausa / Riprendi / Annulla.
- Riconnessione dopo breve perdita di rete.
- Ripresa dello stesso file parziale.
- File duplicato senza sovrascrittura.
- Verifica salvataggio in Download/Invia al PC.

## Sessione
- PIN errato e blocco tentativi.
- PC fidato / revoca PC.
- Cambio Wi-Fi durante sessione.
- Schermo spento durante file grande.
- Timeout sessione.
- Termina dalla notifica.
- Termina durante trasferimento con conferma.

## Android
- Installazione pulita.
- Aggiornamento dalla versione precedente con la stessa chiave di firma.
- Notifiche consentite e negate.
- Modalità scura/chiara.
- Rotazione e ritorno da background.
- Android 10, 13, 15/16 se disponibili.

## Release gate
- Unit test: PASS.
- Android Lint: PASS.
- Slim build: PASS.
- R8/resource shrink: PASS.
- apksigner: PASS.
- SHA-256 pubblicato.
