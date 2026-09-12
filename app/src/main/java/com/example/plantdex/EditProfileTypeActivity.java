package com.example.plantdex;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditProfileTypeActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE_NAME = "extra_type_name";
    public static final String EXTRA_TYPE_ID = "extra_type_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile_type);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.USER_ADMIN);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.USER_ADMIN));

        String name = getIntent().getStringExtra(EXTRA_TYPE_NAME);
        String id = getIntent().getStringExtra(EXTRA_TYPE_ID);
        if (name == null) name = "Researcher";
        if (id == null) id = "003";

        String titleCase = name.charAt(0) + name.substring(1).toLowerCase();
        ((TextView) findViewById(R.id.tvTypeTitle)).setText(titleCase);
        ((TextView) findViewById(R.id.tvTypeName)).setText("Profile Type Name: " + titleCase);

        findViewById(R.id.btnUpdateType).setOnClickListener(v -> {
            Toast.makeText(this, "Profile type updated.", Toast.LENGTH_SHORT).show();
            finish();
        });
        findViewById(R.id.btnCancelType).setOnClickListener(v -> finish());
    }
}
