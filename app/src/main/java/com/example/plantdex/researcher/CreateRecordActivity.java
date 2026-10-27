package com.example.plantdex.researcher;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;

/** Single Create Record form for the Researcher role, toggled between
 *  Botanical and Architectural record types. */
public class CreateRecordActivity extends AppCompatActivity {

    private static final String EXISTING_SCIENTIFIC_NAME = "Cyrtophyllum fragrans";
    private boolean duplicateWarningAcknowledged = false;
    private boolean isBotanical = true;

    private Button toggleBotanical;
    private Button toggleArchitectural;
    private LinearLayout groupBotanical;
    private LinearLayout groupArchitecture;
    private LinearLayout warningBanner;
    private LinearLayout errorBanner;
    private TextView tvUploadLabel;

    private EditText etCommonName;
    private EditText etScientificName;
    private EditText etStructureName;
    private EditText etLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_record);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.RESEARCHER);

        View.OnClickListener comingSoon = v ->
                Toast.makeText(this, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();

        findViewById(R.id.btnProfile).setOnClickListener(comingSoon);

        toggleBotanical = findViewById(R.id.toggleBotanical);
        toggleArchitectural = findViewById(R.id.toggleArchitectural);
        groupBotanical = findViewById(R.id.groupBotanical);
        groupArchitecture = findViewById(R.id.groupArchitecture);
        warningBanner = findViewById(R.id.warningBanner);
        errorBanner = findViewById(R.id.errorBanner);
        tvUploadLabel = findViewById(R.id.tvUploadLabel);

        etCommonName = findViewById(R.id.etCommonName);
        etScientificName = findViewById(R.id.etScientificName);
        etStructureName = findViewById(R.id.etStructureName);
        etLocation = findViewById(R.id.etLocation);

        LinearLayout btnUploadImage = findViewById(R.id.btnUploadImage);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnCancel = findViewById(R.id.btnCancel);

        toggleBotanical.setOnClickListener(v -> selectType(true));
        toggleArchitectural.setOnClickListener(v -> selectType(false));
        selectType(true);

        btnUploadImage.setOnClickListener(comingSoon);

        btnSave.setOnClickListener(v -> {
            if (isBotanical) {
                saveBotanical();
            } else {
                saveArchitectural();
            }
        });

        btnCancel.setOnClickListener(v -> finish());
    }

    private void selectType(boolean botanical) {
        isBotanical = botanical;

        groupBotanical.setVisibility(botanical ? View.VISIBLE : View.GONE);
        groupArchitecture.setVisibility(botanical ? View.GONE : View.VISIBLE);
        warningBanner.setVisibility(View.GONE);
        errorBanner.setVisibility(View.GONE);
        tvUploadLabel.setText(botanical ? "Upload plant image" : "Upload structure image");

        toggleBotanical.setBackgroundResource(botanical ? R.drawable.btn_primary : R.drawable.bg_toggle_inactive);
        toggleBotanical.setTextColor(ContextCompat.getColor(this, botanical ? R.color.plantdex_button_text : R.color.plantdex_secondary_text));

        toggleArchitectural.setBackgroundResource(botanical ? R.drawable.bg_toggle_inactive : R.drawable.btn_primary);
        toggleArchitectural.setTextColor(ContextCompat.getColor(this, botanical ? R.color.plantdex_secondary_text : R.color.plantdex_button_text));
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
