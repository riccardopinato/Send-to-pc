package com.riccardopinato.inviaalpc.transfer

import android.content.Context
import com.riccardopinato.inviaalpc.AppLanguageStore
import com.riccardopinato.inviaalpc.AppText
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
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class LocalTransferServer(
    private val context: Context,
    private val sessionManager: TransferSessionManager
) {

    companion object {
        private const val MAX_HEADER_BYTES = 64 * 1024
        private const val BUFFER_SIZE = 256 * 1024
        private const val SOCKET_TIMEOUT_MS = 120_000
        private const val SOCKET_BUFFER_SIZE = 512 * 1024
        private const val PREFERRED_PORT = 8734
        private const val AUTH_SESSION_MS = 60L * 60L * 1000L
        private const val MAX_CONCURRENT_CLIENTS = 12
        private const val TRUSTED_COOKIE = "trusted_pc"
        private const val TRUSTED_COOKIE_MAX_AGE_SECONDS =
            30L * 24L * 60L * 60L
    }

    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private val tlsIdentity = LocalTlsIdentity()

    val certificateFingerprintSha256: String
        get() = tlsIdentity.certificateFingerprintSha256
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val receivedFileStore = ReceivedFileStore(context)
    private val resumableUploadStore = ResumableUploadStore(context)
    private val trustedDeviceStore = TransferRuntime.trustedDeviceStore
    private val authenticatedSessions = ConcurrentHashMap<String, Long>()
    private val clientSlots = Semaphore(MAX_CONCURRENT_CLIENTS)

    fun start(
        localIp: String,
        port: Int = 0
    ): Int {
        if (running.get()) {
            return serverSocket?.localPort
                ?: error("Server già avviato")
        }

        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun bind(
            requestedPort: Int
        ): ServerSocket =
            tlsIdentity
                .createServerSocket(
                    localIp = localIp,
                    port = requestedPort
                )

        val socket =
            if (port != 0) {
                bind(port)
            } else {
                try {
                    bind(PREFERRED_PORT)
                } catch (_: BindException) {
                    bind(0)
                }
            }

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

                if (!clientSlots.tryAcquire()) {
                    runCatching {
                        client.close()
                    }
                    continue
                }

                client.soTimeout = SOCKET_TIMEOUT_MS
                client.tcpNoDelay = true

                runCatching {
                    client.receiveBufferSize = SOCKET_BUFFER_SIZE
                    client.sendBufferSize = SOCKET_BUFFER_SIZE
                }

                scope.launch {
                    try {
                        handleClient(client)
                    } finally {
                        clientSlots.release()
                    }
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
                        localized("browser_session_missing"),
                        errorPage(
                            localized("browser_session_missing"),
                            localized("browser_session_missing_body")
                        )
                    )
                    return
                }

                if (session.isExpired()) {
                    sessionManager.expire()
                    sendHtml(
                        output,
                        410,
                        localized("browser_session_expired"),
                        errorPage(
                            localized("browser_session_expired"),
                            localized("browser_session_expired_body")
                        )
                    )
                    return
                }

                if (
                    !HttpRequestSecurityPolicy
                        .isExpectedHost(
                            hostHeader =
                                request.headers["host"],
                            localIp =
                                session.localIp,
                            port =
                                session.port
                        )
                ) {
                    sendJson(
                        output,
                        421,
                        """{"error":"misdirected_request"}"""
                    )
                    return
                }

                if (
                    !HttpRequestSecurityPolicy
                        .isAllowedFetchSite(
                            method =
                                request.method,
                            secFetchSite =
                                request.headers[
                                    "sec-fetch-site"
                                ]
                        )
                ) {
                    sendJson(
                        output,
                        403,
                        """{"error":"cross_site_request"}"""
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
            request.method == "GET" && path == "/" -> {
                if (isAuthenticated(request)) {
                    redirectToSession(
                        output = output,
                        session = session
                    )
                } else if (
                    !tryTrustedRootAccess(
                        request = request,
                        output = output,
                        session = session
                    )
                ) {
                    sendHtml(
                        output,
                        200,
                        localized("browser_quick"),
                        quickCodePage()
                    )
                }
            }

            request.method == "POST" &&
                path == "/quick-auth" -> {

                authenticateQuick(
                    request = request,
                    input = input,
                    output = output,
                    clientAddress = clientAddress,
                    session = session
                )
            }

            request.method == "GET" && path == "/s/" + session.token -> {
                if (isAuthenticated(request)) {
                    sessionManager.updateStatus(TransferStatus.CONNECTED)
                    sendHtml(
                        output,
                        200,
                        localized("app_name"),
                        dashboardPage(session)
                    )
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

                downloadItem(request, output, session, itemId)
            }

            request.method == "POST" &&
                path == "/upload/batch/" + session.token -> {

                if (!isAuthenticated(request)) {
                    sendJson(output, 401, """{"error":"authentication_required"}""")
                    return
                }

                registerUploadBatch(
                    request,
                    input,
                    output
                )
            }

            request.method == "POST" &&
                path == "/content/" + session.token -> {

                if (!isAuthenticated(request)) {
                    sendJson(output, 401, """{"error":"authentication_required"}""")
                    return
                }

                receiveContent(
                    request,
                    input,
                    output
                )
            }

            request.method == "GET" &&
                path.startsWith("/upload/status/" + session.token + "/") -> {

                if (!isAuthenticated(request)) {
                    sendJson(output, 401, """{"error":"authentication_required"}""")
                    return
                }

                val uploadId =
                    path.removePrefix(
                        "/upload/status/" + session.token + "/"
                    ).substringBefore('/')

                uploadStatus(uploadId, output)
            }

            request.method == "POST" &&
                path == "/upload/chunk/" + session.token -> {

                if (!isAuthenticated(request)) {
                    sendJson(output, 401, """{"error":"authentication_required"}""")
                    return
                }

                uploadChunk(request, input, output)
            }

            request.method == "POST" &&
                path == "/upload/cancel/" + session.token -> {

                if (!isAuthenticated(request)) {
                    sendJson(output, 401, """{"error":"authentication_required"}""")
                    return
                }

                cancelUpload(request, input, output)
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
                    localized("browser_not_found"),
                    errorPage(
                        "404",
                        localized("browser_not_found_body")
                    )
                )
            }
        }
    }

    private fun authenticateQuick(
        request: HttpRequest,
        input: InputStream,
        output: OutputStream,
        clientAddress: String,
        session: TransferSession
    ) {
        val length =
            request.contentLength
                .coerceAtMost(4096L)
                .toInt()

        val body =
            readExact(
                input,
                length
            ).toString(
                StandardCharsets.UTF_8
            )

        val suppliedCode =
            parseUrlEncoded(
                body
            )["code"].orEmpty()

        val valid =
            SessionSecurity
                .validateQuickCode(
                    clientKey = clientAddress,
                    expectedCode = session.quickCode,
                    suppliedCode = suppliedCode
                )

        if (!valid) {
            sendHtml(
                output,
                401,
                localized("browser_wrong_code"),
                quickCodePage(
                    localized("browser_wrong_code")
                )
            )
            return
        }

        val sid = SessionSecurity.generateToken()
        authenticatedSessions[sid] =
            System.currentTimeMillis() +
                AUTH_SESSION_MS

        sessionManager.updateStatus(
            TransferStatus.CONNECTED
        )

        redirectToSession(
            output = output,
            session = session,
            sessionId = sid
        )
    }

    private fun tryTrustedRootAccess(
        request: HttpRequest,
        output: OutputStream,
        session: TransferSession
    ): Boolean {
        val rawToken =
            cookieValue(
                request,
                TRUSTED_COOKIE
            ).orEmpty()

        if (rawToken.isBlank()) {
            return false
        }

        val trusted =
            trustedDeviceStore
                .validateAndRotate(
                    rawToken
                )
                ?: return false

        val sid = SessionSecurity.generateToken()

        authenticatedSessions[sid] =
            System.currentTimeMillis() +
                AUTH_SESSION_MS

        sessionManager.updateStatus(
            TransferStatus.CONNECTED
        )

        redirectToSession(
            output = output,
            session = session,
            sessionId = sid,
            trustedToken = trusted.second
        )

        return true
    }

    private fun redirectToSession(
        output: OutputStream,
        session: TransferSession,
        sessionId: String? = null,
        trustedToken: String? = null
    ) {
        writeStatus(
            output,
            303,
            "See Other"
        )
        writeHeader(
            output,
            "Location",
            "/s/" + session.token
        )

        sessionId?.let {
            writeHeader(
                output,
                "Set-Cookie",
                sessionCookie(it)
            )
        }

        trustedToken?.let {
            writeHeader(
                output,
                "Set-Cookie",
                trustedCookie(it)
            )
        }

        securityHeaders(output)
        writeHeader(output, "Content-Length", "0")
        writeHeader(output, "Connection", "close")
        endHeaders(output)
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
                localized("browser_wrong_pin"),
                pinPage(
                    session.token,
                    localized("browser_wrong_pin")
                )
            )
            return
        }

        val sid = SessionSecurity.generateToken()
        authenticatedSessions[sid] = System.currentTimeMillis() + AUTH_SESSION_MS
        sessionManager.updateStatus(TransferStatus.CONNECTED)

        writeStatus(output, 303, "See Other")
        writeHeader(output, "Location", "/s/" + session.token)
        writeHeader(
            output,
            "Set-Cookie",
            sessionCookie(sid)
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
        if (request.contentLength > 0L) {
            readExact(
                input,
                request.contentLength
                    .coerceAtMost(4096L)
                    .toInt()
            )
        }

        val rawToken =
            cookieValue(
                request,
                TRUSTED_COOKIE
            ).orEmpty()

        val trusted =
            trustedDeviceStore
                .validateAndRotate(
                    rawToken
                )

        if (trusted == null) {
            sendJson(
                output,
                401,
                """{"error":"trusted_device_not_found"}"""
            )
            return
        }

        val sid =
            SessionSecurity.generateToken()

        authenticatedSessions[sid] =
            System.currentTimeMillis() +
                AUTH_SESSION_MS

        sessionManager.updateStatus(
            TransferStatus.CONNECTED
        )

        writeStatus(
            output,
            204,
            "No Content"
        )
        writeHeader(
            output,
            "Set-Cookie",
            sessionCookie(sid)
        )
        writeHeader(
            output,
            "Set-Cookie",
            trustedCookie(
                trusted.second
            )
        )
        securityHeaders(output)
        writeHeader(
            output,
            "Content-Length",
            "0"
        )
        writeHeader(
            output,
            "Connection",
            "close"
        )
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

        val json =
            """{"ok":true,"id":"""" +
                jsonEscape(pair.first.id) +
                """","name":"""" +
                jsonEscape(pair.first.name) +
                """","expiresAt":""" +
                pair.first.expiresAtMillis +
                "}"

        sendJsonWithCookie(
            output = output,
            code = 200,
            json = json,
            cookie =
                trustedCookie(
                    pair.second
                )
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
        request: HttpRequest,
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

        val totalBytes = item.sizeBytes
        val range =
            totalBytes?.let {
                HttpRange.parse(
                    request.headers["range"],
                    it
                )
            }

        if (
            request.headers["range"] != null &&
            totalBytes != null &&
            range == null
        ) {
            writeStatus(
                output,
                416,
                "Range Not Satisfiable"
            )
            writeHeader(
                output,
                "Content-Range",
                "bytes */" + totalBytes
            )
            securityHeaders(output)
            writeHeader(
                output,
                "Content-Length",
                "0"
            )
            writeHeader(
                output,
                "Connection",
                "close"
            )
            endHeaders(output)
            stream.close()
            return
        }

        val start = range?.first ?: 0L
        val end =
            range?.last
                ?: totalBytes?.minus(1L)
        val responseLength =
            if (
                totalBytes != null &&
                end != null
            ) {
                end - start + 1L
            } else {
                null
            }

        writeStatus(
            output,
            if (range != null) 206 else 200,
            if (range != null) "Partial Content" else "OK"
        )
        writeHeader(output, "Content-Type", mime)
        writeHeader(
            output,
            "Accept-Ranges",
            "bytes"
        )
        writeHeader(
            output,
            "Content-Disposition",
            "attachment; filename*=UTF-8''" + encodedName
        )

        if (
            range != null &&
            totalBytes != null
        ) {
            writeHeader(
                output,
                "Content-Range",
                "bytes " +
                    start +
                    "-" +
                    range.last +
                    "/" +
                    totalBytes
            )
        }

        responseLength?.let {
            writeHeader(
                output,
                "Content-Length",
                it.toString()
            )
        } ?: totalBytes?.let {
            writeHeader(
                output,
                "Content-Length",
                it.toString()
            )
        }

        securityHeaders(output)
        writeHeader(output, "Connection", "close")
        endHeaders(output)

        stream.use { source ->
            skipFully(source, start)

            val buffer = ByteArray(BUFFER_SIZE)
            var transferred = start
            var remaining = responseLength
            var sampleBytes = 0L
            var sampleAt = System.currentTimeMillis()

            while (
                remaining == null ||
                remaining > 0L
            ) {
                val wanted =
                    if (remaining == null) {
                        buffer.size
                    } else {
                        min(
                            remaining,
                            buffer.size.toLong()
                        ).toInt()
                    }

                val read =
                    source.read(
                        buffer,
                        0,
                        wanted
                    )

                if (read <= 0) break

                output.write(buffer, 0, read)
                transferred += read
                sampleBytes += read
                remaining =
                    remaining?.minus(read.toLong())

                val now = System.currentTimeMillis()
                val elapsed = now - sampleAt

                if (elapsed >= 500L) {
                    val speed =
                        if (elapsed > 0L) {
                            sampleBytes * 1000L / elapsed
                        } else {
                            0L
                        }

                    sessionManager.updateProgress(
                        TransferProgress(
                            itemId = item.id,
                            fileName = item.displayName,
                            transferredBytes = transferred,
                            totalBytes = totalBytes,
                            bytesPerSecond = speed,
                            direction = TransferDirection.PHONE_TO_PC
                        )
                    )

                    sampleBytes = 0L
                    sampleAt = now
                }
            }

            output.flush()

            if (
                totalBytes == null ||
                transferred >= totalBytes
            ) {
                sessionManager.completeTransfer(
                    fileName = item.displayName,
                    sizeBytes =
                        totalBytes
                            ?: transferred,
                    direction = TransferDirection.PHONE_TO_PC,
                    contentUri = item.uri.toString(),
                    mimeType = mime
                )
            } else {
                sessionManager.updateStatus(
                    TransferStatus.CONNECTED
                )
            }
        }
    }

    private fun registerUploadBatch(
        request: HttpRequest,
        input: InputStream,
        output: OutputStream
    ) {
        val length =
            request.contentLength
                .coerceAtMost(4096L)
                .toInt()

        val body =
            readExact(
                input,
                length
            ).toString(
                StandardCharsets.UTF_8
            )

        val values =
            parseUrlEncoded(body)

        val batchId =
            values["batchId"]
                ?.trim()
                .orEmpty()

        val fileCount =
            values["fileCount"]
                ?.toIntOrNull()
                ?: 0

        val totalBytes =
            values["totalBytes"]
                ?.toLongOrNull()
                ?: -1L

        if (
            !TransferReliabilityPolicy
                .isValidBatch(
                    batchId =
                        batchId,
                    fileCount =
                        fileCount,
                    totalBytes =
                        totalBytes
                )
        ) {
            sendJson(
                output,
                400,
                """{"error":"invalid_batch"}"""
            )
            return
        }

        sessionManager
            .registerExpectedBatch(
                batchId = batchId,
                fileCount = fileCount,
                totalBytes = totalBytes
            )

        sendJson(
            output,
            200,
            """{"ok":true}"""
        )
    }

    private fun receiveContent(
        request: HttpRequest,
        input: InputStream,
        output: OutputStream
    ) {
        val maxEncodedBody =
            TransferLimits
                .MAX_SHARED_TEXT_LENGTH
                .toLong() *
                4L

        if (
            request.contentLength <= 0L ||
            request.contentLength >
                maxEncodedBody
        ) {
            sendJson(
                output,
                413,
                """{"error":"content_too_large"}"""
            )
            return
        }

        val body =
            readExact(
                input,
                request.contentLength
                    .toInt()
            ).toString(
                StandardCharsets.UTF_8
            )

        val values =
            parseUrlEncoded(body)

        val type =
            values["type"]
                ?.trim()
                ?.lowercase(
                    Locale.ROOT
                )
                .orEmpty()

        val rawValue =
            values["value"]
                ?.trim()
                .orEmpty()

        when (type) {
            "text" -> {
                if (
                    rawValue.isBlank() ||
                    rawValue.length >
                        TransferLimits
                            .MAX_SHARED_TEXT_LENGTH
                ) {
                    sendJson(
                        output,
                        400,
                        """{"error":"invalid_text"}"""
                    )
                    return
                }

                sessionManager
                    .completeTransfer(
                        fileName =
                            localized("text"),
                        sizeBytes =
                            rawValue
                                .toByteArray(
                                    StandardCharsets.UTF_8
                                ).size
                                .toLong(),
                        direction =
                            TransferDirection
                                .PC_TO_PHONE,
                        kind =
                            TransferContentKind
                                .TEXT,
                        mimeType =
                            "text/plain",
                        textContent =
                            rawValue
                    )
            }

            "link" -> {
                val normalized =
                    UrlValidator
                        .normalize(
                            rawValue
                        )

                if (normalized == null) {
                    sendJson(
                        output,
                        400,
                        """{"error":"invalid_link"}"""
                    )
                    return
                }

                sessionManager
                    .completeTransfer(
                        fileName =
                            localized("link"),
                        sizeBytes =
                            normalized
                                .toByteArray(
                                    StandardCharsets.UTF_8
                                ).size
                                .toLong(),
                        direction =
                            TransferDirection
                                .PC_TO_PHONE,
                        kind =
                            TransferContentKind
                                .LINK,
                        mimeType =
                            "text/uri-list",
                        textContent =
                            normalized
                    )
            }

            else -> {
                sendJson(
                    output,
                    400,
                    """{"error":"invalid_content_type"}"""
                )
                return
            }
        }

        sendJson(
            output,
            200,
            """{"ok":true}"""
        )
    }

    private fun uploadStatus(
        uploadId: String,
        output: OutputStream
    ) {
        val status =
            resumableUploadStore.status(
                uploadId
            )

        if (status == null) {
            sendJson(
                output,
                404,
                """{"error":"upload_not_found","received":0}"""
            )
            return
        }

        sendJson(
            output,
            200,
            uploadStatusJson(status)
        )
    }

    private fun uploadChunk(
        request: HttpRequest,
        input: InputStream,
        output: OutputStream
    ) {
        val uploadId =
            request.headers["x-upload-id"]
                ?.trim()
                .orEmpty()

        val totalBytes =
            request.headers["x-file-size"]
                ?.toLongOrNull()
                ?: 0L

        val offset =
            request.headers["x-chunk-offset"]
                ?.toLongOrNull()
                ?: -1L

        val encodedName =
            request.headers["x-file-name"]

        if (
            encodedName.isNullOrBlank() ||
            FileNameUtils.containsHeaderInjection(
                encodedName
            )
        ) {
            sendJson(
                output,
                400,
                """{"error":"invalid_file_name"}"""
            )
            return
        }

        val decodedName =
            runCatching {
                URLDecoder.decode(
                    encodedName,
                    StandardCharsets.UTF_8.name()
                )
            }.getOrNull()

        if (
            decodedName.isNullOrBlank() ||
            FileNameUtils.containsHeaderInjection(
                decodedName
            )
        ) {
            sendJson(
                output,
                400,
                """{"error":"invalid_file_name"}"""
            )
            return
        }

        if (
            !TransferReliabilityPolicy
                .isValidFileSize(
                    totalBytes
                )
        ) {
            sendJson(
                output,
                413,
                """{"error":"file_too_large"}"""
            )
            return
        }

        if (
            offset == 0L &&
            resumableUploadStore.status(
                uploadId
            ) == null &&
            !StorageUtils.canReceive(
                context,
                totalBytes
            )
        ) {
            sendJson(
                output,
                507,
                """{"error":"insufficient_storage"}"""
            )
            return
        }

        val mimeType =
            request.headers["x-file-type"]
                ?.takeIf {
                    it.length <= 150
                }

        val expectedHash =
            request.headers["x-file-sha256"]
                ?.trim()
                ?.lowercase(Locale.ROOT)

        val chunkStartedAtMillis =
            System.currentTimeMillis()

        try {
            val status =
                resumableUploadStore.writeChunk(
                    uploadId = uploadId,
                    requestedName = decodedName,
                    mimeType = mimeType,
                    totalBytes = totalBytes,
                    offset = offset,
                    input = input,
                    contentLength =
                        request.contentLength,
                    expectedSha256 = expectedHash
                )

            sessionManager.updateProgress(
                TransferProgress(
                    itemId = uploadId,
                    fileName =
                        status.displayName,
                    transferredBytes =
                        status.receivedBytes,
                    totalBytes =
                        status.totalBytes,
                    bytesPerSecond =
                        TransferReliabilityPolicy
                            .bytesPerSecond(
                                transferredBytes =
                                    (
                                        status.receivedBytes -
                                            offset
                                        ).coerceAtLeast(0L),
                                elapsedMillis =
                                    (
                                        System.currentTimeMillis() -
                                            chunkStartedAtMillis
                                        ).coerceAtLeast(1L)
                            ),
                    direction =
                        TransferDirection.PC_TO_PHONE
                )
            )

            if (status.completed) {
                sessionManager.completeTransfer(
                    fileName =
                        status.displayName,
                    sizeBytes =
                        status.totalBytes,
                    direction =
                        TransferDirection.PC_TO_PHONE,
                    contentUri =
                        status.contentUri,
                    mimeType =
                        status.mimeType,
                    completionKey =
                        "upload:" + uploadId
                )
            }

            sendJson(
                output,
                200,
                uploadStatusJson(status)
            )
        } catch (error: Throwable) {
            val message =
                error.message.orEmpty()

            when {
                message.startsWith(
                    "OFFSET_MISMATCH:"
                ) -> {
                    val received =
                        message.substringAfter(':')
                            .toLongOrNull()
                            ?: 0L

                    sendJson(
                        output,
                        409,
                        """{"error":"offset_mismatch","received":""" +
                            received +
                            "}"
                    )
                }

                message == "HASH_MISMATCH" ->
                    sendJson(
                        output,
                        409,
                        """{"error":"hash_mismatch"}"""
                    )

                else ->
                    sendJson(
                        output,
                        400,
                        """{"error":"upload_chunk_failed"}"""
                    )
            }
        }
    }

    private fun cancelUpload(
        request: HttpRequest,
        input: InputStream,
        output: OutputStream
    ) {
        val length =
            request.contentLength
                .coerceAtMost(4096L)
                .toInt()

        val body =
            readExact(
                input,
                length
            ).toString(
                StandardCharsets.UTF_8
            )

        val uploadId =
            parseUrlEncoded(
                body
            )["uploadId"].orEmpty()

        val cancelled =
            resumableUploadStore.cancel(
                uploadId
            )

        sendJson(
            output,
            200,
            """{"ok":""" +
                cancelled +
                "}"
        )
    }

    private fun uploadStatusJson(
        status: ResumableUploadStatus
    ): String =
        """{"ok":true,"id":"""" +
            jsonEscape(
                status.uploadId
            ) +
            """","name":"""" +
            jsonEscape(
                status.displayName
            ) +
            """","received":""" +
            status.receivedBytes +
            ""","total":""" +
            status.totalBytes +
            ""","completed":""" +
            status.completed +
            ""","sha256":""" +
            (
                status.sha256
                    ?.let {
                        "\"" +
                            jsonEscape(it) +
                            "\""
                    }
                    ?: "null"
            ) +
            "}"

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

        if (
            !TransferReliabilityPolicy
                .isValidFileSize(
                    contentLength
                )
        ) {
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
        var lastProgressAt = startedAt

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

                    val now = System.currentTimeMillis()
                    val shouldPublish =
                        now - lastProgressAt >= 350L ||
                            remaining == 0L

                    if (shouldPublish) {
                        val elapsed = (now - startedAt).coerceAtLeast(1L)

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

                        lastProgressAt = now
                    }
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

            if (
                name.isBlank() ||
                value.contains('\r') ||
                value.contains('\n')
            ) {
                error("Header non valido")
            }

            if (headers.containsKey(name)) {
                error("Header duplicato")
            }

            headers[name] = value
        }

        if (
            headers.containsKey(
                "transfer-encoding"
            )
        ) {
            error(
                "Transfer-Encoding non supportato"
            )
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

    private fun skipFully(
        input: InputStream,
        bytes: Long
    ) {
        var remaining = bytes

        while (remaining > 0L) {
            val skipped =
                input.skip(remaining)

            if (skipped > 0L) {
                remaining -= skipped
                continue
            }

            if (input.read() == -1) {
                break
            }

            remaining--
        }
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

    private fun quickCodePage(
        error: String? = null
    ): String {
        val language =
            AppLanguageStore.current(
                context
            )

        val errorHtml =
            error
                ?.let {
                    "<div class=\"error\">" +
                        htmlEscape(it) +
                        "</div>"
                }
                .orEmpty()

        val appName =
            htmlEscape(
                localized("app_name")
            )

        val title =
            htmlEscape(
                localized("browser_quick")
            )

        val hint =
            htmlEscape(
                localized(
                    "browser_quick_code_hint"
                )
            )

        val connect =
            htmlEscape(
                localized("browser_connect")
            )

        return """<!doctype html>
<html lang="${language.code}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>""" + appName + """</title>
<style>
:root{--primary:#4f63f2;--primary-soft:#e9ecff;--text:#17181d;--muted:#6c7180;--surface:#fff;--line:#dde1eb;--danger:#b3261e}
*{box-sizing:border-box}body{margin:0;font-family:Inter,ui-sans-serif,system-ui,-apple-system,sans-serif;background:radial-gradient(circle at top,#eef0ff 0,#f7f8fc 42%,#f7f8fc 100%);color:var(--text);min-height:100vh;display:grid;place-items:center;padding:24px}
.card{width:min(440px,100%);background:var(--surface);padding:34px;border:1px solid rgba(79,99,242,.08);border-radius:30px;box-shadow:0 24px 70px rgba(37,48,112,.12)}
.card:before{content:"";display:block;width:52px;height:52px;border-radius:17px;background:var(--primary);margin-bottom:22px;box-shadow:inset 0 0 0 13px var(--primary-soft)}
h1{margin:0 0 8px;font-size:30px;letter-spacing:-.03em}.muted{color:var(--muted);line-height:1.5}.code{width:100%;font-size:28px;font-weight:850;letter-spacing:6px;text-align:center;text-transform:uppercase;padding:17px;border:2px solid var(--line);border-radius:19px;margin:20px 0;outline:none;background:#fbfbfe;color:var(--text)}.code:focus{border-color:var(--primary);box-shadow:0 0 0 4px rgba(79,99,242,.11)}
button{width:100%;border:0;border-radius:17px;padding:15px;background:var(--primary);color:white;font-size:16px;font-weight:800;cursor:pointer;box-shadow:0 10px 24px rgba(79,99,242,.22)}.error{background:#fff0ef;color:var(--danger);padding:12px;border-radius:14px;margin-top:12px}
@media(prefers-color-scheme:dark){:root{--text:#e7e7ef;--muted:#b4b6c2;--surface:#191a21;--line:#3b3e49}.card{border-color:#2c2e39}.code{background:#20212a;color:#fff}body{background:radial-gradient(circle at top,#22274a 0,#111217 42%,#111217 100%)}}
</style>
</head>
<body>
<div class="card">
<h1>""" + title + """</h1>
<p class="muted">""" + hint + """</p>
""" + errorHtml + """
<form method="post" action="/quick-auth">
<input class="code" name="code" type="text" maxlength="9" autocomplete="one-time-code" autocapitalize="characters" spellcheck="false" autofocus required>
<button type="submit">""" + connect + """</button>
</form>
</div>
</body>
</html>"""
    }

    private fun pinPage(
        token: String,
        error: String? = null
    ): String {
        val language =
            AppLanguageStore.current(
                context
            )

        val errorHtml =
            error
                ?.let {
                    "<div class=\"error\">" +
                        htmlEscape(it) +
                        "</div>"
                }
                .orEmpty()

        val appName =
            htmlEscape(
                localized("app_name")
            )

        val hint =
            htmlEscape(
                localized(
                    "browser_pin_hint"
                )
            )

        val connect =
            htmlEscape(
                localized(
                    "browser_connect"
                )
            )

        val checkingJs =
            jsonEscape(
                localized(
                    "browser_check_trusted"
                )
            )

        val hintJs =
            jsonEscape(
                localized(
                    "browser_pin_hint"
                )
            )

        return """<!doctype html>
<html lang="${language.code}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>""" + appName + """</title>
<style>
:root{--primary:#4f63f2;--primary-soft:#e9ecff;--text:#17181d;--muted:#6c7180;--surface:#fff;--bg:#f7f8fc;--line:#dde1eb;--danger:#b3261e}
*{box-sizing:border-box}body{margin:0;font-family:Inter,ui-sans-serif,system-ui,-apple-system,sans-serif;background:radial-gradient(circle at top,#eef0ff 0,#f7f8fc 42%,#f7f8fc 100%);color:var(--text);min-height:100vh;display:grid;place-items:center;padding:24px}
.card{width:min(420px,100%);background:var(--surface);padding:34px;border:1px solid rgba(79,99,242,.08);border-radius:30px;box-shadow:0 24px 70px rgba(37,48,112,.12)}
.card:before{content:"";display:block;width:52px;height:52px;border-radius:17px;background:var(--primary);margin-bottom:22px;box-shadow:inset 0 0 0 13px var(--primary-soft)}
h1{margin:0 0 8px;font-size:30px;letter-spacing:-.03em}.muted{color:var(--muted);line-height:1.5}.pin{width:100%;font-size:32px;font-weight:850;letter-spacing:14px;text-align:center;padding:17px;border:2px solid var(--line);border-radius:19px;margin:20px 0;outline:none;background:#fbfbfe;color:var(--text);transition:.15s}.pin:focus{border-color:var(--primary);box-shadow:0 0 0 4px rgba(79,99,242,.11)}
button{width:100%;border:0;border-radius:17px;padding:15px;background:var(--primary);color:white;font-size:16px;font-weight:800;cursor:pointer;box-shadow:0 10px 24px rgba(79,99,242,.22)}button:hover{filter:brightness(.98)}.error{background:#fff0ef;color:var(--danger);padding:12px;border-radius:14px;margin-top:12px}
@media(prefers-color-scheme:dark){:root{--text:#e7e7ef;--muted:#b4b6c2;--surface:#191a21;--bg:#111217;--line:#3b3e49}.card{border-color:#2c2e39}.pin{background:#20212a;color:#fff}body{background:radial-gradient(circle at top,#22274a 0,#111217 42%,#111217 100%)}}
</style>
</head>
<body>
<div class="card">
<h1>""" + appName + """</h1>
<p class="muted" id="hint">""" + hint + """</p>
""" + errorHtml + """
<form id="pinForm" method="post" action="/auth/""" + htmlEscape(token) + """">
<input class="pin" name="pin" type="password" inputmode="numeric" pattern="[0-9]{4}" maxlength="4" autocomplete="off" autofocus required>
<button type="submit">""" + connect + """</button>
</form>
<script>
(async function(){
  try{
    document.getElementById('hint').textContent='""" + checkingJs + """';
    document.getElementById('pinForm').style.opacity='.45';

    const r=await fetch('/auth/trusted/""" + htmlEscape(token) + """',{
      method:'POST',
      credentials:'same-origin'
    });

    if(r.ok){
      location.reload();
      return;
    }

    document.getElementById('hint').textContent='""" + hintJs + """';
    document.getElementById('pinForm').style.opacity='1';
  }catch(e){
    document.getElementById('hint').textContent='""" + hintJs + """';
    document.getElementById('pinForm').style.opacity='1';
  }
})();
</script>
</div>
</body>
</html>"""
    }

    private fun dashboardPage(session: TransferSession): String {
        val language =
            AppLanguageStore.current(
                context
            )

        val filesHtml = if (session.sharedItems.isEmpty()) {
            "<div class=\"empty\">" +
                htmlEscape(
                    localized(
                        "browser_no_files"
                    )
                ) +
                "</div>"
        } else {
            session.sharedItems.joinToString("\n") { item ->
                val size =
                    item.sizeBytes
                        ?.let { humanBytes(it) }
                        ?: localized(
                            "browser_unknown_size"
                        )

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
                    """">""" +
                    htmlEscape(
                        localized(
                            "browser_download"
                        )
                    ) +
                    """</a>
</div>"""
            }
        }

        val textHtml = session.sharedText
            ?.takeIf { it.isNotBlank() }
            ?.let {
                """<div class="contentCard"><div class="label">""" +
                    htmlEscape(
                        localized("text")
                    ).uppercase(
                        Locale.forLanguageTag(
                            language.code
                        )
                    ) +
                    """</div><div id="sharedText" class="sharedText">""" +
                    htmlEscape(it) +
                    """</div><button class="secondary" onclick="copyText()">""" +
                    htmlEscape(
                        localized(
                            "browser_copy_text"
                        )
                    ) +
                    """</button></div>"""
            }
            .orEmpty()

        val linkHtml = session.sharedLink
            ?.takeIf { it.isNotBlank() }
            ?.let {
                val escaped = htmlEscape(it)
                """<div class="contentCard"><div class="label">""" +
                    htmlEscape(
                        localized("link")
                    ).uppercase(
                        Locale.forLanguageTag(
                            language.code
                        )
                    ) +
                    """</div><a class="sharedLink" target="_blank" rel="noopener noreferrer" href="""" +
                    escaped +
                    """">""" +
                    escaped +
                    """</a></div>"""
            }
            .orEmpty()

        val browserI18n =
            """const I={
queue:${jsQuoted("browser_queue")},
paused:${jsQuoted("browser_paused")},
pause:${jsQuoted("browser_pause")},
resume:${jsQuoted("browser_resume")},
cancel:${jsQuoted("browser_cancel")},
transfer:${jsQuoted("browser_transfer")},
cancelled:${jsQuoted("browser_cancelled")},
completed:${jsQuoted("browser_completed")},
reconnecting:${jsQuoted("browser_reconnecting")},
preparing:${jsQuoted("browser_preparing")},
notTransferred:${jsQuoted("browser_not_transferred")},
alreadyCompleted:${jsQuoted("browser_already_completed")},
resumedFrom:${jsQuoted("browser_resumed_from")},
integrity:${jsQuoted("browser_integrity_verified")},
hashSkipped:${jsQuoted("browser_hash_skipped")},
copied:${jsQuoted("browser_copied")},
copyManual:${jsQuoted("browser_copy_manual")},
saving:${jsQuoted("browser_saving")},
pcName:${jsQuoted("browser_pc_name")},
remembered:${jsQuoted("browser_remembered")},
trustSaved:${jsQuoted("browser_trust_saved")},
trustFailed:${jsQuoted("browser_trust_failed")},
device:${jsQuoted("browser_device")},
limit:${jsQuoted("browser_limit")},
summary:${jsQuoted("browser_summary")},
summaryOk:${jsQuoted("browser_summary_ok")},
filePosition:${jsQuoted("browser_file_position")},
contentSent:${jsQuoted("browser_content_sent")},
contentFailed:${jsQuoted("browser_content_failed")},
invalidLink:${jsQuoted("browser_invalid_link")}
};
function fmt(t,...v){let i=0;return t.replace(/%[ds]/g,()=>String(v[i++]));}
"""

        return """<!doctype html>
<html lang="${language.code}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>""" + htmlEscape(localized("app_name")) + """</title>
<style>
:root{--primary:#4f63f2;--primary-soft:#e9ecff;--secondary:#008c95;--secondary-soft:#d8f5f4;--text:#17181d;--muted:#6c7180;--surface:#fff;--surface-2:#f5f6fa;--bg:#f7f8fc;--line:#e2e5ed;--danger:#b3261e}
*{box-sizing:border-box}body{margin:0;font-family:Inter,ui-sans-serif,system-ui,-apple-system,sans-serif;background:linear-gradient(180deg,#f0f2ff 0,#f7f8fc 240px);color:var(--text)}.page{width:min(960px,calc(100% - 28px));margin:34px auto 64px}.header{display:flex;justify-content:space-between;align-items:center;gap:16px;margin-bottom:24px}.brand{display:flex;align-items:center;gap:12px;font-size:29px;font-weight:850;letter-spacing:-.035em}.brandMark{width:42px;height:42px;border-radius:14px;background:var(--primary);position:relative;box-shadow:0 10px 24px rgba(79,99,242,.22)}.brandMark:before{content:"";position:absolute;left:8px;top:10px;width:19px;height:13px;border:2px solid white;border-radius:2px}.brandMark:after{content:"";position:absolute;right:7px;bottom:8px;width:7px;height:15px;border:2px solid white;border-radius:2px}.local{padding:9px 14px;border-radius:999px;background:var(--secondary-soft);color:#176f72;font-size:13px;font-weight:780}.card{background:var(--surface);border:1px solid rgba(79,99,242,.06);border-radius:28px;padding:26px;margin-bottom:18px;box-shadow:0 14px 44px rgba(26,34,77,.07)}h2{margin:0 0 8px;font-size:22px;letter-spacing:-.02em}.muted{color:var(--muted);line-height:1.5}.file{display:flex;align-items:center;gap:14px;border-bottom:1px solid var(--line);padding:14px 0}.file:last-child{border-bottom:0}.fileInfo{min-width:0;flex:1}.fileName{font-weight:740;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.fileSize{font-size:13px;color:var(--muted);margin-top:3px}.download,.secondary{border:0;text-decoration:none;cursor:pointer;font-weight:760;border-radius:15px;padding:11px 16px}.download{background:var(--primary);color:white;box-shadow:0 8px 20px rgba(79,99,242,.18)}.secondary{background:var(--primary-soft);color:#25327e;margin-top:14px}.drop{border:2px dashed #b7bdd0;border-radius:24px;padding:44px 20px;text-align:center;cursor:pointer;transition:.15s;background:linear-gradient(180deg,#fbfbfe,#f7f8fc)}.drop:hover,.drop.active{border-color:var(--primary);background:var(--primary-soft)}.dropTitle{font-size:20px;font-weight:820}.dropSub{margin-top:6px;color:var(--muted)}input[type=file]{display:none}.progress{display:none;height:10px;border-radius:999px;overflow:hidden;margin-top:18px;background:#e7e9ef}.bar{height:100%;width:0;background:linear-gradient(90deg,var(--primary),#7282ff)}.status{margin-top:10px;color:var(--muted);font-size:14px}.queue{margin-top:16px;display:grid;gap:8px}.queueItem{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:10px;align-items:center;padding:12px 13px;border-radius:15px;background:var(--surface-2);border:1px solid var(--line)}.queueName{font-weight:700;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.queueMeta{font-size:12px;color:var(--muted)}.queueOk{color:#19733f}.queueErr{color:var(--danger)}.queueActions{display:flex;gap:6px;align-items:center}.queueActions button{border:0;border-radius:10px;padding:7px 9px;font-size:12px;font-weight:750;cursor:pointer;background:#e9ecf4;color:#25282e}.queueActions button.cancel{background:#fff0ef;color:var(--danger)}.queueHash{font-family:ui-monospace,monospace;font-size:11px;color:var(--muted);overflow-wrap:anywhere}.contentCard{margin-top:18px;background:var(--surface-2);border:1px solid var(--line);border-radius:20px;padding:18px}.label{font-size:11px;font-weight:850;letter-spacing:.1em;color:var(--muted)}.sharedText{margin-top:10px;white-space:pre-wrap;line-height:1.5}.sharedLink{display:block;margin-top:10px;overflow-wrap:anywhere;color:var(--primary);font-weight:700}.sendGrid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:18px}.sendBox{background:var(--surface-2);border:1px solid var(--line);border-radius:20px;padding:16px}.sendBox textarea,.sendBox input{width:100%;margin-top:10px;border:1px solid var(--line);border-radius:13px;padding:11px 12px;font:inherit;color:var(--text);background:var(--surface);outline:none}.sendBox textarea{min-height:104px;resize:vertical}.sendBox textarea:focus,.sendBox input:focus{border-color:var(--primary)}.contentStatus{margin-top:12px;color:var(--muted);font-size:13px}.empty{color:var(--muted);padding:12px 0}.footer{text-align:center;color:#8b9099;font-size:13px;margin-top:28px}
@media(max-width:600px){.header{align-items:flex-start;flex-direction:column}.file{align-items:flex-start}.download{padding:9px 11px}.queueItem{grid-template-columns:1fr}.queueActions{justify-content:flex-start}.sendGrid{grid-template-columns:1fr}}
@media(prefers-color-scheme:dark){:root{--text:#e7e7ef;--muted:#b8bac5;--surface:#181920;--surface-2:#22242c;--bg:#111217;--line:#353844;--primary-soft:#29305f;--secondary-soft:#173c3f}.card{border-color:#262936}.secondary{color:#dfe2ff}.drop{background:#20222a}.queueActions button{background:#2c2f39;color:#e7e7ef}body{background:linear-gradient(180deg,#20254a 0,#111217 240px)}}
</style>
</head>
<body>
<div class="page">
<div class="header"><div class="brand"><span class="brandMark" aria-hidden="true"></span><span>""" +
htmlEscape(localized("app_name")) +
"""</span></div><div class="local">● """ +
htmlEscape(localized("browser_local_connection")) +
"""</div></div>
<section class="card"><h2>""" +
htmlEscape(localized("browser_from_phone")) +
"""</h2><p class="muted">""" +
htmlEscape(localized("browser_available")) +
"""</p>
""" + filesHtml + textHtml + linkHtml + """
</section>
<section class="card"><h2>""" +
htmlEscape(localized("browser_to_phone")) +
"""</h2><p class="muted">""" +
htmlEscape(localized("browser_save_path")) +
"""</p>
<label class="drop" id="dropZone" for="fileInput"><div class="dropTitle">""" +
htmlEscape(localized("browser_drop")) +
"""</div><div class="dropSub">""" +
htmlEscape(localized("browser_choose")) +
"""</div></label>
<input id="fileInput" type="file" multiple>
<div class="progress" id="progress"><div class="bar" id="bar"></div></div>
<div class="status" id="status">""" +
htmlEscape(localized("browser_ready")) +
"""</div>
<div class="queue" id="queue"></div>
<div class="sendGrid">
<div class="sendBox">
<div class="label">""" +
htmlEscape(localized("browser_text_to_phone")).uppercase(
    Locale.forLanguageTag(language.code)
) +
"""</div>
<textarea id="textToPhone" maxlength="250000" placeholder="""" +
htmlEscape(localized("browser_text_hint")) +
""""></textarea>
<button class="secondary" type="button" onclick="sendTextToPhone()">""" +
htmlEscape(localized("browser_send_text")) +
"""</button>
</div>
<div class="sendBox">
<div class="label">""" +
htmlEscape(localized("browser_link_to_phone")).uppercase(
    Locale.forLanguageTag(language.code)
) +
"""</div>
<input id="linkToPhone" type="url" maxlength="4096" placeholder="""" +
htmlEscape(localized("browser_link_hint")) +
"""">
<button class="secondary" type="button" onclick="sendLinkToPhone()">""" +
htmlEscape(localized("browser_send_link")) +
"""</button>
</div>
</div>
<div class="contentStatus" id="contentStatus"></div>
</section>
<section class="card" id="trustCard">
<h2>""" +
htmlEscape(localized("browser_quick")) +
"""</h2>
<p class="muted">""" +
htmlEscape(localized("browser_trust_desc")) +
"""</p>
<button class="secondary" id="trustButton" onclick="trustThisPc()">""" +
htmlEscape(localized("browser_remember")) +
"""</button>
<div class="status" id="trustStatus"></div>
</section>
<div class="footer">""" +
htmlEscape(localized("browser_private_wifi")) +
"""</div>
</div>
<script>
""" + browserI18n + """
const input=document.getElementById('fileInput');
const zone=document.getElementById('dropZone');
const progress=document.getElementById('progress');
const bar=document.getElementById('bar');
const status=document.getElementById('status');
const contentStatus=document.getElementById('contentStatus');
zone.addEventListener('dragover',e=>{e.preventDefault();zone.classList.add('active')});
zone.addEventListener('dragleave',()=>zone.classList.remove('active'));
zone.addEventListener('drop',e=>{e.preventDefault();zone.classList.remove('active');uploadFiles(Array.from(e.dataTransfer.files))});
input.addEventListener('change',()=>{uploadFiles(Array.from(input.files));input.value=''});
const queue=document.getElementById('queue');
const CHUNK_SIZE=4*1024*1024;
const HASH_CLIENT_LIMIT=32*1024*1024;
const MAX_BROWSER_FILES=100;
const uploadStates=new Map();

function humanBytes(v){
  if(v<1024)return v+' B';
  const k=v/1024;
  if(k<1024)return k.toFixed(1)+' KB';
  const m=k/1024;
  if(m<1024)return m.toFixed(1)+' MB';
  return (m/1024).toFixed(2)+' GB';
}

function queueRow(file,index,total){
  const row=document.createElement('div');
  row.className='queueItem';

  const left=document.createElement('div');
  const name=document.createElement('div');
  name.className='queueName';
  name.textContent=file.name;

  const meta=document.createElement('div');
  meta.className='queueMeta';
  meta.textContent=I.queue+' • '+humanBytes(file.size);

  const hash=document.createElement('div');
  hash.className='queueHash';

  left.appendChild(name);
  left.appendChild(meta);
  left.appendChild(hash);

  const actions=document.createElement('div');
  actions.className='queueActions';

  const pause=document.createElement('button');
  pause.textContent=I.pause;

  const cancel=document.createElement('button');
  cancel.className='cancel';
  cancel.textContent=I.cancel;

  actions.appendChild(pause);
  actions.appendChild(cancel);

  row.appendChild(left);
  row.appendChild(actions);
  queue.appendChild(row);

  return {row,meta,hash,pause,cancel,index,total};
}

async function stableUploadId(file){
  const source=file.name+'|'+file.size+'|'+file.lastModified;

  if(window.crypto && crypto.subtle){
    const bytes=new TextEncoder().encode(source);
    const digest=await crypto.subtle.digest('SHA-256',bytes);
    return Array.from(new Uint8Array(digest))
      .map(b=>b.toString(16).padStart(2,'0'))
      .join('')
      .slice(0,40);
  }

  let h1=2166136261>>>0;
  let h2=2654435761>>>0;
  for(let i=0;i<source.length;i++){
    const code=source.charCodeAt(i);
    h1^=code;
    h1=Math.imul(h1,16777619)>>>0;
    h2^=(code+i);
    h2=Math.imul(h2,2246822519)>>>0;
  }
  const a=h1.toString(16).padStart(8,'0');
  const b=h2.toString(16).padStart(8,'0');
  return 'local_'+a+b+'_'+file.size.toString(36);
}

async function clientSha256(file){
  if(
    file.size>HASH_CLIENT_LIMIT ||
    !window.crypto ||
    !crypto.subtle
  )return null;

  const digest=await crypto.subtle.digest(
    'SHA-256',
    await file.arrayBuffer()
  );

  return Array.from(new Uint8Array(digest))
    .map(b=>b.toString(16).padStart(2,'0'))
    .join('');
}

async function fetchUploadStatus(uploadId){
  const r=await fetch('/upload/status/""" +
            session.token +
            """/'+encodeURIComponent(uploadId),{cache:'no-store'});
  if(r.status===404)return {received:0,completed:false};
  if(!r.ok)throw new Error('status_'+r.status);
  return await r.json();
}

async function registerUploadBatch(batchId,files){
  const totalBytes=files.reduce((sum,file)=>sum+file.size,0);
  const body=
    'batchId='+encodeURIComponent(batchId)+
    '&fileCount='+encodeURIComponent(String(files.length))+
    '&totalBytes='+encodeURIComponent(String(totalBytes));

  const r=await fetch('/upload/batch/""" +
            session.token +
            """',{
    method:'POST',
    headers:{'Content-Type':'application/x-www-form-urlencoded'},
    body
  });

  if(!r.ok)throw new Error('batch_'+r.status);
}

function waitWhilePaused(state,row){
  return new Promise((resolve,reject)=>{
    const tick=()=>{
      if(state.cancelled){reject(new Error('cancelled'));return;}
      if(!state.paused){resolve();return;}
      row.meta.textContent=I.paused+' • '+humanBytes(state.offset)+' / '+humanBytes(state.file.size);
      setTimeout(tick,200);
    };
    tick();
  });
}

async function cancelRemote(uploadId){
  try{
    await fetch('/upload/cancel/""" +
            session.token +
            """',{
      method:'POST',
      headers:{'Content-Type':'application/x-www-form-urlencoded'},
      body:'uploadId='+encodeURIComponent(uploadId)
    });
  }catch(e){}
}

function uploadChunk(file,state,row,expectedHash){
  return new Promise((resolve,reject)=>{
    const start=state.offset;
    const end=Math.min(file.size,start+CHUNK_SIZE);
    const blob=file.slice(start,end);

    const xhr=new XMLHttpRequest();
    state.xhr=xhr;

    xhr.open('POST','/upload/chunk/""" +
            session.token +
            """');
    xhr.timeout=300000;
    xhr.setRequestHeader('Content-Type','application/octet-stream');
    xhr.setRequestHeader('X-Upload-Id',state.uploadId);
    xhr.setRequestHeader('X-File-Name',encodeURIComponent(file.name));
    xhr.setRequestHeader('X-File-Type',file.type||'application/octet-stream');
    xhr.setRequestHeader('X-File-Size',String(file.size));
    xhr.setRequestHeader('X-Chunk-Offset',String(start));
    if(expectedHash)xhr.setRequestHeader('X-File-Sha256',expectedHash);

    xhr.upload.onprogress=e=>{
      if(!e.lengthComputable)return;
      const absolute=start+e.loaded;
      const p=Math.min(100,Math.round(absolute/file.size*100));
      bar.style.width=p+'%';
      row.meta.textContent=I.transfer+' • '+p+'% • '+humanBytes(absolute)+' / '+humanBytes(file.size);
      status.textContent=file.name+' — '+p+'%';
    };

    xhr.onload=()=>{
      state.xhr=null;
      let data={};
      try{data=JSON.parse(xhr.responseText||'{}')}catch(e){}

      if(xhr.status===409 && typeof data.received==='number'){
        state.offset=data.received;
        resolve({retry:true});
        return;
      }

      if(xhr.status>=200 && xhr.status<300){
        if(typeof data.received==='number')state.offset=data.received;
        resolve(data);
        return;
      }

      reject(new Error('HTTP '+xhr.status));
    };

    xhr.onerror=()=>{state.xhr=null;reject(new Error('network'))};
    xhr.ontimeout=()=>{state.xhr=null;reject(new Error('timeout'))};
    xhr.onabort=()=>{state.xhr=null;reject(new Error(state.cancelled?'cancelled':'paused'))};
    xhr.send(blob);
  });
}

async function uploadResumable(file,row){
  const uploadId=await stableUploadId(file);
  const expectedHash=await clientSha256(file);
  let remote=await fetchUploadStatus(uploadId);

  const state={
    uploadId,
    file,
    offset:Math.min(Number(remote.received||0),file.size),
    paused:false,
    cancelled:false,
    xhr:null
  };
  uploadStates.set(uploadId,state);

  if(remote.completed){
    row.meta.textContent=I.alreadyCompleted+' • '+humanBytes(file.size);
    row.meta.className='queueMeta queueOk';
    row.pause.disabled=true;
    row.cancel.disabled=true;
    if(remote.sha256)row.hash.textContent='SHA-256 '+remote.sha256;
    return remote;
  }

  row.pause.onclick=()=>{
    if(state.cancelled)return;
    state.paused=!state.paused;
    row.pause.textContent=state.paused?I.resume:I.pause;
    if(state.paused && state.xhr)state.xhr.abort();
  };

  row.cancel.onclick=async()=>{
    if(state.cancelled)return;
    state.cancelled=true;
    if(state.xhr)state.xhr.abort();
    await cancelRemote(uploadId);
    row.meta.textContent=I.cancelled;
    row.meta.className='queueMeta queueErr';
    row.pause.disabled=true;
    row.cancel.disabled=true;
  };

  if(state.offset>0){
    row.meta.textContent=fmt(I.resumedFrom,humanBytes(state.offset))+' • '+Math.round(state.offset/file.size*100)+'%';
  }

  let finalData=remote;

  while(state.offset<file.size){
    await waitWhilePaused(state,row);
    if(state.cancelled)throw new Error('cancelled');

    let attempt=0;
    let done=false;

    while(!done){
      try{
        finalData=await uploadChunk(file,state,row,expectedHash);
        if(finalData.retry===true){
          attempt++;
          if(attempt>4)throw new Error('offset_loop');
          continue;
        }
        done=true;
      }catch(e){
        if(e.message==='cancelled')throw e;
        if(e.message==='paused'){
          await waitWhilePaused(state,row);
          remote=await fetchUploadStatus(uploadId);
          state.offset=Math.min(Number(remote.received||state.offset),file.size);
          done=true;
          continue;
        }

        attempt++;
        if(attempt>4)throw e;

        row.meta.textContent=I.reconnecting+' '+attempt+'/4...';
        await new Promise(r=>setTimeout(r,800*attempt));

        try{
          remote=await fetchUploadStatus(uploadId);
          state.offset=Math.min(Number(remote.received||state.offset),file.size);
        }catch(ignore){}
      }
    }
  }

  remote=await fetchUploadStatus(uploadId);
  if(!remote.completed)throw new Error('not_completed');

  row.pause.disabled=true;
  row.cancel.disabled=true;

  if(remote.sha256){
    row.hash.textContent=
      expectedHash
        ? I.integrity+' • SHA-256 '+remote.sha256
        : 'SHA-256 '+remote.sha256;
  }else if(file.size>256*1024*1024){
    row.hash.textContent=I.hashSkipped;
  }

  return remote;
}

async function uploadFiles(files){
  if(files.length===0)return;

  const requestedCount=files.length;
  files=files.slice(0,MAX_BROWSER_FILES);

  queue.innerHTML='';
  progress.style.display='block';

  if(requestedCount>MAX_BROWSER_FILES){
    status.textContent=fmt(I.limit,MAX_BROWSER_FILES);
  }

  const batchId=
    'batch_'+Date.now().toString(36)+'_'+
    Math.random().toString(36).slice(2,10);

  try{
    await registerUploadBatch(batchId,files);
  }catch(e){}

  const rows=files.map((f,i)=>queueRow(f,i,files.length));
  let ok=0,failed=0,cancelled=0;

  for(let i=0;i<files.length;i++){
    const file=files[i];
    const row=rows[i];
    status.textContent=fmt(I.filePosition,i+1,files.length);
    row.meta.textContent=I.preparing;

    try{
      await uploadResumable(file,row);
      ok++;
      row.meta.textContent=I.completed+' • '+humanBytes(file.size);
      row.meta.className='queueMeta queueOk';
    }catch(e){
      if(e.message==='cancelled'){
        cancelled++;
        row.meta.textContent=I.cancelled;
      }else{
        failed++;
        row.meta.textContent=I.notTransferred;
      }
      row.meta.className='queueMeta queueErr';
    }
  }

  if(failed===0 && cancelled===0){
    bar.style.width='100%';
    status.textContent=fmt(I.summaryOk,ok);
  }else{
    status.textContent=fmt(I.summary,ok,failed,cancelled);
  }
}

async function copyText(){const el=document.getElementById('sharedText');if(!el)return;const value=el.innerText;try{if(navigator.clipboard&&window.isSecureContext){await navigator.clipboard.writeText(value)}else{const t=document.createElement('textarea');t.value=value;t.style.position='fixed';t.style.opacity='0';document.body.appendChild(t);t.focus();t.select();document.execCommand('copy');t.remove()}status.textContent=I.copied}catch(e){status.textContent=I.copyManual}}

async function sendContentToPhone(type,value){
  const clean=(value||'').trim();
  if(!clean)return false;

  if(type==='link'&&!/^https?:\/\//i.test(clean)){
    contentStatus.textContent=I.invalidLink;
    return false;
  }

  try{
    const r=await fetch('/content/""" +
            session.token +
            """',{
      method:'POST',
      headers:{'Content-Type':'application/x-www-form-urlencoded'},
      body:'type='+encodeURIComponent(type)+'&value='+encodeURIComponent(clean)
    });

    if(!r.ok)throw new Error('HTTP '+r.status);
    contentStatus.textContent=I.contentSent;
    return true;
  }catch(e){
    contentStatus.textContent=I.contentFailed;
    return false;
  }
}

async function sendTextToPhone(){
  const field=document.getElementById('textToPhone');
  if(await sendContentToPhone('text',field.value))field.value='';
}

async function sendLinkToPhone(){
  const field=document.getElementById('linkToPhone');
  if(await sendContentToPhone('link',field.value))field.value='';
}

async function trustThisPc(){
  const trustStatus=document.getElementById('trustStatus');
  const current=localStorage.getItem('inviaalpc.trustedName');
  const suggested=current||((navigator.userAgentData&&navigator.userAgentData.platform)||navigator.platform||'PC');
  const name=prompt(I.pcName,suggested);
  if(!name)return;
  trustStatus.textContent=I.saving;
  try{
    const r=await fetch('/trust/""" + session.token + """',{
      method:'POST',
      headers:{'Content-Type':'application/x-www-form-urlencoded'},
      body:'name='+encodeURIComponent(name)
    });
    if(!r.ok)throw new Error('HTTP '+r.status);
    const data=await r.json();
    localStorage.setItem('inviaalpc.trustedName',data.name);
    document.getElementById('trustButton').textContent=I.remembered;
    trustStatus.textContent=fmt(I.trustSaved,data.name);
  }catch(e){
    trustStatus.textContent=I.trustFailed;
  }
}
(function(){
  const name=localStorage.getItem('inviaalpc.trustedName');
  if(name){
    document.getElementById('trustButton').textContent=I.remembered;
    document.getElementById('trustStatus').textContent=fmt(I.device,name);
  }
})();
</script>
</body>
</html>"""
    }

    private fun localized(
        key: String,
        vararg args: Any
    ): String =
        AppText.get(
            AppLanguageStore.current(context),
            key,
            *args
        )

    private fun jsQuoted(
        key: String
    ): String =
        "'" +
            jsonEscape(
                localized(key)
            ).replace(
                "</",
                "<\\/"
            ) +
            "'"

    private fun errorPage(
        title: String,
        message: String
    ): String {
        val language =
            AppLanguageStore.current(
                context
            )

        return """<!doctype html><html lang="${language.code}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>""" +
            htmlEscape(title) +
            """</title></head><body style="font-family:system-ui;background:#f4f6fb;padding:30px"><div style="max-width:500px;margin:auto;background:white;padding:28px;border-radius:24px"><h1>""" +
            htmlEscape(title) +
            """</h1><p>""" +
            htmlEscape(message) +
            """</p></div></body></html>"""
    }

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

    private fun cookieValue(
        request: HttpRequest,
        name: String
    ): String? =
        request.headers["cookie"]
            ?.split(';')
            ?.asSequence()
            ?.map { it.trim() }
            ?.firstOrNull {
                it.startsWith(
                    name + "="
                )
            }
            ?.substringAfter('=')
            ?.takeIf {
                it.isNotBlank()
            }

    private fun sessionCookie(
        sid: String
    ): String =
        "sid=" + sid +
            "; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age=3600"

    private fun trustedCookie(
        token: String
    ): String =
        TRUSTED_COOKIE +
            "=" +
            token +
            "; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age=" +
            TRUSTED_COOKIE_MAX_AGE_SECONDS

    private fun sendJsonWithCookie(
        output: OutputStream,
        code: Int,
        json: String,
        cookie: String
    ) {
        val data =
            json.toByteArray(
                StandardCharsets.UTF_8
            )

        writeStatus(
            output,
            code,
            statusReason(code)
        )
        writeHeader(
            output,
            "Content-Type",
            "application/json; charset=utf-8"
        )
        writeHeader(
            output,
            "Content-Length",
            data.size.toString()
        )
        writeHeader(
            output,
            "Set-Cookie",
            cookie
        )
        securityHeaders(output)
        writeHeader(
            output,
            "Connection",
            "close"
        )
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
            204 -> "No Content"
            206 -> "Partial Content"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            409 -> "Conflict"
            410 -> "Gone"
            413 -> "Payload Too Large"
            416 -> "Range Not Satisfiable"
            421 -> "Misdirected Request"
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
