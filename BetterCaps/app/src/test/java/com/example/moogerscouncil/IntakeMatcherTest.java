package com.example.moogerscouncil;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Test
    public void primaryTagScoresHigherThanSecondary() {
        IntakeAssessment assessment = new IntakeAssessment();
        assessment.setRecommendedSpecializations(
                Arrays.asList(SpecializationTags.ANXIETY, SpecializationTags.DEPRESSION));

        Counselor primaryMatch = counselor("primary", false, SpecializationTags.ANXIETY);
        Counselor secondaryMatch = counselor("secondary", false, SpecializationTags.DEPRESSION);

        assertTrue(IntakeMatcher.scoreCounselor(primaryMatch, assessment)
                > IntakeMatcher.scoreCounselor(secondaryMatch, assessment));
    }

    @Test
    public void tiedScoresResolvedByLowestRecommendationCount() {
        IntakeAssessment assessment = new IntakeAssessment();
        assessment.setRecommendedSpecializations(Arrays.asList(SpecializationTags.GRIEF));

        Counselor alice = counselor("Alice", false, SpecializationTags.GRIEF);
        alice.setName("Alice");
        Counselor bob = counselor("Bob", false, SpecializationTags.GRIEF);
        bob.setName("Bob");

        Map<String, Integer> counts = new HashMap<>();
        counts.put("Alice", 5);
        counts.put("Bob", 2);

        Counselor result = IntakeMatcher.findBestCounselor(
                Arrays.asList(alice, bob), assessment, counts);
        assertSame(bob, result);
    }

    @Test
    public void tiedScoresAndCountsResolvedByName() {
        IntakeAssessment assessment = new IntakeAssessment();
        assessment.setRecommendedSpecializations(Arrays.asList(SpecializationTags.GRIEF));

        Counselor alice = counselor("Alice", false, SpecializationTags.GRIEF);
        alice.setName("Alice");
        Counselor bob = counselor("Bob", false, SpecializationTags.GRIEF);
        bob.setName("Bob");

        Map<String, Integer> counts = new HashMap<>();
        counts.put("Alice", 3);
        counts.put("Bob", 3);

        Counselor result = IntakeMatcher.findBestCounselor(
                Arrays.asList(bob, alice), assessment, counts);
        assertSame(alice, result);
    }

    @Test
    public void counselorWithBioMentioningTagGetsBonus() {
        IntakeAssessment assessment = new IntakeAssessment();
        assessment.setRecommendedSpecializations(
                Arrays.asList(SpecializationTags.ANXIETY));

        Counselor withBio = counselor("withBio", false, SpecializationTags.ANXIETY);
        withBio.setBio("I specialize in General Anxiety and panic disorders.");

        Counselor noBio = counselor("noBio", false, SpecializationTags.ANXIETY);

        assertTrue(IntakeMatcher.scoreCounselor(withBio, assessment)
                > IntakeMatcher.scoreCounselor(noBio, assessment));
    }

    @Test
    public void recCountBeatsBioWhenSpecializationsMatch() {
        // Both counselors have the same specialization tag, so the rec count
        // (lower wins) must take priority over the bio bonus.
        IntakeAssessment assessment = new IntakeAssessment();
        assessment.setRecommendedSpecializations(
                Arrays.asList(SpecializationTags.ANXIETY));

        Counselor popular = counselor("popular", false, SpecializationTags.ANXIETY);
        popular.setName("Popular");
        popular.setBio("I specialize in General Anxiety and have years of experience");

        Counselor newcomer = counselor("newcomer", false, SpecializationTags.ANXIETY);
        newcomer.setName("Newcomer");
        // No bio — would lose on bio bonus alone

        Map<String, Integer> counts = new HashMap<>();
        counts.put("popular", 47);
        counts.put("newcomer", 0);

        Counselor result = IntakeMatcher.findBestCounselor(
                Arrays.asList(popular, newcomer), assessment, counts);
        assertSame(newcomer, result);
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
