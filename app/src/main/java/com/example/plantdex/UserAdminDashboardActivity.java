package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class UserAdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_admin_dashboard);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.USER_ADMIN);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.USER_ADMIN));

        View tileProfileType = findViewById(R.id.tileProfileType);
        ((ImageView) tileProfileType.findViewById(R.id.actionIcon)).setImageResource(R.drawable.ic_person);
        ((TextView) tileProfileType.findViewById(R.id.actionLabel)).setText("Profile Type Management");
        tileProfileType.setOnClickListener(v -> startActivity(new Intent(this, ProfileTypeManagementActivity.class)));

        View tileUserAccount = findViewById(R.id.tileUserAccount);
        ((ImageView) tileUserAccount.findViewById(R.id.actionIcon)).setImageResource(R.drawable.ic_person);
        ((TextView) tileUserAccount.findViewById(R.id.actionLabel)).setText("User Account Management");
        tileUserAccount.setOnClickListener(v -> startActivity(new Intent(this, UserAccountManagementActivity.class)));

        View.OnClickListener auditEntryClick = v -> {
            Intent intent = new Intent(this, AuditLogActivity.class);
            intent.putExtra(MyAccountActivity.EXTRA_ROLE, Roles.USER_ADMIN);
            startActivity(intent);
        };
        findViewById(R.id.latestAuditEntry).setOnClickListener(auditEntryClick);
        findViewById(R.id.tvViewMoreAudit).setOnClickListener(auditEntryClick);
    }
}
