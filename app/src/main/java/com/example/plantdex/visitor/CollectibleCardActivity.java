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

// in live time there are collectible card shown
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** #VS-12 (view collectible details) — reached from Inventory or Search. */
public class CollectibleCardActivity extends AppCompatActivity {

    private static final String EXTRA_DISCOVERED = "extra_discovered";
    private static final String EXTRA_ID = "extra_id";
    private static final String EXTRA_NAME = "extra_name";
    private static final String EXTRA_CATEGORY = "extra_category";
    private static final String EXTRA_RARITY = "extra_rarity";
    private static final String EXTRA_SCIENTIFIC = "extra_scientific";
    private static final String EXTRA_DESCRIPTION = "extra_description";
    // live collectible
    private static final String EXTRA_SCAN_RESULT = "extra_scan_result";
    private static final String EXTRA_SCORE = "extra_score";
    private static final String EXTRA_SCANNED_AT = "extra_scanned_at";
    private static final String EXTRA_IMAGE_URL = "extra_image_url";
    private static final String EXTRA_IMAGE_CREDIT = "extra_image_credit";

    public static Intent discovered(Context context, Collectible c) {
        Intent intent = new Intent(context, CollectibleCardActivity.class);
        intent.putExtra(EXTRA_DISCOVERED, true);
        intent.putExtra(EXTRA_ID, c.getId());
        intent.putExtra(EXTRA_NAME, c.getName());
        intent.putExtra(EXTRA_CATEGORY, c.getCategory());
        intent.putExtra(EXTRA_RARITY, c.getRarity());
        intent.putExtra(EXTRA_SCIENTIFIC, c.getScientificName());
        intent.putExtra(EXTRA_DESCRIPTION, c.getDescription());
        return intent;
    }

    public static Intent undiscovered(Context context, Collectible c) {
        Intent intent = new Intent(context, CollectibleCardActivity.class);
        intent.putExtra(EXTRA_DISCOVERED, false);
        intent.putExtra(EXTRA_ID, c.getId());
        intent.putExtra(EXTRA_RARITY, c.getRarity());
        return intent;
    }
    //live collectible
    public static Intent fromScan(Context context, String name, String scientificName,
                                  double score, String scannedAt,
                                  String imageUrl, String imageCredit) {
        Intent intent = new Intent(context, CollectibleCardActivity.class);
        intent.putExtra(EXTRA_SCAN_RESULT, true);
        intent.putExtra(EXTRA_NAME, name);
        intent.putExtra(EXTRA_SCIENTIFIC, scientificName);
        intent.putExtra(EXTRA_CATEGORY, "Flora");
        intent.putExtra(EXTRA_RARITY, "Rare");
        intent.putExtra(EXTRA_SCORE, score);
        intent.putExtra(EXTRA_SCANNED_AT, scannedAt);
        intent.putExtra(EXTRA_IMAGE_URL, imageUrl);
        intent.putExtra(EXTRA_IMAGE_CREDIT, imageCredit);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collectible_card);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.VISITOR));

        //live collectible
        boolean scanResult = getIntent().getBooleanExtra(EXTRA_SCAN_RESULT, false);
        boolean discovered = scanResult
                || getIntent().getBooleanExtra(EXTRA_DISCOVERED, true);

        String id = getIntent().getStringExtra(EXTRA_ID);
        String name = getIntent().getStringExtra(EXTRA_NAME);
        String category = getIntent().getStringExtra(EXTRA_CATEGORY);
        String rarity = getIntent().getStringExtra(EXTRA_RARITY);
        String scientificName = getIntent().getStringExtra(EXTRA_SCIENTIFIC);
        String description = getIntent().getStringExtra(EXTRA_DESCRIPTION);

        findViewById(R.id.layoutLockedNotice).setVisibility(discovered ? View.GONE : View.VISIBLE);
        findViewById(R.id.rowScientific).setVisibility(discovered && scientificName != null ? View.VISIBLE : View.GONE);
        findViewById(R.id.rowLinkedRecord).setVisibility(View.GONE); // no linked-record data source anymore
        findViewById(R.id.rowUnlocked).setVisibility(discovered ? View.VISIBLE : View.GONE);
        findViewById(R.id.rowRarity).setVisibility(discovered ? View.GONE : View.VISIBLE);
        findViewById(R.id.rowStatus).setVisibility(discovered ? View.GONE : View.VISIBLE);
        findViewById(R.id.dividerDescription).setVisibility(discovered ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvCardDescription).setVisibility(discovered ? View.VISIBLE : View.GONE);
        findViewById(R.id.groupDiscoveredActions).setVisibility(discovered ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnStartIdentifying).setVisibility(discovered ? View.GONE : View.VISIBLE);

        TextView tvStatusPill = findViewById(R.id.tvCardStatusPill);

        if (discovered) {
            // Replace the tvCardSubtitle setText line:
            ((TextView) findViewById(R.id.tvCardSubtitle)).setText(
                    scanResult ? "Pl@ntNet identification" : "Collectible #" + id);
        } else {
            ((TextView) findViewById(R.id.tvCardName)).setText("???");
            ((TextView) findViewById(R.id.tvCardSubtitle)).setText("Collectible #" + id);
            // Replace the tvCardUnlocked setText statement:
            String unlockedAt = scanResult
                    ? getIntent().getStringExtra(EXTRA_SCANNED_AT)
                    : DiscoveryStore.getUnlockedAt(this, id);
            ((TextView) findViewById(R.id.tvCardUnlocked)).setText(orDash(unlockedAt));
            // Replace the tvCardDescription setText statement:
            TextView descriptionView = findViewById(R.id.tvCardDescription);
            if (scanResult) {
                long confidence = Math.round(
                        getIntent().getDoubleExtra(EXTRA_SCORE, 0) * 100);
                descriptionView.setText(
                        "Pl@ntNet identification confidence: " + confidence + "%.");
            }
            else {
                descriptionView.setText(
                        description != null && !description.isEmpty()
                                ? description : "No description yet.");
            }
            if (scanResult) {
                loadScanPhoto(
                        getIntent().getStringExtra(EXTRA_IMAGE_URL),
                        getIntent().getStringExtra(EXTRA_IMAGE_CREDIT));
            }
            tvStatusPill.setText("Undiscovered");
            tvStatusPill.setBackgroundResource(R.drawable.bg_chip_unselected);
            tvStatusPill.setTextColor(getColor(R.color.plantdex_text_muted));
            ((TextView) findViewById(R.id.tvCardRarityValue)).setText("Hidden");

            findViewById(R.id.framePhoto).setBackgroundColor(getColor(R.color.plantdex_common_bg));
            findViewById(R.id.ivPhotoIcon).setVisibility(View.GONE);

            findViewById(R.id.btnStartIdentifying).setOnClickListener(v ->
                    startActivity(new Intent(this, CapturePhotoActivity.class)));
        }
    }

    private static String orDash(String value) {
        return value != null ? value : "—";
    }
    // load scan photo
    private void loadScanPhoto(String imageUrl, String imageCredit) {
        if (imageUrl == null || !imageUrl.startsWith("https://")) return;

        new Thread(() -> {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection) new URL(imageUrl).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                try (InputStream input = connection.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(input);
                }
            } catch (IOException e) {
                Log.w("CollectibleCard", "Could not load plant image", e);
            } finally {
                if (connection != null) connection.disconnect();
            }

            Bitmap loadedBitmap = bitmap;
            if (loadedBitmap == null) return;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                ImageView photo = findViewById(R.id.ivPlantNetPhoto);
                photo.setImageBitmap(loadedBitmap);
                photo.setVisibility(View.VISIBLE);
                findViewById(R.id.ivPhotoIcon).setVisibility(View.GONE);

                if (imageCredit != null && !imageCredit.isEmpty()) {
                    TextView credit = findViewById(R.id.tvPlantNetImageCredit);
                    credit.setText("Photo: " + imageCredit);
                    credit.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }
}
