package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SystemAdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_admin_dashboard);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.SYSTEM_ADMIN);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.SYSTEM_ADMIN));

        View tileCollectibleMgmt = findViewById(R.id.tileCollectibleMgmt);
        ((ImageView) tileCollectibleMgmt.findViewById(R.id.actionIcon)).setImageResource(R.drawable.ic_leaf);
        ((TextView) tileCollectibleMgmt.findViewById(R.id.actionLabel)).setText("Collectible Management");
        tileCollectibleMgmt.setOnClickListener(v -> startActivity(new Intent(this, CollectibleManagementActivity.class)));

        View.OnClickListener openAudit = v -> {
            Intent intent = new Intent(this, AuditLogActivity.class);
            intent.putExtra(MyAccountActivity.EXTRA_ROLE, Roles.SYSTEM_ADMIN);
            startActivity(intent);
        };
        findViewById(R.id.latestAuditEntry).setOnClickListener(openAudit);
        findViewById(R.id.tvViewMoreAudit).setOnClickListener(openAudit);

        View.OnClickListener openErrors = v -> startActivity(new Intent(this, CollectibleErrorLogActivity.class));
        findViewById(R.id.latestErrorEntry).setOnClickListener(openErrors);
        findViewById(R.id.tvViewMoreErrors).setOnClickListener(openErrors);
    }
}
