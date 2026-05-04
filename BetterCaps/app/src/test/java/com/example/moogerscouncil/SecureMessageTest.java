package com.example.moogerscouncil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class SecureMessageTest {
    @Test
    public void emptyConstructorExists() {
        assertNotNull(new SecureMessage());
    }

    @Test
    public void constructorSetsCoreFields() {
        SecureMessage message = new SecureMessage(
                "apt", "counselor", "student", "sender", "receiver", "counselor", "hello");

        assertEquals("apt", message.getAppointmentId());
        assertEquals("counselor", message.getCounselorId());
        assertEquals("student", message.getStudentId());
        assertEquals("sender", message.getSenderId());
        assertEquals("receiver", message.getReceiverId());
        assertEquals("counselor", message.getSenderRole());
        assertEquals("hello", message.getMessageText());
        assertFalse(message.isRead());
        assertNotNull(message.getCreatedAt());
    }

    @Test
    public void settersWork() {
        SecureMessage message = new SecureMessage();
        message.setId("id");
        message.setAppointmentId("apt");
        message.setCounselorId("c");
        message.setStudentId("s");
        message.setSenderId("from");
        message.setReceiverId("to");
        message.setSenderRole("student");
        message.setMessageText("body");
        message.setRead(true);

        assertEquals("id", message.getId());
        assertEquals("apt", message.getAppointmentId());
        assertEquals("c", message.getCounselorId());
        assertEquals("s", message.getStudentId());
        assertEquals("from", message.getSenderId());
        assertEquals("to", message.getReceiverId());
        assertEquals("student", message.getSenderRole());
        assertEquals("body", message.getMessageText());
    }
}
