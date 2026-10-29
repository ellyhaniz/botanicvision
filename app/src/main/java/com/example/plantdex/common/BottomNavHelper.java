package com.example.plantdex.common;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.boundary.MyAccountActivity;
import com.example.plantdex.common.control.AuthController;
import com.example.plantdex.visitor.CapturePhotoActivity;

/**
 * Inflates and wires the bottom navigation bar shown on every Visitor
 * screen (the 5-icon bar, with the camera / identify shortcut).
 */
public final class BottomNavHelper {

    private BottomNavHelper() {}

    public static void setup(AppCompatActivity activity, FrameLayout container, String role) {
        if (container == null) return;

        android.view.View navView = LayoutInflater.from(activity).inflate(R.layout.bottom_nav_5, container, true);

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

        ImageButton navCamera = navView.findViewById(R.id.navCamera);
        navCamera.setOnClickListener(v -> activity.startActivity(new Intent(activity, CapturePhotoActivity.class)));

        // The header avatar (header_role.xml's btnProfile) is on every screen that
        // has a header — wired here, once, instead of separately in each Activity.
        View btnProfile = activity.findViewById(R.id.btnProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                Intent intent = new Intent(activity, MyAccountActivity.class);
                intent.putExtra(MyAccountActivity.EXTRA_ROLE, role);
                activity.startActivity(intent);
            });
        }
    }

    /** Shows the "are you sure?" confirmation, then actually logs the account out once confirmed. */
    public static void showLogoutDialog(AppCompatActivity activity, String role) {
        String message = "Your session will end. Your collectibles and discovery history stay saved.";

        DialogHelper.showLogout(activity, message, null, () -> {
            int userId = Session.getUserId(activity);
            AuthController.logout(activity, userId, new AuthController.SimpleCallback() {
                @Override
                public void onSuccess() {
                    DialogHelper.logoutToLogin(activity);
                }

                @Override
                public void onError(String message) {
                    // Even if the server call failed (e.g. no network), still
                    // send them back to Login — getting stuck on a dead
                    // screen would be worse than a logout that didn't reach
                    // the audit log.
                    DialogHelper.logoutToLogin(activity);
                }
            });
        });
    }
}
