package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileTypeManagementActivity extends AppCompatActivity {

    private static final String[][] TYPES = {
            {"USER ADMIN", "001"},
            {"SYSTEM ADMIN", "002"},
            {"RESEARCHER", "003"},
            {"VISITORS", "004"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_with_search);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.USER_ADMIN);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.USER_ADMIN));
        ((TextView) findViewById(R.id.tvScreenTitle)).setText("User Profile Type Management");
        findViewById(R.id.etSearch).setVisibility(View.VISIBLE);

        LinearLayout container = findViewById(R.id.listContainer);
        for (String[] type : TYPES) {
            View tile = LayoutInflater.from(this).inflate(R.layout.tile_profile_type, container, false);
            ((TextView) tile.findViewById(R.id.typeName)).setText(type[0]);
            ((TextView) tile.findViewById(R.id.typeIdLabel)).setText("Profile Type ID: " + type[1]);

            View.OnClickListener open = v -> {
                Intent intent = new Intent(this, EditProfileTypeActivity.class);
                intent.putExtra(EditProfileTypeActivity.EXTRA_TYPE_NAME, type[0]);
                intent.putExtra(EditProfileTypeActivity.EXTRA_TYPE_ID, type[1]);
                startActivity(intent);
            };
            tile.findViewById(R.id.btnEditType).setOnClickListener(open);
            tile.setOnClickListener(open);
            container.addView(tile);
        }
    }
}
