package com.example.plantdex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CollectibleCardActivity extends AppCompatActivity {

    private static final String EXTRA_DISCOVERED = "extra_discovered";
    private static final String EXTRA_NAME = "extra_name";
    private static final String EXTRA_ID = "extra_id";

    public static Intent discovered(Context context, String name, String id) {
        Intent intent = new Intent(context, CollectibleCardActivity.class);
        intent.putExtra(EXTRA_DISCOVERED, true);
        intent.putExtra(EXTRA_NAME, name);
        intent.putExtra(EXTRA_ID, id);
        return intent;
    }

    public static Intent undiscovered(Context context, String id) {
        Intent intent = new Intent(context, CollectibleCardActivity.class);
        intent.putExtra(EXTRA_DISCOVERED, false);
        intent.putExtra(EXTRA_ID, id);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collectible_card);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.VISITOR));

        boolean discovered = getIntent().getBooleanExtra(EXTRA_DISCOVERED, true);
        String name = getIntent().getStringExtra(EXTRA_NAME);
        String id = getIntent().getStringExtra(EXTRA_ID);

        findViewById(R.id.layoutLockedNotice).setVisibility(discovered ? View.GONE : View.VISIBLE);
        findViewById(R.id.rowScientific).setVisibility(discovered ? View.VISIBLE : View.GONE);
        findViewById(R.id.rowLinkedRecord).setVisibility(discovered ? View.VISIBLE : View.GONE);
        findViewById(R.id.rowUnlocked).setVisibility(discovered ? View.VISIBLE : View.GONE);
        findViewById(R.id.rowRarity).setVisibility(discovered ? View.GONE : View.VISIBLE);
        findViewById(R.id.rowStatus).setVisibility(discovered ? View.GONE : View.VISIBLE);
        findViewById(R.id.dividerDescription).setVisibility(discovered ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvCardDescription).setVisibility(discovered ? View.VISIBLE : View.GONE);
        findViewById(R.id.groupDiscoveredActions).setVisibility(discovered ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnStartIdentifying).setVisibility(discovered ? View.GONE : View.VISIBLE);

        TextView tvStatusPill = findViewById(R.id.tvCardStatusPill);

        if (discovered) {
            ((TextView) findViewById(R.id.tvCardName)).setText(name != null ? name : "Tembusu Tree");
            ((TextView) findViewById(R.id.tvCardSubtitle)).setText("Collectible #" + id);
            tvStatusPill.setText("Rare");

            findViewById(R.id.ivPhotoIcon).setVisibility(View.VISIBLE);
            findViewById(R.id.btnMoreInfo).setOnClickListener(v ->
                    VisitorSheets.showSearchMoreInfo(this, name + " Singapore Botanic Gardens"));
            findViewById(R.id.btnShare).setOnClickListener(v ->
                    VisitorSheets.showShareDiscovery(this, name, "Rare", 18, 60));
        } else {
            ((TextView) findViewById(R.id.tvCardName)).setText("???");
            ((TextView) findViewById(R.id.tvCardSubtitle)).setText("Collectible #" + id);
            tvStatusPill.setText("Undiscovered");
            tvStatusPill.setBackgroundResource(R.drawable.bg_chip_unselected);
            tvStatusPill.setTextColor(getColor(R.color.plantdex_text_muted));

            findViewById(R.id.framePhoto).setBackgroundColor(getColor(R.color.plantdex_common_bg));
            findViewById(R.id.ivPhotoIcon).setVisibility(View.GONE);

            findViewById(R.id.btnStartIdentifying).setOnClickListener(v ->
                    startActivity(new Intent(this, CapturePhotoActivity.class)));
        }
    }
}
