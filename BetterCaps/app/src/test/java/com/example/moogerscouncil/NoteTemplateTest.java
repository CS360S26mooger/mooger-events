package com.example.moogerscouncil;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link NoteTemplate}. */
public class NoteTemplateTest {

    @Test
    public void allKeysReturnNonEmptyDisplayNames() {
        for (String key : NoteTemplate.ALL_KEYS) {
            assertFalse(NoteTemplate.getDisplayName(key).isEmpty());
        }
    }

    @Test
    public void allKeysReturnNonEmptyTemplateText() {
        for (String key : NoteTemplate.ALL_KEYS) {
            assertFalse(NoteTemplate.getTemplateText(key).isEmpty());
        }
    }

    @Test
    public void unknownKeyFallsBackToGeneralSession() {
        String text = NoteTemplate.getTemplateText("UNKNOWN");

        assertTrue(text.contains("Session summary"));
    }
}
