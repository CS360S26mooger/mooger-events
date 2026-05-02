package com.example.moogerscouncil;

import java.util.ArrayList;
import java.util.List;

/**
 * Scores counselors against a student's intake assessment.
 * Kept outside activities so matching behavior can be unit tested.
 */
public final class IntakeMatcher {

    private IntakeMatcher() {}

    /**
     * Maps quiz answers to canonical specialization tags.
     *
     * @param primaryConcern Main concern answer.
     * @param supportType Preferred support answer.
     * @param urgencyLevel Normalized urgency level.
     * @return Ordered list of recommended specialization tags.
     */
    public static List<String> tagsForAnswers(String primaryConcern,
                                              String supportType,
                                              String urgencyLevel) {
        List<String> tags = new ArrayList<>();

        String concern = primaryConcern == null ? "" : primaryConcern.toLowerCase();
        String support = supportType == null ? "" : supportType.toLowerCase();
        String urgency = urgencyLevel == null ? IntakeAssessment.URGENCY_LOW : urgencyLevel;

        if (concern.contains("academic")) {
            tags.add(SpecializationTags.ACADEMIC_STRESS);
            tags.add(SpecializationTags.CAREER_GUIDANCE);
        } else if (concern.contains("career")) {
            tags.add(SpecializationTags.CAREER_GUIDANCE);
            tags.add(SpecializationTags.ACADEMIC_STRESS);
        } else if (concern.contains("relationship")) {
            tags.add(SpecializationTags.RELATIONSHIPS);
            tags.add(SpecializationTags.FAMILY_ISSUES);
        } else if (concern.contains("emotional") || concern.contains("personal")) {
            tags.add(SpecializationTags.ANXIETY);
            tags.add(SpecializationTags.DEPRESSION);
        } else {
            tags.add(SpecializationTags.ANXIETY);
            tags.add(SpecializationTags.ACADEMIC_STRESS);
        }

        if (support.contains("crisis") || IntakeAssessment.URGENCY_CRISIS.equals(urgency)) {
            addIfMissing(tags, SpecializationTags.TRAUMA);
        }

        return tags;
    }

    /**
     * Scores one counselor for an assessment. On-leave counselors are excluded.
     *
     * @param counselor Candidate counselor.
     * @param assessment Intake assessment.
     * @return Score, or -1 when the counselor is not matchable.
     */
    public static int scoreCounselor(Counselor counselor, IntakeAssessment assessment) {
        if (counselor == null || assessment == null) return -1;
        if (Boolean.TRUE.equals(counselor.getOnLeave())) return -1;

        int score = 0;
        List<String> needed = assessment.getRecommendedSpecializations();
        List<String> offered = counselor.getSpecializations();

        if (needed != null && offered != null) {
            for (String tag : needed) {
                for (String counselorTag : offered) {
                    if (tag != null && tag.equalsIgnoreCase(counselorTag)) {
                        score += 10;
                        break;
                    }
                }
            }
        }

        if (counselor.getBio() != null && assessment.getPrimaryConcern() != null) {
            String bio = counselor.getBio().toLowerCase();
            String concern = assessment.getPrimaryConcern().toLowerCase();
            if (!concern.isEmpty() && bio.contains(concern)) score += 2;
        }

        return score;
    }

    /**
     * Finds the highest-scoring counselor, skipping anyone marked on leave.
     *
     * @param counselors Candidate counselors.
     * @param assessment Intake assessment.
     * @return Best counselor, or null when no active counselor exists.
     */
    public static Counselor findBestCounselor(List<Counselor> counselors,
                                              IntakeAssessment assessment) {
        if (counselors == null || counselors.isEmpty()) return null;

        Counselor best = null;
        int bestScore = -1;
        for (Counselor counselor : counselors) {
            int score = scoreCounselor(counselor, assessment);
            if (score > bestScore) {
                bestScore = score;
                best = counselor;
            }
        }

        if (best != null) return best;

        for (Counselor counselor : counselors) {
            if (!Boolean.TRUE.equals(counselor.getOnLeave())) {
                return counselor;
            }
        }
        return null;
    }

    private static void addIfMissing(List<String> tags, String tag) {
        if (!tags.contains(tag)) tags.add(tag);
    }
}
