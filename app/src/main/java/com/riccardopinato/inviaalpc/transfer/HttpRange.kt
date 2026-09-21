package com.riccardopinato.inviaalpc.transfer

object HttpRange {

    fun parse(
        header: String?,
        totalBytes: Long
    ): LongRange? {
        if (
            header.isNullOrBlank() ||
            totalBytes <= 0L
        ) {
            return null
        }

        if (
            !header.startsWith(
                "bytes=",
                ignoreCase = true
            )
        ) {
            return null
        }

        val raw =
            header.substringAfter('=')
                .substringBefore(',')
                .trim()

        val parts =
            raw.split(
                '-',
                limit = 2
            )

        if (parts.size != 2) {
            return null
        }

        val startText = parts[0].trim()
        val endText = parts[1].trim()

        if (startText.isBlank()) {
            val suffixLength =
                endText.toLongOrNull()
                    ?: return null

            if (suffixLength <= 0L) {
                return null
            }

            val length =
                minOf(
                    suffixLength,
                    totalBytes
                )

            return (
                totalBytes - length
            ) until totalBytes
        }

        val start =
            startText.toLongOrNull()
                ?: return null

        if (
            start < 0L ||
            start >= totalBytes
        ) {
            return null
        }

        val end =
            if (endText.isBlank()) {
                totalBytes - 1L
            } else {
                minOf(
                    endText.toLongOrNull()
                        ?: return null,
                    totalBytes - 1L
                )
            }

        if (end < start) {
            return null
        }

        return start..end
    }
}
