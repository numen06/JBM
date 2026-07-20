package com.jbm.cluster.center.controller;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseDicControllerTest {

    @Test
    void supportsLegacyUnderscoreContentLanguage() {
        assertEquals(Locale.US, BaseDicController.resolveLocale("en_US", Locale.CHINESE));
    }

    @Test
    void fallsBackToAcceptLanguageWhenLegacyHeaderIsMissing() {
        assertEquals(Locale.FRANCE, BaseDicController.resolveLocale(null, Locale.FRANCE));
    }

    @Test
    void defaultsToChineseWhenNoLanguageWasProvided() {
        assertEquals(Locale.CHINESE, BaseDicController.resolveLocale(null, null));
    }
}
