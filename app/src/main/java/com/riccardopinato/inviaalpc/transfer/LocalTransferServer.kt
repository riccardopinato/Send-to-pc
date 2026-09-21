package com.riccardopinato.inviaalpc.transfer

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class LocalTransferServer(
    private val context: Context,
    private val sessionManager: TransferSessionManager
) {

    companion object {
        private const val MAX_HEADER_BYTES = 64 * 1024
        private const val BUFFER_SIZE = 64 * 1024
        private const val SOCKET_TIMEOUT_MS = 30_000
        private const val PREFERRED_PORT = 8734
    }

    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val receivedFileStore = ReceivedFileStore(context)
    private val trustedDeviceStore = TransferRuntime.trustedDeviceStore
    private val authenticatedSessions = ConcurrentHashMap<String, Long>()

    fun start(port: Int = 0): Int {
        if (running.get()) {
            return serverSocket?.localPort
                ?: error("Server già avviato")
        }

        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val socket =
            if (port != 0) {
                ServerSocket(port)
            } else {
                try {
                    ServerSocket(PREFERRED_PORT)
                } catch (_: BindException) {
                    ServerSocket(0)
                }
            }

        socket.reuseAddress = true

        serverSocket = socket
        running.set(true)

        scope.launch {
            acceptLoop(socket)
        }

        return socket.localPort
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return

        runCatching { serverSocket?.close() }
        serverSocket = null
        authenticatedSessions.clear()
        scope.cancel()
    }

    private suspend fun acceptLoop(server: ServerSocket) {
        while (running.get()) {
            try {
                val client = server.accept()
                client.soTimeout = SOCKET_TIMEOUT_MS
                client.tcpNoDelay = true

                scope.launch {
                    handleClient(client)
                }
            } catch (_: Exception) {
                if (!running.get()) return
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            val input = BufferedInputStream(client.getInputStream())
            val output = BufferedOutputStream(client.getOutputStream())

            try {
                val request = readRequest(input) ?: return
                val session = sessionManager.session.value

                if (session == null) {
                    sendHtml(
                        output,
                        503,
                        "Sessione non disponibile",
                        errorPage("Sessione non disponibile", "Avvia una nuova sessione dal telefono.")
                    )
                    return
                }

                if (session.isExpired()) {
                    sessionManager.expire()
                    sendHtml(
                        output,
                        410,
                        "Sessione scaduta",
                        errorPage("Sessione scaduta", "Crea un nuovo QR dal telefono.")
                    )
                    return
                }

                route(
                    request = request,
                    input = input,
                    output = output,
                    clientAddress = client.inetAddress.hostAddress ?: "unknown",
                    session = session
                )

                output.flush()
            } catch (_: Exception) {
                runCatching {
                    sendJson(output, 400, """{"error":"bad_request"}""")
                    output.flush()
                }
            }
        }
    }

    private fun route(
        request: HttpRequest,
        input: InputStream,
        output: OutputStream,
        clientAddress: String,
        session: TransferSession
    ) {
        val path = request.path

        when {
            request.method == "GET" && path == "/s/" + session.token -> {
                if (isAuthenticated(request)) {
                    sessionManager.updateStatus(TransferStatus.CONNECTED)
                    sendHtml(output, 200, "Invia al PC", dashboardPage(session))
                } else {
                    sendHtml(output, 200, "PIN", pinPage(session.token))
                }
            }

            request.method == "POST" && path == "/auth/" + session.token -> {
                authenticate(request, input, output, clientAddress, session)
            }

            request.method == "POST" && path == "/auth/trusted/" + session.token -> {
                authenticateTrusted(request, input, output, session)
            }

            request.method == "POST" && path == "/trust/" + session.token -> {
                if (!isAuthenticated(request)) {
                    sendJson(output, 401, """{"error":"authentication_required"}""")
                    return
                }

                trustCurrentDevice(request, input, output)
            }

            request.method == "GET" &&
                path.startsWith("/download/" + session.token + "/") -> {

                if (!isAuthenticated(request)) {
                    sendJson(output, 401, """{"error":"authentication_required"}""")
                    return
                }

                val itemId = path.removePrefix("/download/" + session.token + "/")
                    .substringBefore('/')

                downloadItem(output, session, itemId)
            }

            request.method == "POST" && path == "/upload/" + session.token -> {
                if (!isAuthenticated(request)) {
                    sendJson(output, 401, """{"error":"authentication_required"}""")
                    return
                }

                uploadFile(request, input, output)
            }

            else -> {
                sendHtml(
                    output,
                    404,
                    "Non trovato",
                    errorPage("404", "La risorsa richiesta non esiste.")
                )
            }
        }
    }

    private fun authenticate(
        request: HttpRequest,
        input: InputStream,
        output: OutputStream,
        clientAddress: String,
        session: TransferSession
    ) {
        val length = request.contentLength.coerceAtMost(4096L).toInt()
        val body = readExact(input, length).toString(StandardCharsets.UTF_8)
        val suppliedPin = parseUrlEncoded(body)["pin"].orEmpty()

        val valid = SessionSecurity.validatePin(
            clientKey = clientAddress,
            expectedPin = session.pin,
            suppliedPin = suppliedPin
        )

        if (!valid) {
            sendHtml(
                output,
                401,
                "PIN errato",
                pinPage(session.token, "PIN errato. Riprova.")
            )
            return
        }

        val sid = SessionSecurity.generateToken()
        authenticatedSessions[sid] = System.currentTimeMillis() + TransferLimits.SESSION_DURATION_MS
        sessionManager.updateStatus(TransferStatus.CONNECTED)

        writeStatus(output, 303, "See Other")
        writeHeader(output, "Location", "/s/" + session.token)
        writeHeader(
            output,
            "Set-Cookie",
            "sid=" + sid + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=600"
        )
        securityHeaders(output)
        writeHeader(output, "Content-Length", "0")
        writeHeader(output, "Connection", "close")
        endHeaders(output)
    }

    private fun authenticateTrusted(
        request: HttpRequest,
        input: InputStream,
        output: OutputStream,
        session: TransferSession
    ) {
        val length = request.contentLength.coerceAtMost(4096L).toInt()
        val body = readExact(input, length).toString(StandardCharsets.UTF_8)
        val rawToken = parseUrlEncoded(body)["token"].orEmpty()

        val trusted =
            trustedDeviceStore.validate(rawToken)

        if (trusted == null) {
            sendJson(output, 401, """{"error":"trusted_device_not_found"}""")
            return
        }

        val sid = SessionSecurity.generateToken()
        authenticatedSessions[sid] =
            System.currentTimeMillis() +
                TransferLimits.SESSION_DURATION_MS

        sessionManager.updateStatus(
            TransferStatus.CONNECTED
        )

        writeStatus(output, 204, "No Content")
        writeHeader(
            output,
            "Set-Cookie",
            "sid=" + sid +
                "; Path=/; HttpOnly; SameSite=Strict; Max-Age=600"
        )
        securityHeaders(output)
        writeHeader(output, "Content-Length", "0")
        writeHeader(output, "Connection", "close")
        endHeaders(output)
    }

    private fun trustCurrentDevice(
        request: HttpRequest,
        input: InputStream,
        output: OutputStream
    ) {
        val length = request.contentLength.coerceAtMost(4096L).toInt()
        val body = readExact(input, length).toString(StandardCharsets.UTF_8)
        val name =
            parseUrlEncoded(body)["name"]
                ?.take(60)
                ?: "PC fidato"

        val pair =
            trustedDeviceStore.create(name)

        sendJson(
            output,
            200,
            """{"ok":true,"id":"""" +
                jsonEscape(pair.first.id) +
                """","name":"""" +
                jsonEscape(pair.first.name) +
                """","token":"""" +
                jsonEscape(pair.second) +
                """"}"""
        )
    }

    private fun isAuthenticated(request: HttpRequest): Boolean {
        val cookie = request.headers["cookie"] ?: return false
        val sid = cookie.split(';')
            .map { it.trim() }
            .firstOrNull { it.startsWith("sid=") }
            ?.substringAfter("sid=")
            ?.takeIf { it.isNotBlank() }
            ?: return false

        val expiresAt = authenticatedSessions[sid] ?: return false

        if (System.currentTimeMillis() >= expiresAt) {
            authenticatedSessions.remove(sid)
            return false
        }

        return true
    }

    private fun downloadItem(
        output: OutputStream,
        session: TransferSession,
        itemId: String
    ) {
        val item = session.sharedItems.firstOrNull { it.id == itemId }

        if (item == null) {
            sendJson(output, 404, """{"error":"file_not_found"}""")
            return
        }

        val resolver = context.contentResolver
        val stream = resolver.openInputStream(item.uri)

        if (stream == null) {
            sendJson(output, 404, """{"error":"cannot_open_file"}""")
            return
        }

        val mime = item.mimeType
            ?: resolver.getType(item.uri)
            ?: "application/octet-stream"

        val safeName = FileNameUtils.sanitize(item.displayName)
        val encodedName = URLEncoder.encode(
            safeName,
            StandardCharsets.UTF_8.name()
        ).replace("+", "%20")

        writeStatus(output, 200, "OK")
        writeHeader(output, "Content-Type", mime)
        writeHeader(
            output,
            "Content-Disposition",
            "attachment; filename*=UTF-8''" + encodedName
        )
        item.sizeBytes?.let {
            writeHeader(output, "Content-Length", it.toString())
        }
        securityHeaders(output)
        writeHeader(output, "Connection", "close")
        endHeaders(output)

        stream.use { source ->
            val buffer = ByteArray(BUFFER_SIZE)
            var transferred = 0L
            var sampleBytes = 0L
            var sampleAt = System.currentTimeMillis()

            while (true) {
                val read = source.read(buffer)
                if (read <= 0) break

                output.write(buffer, 0, read)
                transferred += read
                sampleBytes += read

                val now = System.currentTimeMillis()
                val elapsed = now - sampleAt

                if (elapsed >= 500L) {
                    val speed = if (elapsed > 0L) sampleBytes * 1000L / elapsed else 0L

                    sessionManager.updateProgress(
                        TransferProgress(
                            itemId = item.id,
                            fileName = item.displayName,
                            transferredBytes = transferred,
                            totalBytes = item.sizeBytes,
                            bytesPerSecond = speed,
                            direction = TransferDirection.PHONE_TO_PC
                        )
                    )

                    sampleBytes = 0L
                    sampleAt = now
                }
            }

            output.flush()

            sessionManager.completeTransfer(
                fileName = item.displayName,
                sizeBytes = transferred,
                direction = TransferDirection.PHONE_TO_PC,
                contentUri = item.uri.toString(),
                mimeType = mime
            )
        }
    }

    private fun uploadFile(
        request: HttpRequest,
        input: InputStream,
        output: OutputStream
    ) {
        val contentLength = request.contentLength

        if (contentLength <= 0L) {
            sendJson(output, 400, """{"error":"empty_upload"}""")
            return
        }

        if (contentLength > TransferLimits.MAX_SINGLE_UPLOAD_BYTES) {
            sendJson(output, 413, """{"error":"file_too_large"}""")
            return
        }

        if (!StorageUtils.canReceive(context, contentLength)) {
            sendJson(output, 507, """{"error":"insufficient_storage"}""")
            return
        }

        val encodedName = request.headers["x-file-name"]

        if (
            encodedName.isNullOrBlank() ||
            FileNameUtils.containsHeaderInjection(encodedName)
        ) {
            sendJson(output, 400, """{"error":"invalid_file_name"}""")
            return
        }

        val decodedName = runCatching {
            URLDecoder.decode(encodedName, StandardCharsets.UTF_8.name())
        }.getOrNull()

        if (
            decodedName.isNullOrBlank() ||
            FileNameUtils.containsHeaderInjection(decodedName)
        ) {
            sendJson(output, 400, """{"error":"invalid_file_name"}""")
            return
        }

        val mimeType = request.headers["x-file-type"]
            ?.takeIf { it.length <= 150 }

        val destination = try {
            receivedFileStore.create(decodedName, mimeType)
        } catch (_: Throwable) {
            sendJson(output, 500, """{"error":"cannot_create_destination"}""")
            return
        }

        var remaining = contentLength
        var transferred = 0L
        val startedAt = System.currentTimeMillis()

        try {
            destination.output.buffered(BUFFER_SIZE).use { target ->
                val buffer = ByteArray(BUFFER_SIZE)

                while (remaining > 0L) {
                    val requested = min(remaining, buffer.size.toLong()).toInt()
                    val read = input.read(buffer, 0, requested)

                    if (read <= 0) error("Upload interrotto")

                    target.write(buffer, 0, read)
                    transferred += read
                    remaining -= read

                    val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)

                    sessionManager.updateProgress(
                        TransferProgress(
                            itemId = destination.uri.toString(),
                            fileName = destination.displayName,
                            transferredBytes = transferred,
                            totalBytes = contentLength,
                            bytesPerSecond = transferred * 1000L / elapsed,
                            direction = TransferDirection.PC_TO_PHONE
                        )
                    )
                }

                target.flush()
            }

            receivedFileStore.complete(destination)

            sessionManager.completeTransfer(
                fileName = destination.displayName,
                sizeBytes = transferred,
                direction = TransferDirection.PC_TO_PHONE,
                contentUri = destination.uri.toString(),
                mimeType = mimeType
            )

            sendJson(
                output,
                200,
                """{"ok":true,"name":"""" +
                    jsonEscape(destination.displayName) +
                    """","size":""" +
                    transferred +
                    "}"
            )
        } catch (_: Throwable) {
            receivedFileStore.abort(destination)
            sendJson(output, 500, """{"error":"upload_failed"}""")
        }
    }

    private fun readRequest(input: InputStream): HttpRequest? {
        val headerBytes = ByteArrayOutputStream()
        var state = 0

        while (headerBytes.size() < MAX_HEADER_BYTES) {
            val value = input.read()
            if (value == -1) return null

            headerBytes.write(value)

            state = when {
                state == 0 && value == '\r'.code -> 1
                state == 1 && value == '\n'.code -> 2
                state == 2 && value == '\r'.code -> 3
                state == 3 && value == '\n'.code -> 4
                value == '\r'.code -> 1
                else -> 0
            }

            if (state == 4) break
        }

        if (state != 4) error("Header HTTP incompleto")

        val text = headerBytes.toString(StandardCharsets.ISO_8859_1.name())
        val lines = text.split("\r\n")
        val first = lines.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            ?: error("Request line mancante")

        val parts = first.split(' ')
        if (parts.size != 3) error("Request line non valida")

        val method = parts[0].uppercase(Locale.ROOT)
        if (method != "GET" && method != "POST") error("Metodo non supportato")

        val target = parts[1]
        if (!target.startsWith("/") || target.contains('\\') || target.contains('\u0000')) {
            error("Target non valido")
        }

        val protocol = parts[2]
        if (protocol != "HTTP/1.1" && protocol != "HTTP/1.0") {
            error("Protocollo non supportato")
        }

        val headers = LinkedHashMap<String, String>()

        lines.drop(1).forEach { line ->
            if (line.isBlank()) return@forEach

            val separator = line.indexOf(':')
            if (separator <= 0) error("Header non valido")

            val name = line.substring(0, separator)
                .trim()
                .lowercase(Locale.ROOT)

            val value = line.substring(separator + 1).trim()

            if (name.isBlank() || value.contains('\r') || value.contains('\n')) {
                error("Header non valido")
            }

            headers[name] = value
        }

        val contentLength = headers["content-length"]?.toLongOrNull() ?: 0L
        if (contentLength < 0L) error("Content-Length non valido")

        return HttpRequest(
            method = method,
            path = target.substringBefore('?'),
            headers = headers,
            contentLength = contentLength
        )
    }

    private fun parseUrlEncoded(body: String): Map<String, String> {
        if (body.isBlank()) return emptyMap()

        return body.split('&')
            .mapNotNull { pair ->
                val parts = pair.split('=', limit = 2)
                val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name())
                val value = URLDecoder.decode(
                    parts.getOrElse(1) { "" },
                    StandardCharsets.UTF_8.name()
                )
                key to value
            }
            .toMap()
    }

    private fun readExact(input: InputStream, length: Int): ByteArray {
        val result = ByteArray(length)
        var offset = 0

        while (offset < length) {
            val read = input.read(result, offset, length - offset)
            if (read <= 0) break
            offset += read
        }

        return if (offset == length) result else result.copyOf(offset)
    }

    private fun pinPage(token: String, error: String? = null): String {
        val errorHtml = error
            ?.let { "<div class=\"error\">" + htmlEscape(it) + "</div>" }
            .orEmpty()

        return """<!doctype html>
<html lang="it">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Invia al PC</title>
<style>
*{box-sizing:border-box}body{margin:0;font-family:system-ui,-apple-system,sans-serif;background:#f5f6fa;color:#17191e;min-height:100vh;display:grid;place-items:center;padding:24px}
.card{width:min(420px,100%);background:white;padding:32px;border-radius:28px;box-shadow:0 18px 60px rgba(0,0,0,.10)}
h1{margin:0 0 8px}.muted{color:#6d727c}.pin{width:100%;font-size:30px;letter-spacing:12px;text-align:center;padding:16px;border:2px solid #dfe3ea;border-radius:18px;margin:18px 0;outline:none}.pin:focus{border-color:#5267ff}
button{width:100%;border:0;border-radius:17px;padding:15px;background:#5267ff;color:white;font-size:16px;font-weight:800;cursor:pointer}.error{background:#fff0ef;color:#b3261e;padding:12px;border-radius:14px;margin-top:12px}
</style>
</head>
<body>
<div class="card">
<h1>Invia al PC</h1>
<p class="muted" id="hint">Inserisci il PIN mostrato sul telefono.</p>
""" + errorHtml + """
<form id="pinForm" method="post" action="/auth/""" + htmlEscape(token) + """">
<input class="pin" name="pin" type="password" inputmode="numeric" pattern="[0-9]{4}" maxlength="4" autocomplete="off" autofocus required>
<button type="submit">Connetti</button>
</form>
<script>
(async function(){
  try{
    const trusted=localStorage.getItem('inviaalpc.trustedToken');
    if(!trusted)return;
    document.getElementById('hint').textContent='PC riconosciuto. Connessione rapida...';
    document.getElementById('pinForm').style.opacity='.45';
    const body='token='+encodeURIComponent(trusted);
    const r=await fetch('/auth/trusted/""" + htmlEscape(token) + """',{
      method:'POST',
      headers:{'Content-Type':'application/x-www-form-urlencoded'},
      body
    });
    if(r.ok){location.reload();return;}
    localStorage.removeItem('inviaalpc.trustedToken');
    localStorage.removeItem('inviaalpc.trustedName');
    document.getElementById('hint').textContent='Inserisci il PIN mostrato sul telefono.';
    document.getElementById('pinForm').style.opacity='1';
  }catch(e){}
})();
</script>
</div>
</body>
</html>"""
    }

    private fun dashboardPage(session: TransferSession): String {
        val filesHtml = if (session.sharedItems.isEmpty()) {
            "<div class=\"empty\">Nessun file condiviso dal telefono.</div>"
        } else {
            session.sharedItems.joinToString("\n") { item ->
                val size = item.sizeBytes?.let { humanBytes(it) } ?: "Dimensione sconosciuta"

                """<div class="file">
<div class="fileInfo"><div class="fileName">""" +
                    htmlEscape(item.displayName) +
                    """</div><div class="fileSize">""" +
                    htmlEscape(size) +
                    """</div></div>
<a class="download" href="/download/""" +
                    session.token +
                    "/" +
                    item.id +
                    """">Scarica</a>
</div>"""
            }
        }

        val textHtml = session.sharedText
            ?.takeIf { it.isNotBlank() }
            ?.let {
                """<div class="contentCard"><div class="label">TESTO</div><div id="sharedText" class="sharedText">""" +
                    htmlEscape(it) +
                    """</div><button class="secondary" onclick="copyText()">Copia testo</button></div>"""
            }
            .orEmpty()

        val linkHtml = session.sharedLink
            ?.takeIf { it.isNotBlank() }
            ?.let {
                val escaped = htmlEscape(it)
                """<div class="contentCard"><div class="label">LINK</div><a class="sharedLink" target="_blank" rel="noopener noreferrer" href="""" +
                    escaped +
                    """">""" +
                    escaped +
                    """</a></div>"""
            }
            .orEmpty()

        return """<!doctype html>
<html lang="it">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Invia al PC</title>
<style>
*{box-sizing:border-box}body{margin:0;font-family:system-ui,-apple-system,sans-serif;background:#f4f6fb;color:#16181d}.page{width:min(940px,calc(100% - 28px));margin:36px auto 60px}.header{display:flex;justify-content:space-between;align-items:center;gap:16px;margin-bottom:24px}.brand{font-size:30px;font-weight:850}.local{padding:9px 14px;border-radius:999px;background:#e8f7ed;color:#176f39;font-size:13px;font-weight:750}.card{background:white;border-radius:28px;padding:26px;margin-bottom:18px;box-shadow:0 16px 55px rgba(20,30,60,.075)}h2{margin:0 0 8px}.muted{color:#707681}.file{display:flex;align-items:center;gap:14px;border-bottom:1px solid #eceef3;padding:14px 0}.file:last-child{border-bottom:0}.fileInfo{min-width:0;flex:1}.fileName{font-weight:700;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.fileSize{font-size:13px;color:#777d88;margin-top:3px}.download,.secondary{border:0;text-decoration:none;cursor:pointer;font-weight:750;border-radius:15px;padding:11px 16px}.download{background:#5267ff;color:white}.secondary{background:#eef0f6;color:#25282e;margin-top:14px}.drop{border:2px dashed #b8becc;border-radius:22px;padding:42px 20px;text-align:center;cursor:pointer;transition:.15s}.drop.active{border-color:#5267ff;background:#eef0ff}.dropTitle{font-size:20px;font-weight:800}.dropSub{margin-top:6px;color:#767c87}input[type=file]{display:none}.progress{display:none;height:10px;border-radius:999px;overflow:hidden;margin-top:18px;background:#e7e9ef}.bar{height:100%;width:0;background:#5267ff}.status{margin-top:10px;color:#686e78;font-size:14px}.contentCard{margin-top:18px;background:#f7f8fb;border-radius:20px;padding:18px}.label{font-size:11px;font-weight:850;letter-spacing:.1em;color:#767d89}.sharedText{margin-top:10px;white-space:pre-wrap;line-height:1.5}.sharedLink{display:block;margin-top:10px;overflow-wrap:anywhere;color:#4054e7;font-weight:700}.empty{color:#777d87;padding:12px 0}.footer{text-align:center;color:#8b9099;font-size:13px;margin-top:28px}
@media(max-width:600px){.header{align-items:flex-start;flex-direction:column}.file{align-items:flex-start}.download{padding:9px 11px}}
</style>
</head>
<body>
<div class="page">
<div class="header"><div class="brand">📱 Invia al PC</div><div class="local">● Connessione locale</div></div>
<section class="card"><h2>Dal telefono</h2><p class="muted">Contenuti disponibili in questa sessione.</p>
""" + filesHtml + textHtml + linkHtml + """
</section>
<section class="card"><h2>Invia al telefono</h2><p class="muted">I file verranno salvati in Download/Invia al PC.</p>
<label class="drop" id="dropZone" for="fileInput"><div class="dropTitle">Trascina qui i file</div><div class="dropSub">oppure clicca per selezionarli</div></label>
<input id="fileInput" type="file" multiple>
<div class="progress" id="progress"><div class="bar" id="bar"></div></div>
<div class="status" id="status">Pronto.</div>
</section>
<section class="card" id="trustCard">
<h2>Connessione rapida</h2>
<p class="muted">Puoi ricordare questo PC. Dalla prossima sessione, sulla stessa rete e porta, proverà a collegarsi senza richiedere il PIN.</p>
<button class="secondary" id="trustButton" onclick="trustThisPc()">Ricorda questo PC</button>
<div class="status" id="trustStatus"></div>
</section>
<div class="footer">Trasferimento diretto sulla rete locale. Nessun file viene caricato online.</div>
</div>
<script>
const input=document.getElementById('fileInput');
const zone=document.getElementById('dropZone');
const progress=document.getElementById('progress');
const bar=document.getElementById('bar');
const status=document.getElementById('status');
zone.addEventListener('dragover',e=>{e.preventDefault();zone.classList.add('active')});
zone.addEventListener('dragleave',()=>zone.classList.remove('active'));
zone.addEventListener('drop',e=>{e.preventDefault();zone.classList.remove('active');uploadFiles(Array.from(e.dataTransfer.files))});
input.addEventListener('change',()=>{uploadFiles(Array.from(input.files));input.value=''});
async function uploadFiles(files){if(files.length===0)return;for(let i=0;i<files.length;i++){status.textContent='File '+(i+1)+' di '+files.length;await uploadFile(files[i])}bar.style.width='100%';status.textContent='Trasferimento completato.'}
function uploadFile(file){return new Promise((resolve,reject)=>{progress.style.display='block';bar.style.width='0%';const xhr=new XMLHttpRequest();xhr.open('POST','/upload/""" +
            session.token +
            """');xhr.setRequestHeader('Content-Type','application/octet-stream');xhr.setRequestHeader('X-File-Name',encodeURIComponent(file.name));xhr.setRequestHeader('X-File-Type',file.type||'application/octet-stream');xhr.upload.onprogress=e=>{if(!e.lengthComputable)return;const p=Math.round(e.loaded/e.total*100);bar.style.width=p+'%';status.textContent=file.name+' — '+p+'%'};xhr.onload=()=>{if(xhr.status>=200&&xhr.status<300)resolve();else{status.textContent='Errore durante il trasferimento.';reject(new Error('HTTP '+xhr.status))}};xhr.onerror=()=>{status.textContent='Connessione interrotta.';reject(new Error('network'))};xhr.send(file)})}
async function copyText(){const el=document.getElementById('sharedText');if(!el)return;const value=el.innerText;try{if(navigator.clipboard&&window.isSecureContext){await navigator.clipboard.writeText(value)}else{const t=document.createElement('textarea');t.value=value;t.style.position='fixed';t.style.opacity='0';document.body.appendChild(t);t.focus();t.select();document.execCommand('copy');t.remove()}status.textContent='Testo copiato.'}catch(e){status.textContent='Seleziona il testo e copialo manualmente.'}}
async function trustThisPc(){
  const trustStatus=document.getElementById('trustStatus');
  const current=localStorage.getItem('inviaalpc.trustedName');
  const suggested=current||((navigator.userAgentData&&navigator.userAgentData.platform)||navigator.platform||'PC');
  const name=prompt('Nome di questo PC',suggested);
  if(!name)return;
  trustStatus.textContent='Salvataggio...';
  try{
    const r=await fetch('/trust/""" + session.token + """',{
      method:'POST',
      headers:{'Content-Type':'application/x-www-form-urlencoded'},
      body:'name='+encodeURIComponent(name)
    });
    if(!r.ok)throw new Error('HTTP '+r.status);
    const data=await r.json();
    localStorage.setItem('inviaalpc.trustedToken',data.token);
    localStorage.setItem('inviaalpc.trustedName',data.name);
    document.getElementById('trustButton').textContent='PC ricordato';
    trustStatus.textContent='Connessione rapida attivata per '+data.name+'.';
  }catch(e){
    trustStatus.textContent='Impossibile ricordare questo PC.';
  }
}
(function(){
  const name=localStorage.getItem('inviaalpc.trustedName');
  if(name){
    document.getElementById('trustButton').textContent='PC ricordato';
    document.getElementById('trustStatus').textContent='Dispositivo: '+name;
  }
})();
</script>
</body>
</html>"""
    }

    private fun errorPage(title: String, message: String): String =
        """<!doctype html><html lang="it"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>""" +
            htmlEscape(title) +
            """</title></head><body style="font-family:system-ui;background:#f4f6fb;padding:30px"><div style="max-width:500px;margin:auto;background:white;padding:28px;border-radius:24px"><h1>""" +
            htmlEscape(title) +
            """</h1><p>""" +
            htmlEscape(message) +
            """</p></div></body></html>"""

    private fun sendHtml(
        output: OutputStream,
        code: Int,
        reason: String,
        html: String
    ) {
        val data = html.toByteArray(StandardCharsets.UTF_8)

        writeStatus(output, code, reason)
        writeHeader(output, "Content-Type", "text/html; charset=utf-8")
        writeHeader(output, "Content-Length", data.size.toString())
        securityHeaders(output)
        writeHeader(output, "Connection", "close")
        endHeaders(output)
        output.write(data)
    }

    private fun sendJson(output: OutputStream, code: Int, json: String) {
        val data = json.toByteArray(StandardCharsets.UTF_8)

        writeStatus(output, code, statusReason(code))
        writeHeader(output, "Content-Type", "application/json; charset=utf-8")
        writeHeader(output, "Content-Length", data.size.toString())
        securityHeaders(output)
        writeHeader(output, "Connection", "close")
        endHeaders(output)
        output.write(data)
    }

    private fun securityHeaders(output: OutputStream) {
        writeHeader(output, "Cache-Control", "no-store, no-cache, must-revalidate")
        writeHeader(output, "Pragma", "no-cache")
        writeHeader(output, "X-Content-Type-Options", "nosniff")
        writeHeader(output, "X-Frame-Options", "DENY")
        writeHeader(output, "Referrer-Policy", "no-referrer")
        writeHeader(
            output,
            "Content-Security-Policy",
            "default-src 'self'; style-src 'unsafe-inline'; script-src 'unsafe-inline'; connect-src 'self'; img-src 'self' data:"
        )
    }

    private fun writeStatus(output: OutputStream, code: Int, reason: String) {
        output.write(
            "HTTP/1.1 $code $reason\r\n"
                .toByteArray(StandardCharsets.ISO_8859_1)
        )
    }

    private fun writeHeader(output: OutputStream, name: String, value: String) {
        output.write(
            "$name: $value\r\n"
                .toByteArray(StandardCharsets.ISO_8859_1)
        )
    }

    private fun endHeaders(output: OutputStream) {
        output.write("\r\n".toByteArray(StandardCharsets.ISO_8859_1))
    }

    private fun humanBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"

        val kb = bytes / 1024.0
        if (kb < 1024.0) return "%.1f KB".format(Locale.US, kb)

        val mb = kb / 1024.0
        if (mb < 1024.0) return "%.1f MB".format(Locale.US, mb)

        return "%.2f GB".format(Locale.US, mb / 1024.0)
    }

    private fun htmlEscape(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")

    private fun jsonEscape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    private fun statusReason(code: Int): String =
        when (code) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            410 -> "Gone"
            413 -> "Payload Too Large"
            500 -> "Internal Server Error"
            503 -> "Service Unavailable"
            507 -> "Insufficient Storage"
            else -> "Response"
        }

    private data class HttpRequest(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
        val contentLength: Long
    )
}
