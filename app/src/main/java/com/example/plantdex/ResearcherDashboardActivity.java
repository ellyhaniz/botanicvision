package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ResearcherDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_researcher_dashboard);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.RESEARCHER);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.RESEARCHER));

        findViewById(R.id.tileBotanical).setOnClickListener(v ->
                startActivity(new Intent(this, BotanicalRecordManagementActivity.class)));
        findViewById(R.id.tileArchitecture).setOnClickListener(v ->
                startActivity(new Intent(this, ArchitectureRecordManagementActivity.class)));

        View.OnClickListener openAudit = v -> startActivity(new Intent(this, ResearcherAuditLogActivity.class));
        findViewById(R.id.latestAuditEntry).setOnClickListener(openAudit);
        findViewById(R.id.tvViewMoreAudit).setOnClickListener(openAudit);
    }
}
