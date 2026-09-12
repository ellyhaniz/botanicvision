package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MyAccountActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "extra_role";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);

        String role = getIntent().getStringExtra(EXTRA_ROLE);
        if (role == null) role = Roles.VISITOR;
        final String finalRole = role;

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), role);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(role));

        TextView tvUsername = findViewById(R.id.tvAccountUsername);
        TextView tvRole = findViewById(R.id.tvAccountRole);
        TextView tvUserId = findViewById(R.id.tvAccountUserId);
        TextView tvEmail = findViewById(R.id.tvAccountEmail);
        TextView tvProfileType = findViewById(R.id.tvAccountProfileType);
        TextView tvMemberSince = findViewById(R.id.tvAccountMemberSince);

        switch (role) {
            case Roles.RESEARCHER:
                tvUsername.setText("RFan_042");
                tvRole.setText("Researcher");
                tvUserId.setText("#R-2312B");
                tvEmail.setText("RFan042@mail.com");
                tvProfileType.setText("Researcher");
                break;
            case Roles.USER_ADMIN:
                tvUsername.setText("jtan_042");
                tvRole.setText("User Admin");
                tvUserId.setText("#A-1042");
                tvEmail.setText("jtan042@mail.com");
                tvProfileType.setText("User Admin");
                break;
            case Roles.SYSTEM_ADMIN:
                tvUsername.setText("jtan_042");
                tvRole.setText("System Admin");
                tvUserId.setText("#A-1042");
                tvEmail.setText("jtan042@mail.com");
                tvProfileType.setText("System Admin");
                break;
            default:
                tvUsername.setText("jtan_042");
                tvRole.setText("Visitor");
                tvUserId.setText("#U-10428");
                tvEmail.setText("jtan042@mail.com");
                tvProfileType.setText("Visitor");
        }
        tvMemberSince.setText("14 Sep 2026");

        findViewById(R.id.groupDiscoveryProgress).setVisibility(
                Roles.VISITOR.equals(role) ? View.VISIBLE : View.GONE);

        findViewById(R.id.btnEditAccount).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditMyAccountActivity.class);
            intent.putExtra(EXTRA_ROLE, finalRole);
            startActivity(intent);
        });

        findViewById(R.id.btnLogOut).setOnClickListener(v -> BottomNavHelper.showLogoutDialog(this, finalRole));
    }
}
