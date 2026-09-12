package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class IdentificationResultActivity extends AppCompatActivity {

    public static final String EXTRA_MATCHED = "extra_matched";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identification_result);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.VISITOR));

        boolean matched = getIntent().getBooleanExtra(EXTRA_MATCHED, true);

        findViewById(R.id.bannerNoMatch).setVisibility(matched ? View.GONE : View.VISIBLE);
        findViewById(R.id.bannerUnlocked).setVisibility(matched ? View.VISIBLE : View.GONE);
        findViewById(R.id.groupNoMatch).setVisibility(matched ? View.GONE : View.VISIBLE);
        findViewById(R.id.groupUnlocked).setVisibility(matched ? View.VISIBLE : View.GONE);

        TextView btnPrimary = findViewById(R.id.btnPrimaryAction);
        TextView btnSecondary = findViewById(R.id.btnSecondaryAction);

        if (matched) {
            btnPrimary.setText("🔍 More info");
            btnSecondary.setText("⇗ Share");
            btnPrimary.setOnClickListener(v ->
                    VisitorSheets.showSearchMoreInfo(this, "Tembusu Tree Singapore Botanic Gardens"));
            btnSecondary.setOnClickListener(v ->
                    VisitorSheets.showShareDiscovery(this, "Tembusu Tree", "Rare", 18, 60));
        } else {
            btnPrimary.setText("↻ Retake");
            btnSecondary.setText("Search manually");
            btnPrimary.setOnClickListener(v -> {
                startActivity(new Intent(this, CapturePhotoActivity.class));
                finish();
            });
            btnSecondary.setOnClickListener(v -> VisitorSheets.showSearchMoreInfo(this, ""));
        }
    }
}
