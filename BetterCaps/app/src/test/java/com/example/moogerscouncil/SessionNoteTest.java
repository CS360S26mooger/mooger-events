package com.example.moogerscouncil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link SessionNote}. */
public class SessionNoteTest {

    @Test
    public void constructorStoresAppointmentCounselorStudentTemplateAndText() {
        SessionNote note = new SessionNote(
                "appointment1",
                "counselor1",
                "student1",
                NoteTemplate.ACADEMIC_STRESS,
                "Session note");

        assertEquals("appointment1", note.getAppointmentId());
        assertEquals("counselor1", note.getCounselorId());
        assertEquals("student1", note.getStudentId());
        assertEquals(NoteTemplate.ACADEMIC_STRESS, note.getTemplateKey());
        assertEquals("Session note", note.getNoteText());
        assertNotNull(note.getCreatedAt());
        assertNotNull(note.getUpdatedAt());
    }

    @Test
    public void defaultPrivateToCounselorIsTrue() {
        SessionNote note = new SessionNote("a", "c", "s", null, "Text");

        assertTrue(note.isPrivateToCounselor());
    }

    @Test
    public void settersAndGettersWorkForFirestore() {
        SessionNote note = new SessionNote();
        note.setId("note1");
        note.setAppointmentId("appointment1");
        note.setCounselorId("counselor1");
        note.setStudentId("student1");
        note.setTemplateKey(NoteTemplate.GENERAL_SESSION);
        note.setNoteText("Updated");
        note.setPrivateToCounselor(true);

        assertEquals("note1", note.getId());
        assertEquals("appointment1", note.getAppointmentId());
        assertEquals("counselor1", note.getCounselorId());
        assertEquals("student1", note.getStudentId());
        assertEquals(NoteTemplate.GENERAL_SESSION, note.getTemplateKey());
        assertEquals("Updated", note.getNoteText());
        assertTrue(note.isPrivateToCounselor());
    }
}
