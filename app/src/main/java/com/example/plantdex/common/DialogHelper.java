package com.example.plantdex.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.plantdex.R;

/**
 * Small shared helpers for the confirmation dialogs used across every actor
 * (User Admin, System Admin, Researcher, Visitor).
 */
public final class DialogHelper {

    private DialogHelper() {}

    public interface OnConfirm {
        void onConfirm();
    }

    /** Shows the "Log out of PlantDex?" confirmation dialog used on every role's account screen. */
    public static void showLogout(Activity activity, String message, String note, OnConfirm onConfirm) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_logout, null);

        TextView tvMessage = view.findViewById(R.id.tvLogoutMessage);
        tvMessage.setText(message);

        LinearLayout layoutNote = view.findViewById(R.id.layoutLogoutNote);
        TextView tvNote = view.findViewById(R.id.tvLogoutNote);
        if (note != null && !note.isEmpty()) {
            layoutNote.setVisibility(View.VISIBLE);
            tvNote.setText(note);
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0x99000000));
        }

        view.findViewById(R.id.btnConfirmLogout).setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.onConfirm();
        });
        view.findViewById(R.id.btnCancelLogout).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /** Sends the user back to the login screen and clears the activity stack. */
    public static void logoutToLogin(Activity activity) {
        android.content.Intent intent = new android.content.Intent(activity, LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
