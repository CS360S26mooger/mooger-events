package com.example.moogerscouncil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Scoring weights (higher = better match):
     *   - Primary specialization match (first recommended tag):  +20
     *   - Secondary specialization match (second tag):           +12
     *   - Any further tag overlap:                               +6 each
     *   - Bio contains any recommended tag keyword:              +3 per keyword
     *   - Counselor has a non-empty bio (signals active profile): +1
     *   - Has specializations set at all:                         +1
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

        if (offered != null && !offered.isEmpty()) {
            score += 1;
        }

        if (counselor.getBio() != null && !counselor.getBio().trim().isEmpty()) {
            score += 1;
        }

        if (needed != null && offered != null) {
            for (int i = 0; i < needed.size(); i++) {
                String tag = needed.get(i);
                if (tag == null) continue;
                for (String counselorTag : offered) {
                    if (tag.equalsIgnoreCase(counselorTag)) {
                        if (i == 0) score += 20;
                        else if (i == 1) score += 12;
                        else score += 6;
                        break;
                    }
                }
            }
        }

        if (counselor.getBio() != null && needed != null) {
            String bioLower = counselor.getBio().toLowerCase();
            for (String tag : needed) {
                if (tag != null && bioLower.contains(tag.toLowerCase())) {
                    score += 3;
                }
            }
        }

        return score;
    }

    /**
     * Convenience overload when recommendation counts are not available.
     */
    public static Counselor findBestCounselor(List<Counselor> counselors,
                                              IntakeAssessment assessment) {
        return findBestCounselor(counselors, assessment, Collections.emptyMap());
    }

    /**
     * Finds the highest-scoring counselor, skipping anyone marked on leave.
     * Ties are broken by fewest past recommendations (global counter from
     * Firestore) so that traffic is distributed evenly across counselors.
     *
     * @param counselors Candidate counselors.
     * @param assessment Intake assessment.
     * @param recCounts  Map of counselorId → total times recommended (from Firestore).
     * @return Best counselor, or null when no active counselor exists.
     */
    public static Counselor findBestCounselor(List<Counselor> counselors,
                                              IntakeAssessment assessment,
                                              Map<String, Integer> recCounts) {
        if (counselors == null || counselors.isEmpty()) return null;
        Map<String, Integer> counts = recCounts != null ? recCounts : Collections.emptyMap();

        Counselor best = null;
        int bestScore = -1;
        for (Counselor counselor : counselors) {
            int score = scoreCounselor(counselor, assessment);
            if (score < 0) continue;
            if (score > bestScore) {
                bestScore = score;
                best = counselor;
            } else if (score == bestScore && best != null) {
                int bestRecs = getCount(counts, best);
                int curRecs = getCount(counts, counselor);
                if (curRecs < bestRecs) {
                    best = counselor;
                } else if (curRecs == bestRecs) {
                    String bestName = best.getName() != null ? best.getName() : "";
                    String curName = counselor.getName() != null ? counselor.getName() : "";
                    if (curName.compareTo(bestName) < 0) {
                        best = counselor;
                    }
                }
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

    private static int getCount(Map<String, Integer> counts, Counselor c) {
        String id = c.getId();
        if (id != null && counts.containsKey(id)) return counts.get(id);
        String uid = c.getUid();
        if (uid != null && counts.containsKey(uid)) return counts.get(uid);
        return 0;
    }

    private static void addIfMissing(List<String> tags, String tag) {
        if (!tags.contains(tag)) tags.add(tag);
    }
}
