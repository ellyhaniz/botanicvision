package com.example.plantdex;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CreateBotanicalRecordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_botanical_record);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.RESEARCHER);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.RESEARCHER));

        findViewById(R.id.btnSaveRecord).setOnClickListener(v -> {
            Toast.makeText(this, "Botanical record saved.", Toast.LENGTH_SHORT).show();
            finish();
        });
        findViewById(R.id.btnCancelRecord).setOnClickListener(v -> finish());
    }
}
