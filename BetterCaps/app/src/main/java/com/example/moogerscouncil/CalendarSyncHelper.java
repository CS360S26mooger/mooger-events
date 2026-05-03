package com.example.moogerscouncil;

import android.content.Intent;
import android.provider.CalendarContract;

/** Helper for prototype-level external calendar integration. */
public final class CalendarSyncHelper {

    private CalendarSyncHelper() {}

    /** Returns the Android action used for calendar insert intents. */
    public static String getInsertEventAction() {
        return Intent.ACTION_INSERT;
    }

    /** Builds an Android calendar insert intent for one appointment. */
    public static Intent buildInsertEventIntent(Appointment appointment, String counselorName) {
        Intent intent = new Intent(getInsertEventAction());
        intent.setData(CalendarContract.Events.CONTENT_URI);
        intent.putExtra(CalendarContract.Events.TITLE,
                "BetterCAPS session with " + safeName(counselorName));
        intent.putExtra(CalendarContract.Events.DESCRIPTION,
                "Counseling appointment booked through BetterCAPS.");
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, "LUMS CAPS");
        return intent;
    }

    /** Builds a minimal ICS event block for one appointment. */
    public static String buildIcsLine(Appointment appointment, String counselorName) {
        String date = appointment == null ? "" : String.valueOf(appointment.getDate());
        String time = appointment == null ? "" : String.valueOf(appointment.getTime());
        return "BEGIN:VEVENT\n"
                + "SUMMARY:BetterCAPS session with " + safeName(counselorName) + "\n"
                + "DESCRIPTION:Counseling appointment booked through BetterCAPS\n"
                + "DTSTART:" + date.replace("-", "") + "T" + time.replace(":", "") + "00\n"
                + "END:VEVENT";
    }

    private static String safeName(String counselorName) {
        return counselorName == null || counselorName.isEmpty() ? "Counselor" : counselorName;
    }
}
