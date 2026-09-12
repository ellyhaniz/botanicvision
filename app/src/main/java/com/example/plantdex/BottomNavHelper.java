package com.example.plantdex;

import android.content.Intent;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Inflates and wires the bottom navigation bar shared by every actor's
 * screens. Visitor gets the 5-icon bar (with the camera / identify
 * shortcut); every other role gets the 4-icon bar.
 */
public final class BottomNavHelper {

    private BottomNavHelper() {}

    public static void setup(AppCompatActivity activity, FrameLayout container, String role) {
        if (container == null) return;

        int layoutRes = Roles.hasCameraTab(role) ? R.layout.bottom_nav_5 : R.layout.bottom_nav_4;
        android.view.View navView = LayoutInflater.from(activity).inflate(layoutRes, container, true);

        ImageButton navHome = navView.findViewById(R.id.navHome);
        ImageButton navProfile = navView.findViewById(R.id.navProfile);
        ImageButton navNotifications = navView.findViewById(R.id.navNotifications);
        ImageButton navLogout = navView.findViewById(R.id.navLogout);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(activity, Roles.dashboardFor(role));
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(activity, MyAccountActivity.class);
            intent.putExtra(MyAccountActivity.EXTRA_ROLE, role);
            activity.startActivity(intent);
        });

        navNotifications.setOnClickListener(v ->
                Toast.makeText(activity, "You're all caught up — no new notifications.", Toast.LENGTH_SHORT).show());

        navLogout.setOnClickListener(v -> showLogoutDialog(activity, role));

        if (Roles.hasCameraTab(role)) {
            ImageButton navCamera = navView.findViewById(R.id.navCamera);
            navCamera.setOnClickListener(v -> activity.startActivity(new Intent(activity, CapturePhotoActivity.class)));
        }
    }

    public static void showLogoutDialog(AppCompatActivity activity, String role) {
        String message;
        String note = null;

        switch (role) {
            case Roles.USER_ADMIN:
                message = "Your admin session will end and you will return to the login page.";
                note = "This log out will be recorded in the Audit Log against User Admin #ID.";
                break;
            case Roles.SYSTEM_ADMIN:
                message = "Your admin session will end and you will return to the login page.";
                note = "Administration functions will not be accessible until System Admin #ID logs in again.";
                break;
            case Roles.RESEARCHER:
                message = "Your session will end. Your collectibles and discovery history stay saved.";
                break;
            default:
                message = "Your session will end. Your collectibles and discovery history stay saved.";
        }

        DialogHelper.showLogout(activity, message, note, () -> DialogHelper.logoutToLogin(activity));
    }
}
