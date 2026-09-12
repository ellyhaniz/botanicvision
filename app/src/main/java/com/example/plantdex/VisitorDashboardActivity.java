package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class VisitorDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_dashboard);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);

        findViewById(R.id.btnProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, MyAccountActivity.class);
            intent.putExtra(MyAccountActivity.EXTRA_ROLE, Roles.VISITOR);
            startActivity(intent);
        });

        LinearLayout cardIdentify = findViewById(R.id.cardIdentify);
        cardIdentify.setOnClickListener(v -> startActivity(new Intent(this, CapturePhotoActivity.class)));

        LinearLayout cardRecentDiscovery = findViewById(R.id.cardRecentDiscovery);
        cardRecentDiscovery.setOnClickListener(v ->
                startActivity(CollectibleCardActivity.discovered(this, "Tembusu Tree", "0087")));

        findViewById(R.id.tvViewMoreProgress).setOnClickListener(v ->
                startActivity(new Intent(this, CollectibleInventoryActivity.class)));
        findViewById(R.id.tvViewMoreRecent).setOnClickListener(v ->
                startActivity(new Intent(this, CollectibleInventoryActivity.class)));
    }
}
