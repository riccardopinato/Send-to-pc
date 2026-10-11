package com.riccardopinato.inviaalpc

import android.content.Context
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val nativeName: String
) {
    EN("en", "English"),
    IT("it", "Italiano"),
    ES("es", "Español"),
    FR("fr", "Français"),
    DE("de", "Deutsch"),
    PT("pt", "Português");

    companion object {
        fun fromCode(code: String?): AppLanguage? =
            entries.firstOrNull {
                it.code.equals(
                    code,
                    ignoreCase = true
                )
            }
    }
}

object AppLanguageStore {
    private const val PREFS = "app_preferences"
    private const val KEY = "language_override"

    fun current(
        context: Context
    ): AppLanguage {
        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val override =
            AppLanguage.fromCode(
                prefs.getString(KEY, null)
            )

        if (override != null) {
            return override
        }

        val deviceCode =
            Locale.getDefault().language

        return AppLanguage.fromCode(
            deviceCode
        ) ?: AppLanguage.EN
    }

    fun overrideCode(
        context: Context
    ): String? =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).getString(KEY, null)

    fun setOverride(
        context: Context,
        language: AppLanguage?
    ) {
        val editor =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            ).edit()

        if (language == null) {
            editor.remove(KEY)
        } else {
            editor.putString(
                KEY,
                language.code
            )
        }

        editor.apply()
    }
}

object AppText {
    private val en = mapOf(
        "app_name" to "Send to PC",
        "help" to "Help",
        "connect" to "Connect",
        "recents" to "Recent",
        "trusted_pcs" to "Trusted PCs",
        "pc" to "PC",
        "network_ready" to "Local network • %s",
        "network_unavailable" to "Local Wi-Fi unavailable",
        "phone_pc" to "PHONE ↔ PC",
        "connect_devices" to "Connect your devices",
        "connect_subtitle" to "Create a local session. From the PC browser you can send and receive files in the same connection.",
        "phone_content_optional" to "Content from phone (optional)",
        "file" to "File",
        "photo" to "Photo",
        "text" to "Text",
        "text_hint" to "Write or paste text...",
        "link" to "Link",
        "preparing" to "Preparing...",
        "connect_pc" to "Connect to PC",
        "selected_files" to "Selected files",
        "remove" to "Remove %s",
        "close" to "Close",
        "receive_from_pc" to "Receive from PC",
        "same_wifi_tip" to "Phone and PC must be on the same private Wi-Fi network.",
        "session_local" to "LOCAL SESSION",
        "connection_ready" to "Connection ready",
        "waiting_pc" to "Waiting for the PC. Scan the QR code or copy the address.",
        "pc_connected" to "PC connected and authenticated.",
        "transferring" to "Transfer in progress.",
        "completed_open" to "Transfer complete. The session remains open.",
        "network_changed" to "The network changed or the session is no longer reachable.",
        "session_expired" to "The session expired.",
        "session_stopped" to "Session ended.",
        "status_waiting" to "Waiting",
        "status_connected" to "PC connected",
        "status_transferring" to "Transfer",
        "status_completed" to "Completed",
        "status_expired" to "Expired",
        "status_stopped" to "Ended",
        "status_error" to "Network error",
        "pin_access" to "ACCESS PIN",
        "expires_in" to "Expires in %s",
        "copy_address" to "Copy address",
        "open_last_received" to "Open last received file",
        "end_session" to "End session",
        "end_transfer_title" to "Stop the transfer?",
        "end_transfer_body" to "The current file will remain resumable when possible, but the current session will be closed.",
        "end" to "End",
        "continue" to "Continue",
        "direct_local" to "Direct connection over the local network. No file is uploaded to external servers.",
        "recent_transfers" to "Recent transfers",
        "recent_empty" to "No completed transfers yet.",
        "clear" to "Clear",
        "clear_history_title" to "Clear history?",
        "clear_history_body" to "Only the local history will be removed. Transferred files will not be deleted.",
        "cancel" to "Cancel",
        "open" to "Open",
        "send_again" to "Send again",
        "trusted_devices" to "Trusted PCs",
        "trusted_empty" to "No trusted PC yet.",
        "revoke_all" to "Revoke all",
        "revoke_all_title" to "Revoke all PCs?",
        "revoke_all_body" to "All trusted PCs will have to use the PIN again.",
        "revoke" to "Revoke %s",
        "onboarding_intro" to "Transfer files, photos, text and links directly between phone and PC.",
        "onboarding_step1_title" to "Same network",
        "onboarding_step1_body" to "Phone and PC must be connected to the same private local Wi-Fi.",
        "onboarding_step2_title" to "Create the session",
        "onboarding_step2_body" to "Optionally choose content from the phone, then connect to the PC.",
        "onboarding_step3_title" to "Open from the PC",
        "onboarding_step3_body" to "Scan the QR code or open the address in the browser. The PIN protects the session.",
        "onboarding_privacy" to "No account and no cloud. Files stay on the local network.",
        "version" to "Version %s",
        "start" to "Start",
        "language" to "Language",
        "system_language" to "System",
        "unsafe_wifi" to "Use only a private or trusted Wi-Fi network: local HTTP traffic is not encrypted.",
        "err_file_limit" to "You can share up to %d files per session.",
        "err_file_access" to "I cannot access the selected files. Choose them again from the system picker.",
        "err_first_files" to "Only the first %d files were added.",
        "err_invalid_link" to "The link is invalid. Use an http:// or https:// address.",
        "err_same_wifi" to "Phone and PC must be connected to the same Wi-Fi.",
        "err_android_block" to "Android blocked the session start. Check the app permissions and try again.",
        "err_port_busy" to "The network port is busy. Close any previous session and try again.",
        "err_start_session" to "I cannot start the local transfer. Check that phone and PC are on the same Wi-Fi.",
        "err_file_missing" to "The file is no longer available.",
        "err_no_app_open" to "No app is available to open this file.",
        "err_original_missing" to "The original file is no longer available.",
        "err_original_unreadable" to "The original file is no longer accessible. Select it again.",
        "end_expired" to "The session expired. Create a new connection.",
        "end_network_lost" to "Wi-Fi was unavailable for more than 12 seconds. Reconnect and create a new session.",
        "end_ip_changed" to "The phone network address changed. Create a new QR code to continue.",
        "end_android_timeout" to "Android ended the background transfer service. Create a new session.",
        "end_server_error" to "The local server stopped unexpectedly. Try creating the session again.",
        "selected_count" to "%d items selected",
        "more_files" to "+ %d more files",
        "history_note" to "Only the history is saved, not a copy of the files.",
        "sent" to "Sent",
        "received" to "Received",
        "progress_send" to "Sending to PC",
        "progress_receive" to "Receiving from PC",
        "about_time" to "about %s",
        "address_copied" to "Address copied",
        "trusted_note" to "Trusted PCs can reconnect without entering the PIN until the credential expires.",
        "last_seen" to "Last seen %s",
        "browser_pin_hint" to "Enter the PIN shown on the phone.",
        "browser_check_trusted" to "Checking trusted PC...",
        "browser_wrong_pin" to "Wrong PIN. Try again.",
        "browser_connect" to "Connect",
        "browser_local_connection" to "Local connection",
        "browser_from_phone" to "From phone",
        "browser_available" to "Content available in this session.",
        "browser_no_files" to "No file shared from the phone.",
        "browser_unknown_size" to "Unknown size",
        "browser_download" to "Download",
        "browser_copy_text" to "Copy text",
        "browser_to_phone" to "Send to phone",
        "browser_save_path" to "Files will be saved in Download/Send to PC.",
        "browser_drop" to "Drop files here",
        "browser_choose" to "or click to select them",
        "browser_ready" to "Ready.",
        "browser_quick" to "Quick connection",
        "browser_trust_desc" to "Remember this PC for 30 days. The credential rotates at every reconnection and cannot be read by page scripts.",
        "browser_remember" to "Remember this PC",
        "browser_remembered" to "PC remembered",
        "browser_private_wifi" to "Direct transfer over the local network. Use a private or trusted Wi-Fi: local HTTP traffic is not encrypted.",
        "browser_queue" to "Queued",
        "browser_pause" to "Pause",
        "browser_resume" to "Resume",
        "browser_cancel" to "Cancel",
        "browser_transfer" to "Transfer",
        "browser_cancelled" to "Cancelled",
        "browser_completed" to "Completed",
        "browser_reconnecting" to "Reconnecting",
        "browser_preparing" to "Preparing...",
        "browser_not_transferred" to "Not transferred • select the same file again to resume",
        "browser_copied" to "Text copied.",
        "browser_copy_manual" to "Select the text and copy it manually.",
        "browser_trust_saved" to "Quick connection enabled for %s.",
        "browser_trust_failed" to "Unable to remember this PC.",
        "browser_device" to "Device: %s",
        "browser_limit" to "For stability, only the first %d files will be processed.",
        "browser_summary" to "Completed %d • errors %d • cancelled %d.",
        "browser_summary_ok" to "Completed %d files.",
        "browser_session_missing" to "Session unavailable",
        "browser_session_missing_body" to "Start a new session from the phone.",
        "browser_session_expired" to "Session expired",
        "browser_session_expired_body" to "Create a new QR code from the phone.",
        "browser_not_found" to "Not found",
        "browser_not_found_body" to "The requested resource does not exist.",
        "notification_active" to "Local transfer session active",
        "notification_end" to "End",
        "notification_channel" to "Local transfer",
        "notification_channel_desc" to "File transfer sessions over the local network"
    )

    private val it = mapOf(
        "app_name" to "Invia al PC", "help" to "Guida", "connect" to "Connetti",
        "recents" to "Recenti", "trusted_pcs" to "PC fidati", "pc" to "PC",
        "network_ready" to "Rete locale • %s", "network_unavailable" to "Wi-Fi locale non disponibile",
        "phone_pc" to "TELEFONO ↔ PC", "connect_devices" to "Connetti i tuoi dispositivi",
        "connect_subtitle" to "Crea una sessione locale: dal browser del PC puoi inviare e ricevere file nella stessa connessione.",
        "phone_content_optional" to "Contenuti dal telefono (opzionali)", "file" to "File", "photo" to "Foto",
        "text" to "Testo", "text_hint" to "Scrivi o incolla del testo...", "link" to "Link",
        "preparing" to "Preparazione...", "connect_pc" to "Connetti al PC", "selected_files" to "File selezionati",
        "remove" to "Rimuovi %s", "close" to "Chiudi", "session_local" to "SESSIONE LOCALE",
        "connection_ready" to "Connessione pronta", "waiting_pc" to "In attesa del PC. Scansiona il QR o copia l'indirizzo.",
        "pc_connected" to "PC collegato e autenticato.", "transferring" to "Trasferimento in corso.",
        "completed_open" to "Trasferimento completato. La sessione resta aperta.",
        "network_changed" to "La rete è cambiata o la sessione non è più raggiungibile.",
        "session_expired" to "La sessione è scaduta.", "session_stopped" to "Sessione terminata.",
        "status_waiting" to "In attesa", "status_connected" to "PC collegato", "status_transferring" to "Trasferimento",
        "status_completed" to "Completato", "status_expired" to "Scaduta", "status_stopped" to "Terminata",
        "status_error" to "Errore rete", "pin_access" to "PIN DI ACCESSO", "expires_in" to "Scade tra %s",
        "copy_address" to "Copia indirizzo", "open_last_received" to "Apri ultimo file ricevuto",
        "end_session" to "Termina sessione", "end_transfer_title" to "Interrompere il trasferimento?",
        "end_transfer_body" to "Il file in corso resterà riprendibile quando possibile, ma la sessione corrente verrà chiusa.",
        "end" to "Termina", "continue" to "Continua",
        "direct_local" to "Connessione diretta sulla rete locale. Nessun file viene caricato su server esterni.",
        "recent_transfers" to "Trasferimenti recenti", "recent_empty" to "Nessun trasferimento completato.",
        "clear" to "Cancella", "clear_history_title" to "Cancellare la cronologia?",
        "clear_history_body" to "Verrà rimossa solo la cronologia locale. I file trasferiti non saranno eliminati.",
        "cancel" to "Annulla", "open" to "Apri", "send_again" to "Invia di nuovo",
        "trusted_devices" to "PC fidati", "trusted_empty" to "Nessun PC fidato.",
        "revoke_all" to "Revoca tutti", "revoke_all_title" to "Revocare tutti i PC?",
        "revoke_all_body" to "Tutti i PC fidati dovranno usare nuovamente il PIN per essere associati.",
        "revoke" to "Revoca %s", "onboarding_intro" to "Trasferisci file, foto, testo e link direttamente tra telefono e PC.",
        "onboarding_step1_title" to "Stessa rete", "onboarding_step1_body" to "Telefono e PC devono essere collegati alla stessa Wi-Fi locale privata.",
        "onboarding_step2_title" to "Crea la sessione", "onboarding_step2_body" to "Scegli opzionalmente contenuti dal telefono, poi connettiti al PC.",
        "onboarding_step3_title" to "Apri dal PC", "onboarding_step3_body" to "Scansiona il QR o apri l'indirizzo nel browser. Il PIN protegge la sessione.",
        "onboarding_privacy" to "Nessun account, nessun cloud. I file restano sulla rete locale.",
        "version" to "Versione %s", "start" to "Inizia", "language" to "Lingua", "system_language" to "Sistema",
        "unsafe_wifi" to "Usa solo una Wi-Fi privata o fidata: il traffico HTTP locale non è cifrato.",
        "err_file_limit" to "Puoi condividere fino a %d file per sessione.",
        "err_file_access" to "Non riesco ad accedere ai file selezionati. Sceglili di nuovo dal selettore di sistema.",
        "err_first_files" to "Sono stati aggiunti solo i primi %d file.",
        "err_invalid_link" to "Il link non è valido. Usa un indirizzo http:// o https://.",
        "err_same_wifi" to "Telefono e PC devono essere collegati alla stessa Wi-Fi.",
        "err_android_block" to "Android ha bloccato l'avvio della sessione. Controlla i permessi dell'app e riprova.",
        "err_port_busy" to "La porta di rete è occupata. Chiudi eventuali sessioni precedenti e riprova.",
        "err_start_session" to "Non riesco ad avviare il trasferimento sulla rete locale. Verifica che telefono e PC siano sulla stessa Wi-Fi.",
        "err_file_missing" to "Il file non è più disponibile.", "err_no_app_open" to "Nessuna app disponibile per aprire questo file.",
        "err_original_missing" to "Il file originale non è più disponibile.",
        "err_original_unreadable" to "Il file originale non è più accessibile. Selezionalo di nuovo.",
        "end_expired" to "La sessione è scaduta. Crea un nuovo collegamento.",
        "end_network_lost" to "La Wi-Fi è rimasta non disponibile per più di 12 secondi. Riconnettiti e crea una nuova sessione.",
        "end_ip_changed" to "L'indirizzo di rete del telefono è cambiato. Crea un nuovo QR per continuare.",
        "end_android_timeout" to "Android ha terminato il servizio di trasferimento in background. Crea una nuova sessione.",
        "end_server_error" to "Il server locale si è arrestato in modo inatteso. Riprova a creare la sessione.",
        "selected_count" to "%d elementi selezionati",
        "more_files" to "+ %d altri file",
        "history_note" to "Viene salvata solo la cronologia, non una copia dei file.",
        "sent" to "Inviato",
        "received" to "Ricevuto",
        "progress_send" to "Invio al PC",
        "progress_receive" to "Ricezione dal PC",
        "about_time" to "circa %s",
        "address_copied" to "Indirizzo copiato",
        "trusted_note" to "I PC fidati possono riconnettersi senza PIN finché la credenziale non scade.",
        "last_seen" to "Ultimo accesso %s",
        "browser_pin_hint" to "Inserisci il PIN mostrato sul telefono.", "browser_check_trusted" to "Controllo PC fidato...",
        "browser_wrong_pin" to "PIN errato. Riprova.", "browser_connect" to "Connetti",
        "browser_local_connection" to "Connessione locale", "browser_from_phone" to "Dal telefono",
        "browser_available" to "Contenuti disponibili in questa sessione.", "browser_no_files" to "Nessun file condiviso dal telefono.",
        "browser_unknown_size" to "Dimensione sconosciuta", "browser_download" to "Scarica",
        "browser_copy_text" to "Copia testo", "browser_to_phone" to "Invia al telefono",
        "browser_save_path" to "I file verranno salvati in Download/Invia al PC.",
        "browser_drop" to "Trascina qui i file", "browser_choose" to "oppure clicca per selezionarli",
        "browser_ready" to "Pronto.", "browser_quick" to "Connessione rapida",
        "browser_trust_desc" to "Ricorda questo PC per 30 giorni. La credenziale viene ruotata a ogni riconnessione e non è leggibile dagli script della pagina.",
        "browser_remember" to "Ricorda questo PC", "browser_remembered" to "PC ricordato",
        "browser_private_wifi" to "Trasferimento diretto sulla rete locale. Usa una Wi-Fi privata o fidata: il traffico HTTP locale non è cifrato.",
        "browser_queue" to "In coda", "browser_pause" to "Pausa", "browser_resume" to "Riprendi",
        "browser_cancel" to "Annulla", "browser_transfer" to "Trasferimento", "browser_cancelled" to "Annullato",
        "browser_completed" to "Completato", "browser_reconnecting" to "Riconnessione",
        "browser_preparing" to "Preparazione...", "browser_not_transferred" to "Non trasferito • seleziona di nuovo lo stesso file per riprendere",
        "browser_copied" to "Testo copiato.", "browser_copy_manual" to "Seleziona il testo e copialo manualmente.",
        "browser_trust_saved" to "Connessione rapida attivata per %s.", "browser_trust_failed" to "Impossibile ricordare questo PC.",
        "browser_device" to "Dispositivo: %s", "browser_limit" to "Per stabilità verranno elaborati i primi %d file.",
        "browser_summary" to "Completati %d • errori %d • annullati %d.", "browser_summary_ok" to "Completati %d file.",
        "browser_session_missing" to "Sessione non disponibile", "browser_session_missing_body" to "Avvia una nuova sessione dal telefono.",
        "browser_session_expired" to "Sessione scaduta", "browser_session_expired_body" to "Crea un nuovo QR dal telefono.",
        "browser_not_found" to "Non trovato", "browser_not_found_body" to "La risorsa richiesta non esiste.",
        "notification_active" to "Sessione locale attiva", "notification_end" to "Termina",
        "notification_channel" to "Trasferimento locale", "notification_channel_desc" to "Sessioni di trasferimento file tramite rete locale"
    )

    private val es = mapOf(
        "app_name" to "Enviar al PC", "help" to "Ayuda", "connect" to "Conectar", "recents" to "Recientes",
        "trusted_pcs" to "PC de confianza", "network_unavailable" to "Wi-Fi local no disponible",
        "connect_devices" to "Conecta tus dispositivos", "phone_content_optional" to "Contenido del teléfono (opcional)",
        "photo" to "Foto", "text" to "Texto", "text_hint" to "Escribe o pega texto...", "link" to "Enlace",
        "preparing" to "Preparando...", "connect_pc" to "Conectar al PC", "close" to "Cerrar",
        "copy_address" to "Copiar dirección", "end_session" to "Finalizar sesión", "cancel" to "Cancelar",
        "open" to "Abrir", "send_again" to "Enviar de nuevo", "language" to "Idioma", "system_language" to "Sistema",
        "start" to "Empezar", "browser_connect" to "Conectar", "browser_download" to "Descargar",
        "browser_to_phone" to "Enviar al teléfono", "browser_drop" to "Suelta los archivos aquí",
        "browser_choose" to "o haz clic para seleccionarlos", "browser_ready" to "Listo.",
        "browser_pause" to "Pausa", "browser_resume" to "Reanudar", "browser_cancel" to "Cancelar",
        "session_local" to "SESIÓN LOCAL", "connection_ready" to "Conexión lista",
        "waiting_pc" to "Esperando al PC. Escanea el QR o copia la dirección.",
        "pc_connected" to "PC conectado y autenticado.", "transferring" to "Transferencia en curso.",
        "completed_open" to "Transferencia completada. La sesión sigue abierta.",
        "status_waiting" to "Esperando", "status_connected" to "PC conectado", "status_transferring" to "Transferencia",
        "status_completed" to "Completado", "status_expired" to "Caducada", "status_stopped" to "Finalizada", "status_error" to "Error de red",
        "pin_access" to "PIN DE ACCESO", "copy_address" to "Copiar dirección", "open_last_received" to "Abrir último archivo recibido",
        "end_transfer_title" to "¿Interrumpir la transferencia?", "end" to "Finalizar", "continue" to "Continuar",
        "recent_transfers" to "Transferencias recientes", "clear" to "Borrar", "clear_history_title" to "¿Borrar historial?",
        "trusted_devices" to "PC de confianza", "revoke_all" to "Revocar todos",
        "onboarding_intro" to "Transfiere archivos, fotos, texto y enlaces directamente entre teléfono y PC.",
        "onboarding_step1_title" to "Misma red", "onboarding_step2_title" to "Crear sesión", "onboarding_step3_title" to "Abrir desde el PC",
        "version" to "Versión %s", "selected_count" to "%d elementos seleccionados", "more_files" to "+ %d archivos más",
        "history_note" to "Solo se guarda el historial, no una copia de los archivos.", "sent" to "Enviado", "received" to "Recibido",
        "progress_send" to "Enviando al PC", "progress_receive" to "Recibiendo del PC",
        "notification_active" to "Sesión de transferencia local activa", "notification_end" to "Finalizar"
    )

    private val fr = mapOf(
        "app_name" to "Envoyer au PC", "help" to "Aide", "connect" to "Connecter", "recents" to "Récents",
        "trusted_pcs" to "PC de confiance", "network_unavailable" to "Wi-Fi local indisponible",
        "connect_devices" to "Connectez vos appareils", "phone_content_optional" to "Contenu du téléphone (facultatif)",
        "photo" to "Photo", "text" to "Texte", "text_hint" to "Écrivez ou collez du texte...", "link" to "Lien",
        "preparing" to "Préparation...", "connect_pc" to "Connecter au PC", "close" to "Fermer",
        "copy_address" to "Copier l’adresse", "end_session" to "Terminer la session", "cancel" to "Annuler",
        "open" to "Ouvrir", "send_again" to "Renvoyer", "language" to "Langue", "system_language" to "Système",
        "start" to "Commencer", "browser_connect" to "Connecter", "browser_download" to "Télécharger",
        "browser_to_phone" to "Envoyer au téléphone", "browser_drop" to "Déposez les fichiers ici",
        "browser_choose" to "ou cliquez pour les sélectionner", "browser_ready" to "Prêt.",
        "browser_pause" to "Pause", "browser_resume" to "Reprendre", "browser_cancel" to "Annuler",
        "session_local" to "SESSION LOCALE", "connection_ready" to "Connexion prête",
        "waiting_pc" to "En attente du PC. Scannez le QR ou copiez l’adresse.",
        "pc_connected" to "PC connecté et authentifié.", "transferring" to "Transfert en cours.",
        "completed_open" to "Transfert terminé. La session reste ouverte.",
        "status_waiting" to "En attente", "status_connected" to "PC connecté", "status_transferring" to "Transfert",
        "status_completed" to "Terminé", "status_expired" to "Expirée", "status_stopped" to "Terminée", "status_error" to "Erreur réseau",
        "pin_access" to "PIN D’ACCÈS", "copy_address" to "Copier l’adresse", "open_last_received" to "Ouvrir le dernier fichier reçu",
        "end_transfer_title" to "Interrompre le transfert ?", "end" to "Terminer", "continue" to "Continuer",
        "recent_transfers" to "Transferts récents", "clear" to "Effacer", "clear_history_title" to "Effacer l’historique ?",
        "trusted_devices" to "PC de confiance", "revoke_all" to "Tout révoquer",
        "onboarding_intro" to "Transférez fichiers, photos, texte et liens directement entre téléphone et PC.",
        "version" to "Version %s", "selected_count" to "%d éléments sélectionnés", "more_files" to "+ %d fichiers",
        "history_note" to "Seul l’historique est enregistré, pas une copie des fichiers.", "sent" to "Envoyé", "received" to "Reçu",
        "progress_send" to "Envoi vers le PC", "progress_receive" to "Réception depuis le PC",
        "notification_active" to "Session de transfert local active", "notification_end" to "Terminer"
    )

    private val de = mapOf(
        "app_name" to "An PC senden", "help" to "Hilfe", "connect" to "Verbinden", "recents" to "Letzte",
        "trusted_pcs" to "Vertrauenswürdige PCs", "network_unavailable" to "Lokales WLAN nicht verfügbar",
        "connect_devices" to "Geräte verbinden", "phone_content_optional" to "Inhalte vom Smartphone (optional)",
        "photo" to "Foto", "text" to "Text", "text_hint" to "Text schreiben oder einfügen...", "link" to "Link",
        "preparing" to "Vorbereitung...", "connect_pc" to "Mit PC verbinden", "close" to "Schließen",
        "copy_address" to "Adresse kopieren", "end_session" to "Sitzung beenden", "cancel" to "Abbrechen",
        "open" to "Öffnen", "send_again" to "Erneut senden", "language" to "Sprache", "system_language" to "System",
        "start" to "Start", "browser_connect" to "Verbinden", "browser_download" to "Herunterladen",
        "browser_to_phone" to "An Smartphone senden", "browser_drop" to "Dateien hier ablegen",
        "browser_choose" to "oder klicken, um sie auszuwählen", "browser_ready" to "Bereit.",
        "browser_pause" to "Pause", "browser_resume" to "Fortsetzen", "browser_cancel" to "Abbrechen",
        "session_local" to "LOKALE SITZUNG", "connection_ready" to "Verbindung bereit",
        "waiting_pc" to "Warten auf den PC. QR-Code scannen oder Adresse kopieren.",
        "pc_connected" to "PC verbunden und authentifiziert.", "transferring" to "Übertragung läuft.",
        "completed_open" to "Übertragung abgeschlossen. Die Sitzung bleibt offen.",
        "status_waiting" to "Warten", "status_connected" to "PC verbunden", "status_transferring" to "Übertragung",
        "status_completed" to "Abgeschlossen", "status_expired" to "Abgelaufen", "status_stopped" to "Beendet", "status_error" to "Netzwerkfehler",
        "pin_access" to "ZUGANGS-PIN", "copy_address" to "Adresse kopieren", "open_last_received" to "Letzte empfangene Datei öffnen",
        "end_transfer_title" to "Übertragung stoppen?", "end" to "Beenden", "continue" to "Fortsetzen",
        "recent_transfers" to "Letzte Übertragungen", "clear" to "Löschen", "clear_history_title" to "Verlauf löschen?",
        "trusted_devices" to "Vertrauenswürdige PCs", "revoke_all" to "Alle widerrufen",
        "onboarding_intro" to "Dateien, Fotos, Text und Links direkt zwischen Smartphone und PC übertragen.",
        "version" to "Version %s", "selected_count" to "%d Elemente ausgewählt", "more_files" to "+ %d weitere Dateien",
        "history_note" to "Gespeichert wird nur der Verlauf, keine Kopie der Dateien.", "sent" to "Gesendet", "received" to "Empfangen",
        "progress_send" to "Senden an PC", "progress_receive" to "Empfangen vom PC",
        "notification_active" to "Lokale Übertragungssitzung aktiv", "notification_end" to "Beenden"
    )

    private val pt = mapOf(
        "app_name" to "Enviar para o PC", "help" to "Ajuda", "connect" to "Conectar", "recents" to "Recentes",
        "trusted_pcs" to "PCs confiáveis", "network_unavailable" to "Wi-Fi local indisponível",
        "connect_devices" to "Conecte os dispositivos", "phone_content_optional" to "Conteúdo do telefone (opcional)",
        "photo" to "Foto", "text" to "Texto", "text_hint" to "Escreva ou cole texto...", "link" to "Link",
        "preparing" to "Preparando...", "connect_pc" to "Conectar ao PC", "close" to "Fechar",
        "copy_address" to "Copiar endereço", "end_session" to "Encerrar sessão", "cancel" to "Cancelar",
        "open" to "Abrir", "send_again" to "Enviar novamente", "language" to "Idioma", "system_language" to "Sistema",
        "start" to "Começar", "browser_connect" to "Conectar", "browser_download" to "Baixar",
        "browser_to_phone" to "Enviar para o telefone", "browser_drop" to "Solte os arquivos aqui",
        "browser_choose" to "ou clique para selecioná-los", "browser_ready" to "Pronto.",
        "browser_pause" to "Pausar", "browser_resume" to "Retomar", "browser_cancel" to "Cancelar",
        "session_local" to "SESSÃO LOCAL", "connection_ready" to "Conexão pronta",
        "waiting_pc" to "Aguardando o PC. Escaneie o QR ou copie o endereço.",
        "pc_connected" to "PC conectado e autenticado.", "transferring" to "Transferência em andamento.",
        "completed_open" to "Transferência concluída. A sessão continua aberta.",
        "status_waiting" to "Aguardando", "status_connected" to "PC conectado", "status_transferring" to "Transferência",
        "status_completed" to "Concluído", "status_expired" to "Expirada", "status_stopped" to "Encerrada", "status_error" to "Erro de rede",
        "pin_access" to "PIN DE ACESSO", "copy_address" to "Copiar endereço", "open_last_received" to "Abrir último arquivo recebido",
        "end_transfer_title" to "Interromper a transferência?", "end" to "Encerrar", "continue" to "Continuar",
        "recent_transfers" to "Transferências recentes", "clear" to "Limpar", "clear_history_title" to "Limpar histórico?",
        "trusted_devices" to "PCs confiáveis", "revoke_all" to "Revogar todos",
        "onboarding_intro" to "Transfira arquivos, fotos, texto e links diretamente entre telefone e PC.",
        "version" to "Versão %s", "selected_count" to "%d itens selecionados", "more_files" to "+ %d arquivos",
        "history_note" to "Só o histórico é salvo, não uma cópia dos arquivos.", "sent" to "Enviado", "received" to "Recebido",
        "progress_send" to "Enviando ao PC", "progress_receive" to "Recebendo do PC",
        "notification_active" to "Sessão de transferência local ativa", "notification_end" to "Encerrar"
    )

    private fun mapFor(
        language: AppLanguage
    ): Map<String, String> =
        when (language) {
            AppLanguage.EN -> en
            AppLanguage.IT -> it
            AppLanguage.ES -> es
            AppLanguage.FR -> fr
            AppLanguage.DE -> de
            AppLanguage.PT -> pt
        }

    fun get(
        language: AppLanguage,
        key: String,
        vararg args: Any
    ): String {
        val template =
            mapFor(language)[key]
                ?: en[key]
                ?: key

        return if (args.isEmpty()) {
            template
        } else {
            String.format(
                Locale.forLanguageTag(
                    language.code
                ),
                template,
                *args
            )
        }
    }
}
