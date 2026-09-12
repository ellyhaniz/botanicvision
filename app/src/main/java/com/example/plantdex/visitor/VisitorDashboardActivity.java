package com.example.plantdex.visitor;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;

public class VisitorDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_dashboard);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);

        // Identify Plant, Recent Discovery, and collection inventory are out
        // of scope for now — the cards stay for layout fidelity but are inert.
        View.OnClickListener comingSoon = v ->
                Toast.makeText(this, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();

        findViewById(R.id.btnProfile).setOnClickListener(comingSoon);

        LinearLayout cardIdentify = findViewById(R.id.cardIdentify);
        cardIdentify.setOnClickListener(comingSoon);

        LinearLayout cardRecentDiscovery = findViewById(R.id.cardRecentDiscovery);
        cardRecentDiscovery.setOnClickListener(comingSoon);

        findViewById(R.id.tvViewMoreProgress).setOnClickListener(comingSoon);
        findViewById(R.id.tvViewMoreRecent).setOnClickListener(comingSoon);
    }
}
