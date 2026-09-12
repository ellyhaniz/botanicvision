package com.example.plantdex;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CreateCollectibleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_collectible);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.SYSTEM_ADMIN);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.SYSTEM_ADMIN));

        View rarityError = findViewById(R.id.layoutRarityError);
        TextView tvRarity = findViewById(R.id.tvRarityValue);

        findViewById(R.id.btnSaveCollectible).setOnClickListener(v -> {
            if ("Select".contentEquals(tvRarity.getText())) {
                rarityError.setVisibility(View.VISIBLE);
                return;
            }
            rarityError.setVisibility(View.GONE);
            Toast.makeText(this, "Collectible saved.", Toast.LENGTH_SHORT).show();
            finish();
        });
        findViewById(R.id.btnCancelCollectible).setOnClickListener(v -> finish());
    }
}
