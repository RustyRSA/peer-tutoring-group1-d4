package com.example.peertutoring.utils;

import android.app.Activity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.peertutoring.R;

public class BottomNavHelper {

    public static void updateSelection(Activity activity, int selectedNavId) {
        int[] navIds = {
                R.id.btnHome, R.id.btnSearch, R.id.btnMessages,
                R.id.btnProfile, R.id.btnAdmin
        };

        for (int id : navIds) {
            LinearLayout navItem = activity.findViewById(id);
            ImageView icon = (ImageView) navItem.getChildAt(0);
            TextView text = (TextView) navItem.getChildAt(1);

            if (id == selectedNavId) {
                icon.setColorFilter(ContextCompat.getColor(activity, R.color.brand_blue));
                text.setTextColor(ContextCompat.getColor(activity, R.color.brand_blue));
                text.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                icon.setColorFilter(ContextCompat.getColor(activity, R.color.text_secondary));
                text.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary));
                text.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }
}