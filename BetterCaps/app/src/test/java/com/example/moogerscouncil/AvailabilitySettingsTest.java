package com.example.moogerscouncil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/** Unit tests for {@link AvailabilitySettings}. */
public class AvailabilitySettingsTest {

    @Test
    public void emptyConstructorDefaultsBufferToZero() {
        AvailabilitySettings settings = new AvailabilitySettings();

        assertEquals(0, settings.getBufferMinutes());
        assertEquals(AvailabilitySettings.PROVIDER_NONE, settings.getCalendarProvider());
        assertFalse(settings.isExternalCalendarEnabled());
        assertFalse(settings.isExportIcsEnabled());
    }

    @Test
    public void constructorStoresCounselorIdAndBuffer() {
        AvailabilitySettings settings = new AvailabilitySettings("counselor1", 15);

        assertEquals("counselor1", settings.getCounselorId());
        assertEquals(15, settings.getBufferMinutes());
        assertEquals(AvailabilitySettings.PROVIDER_DEVICE, settings.getCalendarProvider());
        assertNotNull(settings.getUpdatedAt());
    }

    @Test
    public void providerConstantsMatchExpectedStrings() {
        assertEquals("NONE", AvailabilitySettings.PROVIDER_NONE);
        assertEquals("GOOGLE", AvailabilitySettings.PROVIDER_GOOGLE);
        assertEquals("OUTLOOK", AvailabilitySettings.PROVIDER_OUTLOOK);
        assertEquals("DEVICE", AvailabilitySettings.PROVIDER_DEVICE);
    }
}
