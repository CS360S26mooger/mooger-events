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
     * Scores one counselor's TOTAL match for an assessment (specialization +
     * bio + completeness). Used when ranking counselors with no recommendation
     * count tiebreaker. On-leave counselors are excluded.
     *
     * @return Total score, or -1 when the counselor is not matchable.
     */
    public static int scoreCounselor(Counselor counselor, IntakeAssessment assessment) {
        if (counselor == null || assessment == null) return -1;
        if (Boolean.TRUE.equals(counselor.getOnLeave())) return -1;
        return specializationScore(counselor, assessment)
                + bioAndCompletenessScore(counselor, assessment);
    }

    /**
     * Specialization-only match score. This is the PRIMARY ranking key —
     * counselors with the same specialization match are considered equally
     * qualified and should be differentiated only by recommendation traffic.
     *
     * Weights:
     *   - Primary tag (first recommended): +20
     *   - Secondary tag (second):          +12
     *   - Any further tag match:           +6 each
     */
    public static int specializationScore(Counselor counselor, IntakeAssessment assessment) {
        if (counselor == null || assessment == null) return 0;
        if (Boolean.TRUE.equals(counselor.getOnLeave())) return -1;

        int score = 0;
        List<String> needed = assessment.getRecommendedSpecializations();
        List<String> offered = counselor.getSpecializations();
        if (needed == null || offered == null) return score;

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
        return score;
    }

    /**
     * Tertiary tiebreaker — bio keyword matches and profile completeness.
     * Only consulted when specialization score AND recommendation count are
     * both tied between two counselors.
     */
    public static int bioAndCompletenessScore(Counselor counselor, IntakeAssessment assessment) {
        if (counselor == null) return 0;
        int score = 0;
        List<String> offered = counselor.getSpecializations();
        if (offered != null && !offered.isEmpty()) score += 1;

        String bio = counselor.getBio();
        if (bio != null && !bio.trim().isEmpty()) {
            score += 1;
            if (assessment != null) {
                List<String> needed = assessment.getRecommendedSpecializations();
                if (needed != null) {
                    String bioLower = bio.toLowerCase();
                    for (String tag : needed) {
                        if (tag != null && bioLower.contains(tag.toLowerCase())) {
                            score += 3;
                        }
                    }
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
     * Finds the best-matching counselor using a strict multi-key ranking:
     *
     * <ol>
     *   <li><b>Specialization match score</b> — higher is better.</li>
     *   <li><b>Recommendation count</b> — lower is better. When two
     *       counselors match the student's needs equally, the one
     *       recommended fewer times wins so traffic is distributed evenly.</li>
     *   <li><b>Bio + completeness score</b> — higher is better. Used only
     *       when spec score AND rec count are both tied.</li>
     *   <li><b>Alphabetical name</b> — final deterministic fallback.</li>
     * </ol>
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
        final Map<String, Integer> counts = recCounts != null ? recCounts : Collections.emptyMap();

        List<Counselor> eligible = new ArrayList<>();
        for (Counselor c : counselors) {
            if (c == null) continue;
            if (Boolean.TRUE.equals(c.getOnLeave())) continue;
            eligible.add(c);
        }
        if (eligible.isEmpty()) return null;

        eligible.sort((a, b) -> {
            int specA = specializationScore(a, assessment);
            int specB = specializationScore(b, assessment);
            if (specA != specB) return Integer.compare(specB, specA); // higher first

            int recA = getCount(counts, a);
            int recB = getCount(counts, b);
            if (recA != recB) return Integer.compare(recA, recB); // lower first

            int bioA = bioAndCompletenessScore(a, assessment);
            int bioB = bioAndCompletenessScore(b, assessment);
            if (bioA != bioB) return Integer.compare(bioB, bioA); // higher first

            String nameA = a.getName() != null ? a.getName() : "";
            String nameB = b.getName() != null ? b.getName() : "";
            return nameA.compareTo(nameB);
        });

        return eligible.get(0);
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
