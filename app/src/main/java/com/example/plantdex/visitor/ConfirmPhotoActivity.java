package com.example.plantdex.visitor;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;
import com.example.plantdex.common.entity.Collectible;
import com.example.plantdex.visitor.control.CollectibleController;

import java.util.Date;
import java.util.List;
import java.util.Random;

//logic import
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import java.io.IOException;
//Using Pl@ntNet API
import com.example.plantdex.common.ApiClient;
import org.json.JSONObject;
import org.json.JSONArray;

public class ConfirmPhotoActivity extends AppCompatActivity {

    private static final double MATCH_PROBABILITY = 0.7;

    private final List<Collectible> catalog = new java.util.ArrayList<>();
    private String capturedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_photo);
        //keeps a full-size camera image manageable in the preview.

        String photoUriText = getIntent().getStringExtra(CapturePhotoActivity.EXTRA_PHOTO_URI);
        if (photoUriText == null) {
            Toast.makeText(this, "No photo was provided.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView photoView = findViewById(R.id.imgCapturedPhoto);
        try {
            ImageDecoder.Source source =
                    ImageDecoder.createSource(getContentResolver(), Uri.parse(photoUriText));
            photoView.setImageDrawable(ImageDecoder.decodeDrawable(source, (decoder, info, ignored) -> {
                int longestSide = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                decoder.setTargetSampleSize(Math.max(1, (longestSide + 1199) / 1200));
            }));
            photoView.setVisibility(View.VISIBLE);
        } catch (IOException | SecurityException e) {
            Toast.makeText(this, "Could not load the photo.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        //++++++++++++

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.VISITOR));

        capturedAt = DateFormat.format("dd MMM yyyy, HH:mm:ss", new Date()).toString();
        ((TextView) findViewById(R.id.tvCapturedAt)).setText(capturedAt);

        CollectibleController.loadAll(new CollectibleController.Callback() {
            @Override
            public void onSuccess(List<Collectible> collectibles) {
                catalog.clear();
                catalog.addAll(collectibles);
            }

            @Override
            public void onError(String message) {
                // btnUse below falls back to a no-match result if the catalog never loaded.
            }
        });

        findViewById(R.id.btnRetake).setOnClickListener(v -> finish());
        findViewById(R.id.btnUse).setOnClickListener(v -> submit());
    }


    //New submit method
    private void submit() {
        Uri photoUri = Uri.parse(
                getIntent().getStringExtra(CapturePhotoActivity.EXTRA_PHOTO_URI));
        View useButton = findViewById(R.id.btnUse);
        useButton.setEnabled(false);
        //identilying animation
        android.app.Dialog waitingDialog = new android.app.Dialog(
                this, android.R.style.Theme_Material_NoActionBar_Fullscreen);
        waitingDialog.setContentView(R.layout.dialog_identifying_plant);
        waitingDialog.setCancelable(false);
        waitingDialog.show();

        ApiClient.identifyPlant(this, photoUri, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray results = response.optJSONArray("results");
                JSONObject top = results == null ? null : results.optJSONObject(0);
                JSONObject species = top == null ? null : top.optJSONObject("species");

                Intent intent;
                if (species == null) {
                    intent = IdentificationResultActivity.noMatch(
                            ConfirmPhotoActivity.this, capturedAt,photoUri.toString());
                } else {
                    String scientific = species.optString("scientificNameWithoutAuthor", "");
                    JSONArray commonNames = species.optJSONArray("commonNames");
                    String displayName = commonNames == null
                            ? "" : commonNames.optString(0, "");
                    if (displayName.isEmpty()) {
                        displayName = scientific.isEmpty() ? "Unknown plant" : scientific;
                    }
                    //for adding identiflyed plant  to collectible storage
                    JSONArray collectibleImages = top.optJSONArray("images");
                    JSONObject collectiblePhoto = collectibleImages == null
                            ? null : collectibleImages.optJSONObject(0);
                    JSONObject collectibleUrls = collectiblePhoto == null
                            ? null : collectiblePhoto.optJSONObject("url");

                    String collectibleImageUrl = "";
                    if (collectibleUrls != null) {
                        collectibleImageUrl = collectibleUrls.optString("m", "");
                        if (collectibleImageUrl.isEmpty()) {
                            collectibleImageUrl = collectibleUrls.optString("s", "");
                        }
                    }

                    String collectibleImageCredit = collectiblePhoto == null
                            ? "" : collectiblePhoto.optString("citation", "");

                    ScannedCollectibleStore.save(
                            ConfirmPhotoActivity.this,
                            displayName,
                            scientific,
                            top.optDouble("score", 0),
                            capturedAt,
                            collectibleImageUrl,
                            collectibleImageCredit
                    );

                    intent = IdentificationResultActivity.suggestion(
                            ConfirmPhotoActivity.this,
                            displayName,
                            scientific,
                            top.optDouble("score", 0),
                            photoUri.toString(),
                            capturedAt);
                }

                //loading  page
                waitingDialog.dismiss();

                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                waitingDialog.dismiss();
                useButton.setEnabled(true);
                new androidx.appcompat.app.AlertDialog.Builder(ConfirmPhotoActivity.this)
                        .setTitle("Identification error")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

}
