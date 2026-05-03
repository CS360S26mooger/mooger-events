package com.example.moogerscouncil;

/** Quick-insert session note templates for counselor documentation. */
public final class NoteTemplate {

    public static final String ACADEMIC_STRESS = "ACADEMIC_STRESS";
    public static final String ANXIETY_CHECK_IN = "ANXIETY_CHECK_IN";
    public static final String FOLLOW_UP_PLAN = "FOLLOW_UP_PLAN";
    public static final String CRISIS_CHECK = "CRISIS_CHECK";
    public static final String GENERAL_SESSION = "GENERAL_SESSION";

    public static final String[] ALL_KEYS = {
            ACADEMIC_STRESS,
            ANXIETY_CHECK_IN,
            FOLLOW_UP_PLAN,
            CRISIS_CHECK,
            GENERAL_SESSION
    };

    private NoteTemplate() {}

    /** Returns a readable name for a template key. */
    public static String getDisplayName(String key) {
        if (ACADEMIC_STRESS.equals(key)) return "Academic Stress";
        if (ANXIETY_CHECK_IN.equals(key)) return "Anxiety Check-in";
        if (FOLLOW_UP_PLAN.equals(key)) return "Follow-up Plan";
        if (CRISIS_CHECK.equals(key)) return "Crisis Check";
        return "General Session";
    }

    /** Returns the body text inserted by a template key. */
    public static String getTemplateText(String key) {
        if (ACADEMIC_STRESS.equals(key)) {
            return "Presenting concern: academic stress.\n"
                    + "Main stressors discussed:\n"
                    + "Coping strategies suggested:\n"
                    + "Follow-up tasks:";
        }
        if (ANXIETY_CHECK_IN.equals(key)) {
            return "Anxiety check-in.\n"
                    + "Triggers identified:\n"
                    + "Physical/emotional symptoms:\n"
                    + "Grounding or breathing practice:\n"
                    + "Next steps:";
        }
        if (FOLLOW_UP_PLAN.equals(key)) {
            return "Follow-up plan.\n"
                    + "Progress since last session:\n"
                    + "Barriers discussed:\n"
                    + "Agreed action items:\n"
                    + "Next appointment recommendation:";
        }
        if (CRISIS_CHECK.equals(key)) {
            return "Crisis/safety check.\n"
                    + "Immediate risk level:\n"
                    + "Protective factors:\n"
                    + "Action taken:\n"
                    + "Emergency contact/referral notes:";
        }
        return "Session summary:\n"
                + "Key themes:\n"
                + "Interventions used:\n"
                + "Plan before next session:";
    }
}
