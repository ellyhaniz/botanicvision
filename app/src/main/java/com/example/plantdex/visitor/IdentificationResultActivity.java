package com.example.plantdex.visitor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;
import com.example.plantdex.common.entity.Collectible;

//  display not identifly image using users live photo
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.widget.ImageView;
import java.io.IOException;

// Search  manually import
import android.content.ActivityNotFoundException;
import android.widget.Toast;


/** #VS-07 — shows the object's key details after a (mock) identification. */
public class IdentificationResultActivity extends AppCompatActivity {

    private static final String EXTRA_MATCHED = "extra_matched";
    private static final String EXTRA_SUBMITTED_AT = "extra_submitted_at";
    private static final String EXTRA_ID = "extra_id";
    private static final String EXTRA_NAME = "extra_name";
    private static final String EXTRA_CATEGORY = "extra_category";
    private static final String EXTRA_RARITY = "extra_rarity";
    private static final String EXTRA_SCIENTIFIC = "extra_scientific";
    private static final String EXTRA_FEATURE = "extra_feature";
    private static final String EXTRA_FUNFACT = "extra_funfact";

    // identification result plantnet API
    private static final String EXTRA_PLANTNET_SUGGESTION = "extra_plantnet_suggestion";
    private static final String EXTRA_PHOTO_URI = "extra_photo_uri";
    private static final String EXTRA_SCORE = "extra_score";

    public static Intent matched(Context context, Collectible c, String submittedAt) {
        Intent intent = new Intent(context, IdentificationResultActivity.class);
        intent.putExtra(EXTRA_MATCHED, true);
        intent.putExtra(EXTRA_SUBMITTED_AT, submittedAt);
        intent.putExtra(EXTRA_ID, c.getId());
        intent.putExtra(EXTRA_NAME, c.getName());
        intent.putExtra(EXTRA_CATEGORY, c.getCategory());
        intent.putExtra(EXTRA_RARITY, c.getRarity());
        intent.putExtra(EXTRA_SCIENTIFIC, c.getScientificName());
        intent.putExtra(EXTRA_FEATURE, c.getFeature());
        intent.putExtra(EXTRA_FUNFACT, c.getFunFact());
        return intent;


    }

    public static Intent noMatch(Context context, String submittedAt, String photoUri) {
        Intent intent = new Intent(context, IdentificationResultActivity.class);
        intent.putExtra(EXTRA_MATCHED, false);
        intent.putExtra(EXTRA_SUBMITTED_AT, submittedAt);
        intent.putExtra(EXTRA_PHOTO_URI, photoUri);
        return intent;
    }
    public static Intent suggestion(Context context, String displayName,
                                    String scientificName, double score,
                                    String photoUri, String submittedAt) {
        Intent intent = new Intent(context, IdentificationResultActivity.class);
        intent.putExtra(EXTRA_PLANTNET_SUGGESTION, true);
        intent.putExtra(EXTRA_NAME, displayName);
        intent.putExtra(EXTRA_SCIENTIFIC, scientificName);
        intent.putExtra(EXTRA_SCORE, score);
        intent.putExtra(EXTRA_PHOTO_URI, photoUri);
        intent.putExtra(EXTRA_SUBMITTED_AT, submittedAt);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identification_result);

        //display live photo
        showSubmittedPhoto();

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.VISITOR));

        if (getIntent().getBooleanExtra(EXTRA_PLANTNET_SUGGESTION, false)) {
            showSuggestion();
            return;
        }

        boolean matched = getIntent().getBooleanExtra(EXTRA_MATCHED, false);
        String submittedAt = getIntent().getStringExtra(EXTRA_SUBMITTED_AT);

        findViewById(R.id.bannerNoMatch).setVisibility(matched ? View.GONE : View.VISIBLE);
        findViewById(R.id.bannerUnlocked).setVisibility(matched ? View.VISIBLE : View.GONE);
        findViewById(R.id.groupNoMatch).setVisibility(matched ? View.GONE : View.VISIBLE);
        findViewById(R.id.groupUnlocked).setVisibility(matched ? View.VISIBLE : View.GONE);

        TextView btnPrimary = findViewById(R.id.btnPrimaryAction);
        TextView btnSecondary = findViewById(R.id.btnSecondaryAction);

        if (matched) {
            String id = getIntent().getStringExtra(EXTRA_ID);
            String name = getIntent().getStringExtra(EXTRA_NAME);
            String category = getIntent().getStringExtra(EXTRA_CATEGORY);
            String rarity = getIntent().getStringExtra(EXTRA_RARITY);
            String scientificName = getIntent().getStringExtra(EXTRA_SCIENTIFIC);
            String feature = getIntent().getStringExtra(EXTRA_FEATURE);
            String funFact = getIntent().getStringExtra(EXTRA_FUNFACT);

            ((TextView) findViewById(R.id.tvUnlockedBanner)).setText(
                    "✓ New collectible unlocked · " + name + " · " + rarity);
            ((TextView) findViewById(R.id.tvResultName)).setText(name);
            ((TextView) findViewById(R.id.tvResultRecord)).setText("Collectible #" + id + " · " + category);
            ((TextView) findViewById(R.id.tvResultObjectType)).setText(category);
            ((TextView) findViewById(R.id.tvResultFeature)).setText(feature != null && !feature.isEmpty() ? feature : "—");
            ((TextView) findViewById(R.id.tvResultFunFact)).setText(funFact != null && !funFact.isEmpty() ? funFact : "—");

            View rowScientific = findViewById(R.id.rowResultScientific);
            if (scientificName != null && !scientificName.isEmpty()) {
                rowScientific.setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.tvResultScientific)).setText(scientificName);
            } else {
                rowScientific.setVisibility(View.GONE);
            }

            btnPrimary.setText("🔍 More info");
            btnSecondary.setText("⇗ Share");
            btnPrimary.setOnClickListener(v ->
                    VisitorSheets.showSearchMoreInfo(this, name + " Singapore Botanic Gardens"));
            btnSecondary.setOnClickListener(v ->
                    VisitorSheets.showShareDiscovery(this, name, rarity));
        } else {
            ((TextView) findViewById(R.id.tvNoMatchSubmittedAt)).setText(submittedAt != null ? submittedAt : "—");

            btnPrimary.setText("↻ Retake");
            btnSecondary.setText("Search manually");
            btnPrimary.setOnClickListener(v -> {
                startActivity(new Intent(this, CapturePhotoActivity.class));
                finish();
            });
            btnSecondary.setOnClickListener(v -> openPlantSearch(null));
        }
    }



    //open plant search (AKA search manually)
    private void openPlantSearch(String name) {
        String query = name == null || name.trim().isEmpty()
                || "Unknown plant".equalsIgnoreCase(name.trim())
                ? "plants Singapore Botanic Gardens"
                : name.trim();

        Uri searchUrl = new Uri.Builder()
                .scheme("https")
                .authority("www.google.com")
                .appendPath("search")
                .appendQueryParameter("q", query)
                .build();

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, searchUrl));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No browser app is available.", Toast.LENGTH_SHORT).show();
        }
    }


    //showSuggestion() method
    private void showSuggestion() {
        findViewById(R.id.bannerNoMatch).setVisibility(View.GONE);
        findViewById(R.id.bannerUnlocked).setVisibility(View.VISIBLE);
        findViewById(R.id.groupNoMatch).setVisibility(View.GONE);
        findViewById(R.id.groupUnlocked).setVisibility(View.VISIBLE);

        String name = getIntent().getStringExtra(EXTRA_NAME);
        String scientific = getIntent().getStringExtra(EXTRA_SCIENTIFIC);
        double score = getIntent().getDoubleExtra(EXTRA_SCORE, 0);
        if (name == null || name.isEmpty()) {
            name = scientific == null || scientific.isEmpty() ? "Unknown plant" : scientific;
        }

        ((TextView) findViewById(R.id.tvUnlockedBanner))
                .setText("Possible plant match");
        ((TextView) findViewById(R.id.tvResultName)).setText(name);
        ((TextView) findViewById(R.id.tvResultRecord))
                .setText("Pl@ntNet suggestion · score " + Math.round(score * 100) + "%");
        ((TextView) findViewById(R.id.tvResultMatchedPill)).setText("Suggested");
        ((TextView) findViewById(R.id.tvResultObjectType)).setText("Plant");

        if (scientific == null || scientific.isEmpty()) {
            findViewById(R.id.rowResultScientific).setVisibility(View.GONE);
        } else {
            ((TextView) findViewById(R.id.tvResultScientific)).setText(scientific);
        }
        ((View) findViewById(R.id.tvResultFeature).getParent()).setVisibility(View.GONE);
        ((View) findViewById(R.id.tvResultFunFact).getParent()).setVisibility(View.GONE);

        TextView primary = findViewById(R.id.btnPrimaryAction);
        TextView secondary = findViewById(R.id.btnSecondaryAction);
        primary.setText("Retake");
        primary.setOnClickListener(v -> {
            startActivity(new Intent(this, CapturePhotoActivity.class));
            finish();
        });
        secondary.setText("Search manually");
        final String searchName = name;
        secondary.setOnClickListener(v -> openPlantSearch(searchName));
    }
    //This labels the result as a suggestion and hides the empty feature and fun-fact rows.


    //show submitted photo
    private void showSubmittedPhoto() {
        String uriText = getIntent().getStringExtra(EXTRA_PHOTO_URI);
        if (uriText == null || uriText.isEmpty()) return;

        ImageView photoView = findViewById(R.id.imgResultPhoto);
        try {
            ImageDecoder.Source source =
                    ImageDecoder.createSource(getContentResolver(), Uri.parse(uriText));
            photoView.setImageDrawable(ImageDecoder.decodeDrawable(
                    source, (decoder, info, ignored) -> {
                        int longest = Math.max(
                                info.getSize().getWidth(),
                                info.getSize().getHeight());
                        decoder.setTargetSampleSize(
                                Math.max(1, (longest + 1199) / 1200));
                    }));
            photoView.setVisibility(View.VISIBLE);
            findViewById(R.id.imgResultPlaceholder).setVisibility(View.GONE);
        } catch (IOException | SecurityException e) {
            android.util.Log.e("IdentificationResult", "Could not load photo", e);
        }
    }
}
