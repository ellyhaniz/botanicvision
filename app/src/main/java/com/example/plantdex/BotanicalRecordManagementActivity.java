package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class BotanicalRecordManagementActivity extends AppCompatActivity {

    private static final String[][] RECORDS = {
            {"Tembusu", "Cyrtophyllum fragrans", "0114", "Gentianaceae"},
            {"Vanda Miss Joaquim", "Papilionanda Miss Joaquim", "0102", "Orchidaceae"},
            {"Rain Tree", "Samanea saman", "0098", "Fabaceae"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_with_search);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.RESEARCHER);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.RESEARCHER));
        ((TextView) findViewById(R.id.tvScreenTitle)).setText("Botanical Records");

        android.widget.Button btnCreateNew = findViewById(R.id.btnCreateNew);
        btnCreateNew.setVisibility(View.VISIBLE);
        btnCreateNew.setText("+ Create botanical record");
        btnCreateNew.setOnClickListener(v -> startActivity(new Intent(this, CreateBotanicalRecordActivity.class)));

        LinearLayout container = findViewById(R.id.listContainer);
        for (String[] rec : RECORDS) {
            View tile = LayoutInflater.from(this).inflate(R.layout.tile_botanical_record, container, false);
            ((TextView) tile.findViewById(R.id.recName)).setText(rec[0]);
            ((TextView) tile.findViewById(R.id.recSubtitle)).setText(rec[1]);
            ((TextView) tile.findViewById(R.id.recId)).setText("#" + rec[2]);
            ((TextView) tile.findViewById(R.id.recField2)).setText(rec[3]);
            tile.findViewById(R.id.btnEditRec).setOnClickListener(v ->
                    startActivity(new Intent(this, CreateBotanicalRecordActivity.class)));
            container.addView(tile);
        }
    }
}
