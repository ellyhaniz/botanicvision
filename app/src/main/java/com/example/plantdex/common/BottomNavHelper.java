package com.example.plantdex.common;

import android.content.Intent;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.visitor.VisitorDashboardActivity;

/**
 * Inflates and wires the bottom navigation bar for the Visitor screens.
 * Profile and Camera are shown for layout fidelity but are not wired to
 * a screen yet — those flows are out of scope for now.
 */
public final class BottomNavHelper {

    private BottomNavHelper() {}

    public static void setup(AppCompatActivity activity, FrameLayout container, String role) {
        if (container == null) return;

        android.view.View navView = LayoutInflater.from(activity).inflate(R.layout.bottom_nav_5, container, true);

        ImageButton navHome = navView.findViewById(R.id.navHome);
        ImageButton navProfile = navView.findViewById(R.id.navProfile);
        ImageButton navNotifications = navView.findViewById(R.id.navNotifications);
        ImageButton navCamera = navView.findViewById(R.id.navCamera);
        ImageButton navLogout = navView.findViewById(R.id.navLogout);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(activity, VisitorDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
        });

        navProfile.setOnClickListener(v ->
                Toast.makeText(activity, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show());

        navNotifications.setOnClickListener(v ->
                Toast.makeText(activity, "You're all caught up — no new notifications.", Toast.LENGTH_SHORT).show());

        navCamera.setOnClickListener(v ->
                Toast.makeText(activity, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show());

        navLogout.setOnClickListener(v -> showLogoutDialog(activity));
    }

    public static void showLogoutDialog(AppCompatActivity activity) {
        String message = "Your session will end. Your collectibles and discovery history stay saved.";
        DialogHelper.showLogout(activity, message, null, () -> DialogHelper.logoutToLogin(activity));
    }
}
