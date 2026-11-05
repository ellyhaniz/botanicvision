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
import com.example.plantdex.common.boundary.EditMyAccountActivity;
import com.example.plantdex.researcher.CreateRecordActivity;
import com.example.plantdex.researcher.RecordDetailActivity;
import com.example.plantdex.researcher.RecordsListActivity;
import com.example.plantdex.researcher.ResearcherDashboardActivity;
import com.example.plantdex.visitor.CapturePhotoActivity;
import com.example.plantdex.visitor.CollectibleCardActivity;
import com.example.plantdex.visitor.CollectibleInventoryActivity;
import com.example.plantdex.visitor.ConfirmPhotoActivity;
import com.example.plantdex.visitor.IdentificationResultActivity;
import com.example.plantdex.visitor.SearchInventoryActivity;
import com.example.plantdex.visitor.VisitorDashboardActivity;

/**
 * Inflates and wires the bottom navigation bar shared across roles
 * (the 5-icon bar, with the camera / identify shortcut).
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

        // Shade the tab of the section this screen belongs to.
        ImageButton current = null;
        if (isHomeSection(activity)) current = navHome;
        else if (activity instanceof MyAccountActivity || activity instanceof EditMyAccountActivity) current = navProfile;
        else if (isIdentifySection(activity)) current = navCamera;
        if (current != null) {
            current.setBackgroundResource(R.drawable.bg_nav_selected);
            current.setColorFilter(activity.getColor(R.color.plantdex_bg));
            current.setSelected(true);
        }

        tidyHeader(activity);
    }

    /** Dashboards, plus the collection / record screens reached from them. */
    private static boolean isHomeSection(AppCompatActivity a) {
        return a instanceof VisitorDashboardActivity || a instanceof CollectibleInventoryActivity
                || a instanceof SearchInventoryActivity || a instanceof CollectibleCardActivity
                || a instanceof ResearcherDashboardActivity || a instanceof RecordsListActivity
                || a instanceof RecordDetailActivity || a instanceof CreateRecordActivity;
    }

    /** Taking a photo, confirming it, and the identification result. */
    private static boolean isIdentifySection(AppCompatActivity a) {
        return a instanceof CapturePhotoActivity || a instanceof ConfirmPhotoActivity
                || a instanceof IdentificationResultActivity;
    }

    /**
     * Every screen's header used to say "Visitor #ID" / "Researcher #ID" over "PlantDex
     * Management". Show the logged-in username instead, without the subtitle. Some headers are
     * the shared header_role.xml and some are written out in their own layout, so this finds
     * that placeholder text wherever it appears.
     */
    private static void tidyHeader(AppCompatActivity activity) {
        String username = Session.getUsername(activity);
        java.util.Set<String> placeholders = new java.util.HashSet<>(java.util.Arrays.asList(
                activity.getString(R.string.visitor_id), activity.getString(R.string.researcher_id), "Role #ID"));
        tidyHeader(activity.findViewById(android.R.id.content), placeholders,
                username == null || username.isEmpty() ? null : username,
                activity.getString(R.string.plantdex_management));
    }

    private static void tidyHeader(View view, java.util.Set<String> placeholders, String username, String subtitle) {
        if (view instanceof android.widget.TextView && !(view instanceof android.widget.Button)) {
            android.widget.TextView tv = (android.widget.TextView) view;
            String text = tv.getText().toString();
            if (username != null && placeholders.contains(text)) tv.setText(username);
            else if (subtitle.equals(text)) tv.setVisibility(View.GONE);
        } else if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tidyHeader(group.getChildAt(i), placeholders, username, subtitle);
            }
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
