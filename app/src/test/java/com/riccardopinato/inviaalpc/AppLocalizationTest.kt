package com.riccardopinato.inviaalpc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun everySupportedLanguageHasCanonicalParity() {
        AppLanguage.entries.forEach { language ->
            assertTrue(
                "Missing translations for ${language.code}: " +
                    AppText.missingKeys(language).joinToString(),
                AppText.missingKeys(language).isEmpty()
            )

            assertEquals(
                AppText.canonicalKeys().size,
                AppText.canonicalKeys().size -
                    AppText.missingKeys(language).size
            )
        }
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
