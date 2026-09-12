package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ArchitectureRecordManagementActivity extends AppCompatActivity {

    private static final String[][] RECORDS = {
            {"Bandstand", "0198", "Bandstand Hill", "Victorian", "1930"},
            {"Swan Lake Gazebo", "0176", "Swan Lake", "Cast iron pavilion", "1859"},
            {"Tanglin Gate", "0151", "Tanglin Core", "Colonial", "1868"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_with_search);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.RESEARCHER);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.RESEARCHER));
        ((TextView) findViewById(R.id.tvScreenTitle)).setText("Architecture Records");

        android.widget.Button btnCreateNew = findViewById(R.id.btnCreateNew);
        btnCreateNew.setVisibility(View.VISIBLE);
        btnCreateNew.setText("+ Create architecture record");
        btnCreateNew.setOnClickListener(v -> startActivity(new Intent(this, CreateArchitectureRecordActivity.class)));

        LinearLayout container = findViewById(R.id.listContainer);
        for (String[] rec : RECORDS) {
            View tile = LayoutInflater.from(this).inflate(R.layout.tile_architecture_record, container, false);
            ((TextView) tile.findViewById(R.id.archName)).setText(rec[0]);
            ((TextView) tile.findViewById(R.id.archSubtitle)).setText("Record #" + rec[1]);
            ((TextView) tile.findViewById(R.id.archLocation)).setText(rec[2]);
            ((TextView) tile.findViewById(R.id.archStyle)).setText(rec[3]);
            ((TextView) tile.findViewById(R.id.archConstructed)).setText(rec[4]);
            tile.findViewById(R.id.btnEditArch).setOnClickListener(v ->
                    startActivity(new Intent(this, CreateArchitectureRecordActivity.class)));
            container.addView(tile);
        }
    }
}
