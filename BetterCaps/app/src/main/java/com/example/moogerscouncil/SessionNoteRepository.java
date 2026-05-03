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
