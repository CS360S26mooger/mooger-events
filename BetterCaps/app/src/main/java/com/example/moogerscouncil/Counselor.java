/*
 * Counselor.java
 * Role: Firestore-mapped data container for the 'counselors' collection.
 *       Pure model class — no business logic. All persistence goes through
 *       CounselorRepository.
 *
 * Design pattern: Repository pattern (this is the model; CounselorRepository
 *                 handles all reads/writes).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import java.util.List;

/**
 * Represents a counselor profile in the BetterCAPS system.
 * Maps directly to a document in the Firestore 'counselors' collection.
 *
 * <p>Follows the Firestore model convention: no-argument constructor,
 * private fields, public getters and setters.</p>
 */
public class Counselor {

    private String id;
    private String uid;
    private String name;
    private String bio;
    private List<String> specializations;
    private String language;
    private String gender;
    private Boolean isOnLeave;
    private String onLeaveMessage;
    private String referralCounselorId;

    /** Required empty constructor for Firestore deserialization. */
    public Counselor() {}

    /**
     * Returns the Firestore document ID for this counselor.
     * Set after deserialization via {@link #setId(String)}.
     *
     * @return The document ID string, or null before it is set.
     */
    public String getId() { return id; }

    /**
     * Sets the Firestore document ID. Called after deserialization
     * to attach the document's own ID to the object.
     *
     * @param id The Firestore document ID.
     */
    public void setId(String id) { this.id = id; }

    /**
     * Returns the Firebase Auth UID linked to this counselor's user account.
     *
     * @return The UID string.
     */
    public String getUid() { return uid; }

    /**
     * Sets the Firebase Auth UID for this counselor.
     *
     * @param uid The Firebase Auth UID.
     */
    public void setUid(String uid) { this.uid = uid; }

    /**
     * Returns the counselor's display name.
     *
     * @return The name string.
     */
    public String getName() { return name; }

    /**
     * Sets the counselor's display name.
     *
     * @param name The display name.
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the counselor's biography shown on their profile screen.
     *
     * @return The bio string, or null if not set.
     */
    public String getBio() { return bio; }

    /**
     * Sets the counselor's biography.
     *
     * @param bio The biography text.
     */
    public void setBio(String bio) { this.bio = bio; }

    /**
     * Returns the list of specialization tags for this counselor.
     * Values are matched against {@link SpecializationTags} constants.
     *
     * @return List of specialization tag strings, or null if not set.
     */
    public List<String> getSpecializations() { return specializations; }

    /**
     * Sets the counselor's specialization tags.
     *
     * @param specializations List of tag strings from {@link SpecializationTags}.
     */
    public void setSpecializations(List<String> specializations) {
        this.specializations = specializations;
    }

    /**
     * Returns the counselor's spoken language preference.
     *
     * @return Language string (e.g. "English", "Urdu"), or null if not set.
     */
    public String getLanguage() { return language; }

    /**
     * Sets the counselor's spoken language preference.
     *
     * @param language Language string.
     */
    public void setLanguage(String language) { this.language = language; }

    /**
     * Returns the counselor's gender as displayed on their profile.
     *
     * @return Gender string, or null if not set.
     */
    public String getGender() { return gender; }

    /**
     * Sets the counselor's gender.
     *
     * @param gender Gender string.
     */
    public void setGender(String gender) { this.gender = gender; }

    /**
     * Returns whether this counselor is currently on leave.
     * When true, their profile shows an "On Leave" badge and booking is disabled.
     *
     * @return Boolean true if on leave, false or null if available.
     */
    public Boolean getOnLeave() { return isOnLeave; }

    /**
     * Sets the counselor's on-leave status.
     *
     * @param onLeave True if the counselor is currently on leave.
     */
    public void setOnLeave(Boolean onLeave) { this.isOnLeave = onLeave; }

    /**
     * Returns the custom message displayed when the counselor is on leave.
     * Typically explains the absence and/or refers to a colleague.
     *
     * @return The leave message string, or null if not set.
     */
    public String getOnLeaveMessage() { return onLeaveMessage; }

    /**
     * Sets the counselor's on-leave message.
     *
     * @param onLeaveMessage The custom leave message.
     */
    public void setOnLeaveMessage(String onLeaveMessage) {
        this.onLeaveMessage = onLeaveMessage;
    }

    /**
     * Returns the Firestore document ID of the colleague counselor
     * this counselor refers students to during leave (US-19).
     *
     * @return The referral counselor's document ID, or null if not set.
     */
    public String getReferralCounselorId() { return referralCounselorId; }

    /**
     * Sets the referral counselor ID for on-leave redirect (US-19).
     *
     * @param referralCounselorId Document ID of the referring colleague.
     */
    public void setReferralCounselorId(String referralCounselorId) {
        this.referralCounselorId = referralCounselorId;
    }
}