package com.example.plantdex.researcher;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;

/** Read-only detail view for a single logged record, opened from
 *  RecordsListActivity. Shows either the botanical or architectural field
 *  group depending on which category the record was logged under —
 *  mirroring the two groups in CreateRecordActivity's toggle form. */
public class RecordDetailActivity extends AppCompatActivity {

    private static final String EXTRA_NAME = "extra_name";
    private static final String EXTRA_RECORD_NUMBER = "extra_record_number";
    private static final String EXTRA_LOGGED_DATE = "extra_logged_date";
    private static final String EXTRA_IS_BOTANICAL = "extra_is_botanical";
    // Botanical
    private static final String EXTRA_SCIENTIFIC_NAME = "extra_scientific_name";
    private static final String EXTRA_FAMILY = "extra_family";
    private static final String EXTRA_FEATURES = "extra_features";
    private static final String EXTRA_FUN_FACT = "extra_fun_fact";
    // Architectural
    private static final String EXTRA_LOCATION = "extra_location";
    private static final String EXTRA_STYLE = "extra_style";
    private static final String EXTRA_CONSTRUCTED = "extra_constructed";
    private static final String EXTRA_DESCRIPTION = "extra_description";

    public static Intent botanical(Context context, String name, String recordNumber, String loggedDate,
                                    String scientificName, String family, String features, String funFact) {
        Intent intent = new Intent(context, RecordDetailActivity.class);
        intent.putExtra(EXTRA_NAME, name);
        intent.putExtra(EXTRA_RECORD_NUMBER, recordNumber);
        intent.putExtra(EXTRA_LOGGED_DATE, loggedDate);
        intent.putExtra(EXTRA_IS_BOTANICAL, true);
        intent.putExtra(EXTRA_SCIENTIFIC_NAME, scientificName);
        intent.putExtra(EXTRA_FAMILY, family);
        intent.putExtra(EXTRA_FEATURES, features);
        intent.putExtra(EXTRA_FUN_FACT, funFact);
        return intent;
    }

    public static Intent architectural(Context context, String name, String recordNumber, String loggedDate,
                                        String location, String style, String constructed, String description) {
        Intent intent = new Intent(context, RecordDetailActivity.class);
        intent.putExtra(EXTRA_NAME, name);
        intent.putExtra(EXTRA_RECORD_NUMBER, recordNumber);
        intent.putExtra(EXTRA_LOGGED_DATE, loggedDate);
        intent.putExtra(EXTRA_IS_BOTANICAL, false);
        intent.putExtra(EXTRA_LOCATION, location);
        intent.putExtra(EXTRA_STYLE, style);
        intent.putExtra(EXTRA_CONSTRUCTED, constructed);
        intent.putExtra(EXTRA_DESCRIPTION, description);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_detail);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.RESEARCHER);

        findViewById(R.id.btnProfile).setOnClickListener(v ->
                Toast.makeText(this, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show());

        Intent intent = getIntent();
        boolean isBotanical = intent.getBooleanExtra(EXTRA_IS_BOTANICAL, true);

        ((TextView) findViewById(R.id.tvName)).setText(intent.getStringExtra(EXTRA_NAME));
        ((TextView) findViewById(R.id.tvRecordNumber)).setText(intent.getStringExtra(EXTRA_RECORD_NUMBER));
        ((TextView) findViewById(R.id.tvLoggedDate)).setText(intent.getStringExtra(EXTRA_LOGGED_DATE));
        ((TextView) findViewById(R.id.tvCategory)).setText(isBotanical ? R.string.flora : R.string.architecture);

        ImageView imgThumb = findViewById(R.id.imgThumb);
        imgThumb.setImageResource(isBotanical ? R.drawable.ic_eco : R.drawable.ic_building);

        LinearLayout groupBotanical = findViewById(R.id.groupBotanical);
        LinearLayout groupArchitectural = findViewById(R.id.groupArchitectural);

        if (isBotanical) {
            groupBotanical.setVisibility(View.VISIBLE);
            groupArchitectural.setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tvScientificName)).setText(intent.getStringExtra(EXTRA_SCIENTIFIC_NAME));
            ((TextView) findViewById(R.id.tvFamily)).setText(intent.getStringExtra(EXTRA_FAMILY));
            ((TextView) findViewById(R.id.tvFeatures)).setText(intent.getStringExtra(EXTRA_FEATURES));
            ((TextView) findViewById(R.id.tvFunFact)).setText(intent.getStringExtra(EXTRA_FUN_FACT));
        } else {
            groupBotanical.setVisibility(View.GONE);
            groupArchitectural.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvLocation)).setText(intent.getStringExtra(EXTRA_LOCATION));
            ((TextView) findViewById(R.id.tvStyle)).setText(intent.getStringExtra(EXTRA_STYLE));
            ((TextView) findViewById(R.id.tvConstructed)).setText(intent.getStringExtra(EXTRA_CONSTRUCTED));
            ((TextView) findViewById(R.id.tvDescription)).setText(intent.getStringExtra(EXTRA_DESCRIPTION));
        }
    }
}
