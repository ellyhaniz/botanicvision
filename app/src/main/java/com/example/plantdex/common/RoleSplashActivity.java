package com.example.plantdex.common;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;

/**
 * Brief branded splash shown right after a successful login, naming the
 * account type that was selected on the login screen, before handing off
 * to that role's dashboard.
 */
public class RoleSplashActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "extra_role";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_splash);

        String role = getIntent().getStringExtra(EXTRA_ROLE);
        if (role == null) role = Roles.VISITOR;

        TextView tvRoleTitle = findViewById(R.id.tvRoleTitle);
        tvRoleTitle.setText(role.equals(Roles.VISITOR) ? "VISITORS" : role);

        final String finalRole = role;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(RoleSplashActivity.this, Roles.dashboardFor(finalRole));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 1100);
    }
}
