/*
 * CounselorTest.java
 * Role: Unit tests for the Counselor model class.
 *       Verifies on-leave defaults, specialization set/get, Firestore no-arg
 *       constructor, and field setter correctness.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link Counselor}.
 * These tests run on the JVM without an Android runtime.
 */
public class CounselorTest {

    /**
     * A fresh Counselor created with the no-arg constructor must have
     * {@code onLeave} as null (treated as false in the app).
     * This prevents accidentally blocking a counselor whose document
     * lacks the field.
     */
    @Test
    public void testDefaultOnLeaveIsNull() {
        Counselor counselor = new Counselor();
        assertNull(counselor.getOnLeave());
    }

    /**
     * Setting a list of specialization tags and retrieving it must return
     * the same list reference with the same contents.
     */
    @Test
    public void testSpecializationsSetAndGet() {
        Counselor counselor = new Counselor();
        List<String> tags = Arrays.asList(
                SpecializationTags.ANXIETY,
                SpecializationTags.ACADEMIC_STRESS
        );
        counselor.setSpecializations(tags);
        assertEquals(tags, counselor.getSpecializations());
        assertEquals(2, counselor.getSpecializations().size());
        assertTrue(counselor.getSpecializations().contains(SpecializationTags.ANXIETY));
    }

    /**
     * The no-arg constructor required by Firestore deserialization must not throw
     * and must leave all fields null.
     */
    @Test
    public void testEmptyConstructorForFirestore() {
        Counselor counselor = new Counselor();
        assertNull(counselor.getId());
        assertNull(counselor.getUid());
        assertNull(counselor.getName());
        assertNull(counselor.getBio());
        assertNull(counselor.getSpecializations());
        assertNull(counselor.getLanguage());
        assertNull(counselor.getGender());
        assertNull(counselor.getOnLeave());
        assertNull(counselor.getOnLeaveMessage());
        assertNull(counselor.getReferralCounselorId());
    }

    /**
     * Bio, language, and gender setters must override the initial null values.
     */
    @Test
    public void testSettersOverrideFields() {
        Counselor counselor = new Counselor();

        counselor.setBio("Experienced therapist with 10 years in academic settings.");
        assertEquals("Experienced therapist with 10 years in academic settings.",
                counselor.getBio());

        counselor.setLanguage("Urdu");
        assertEquals("Urdu", counselor.getLanguage());

        counselor.setGender("Male");
        assertEquals("Male", counselor.getGender());
    }

    /**
     * Setting onLeave to true must be returned as true by the getter.
     * Setting it back to false must be returned as false.
     */
    @Test
    public void testOnLeaveToggle() {
        Counselor counselor = new Counselor();
        counselor.setOnLeave(true);
        assertEquals(Boolean.TRUE, counselor.getOnLeave());

        counselor.setOnLeave(false);
        assertEquals(Boolean.FALSE, counselor.getOnLeave());
    }

    /**
     * Setting the on-leave message and retrieving it must return the same string.
     */
    @Test
    public void testOnLeaveMessageSetAndGet() {
        Counselor counselor = new Counselor();
        String message = "On leave until end of April. Please see Dr. Smith.";
        counselor.setOnLeaveMessage(message);
        assertEquals(message, counselor.getOnLeaveMessage());
    }

    /**
     * {@link SpecializationTags#ALL_TAGS} must contain exactly 8 entries
     * with no duplicates.
     */
    @Test
    public void testSpecializationTagsAllTagsLength() {
        assertEquals(8, SpecializationTags.ALL_TAGS.length);
    }

    /**
     * All entries in {@link SpecializationTags#ALL_TAGS} must be unique strings.
     */
    @Test
    public void testSpecializationTagsNoDuplicates() {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String tag : SpecializationTags.ALL_TAGS) {
            assertTrue("Duplicate tag found: " + tag, seen.add(tag));
        }
    }
}
