package com.example.plantdex.researcher;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;

public class CreateArchitecturalRecordActivity extends AppCompatActivity {

    private LinearLayout errorBanner;
    private EditText etStructureName;
    private EditText etLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_architectural_record);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.RESEARCHER);

        View.OnClickListener comingSoon = v ->
                Toast.makeText(this, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();

        findViewById(R.id.btnProfile).setOnClickListener(comingSoon);

        errorBanner = findViewById(R.id.errorBanner);
        etStructureName = findViewById(R.id.etStructureName);
        etLocation = findViewById(R.id.etLocation);

        LinearLayout btnUploadImage = findViewById(R.id.btnUploadImage);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnCancel = findViewById(R.id.btnCancel);

        btnUploadImage.setOnClickListener(comingSoon);

        btnSave.setOnClickListener(v -> saveArchitectural());

        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveArchitectural() {
        String structureName = etStructureName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (TextUtils.isEmpty(location)) {
            errorBanner.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Please enter a location.", Toast.LENGTH_SHORT).show();
            return;
        }
        errorBanner.setVisibility(View.GONE);

        if (TextUtils.isEmpty(structureName)) {
            Toast.makeText(this, "Please enter a structure name.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Architecture record saved.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
