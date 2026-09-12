package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ResearcherAuditLogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_researcher_audit_log);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.RESEARCHER);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.RESEARCHER));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tabBotanical).setOnClickListener(v ->
                startActivity(new Intent(this, BotanicalRecordManagementActivity.class)));
        findViewById(R.id.tabArchitecture).setOnClickListener(v ->
                startActivity(new Intent(this, ArchitectureRecordManagementActivity.class)));

        LinearLayout container = findViewById(R.id.entriesContainer);
        View tile = LayoutInflater.from(this).inflate(R.layout.tile_researcher_audit_entry, container, false);
        tile.findViewById(R.id.raActionBox).setVisibility(View.VISIBLE);
        container.addView(tile);
    }
}
