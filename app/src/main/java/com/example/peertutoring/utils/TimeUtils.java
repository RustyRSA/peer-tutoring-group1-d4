package com.example.peertutoring.utils;

import android.text.format.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Turns the backend's ISO timestamps ("2026-10-22T07:40:04.123Z") into text like "5 minutes ago". */
public final class TimeUtils {

    private TimeUtils() {}

    public static String timeAgo(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) return "";
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = format.parse(isoTimestamp);
            if (date == null) return "";
            return DateUtils.getRelativeTimeSpanString(date.getTime(), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS).toString();
        } catch (ParseException e) {
            return "";
        }
    }
}
