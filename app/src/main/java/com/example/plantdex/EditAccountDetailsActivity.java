package com.example.plantdex;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditAccountDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_USERNAME = "extra_username";
    public static final String EXTRA_PROFILE_TYPE = "extra_profile_type";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_account_details);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.USER_ADMIN);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.USER_ADMIN));

        String name = getIntent().getStringExtra(EXTRA_NAME);
        String userId = getIntent().getStringExtra(EXTRA_USER_ID);
        String username = getIntent().getStringExtra(EXTRA_USERNAME);
        String profileType = getIntent().getStringExtra(EXTRA_PROFILE_TYPE);
        if (name == null) name = "John Doe";
        if (userId == null) userId = "U-10428";
        if (username == null) username = "jdoe_042";
        if (profileType == null) profileType = "Visitor";

        ((TextView) findViewById(R.id.tvEditAccName)).setText(name);
        setRow(R.id.rowUserId, "User ID:", "#" + userId);
        setRow(R.id.rowProfileTypeId, "Profile Type ID:", "004");
        setRow(R.id.rowProfileType, "User Profile Type:", profileType);
        setRow(R.id.rowUsername, "Username:", username);
        setRow(R.id.rowFullName, "Full Name:", name);
        setRow(R.id.rowEmail, "Email:", username + "@mail.com");
        setRow(R.id.rowPassword, "Password:", "••••••••");
        setRow(R.id.rowCreationDate, "Creation Date:", "14 Sep 2026");

        findViewById(R.id.btnUpdateAccount).setOnClickListener(v -> {
            Toast.makeText(this, "Account updated.", Toast.LENGTH_SHORT).show();
            finish();
        });
        findViewById(R.id.btnCancelAccount).setOnClickListener(v -> finish());
    }

    private void setRow(int includeId, String label, String value) {
        android.view.View row = findViewById(includeId);
        ((TextView) row.findViewById(R.id.rowLabel)).setText(label);
        ((TextView) row.findViewById(R.id.rowValue)).setText(value);
    }
}
