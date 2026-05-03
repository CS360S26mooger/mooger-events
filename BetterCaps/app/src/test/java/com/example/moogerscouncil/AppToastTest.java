package com.example.moogerscouncil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Pure unit tests for AppToast constants.
 * Toast.LENGTH_SHORT == 0 and Toast.LENGTH_LONG == 1 are Android SDK guarantees;
 * testing the numeric values here ensures AppToast re-exports them correctly.
 */
public class AppToastTest {

    @Test
    public void lengthShortIsZero() {
        // android.widget.Toast.LENGTH_SHORT = 0 — SDK guarantee
        assertEquals(0, AppToast.LENGTH_SHORT);
    }

    @Test
    public void lengthLongIsOne() {
        // android.widget.Toast.LENGTH_LONG = 1 — SDK guarantee
        assertEquals(1, AppToast.LENGTH_LONG);
    }

    @Test
    public void shortAndLongAreDistinct() {
        assertNotEquals(AppToast.LENGTH_SHORT, AppToast.LENGTH_LONG);
    }
}
