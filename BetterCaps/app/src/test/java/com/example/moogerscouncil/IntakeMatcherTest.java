package com.example.moogerscouncil;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link IntakeMatcher}. */
public class IntakeMatcherTest {

    @Test
    public void academicConcernMapsToAcademicStressAndCareerGuidance() {
        List<String> tags = IntakeMatcher.tagsForAnswers(
                "Academic / Career",
                "Practical strategies",
                IntakeAssessment.URGENCY_MEDIUM);

        assertTrue(tags.contains(SpecializationTags.ACADEMIC_STRESS));
        assertTrue(tags.contains(SpecializationTags.CAREER_GUIDANCE));
    }

    @Test
    public void relationshipConcernMapsToRelationships() {
        List<String> tags = IntakeMatcher.tagsForAnswers(
                "Relationships",
                "Someone to talk to",
                IntakeAssessment.URGENCY_LOW);

        assertTrue(tags.contains(SpecializationTags.RELATIONSHIPS));
    }

    @Test
    public void crisisSupportAddsTrauma() {
        List<String> tags = IntakeMatcher.tagsForAnswers(
                "Not sure / General",
                "Crisis or urgent support",
                IntakeAssessment.URGENCY_CRISIS);

        assertTrue(tags.contains(SpecializationTags.TRAUMA));
    }

    @Test
    public void counselorWithOverlappingTagsScoresHigher() {
        IntakeAssessment assessment = new IntakeAssessment();
        assessment.setRecommendedSpecializations(Arrays.asList(SpecializationTags.ANXIETY));

        Counselor matching = counselor("matching", false, SpecializationTags.ANXIETY);
        Counselor other = counselor("other", false, SpecializationTags.CAREER_GUIDANCE);

        assertTrue(IntakeMatcher.scoreCounselor(matching, assessment)
                > IntakeMatcher.scoreCounselor(other, assessment));
    }

    @Test
    public void onLeaveCounselorReturnsMinusOne() {
        Counselor counselor = counselor("onLeave", true, SpecializationTags.ANXIETY);

        assertEquals(-1, IntakeMatcher.scoreCounselor(counselor, new IntakeAssessment()));
    }

    @Test
    public void findBestCounselorSkipsOnLeaveCounselors() {
        IntakeAssessment assessment = new IntakeAssessment();
        assessment.setRecommendedSpecializations(Arrays.asList(SpecializationTags.ANXIETY));
        Counselor unavailable = counselor("unavailable", true, SpecializationTags.ANXIETY);
        Counselor available = counselor("available", false, SpecializationTags.ACADEMIC_STRESS);

        Counselor result = IntakeMatcher.findBestCounselor(
                Arrays.asList(unavailable, available),
                assessment);

        assertSame(available, result);
    }

    private Counselor counselor(String id, boolean onLeave, String tag) {
        Counselor counselor = new Counselor();
        counselor.setId(id);
        counselor.setName(id);
        counselor.setOnLeave(onLeave);
        counselor.setSpecializations(Arrays.asList(tag));
        return counselor;
    }
}
