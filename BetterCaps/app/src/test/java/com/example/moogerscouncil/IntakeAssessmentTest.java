package com.example.moogerscouncil;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link IntakeAssessment}. */
public class IntakeAssessmentTest {

    @Test
    public void emptyConstructorInitializesTagsAndActiveState() {
        IntakeAssessment assessment = new IntakeAssessment();

        assertNotNull(assessment.getRecommendedSpecializations());
        assertTrue(assessment.getRecommendedSpecializations().isEmpty());
        assertTrue(assessment.isActive());
    }

    @Test
    public void constructorStoresStudentIdAndAnswers() {
        IntakeAssessment assessment = new IntakeAssessment(
                "student1",
                "Academic / Career",
                "A few weeks",
                "Practical strategies and coping tools",
                IntakeAssessment.URGENCY_MEDIUM,
                Arrays.asList(SpecializationTags.ACADEMIC_STRESS));

        assertEquals("student1", assessment.getStudentId());
        assertEquals("Academic / Career", assessment.getPrimaryConcern());
        assertEquals("A few weeks", assessment.getDuration());
        assertEquals("Practical strategies and coping tools", assessment.getSupportType());
        assertEquals(IntakeAssessment.URGENCY_MEDIUM, assessment.getUrgencyLevel());
        assertEquals(1, assessment.getRecommendedSpecializations().size());
        assertNotNull(assessment.getCreatedAt());
    }

    @Test
    public void urgencyConstantsEqualExpectedStrings() {
        assertEquals("LOW", IntakeAssessment.URGENCY_LOW);
        assertEquals("MEDIUM", IntakeAssessment.URGENCY_MEDIUM);
        assertEquals("HIGH", IntakeAssessment.URGENCY_HIGH);
        assertEquals("CRISIS", IntakeAssessment.URGENCY_CRISIS);
    }

    @Test
    public void matchedCounselorFieldsCanBeSetAndRetrieved() {
        IntakeAssessment assessment = new IntakeAssessment();

        assessment.setMatchedCounselorId("c1");
        assessment.setMatchedCounselorName("Dr. Test");

        assertEquals("c1", assessment.getMatchedCounselorId());
        assertEquals("Dr. Test", assessment.getMatchedCounselorName());
    }
}
