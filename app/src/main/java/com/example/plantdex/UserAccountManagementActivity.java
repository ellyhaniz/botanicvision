package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class UserAccountManagementActivity extends AppCompatActivity {

    private static final String[][] ACCOUNTS = {
            {"John Doe", "U-10428", "jdoe_042", "Visitor"},
            {"Jane Smith", "U-10429", "jsmith_118", "Researcher"},
            {"Bob Miller", "U-10430", "bmiller_076", "Visitor"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_with_search);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.USER_ADMIN);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.USER_ADMIN));
        ((TextView) findViewById(R.id.tvScreenTitle)).setText("User Account Management");

        LinearLayout container = findViewById(R.id.listContainer);
        for (String[] acc : ACCOUNTS) {
            View tile = LayoutInflater.from(this).inflate(R.layout.tile_user_account, container, false);
            ((TextView) tile.findViewById(R.id.accName)).setText(acc[0]);

            View.OnClickListener open = v -> {
                Intent intent = new Intent(this, EditAccountDetailsActivity.class);
                intent.putExtra(EditAccountDetailsActivity.EXTRA_NAME, acc[0]);
                intent.putExtra(EditAccountDetailsActivity.EXTRA_USER_ID, acc[1]);
                intent.putExtra(EditAccountDetailsActivity.EXTRA_USERNAME, acc[2]);
                intent.putExtra(EditAccountDetailsActivity.EXTRA_PROFILE_TYPE, acc[3]);
                startActivity(intent);
            };
            tile.findViewById(R.id.btnEditAcc).setOnClickListener(open);
            tile.setOnClickListener(open);
            container.addView(tile);
        }
    }
}
