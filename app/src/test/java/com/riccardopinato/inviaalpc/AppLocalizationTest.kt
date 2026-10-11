package com.riccardopinato.inviaalpc

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLocalizationTest {

    @Test
    fun englishKeyResolves() {
        assertEquals(
            "Connect",
            AppText.get(
                AppLanguage.EN,
                "connect"
            )
        )
    }

    @Test
    fun missingTranslationFallsBackToEnglish() {
        assertEquals(
            "The link is invalid. Use an http:// or https:// address.",
            AppText.get(
                AppLanguage.ES,
                "err_invalid_link"
            )
        )
    }

    @Test
    fun formattedStringUsesArguments() {
        assertEquals(
            "3 elementos seleccionados",
            AppText.get(
                AppLanguage.ES,
                "selected_count",
                3
            )
        )
    }

    @Test
    fun unknownKeyFallsBackToKey() {
        assertEquals(
            "unknown_key",
            AppText.get(
                AppLanguage.DE,
                "unknown_key"
            )
        )
    }
}
