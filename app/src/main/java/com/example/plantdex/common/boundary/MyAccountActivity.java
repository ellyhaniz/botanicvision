package com.example.plantdex.common.boundary;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;
import com.example.plantdex.common.Session;

/**
 * Note:
 * "Edit Account" hands off to EditMyAccountActivity for the parts that are
 * actually editable (full name and email; username and role are fixed).
 */

public class MyAccountActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "extra_role";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);

        String role = getIntent().getStringExtra(EXTRA_ROLE);
        if (role == null) role = Session.getRole(this);
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

        String username = Session.getUsername(this);
        int userId = Session.getUserId(this);
        String email = Session.getEmail(this);
        String memberSince = Session.getMemberSince(this);
        String roleDisplay = toTitleCase(role);

        tvUsername.setText(username != null ? username : "—");
        tvRole.setText(roleDisplay);
        tvUserId.setText(userId > 0 ? "#U-" + userId : "—");
        tvEmail.setText(email != null && !email.isEmpty() ? email : "—");
        tvProfileType.setText(roleDisplay);
        tvMemberSince.setText(memberSince != null && !memberSince.isEmpty() ? memberSince : "—");

        findViewById(R.id.groupDiscoveryProgress).setVisibility(
                Roles.VISITOR.equals(role) ? View.VISIBLE : View.GONE);

        findViewById(R.id.btnEditAccount).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditMyAccountActivity.class);
            intent.putExtra(EXTRA_ROLE, finalRole);
            startActivity(intent);
        });

        findViewById(R.id.btnLogOut).setOnClickListener(v -> BottomNavHelper.showLogoutDialog(this, finalRole));
    }

    /** "USER ADMIN" */
    private static String toTitleCase(String role) {
        if (role == null || role.isEmpty()) return "Visitor";
        String[] words = role.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }
}
