package com.example.peertutoring.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ValidationUtils {

    /** Same rule as the backend: starts with a letter, then letters, spaces, apostrophes, dots or hyphens. */
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\p{L}[\\p{L}'\u2019 .-]*$");
    private static final int MAX_NAME_LENGTH = 50;

    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isPasswordValid(String password) {
        return !TextUtils.isEmpty(password) && password.length() >= 6;
    }

    public static boolean arePasswordsMatching(String password, String confirmPassword) {
        return password.equals(confirmPassword);
    }

    /** A first or last name: letters only (plus spaces, apostrophes, dots, hyphens), up to 50 characters. */
    public static boolean isValidName(String name) {
        return name != null && name.length() <= MAX_NAME_LENGTH && NAME_PATTERN.matcher(name).matches();
    }

    /** "ITMDA, Programming" becomes ["ITMDA", "Programming"]; blanks are dropped. */
    public static List<String> splitList(String text) {
        List<String> items = new ArrayList<>();
        if (text == null) return items;
        for (String part : text.split(",")) {
            String item = part.trim();
            if (!item.isEmpty()) items.add(item);
        }
        return items;
    }

    /** Null-safe, trimmed text of any TextView/EditText. */
    public static String text(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    /** Text exactly as typed. Used for passwords, where spaces count. */
    public static String rawText(TextView view) {
        return view.getText() == null ? "" : view.getText().toString();
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
