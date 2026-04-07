/*
 * SpecializationTags.java
 * Role: String constants for all predefined counselor specialization tags.
 *       Single source of truth used in CounselorProfileEditActivity (chip
 *       generation), CounselorListActivity (filter chips), and the Counselor
 *       model's specializations field to prevent typos across the codebase.
 *
 * Design pattern: Constants class (non-instantiable utility).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

/**
 * String constants for counselor specialization tags.
 * Used in CounselorProfileEditActivity for tag selection,
 * CounselorListActivity for filter chips, and the Counselor
 * model's specializations field.
 *
 * <p>Constants class — not instantiable.</p>
 */
public final class SpecializationTags {

    /** Anxiety-related counseling specialization. */
    public static final String ANXIETY = "General Anxiety";

    /** Academic stress counseling specialization. */
    public static final String ACADEMIC_STRESS = "Academic Stress";

    /** Grief counseling specialization. */
    public static final String GRIEF = "Grief";

    /** Relationship counseling specialization. */
    public static final String RELATIONSHIPS = "Relationships";

    /** Career guidance counseling specialization. */
    public static final String CAREER_GUIDANCE = "Career Guidance";

    /** Depression counseling specialization. */
    public static final String DEPRESSION = "Depression";

    /** Trauma counseling specialization. */
    public static final String TRAUMA = "Trauma";

    /** Family issues counseling specialization. */
    public static final String FAMILY_ISSUES = "Family Issues";

    /**
     * All specialization tags in display order.
     * Used to iterate and populate chip groups programmatically
     * without duplicating the list of values.
     */
    public static final String[] ALL_TAGS = {
        ANXIETY, ACADEMIC_STRESS, GRIEF, RELATIONSHIPS,
        CAREER_GUIDANCE, DEPRESSION, TRAUMA, FAMILY_ISSUES
    };

    /** Prevent instantiation — use constants directly. */
    private SpecializationTags() {}
}
