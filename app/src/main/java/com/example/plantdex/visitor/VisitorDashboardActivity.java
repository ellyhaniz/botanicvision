package com.example.plantdex.visitor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;
import com.example.plantdex.common.entity.Collectible;
import com.example.plantdex.visitor.control.CollectibleController;

import java.util.List;

public class VisitorDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_dashboard);

        // Also wires the header's btnProfile (present directly in this layout).
        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);

        DiscoveryStore.seedDefaultsIfFirstRun(this);

        LinearLayout cardIdentify = findViewById(R.id.cardIdentify);
        cardIdentify.setOnClickListener(v -> startActivity(new Intent(this, CapturePhotoActivity.class)));

        // Opens whatever was discovered most recently; falls back to the full
        // inventory if nothing's been discovered yet or the catalog hasn't loaded.
        LinearLayout cardRecentDiscovery = findViewById(R.id.cardRecentDiscovery);
        cardRecentDiscovery.setOnClickListener(v -> {
            String recentId = DiscoveryStore.getMostRecentlyDiscoveredId(this);
            if (recentId == null) {
                startActivity(new Intent(this, CollectibleInventoryActivity.class));
                return;
            }
            CollectibleController.loadAll(new CollectibleController.Callback() {
                @Override
                public void onSuccess(List<Collectible> collectibles) {
                    for (Collectible c : collectibles) {
                        if (c.getId().equals(recentId)) {
                            startActivity(CollectibleCardActivity.discovered(VisitorDashboardActivity.this, c));
                            return;
                        }
                    }
                    startActivity(new Intent(VisitorDashboardActivity.this, CollectibleInventoryActivity.class));
                }

                @Override
                public void onError(String message) {
                    startActivity(new Intent(VisitorDashboardActivity.this, CollectibleInventoryActivity.class));
                }
            });
        });

        findViewById(R.id.tvViewMoreProgress).setOnClickListener(v ->
                startActivity(new Intent(this, CollectibleInventoryActivity.class)));
        findViewById(R.id.tvViewMoreRecent).setOnClickListener(v ->
                startActivity(new Intent(this, CollectibleInventoryActivity.class)));
    }
}
