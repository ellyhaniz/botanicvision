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

public class CreateBotanicalRecordActivity extends AppCompatActivity {

    private static final String EXISTING_SCIENTIFIC_NAME = "Cyrtophyllum fragrans";
    private boolean duplicateWarningAcknowledged = false;

    private LinearLayout warningBanner;
    private EditText etCommonName;
    private EditText etScientificName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_botanical_record);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.RESEARCHER);

        View.OnClickListener comingSoon = v ->
                Toast.makeText(this, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();

        findViewById(R.id.btnProfile).setOnClickListener(comingSoon);

        warningBanner = findViewById(R.id.warningBanner);
        etCommonName = findViewById(R.id.etCommonName);
        etScientificName = findViewById(R.id.etScientificName);

        LinearLayout btnUploadImage = findViewById(R.id.btnUploadImage);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnCancel = findViewById(R.id.btnCancel);

        btnUploadImage.setOnClickListener(comingSoon);

        btnSave.setOnClickListener(v -> saveBotanical());

        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveBotanical() {
        String commonName = etCommonName.getText().toString().trim();
        String scientificName = etScientificName.getText().toString().trim();

        if (TextUtils.isEmpty(commonName) || TextUtils.isEmpty(scientificName)) {
            Toast.makeText(this, "Please fill in the required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!duplicateWarningAcknowledged && scientificName.equalsIgnoreCase(EXISTING_SCIENTIFIC_NAME)) {
            warningBanner.setVisibility(View.VISIBLE);
            duplicateWarningAcknowledged = true;
            Toast.makeText(this, "Tap Save again to continue anyway.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Botanical record saved.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
