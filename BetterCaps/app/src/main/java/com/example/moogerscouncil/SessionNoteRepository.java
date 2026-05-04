package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/** Repository for all Firestore operations on session notes. */
public class SessionNoteRepository {

    private final FirebaseFirestore db;

    public SessionNoteRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /** Returns the notes sub-collection for a specific counselor–student pair. */
    private CollectionReference notesFor(String counselorId, String studentId) {
        return db.collection("counselorNotes")
                .document(counselorId + "_" + studentId)
                .collection("notes");
    }

    public interface OnNoteActionCallback {
        void onSuccess(String noteId);
        void onFailure(Exception e);
    }

    public interface OnNotesLoadedCallback {
        void onSuccess(List<SessionNote> notes);
        void onFailure(Exception e);
    }

    public interface OnSingleNoteCallback {
        /** @param note The existing note, or null if no note has been saved yet. */
        void onSuccess(SessionNote note);
        void onFailure(Exception e);
    }

    /**
     * Fetches the single existing note for an appointment within the counselor–student scope.
     * Used to decide whether to create or update when the counselor opens the notes editor.
     */
    public void getNoteForAppointment(String counselorId, String studentId,
                                      String appointmentId, OnSingleNoteCallback callback) {
        notesFor(counselorId, studentId)
                .whereEqualTo("appointmentId", appointmentId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess(null);
                        return;
                    }
                    SessionNote note = snapshot.getDocuments().get(0).toObject(SessionNote.class);
                    if (note != null) note.setId(snapshot.getDocuments().get(0).getId());
                    callback.onSuccess(note);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Loads all notes written by one counselor for one student, ordered by creation time.
     * This is the primary read path — each counselor only ever sees their own notes.
     */
    public void getNotesForCounselorStudent(String counselorId, String studentId,
                                            OnNotesLoadedCallback callback) {
        notesFor(counselorId, studentId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
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

    /**
     * Updates an existing note within the counselor–student scope.
     * Only touches noteText, templateKey, and updatedAt.
     */
    public void updateNote(String counselorId, String studentId, String noteId,
                           String newText, String templateKey, OnNoteActionCallback callback) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("noteText", newText);
        updates.put("templateKey", templateKey);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        notesFor(counselorId, studentId).document(noteId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess(noteId))
                .addOnFailureListener(callback::onFailure);
    }

    /** Saves a new session note under the counselor–student sub-collection. */
    public void saveNote(SessionNote note, OnNoteActionCallback callback) {
        CollectionReference col = notesFor(note.getCounselorId(), note.getStudentId());
        String id = col.document().getId();
        note.setId(id);
        col.document(id)
                .set(note)
                .addOnSuccessListener(unused -> callback.onSuccess(id))
                .addOnFailureListener(callback::onFailure);
    }

    /** Permanently deletes a session note from within the counselor–student scope. */
    public void deleteNote(String counselorId, String studentId, String noteId,
                           OnNoteActionCallback callback) {
        notesFor(counselorId, studentId).document(noteId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(noteId))
                .addOnFailureListener(callback::onFailure);
    }
}
