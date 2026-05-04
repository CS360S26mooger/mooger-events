package com.example.moogerscouncil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ReminderSettingsTest {
    @Test
    public void emptyConstructorExists() {
        ReminderSettings settings = new ReminderSettings();
        assertNotNull(settings);
    }

    @Test
    public void defaultSettingsAreEnabled() {
        ReminderSettings settings = ReminderSettings.defaultSettings();
        assertTrue(settings.isEnabled24Hour());
        assertTrue(settings.isEnabled1Hour());
        assertNotNull(settings.getMessage24Hour());
        assertNotNull(settings.getMessage1Hour());
    }

    @Test
    public void constructorSetsMessagesAndTimestamp() {
        ReminderSettings settings = new ReminderSettings(true, false, "tomorrow", "soon", "admin");
        assertEquals("tomorrow", settings.getMessage24Hour());
        assertEquals("soon", settings.getMessage1Hour());
        assertEquals("admin", settings.getUpdatedBy());
        assertNotNull(settings.getUpdatedAt());
    }

    @Test
    public void settersUpdateFields() {
        ReminderSettings settings = new ReminderSettings();
        settings.setEnabled24Hour(true);
        settings.setEnabled1Hour(true);
        settings.setMessage24Hour("24");
        settings.setMessage1Hour("1");
        settings.setUpdatedBy("uid");

        assertTrue(settings.isEnabled24Hour());
        assertTrue(settings.isEnabled1Hour());
        assertEquals("24", settings.getMessage24Hour());
        assertEquals("1", settings.getMessage1Hour());
        assertEquals("uid", settings.getUpdatedBy());
    }
}
