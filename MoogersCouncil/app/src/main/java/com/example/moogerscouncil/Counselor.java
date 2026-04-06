package com.mooger.moogerscouncil;
import java.util.List;

/**
 * Represents a counselor in the system.
 * Stores profile data fetched from Firestore.
 */
public class Counselor {
    private String id;
    private String name;
    private List<String> specializations;
    private String language;
    private String gender;
    private String bio;

    public Counselor() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getSpecializations() { return specializations; }
    public void setSpecializations(List<String> s) { this.specializations = s; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}