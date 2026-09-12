package com.example.plantdex;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CreateArchitectureRecordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_architecture_record);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.RESEARCHER);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.RESEARCHER));

        EditText etLocation = findViewById(R.id.etLocation);
        View layoutLocationError = findViewById(R.id.layoutLocationError);

        findViewById(R.id.btnSaveArchRecord).setOnClickListener(v -> {
            if (TextUtils.isEmpty(etLocation.getText().toString().trim())) {
                layoutLocationError.setVisibility(View.VISIBLE);
                return;
            }
            layoutLocationError.setVisibility(View.GONE);
            Toast.makeText(this, "Architecture record saved.", Toast.LENGTH_SHORT).show();
            finish();
        });
        findViewById(R.id.btnCancelArchRecord).setOnClickListener(v -> finish());
    }
}
