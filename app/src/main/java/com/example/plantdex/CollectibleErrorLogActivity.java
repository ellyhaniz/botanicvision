package com.example.plantdex;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CollectibleErrorLogActivity extends AppCompatActivity {

    private static final String[][] ERRORS = {
            {"Failed card unlock", "07 Dec 2026, 18:14:05", "#ERR198", "#0087 Tembusu Tree", "jtan_042", "Unresolved"},
            {"Handbook sync failed", "05 Dec 2026, 14:13:09", "#ERR197", "#0042 Bandstand", "jsmith_118", "Resolved"},
            {"Failed card unlock", "03 Dec 2026, 11:10:05", "#ERR196", "#0055 Vanda Miss Joaquim", "bmiller_076", "Unresolved"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_log);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.SYSTEM_ADMIN);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.SYSTEM_ADMIN));
        ((TextView) findViewById(R.id.tvScreenTitle)).setText("Collectible Error Log");

        LinearLayout container = findViewById(R.id.entriesContainer);
        for (String[] err : ERRORS) {
            View tile = LayoutInflater.from(this).inflate(R.layout.tile_error_log_entry, container, false);
            ((TextView) tile.findViewById(R.id.errTitle)).setText(err[0]);
            ((TextView) tile.findViewById(R.id.errTimestamp)).setText(err[1]);
            ((TextView) tile.findViewById(R.id.errRef)).setText(err[2]);
            ((TextView) tile.findViewById(R.id.errCollectible)).setText(err[3]);
            ((TextView) tile.findViewById(R.id.errUser)).setText(err[4]);

            TextView status = tile.findViewById(R.id.errStatus);
            status.setText(err[5]);
            if ("Resolved".equals(err[5])) {
                status.setBackgroundResource(R.drawable.bg_pill);
                status.setTextColor(getColor(R.color.plantdex_success_text));
                tile.findViewById(R.id.errIcon).setImageResource(R.drawable.ic_sync_fail);
            }

            container.addView(tile);
        }
    }
}
