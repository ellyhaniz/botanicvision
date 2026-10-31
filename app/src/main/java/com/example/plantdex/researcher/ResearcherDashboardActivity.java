package com.example.plantdex.researcher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;

public class ResearcherDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_researcher_dashboard);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.RESEARCHER);

        View.OnClickListener comingSoon = v ->
                Toast.makeText(this, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();

        findViewById(R.id.btnProfile).setOnClickListener(comingSoon);

        LinearLayout cardCreateRecord = findViewById(R.id.cardCreateRecord);
        cardCreateRecord.setOnClickListener(v ->
                startActivity(new Intent(this, CreateRecordActivity.class)));

        View.OnClickListener openRecordsList = v ->
                startActivity(new Intent(this, RecordsListActivity.class));

        LinearLayout cardRecordsLogged = findViewById(R.id.cardRecordsLogged);
        cardRecordsLogged.setOnClickListener(openRecordsList);
        findViewById(R.id.tvViewMoreLogged).setOnClickListener(openRecordsList);

        // Recent Records card previews the same Tembusu Tree record shown
        // first in the full list, so it opens straight to that record's
        // detail screen; its View More opens the full list.
        LinearLayout cardRecentRecord = findViewById(R.id.cardRecentRecord);
        cardRecentRecord.setOnClickListener(v -> startActivity(
                RecordDetailActivity.botanical(this,
                        "Tembusu Tree", "Record #0231", "15 Sep 2026, 10:22:41",
                        "Cyrtophyllum fragrans", "Gentianaceae",
                        "Dark fissured bark, small cream flowers, spreading crown.",
                        "Appears on the reverse of the Singapore five-dollar note.")));

        findViewById(R.id.tvViewMoreRecords).setOnClickListener(openRecordsList);
    }
}
