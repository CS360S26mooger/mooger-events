package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/** Repository for all Firestore operations on session notes. */
public class SessionNoteRepository {

    private final CollectionReference notesCollection;

    /** Initialises the repository with the sessionNotes collection. */
    public SessionNoteRepository() {
        notesCollection = FirebaseFirestore.getInstance().collection("sessionNotes");
    }

    public interface OnNoteActionCallback {
        void onSuccess(String noteId);
        void onFailure(Exception e);
    }

    public interface OnNotesLoadedCallback {
        void onSuccess(List<SessionNote> notes);
        void onFailure(Exception e);
    }

    /**
     * Fetches the single existing note for an appointment, or returns null if none exists yet.
     * Used to decide whether to create or update when the counselor opens the notes dialog.
     *
     * @param appointmentId The appointment to look up.
     * @param callback Returns the existing {@link SessionNote}, or null if none found.
     */
    public void getNoteForAppointment(String appointmentId, OnSingleNoteCallback callback) {
        notesCollection.whereEqualTo("appointmentId", appointmentId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess(null);
                        return;
                    }
                    SessionNote note = snapshot.getDocuments().get(0).toObject(SessionNote.class);
                    if (note != null) {
                        note.setId(snapshot.getDocuments().get(0).getId());
                    }
                    callback.onSuccess(note);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public interface OnSingleNoteCallback {
        /** @param note The existing note, or null if no note has been saved yet. */
        void onSuccess(SessionNote note);
        void onFailure(Exception e);
    }

    /**
     * Updates an existing note document in place.
     * Only touches noteText, templateKey, and updatedAt — no other fields overwritten.
     *
     * @param noteId      Document ID of the existing note.
     * @param newText     Updated note body.
     * @param templateKey Updated template key (may be unchanged).
     * @param callback    Success/failure callback.
     */
    public void updateNote(String noteId, String newText, String templateKey,
                           OnNoteActionCallback callback) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("noteText", newText);
        updates.put("templateKey", templateKey);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        notesCollection.document(noteId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess(noteId))
                .addOnFailureListener(callback::onFailure);
    }

    /** Saves a session note as a new document. */
    public void saveNote(SessionNote note, OnNoteActionCallback callback) {
        String id = notesCollection.document().getId();
        note.setId(id);
        notesCollection.document(id)
                .set(note)
                .addOnSuccessListener(unused -> callback.onSuccess(id))
                .addOnFailureListener(callback::onFailure);
    }

    /** Loads all notes recorded for a student. */
    public void getNotesForStudent(String studentId, OnNotesLoadedCallback callback) {
        notesCollection.whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<SessionNote> notes = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        SessionNote note = doc.toObject(SessionNote.class);
                        if (note != null) {
                            note.setId(doc.getId());
                            notes.add(note);
                        }
                    }
                    callback.onSuccess(notes);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /** Permanently deletes a session note document. */
    public void deleteNote(String noteId, OnNoteActionCallback callback) {
        notesCollection.document(noteId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(noteId))
                .addOnFailureListener(callback::onFailure);
    }

    /** Loads notes recorded for one appointment. */
    public void getNotesForAppointment(String appointmentId, OnNotesLoadedCallback callback) {
        notesCollection.whereEqualTo("appointmentId", appointmentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<SessionNote> notes = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        SessionNote note = doc.toObject(SessionNote.class);
                        if (note != null) {
                            note.setId(doc.getId());
                            notes.add(note);
                        }
                    }
                    callback.onSuccess(notes);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
