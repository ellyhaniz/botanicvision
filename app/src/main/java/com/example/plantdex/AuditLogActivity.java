package com.example.plantdex;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Shared "Audit Logs" list screen used by both User Admin and System Admin. */
public class AuditLogActivity extends AppCompatActivity {

    private static final String[][] ENTRIES = {
            {"John Doe", "03 Dec 2026 11:10:05 AM", "#101578"},
            {"Jane Smith", "05 Dec 2026 14:13:09 PM", "#101577"},
            {"Bob Miller", "07 Dec 2026 18:14:05 PM", "#101576"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_log);

        String role = getIntent().getStringExtra(MyAccountActivity.EXTRA_ROLE);
        if (role == null) role = Roles.USER_ADMIN;

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), role);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(role));

        LinearLayout container = findViewById(R.id.entriesContainer);
        for (String[] entry : ENTRIES) {
            View tile = LayoutInflater.from(this).inflate(R.layout.tile_audit_entry, container, false);
            ((TextView) tile.findViewById(R.id.entryName)).setText(entry[0]);
            ((TextView) tile.findViewById(R.id.entryTimestamp)).setText(entry[1]);
            ((TextView) tile.findViewById(R.id.entryRef)).setText(entry[2]);
            container.addView(tile);
        }
    }
}
