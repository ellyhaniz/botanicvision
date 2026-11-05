package com.example.plantdex.visitor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;
import com.example.plantdex.common.entity.Collectible;
import com.example.plantdex.visitor.control.CollectibleController;

import java.util.ArrayList;
import java.util.List;

/** Visitor home: collection progress and the most recent discovery, from the portal. */
public class VisitorDashboardActivity extends AppCompatActivity {

    private final List<Collectible> catalog = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_dashboard);

        // Also wires the header's btnProfile (present directly in this layout).
        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);

        LinearLayout cardIdentify = findViewById(R.id.cardIdentify);
        cardIdentify.setOnClickListener(v -> startActivity(new Intent(this, CapturePhotoActivity.class)));

        // Opens the most recent discovery; with nothing discovered yet, the inventory.
        findViewById(R.id.cardRecentDiscovery).setOnClickListener(v -> {
            Collectible recent = mostRecent();
            startActivity(recent != null
                    ? CollectibleCardActivity.discovered(this, recent)
                    : new Intent(this, CollectibleInventoryActivity.class));
        });

        findViewById(R.id.tvViewMoreProgress).setOnClickListener(v ->
                startActivity(new Intent(this, CollectibleInventoryActivity.class)));
        findViewById(R.id.tvViewMoreRecent).setOnClickListener(v ->
                startActivity(new Intent(this, CollectibleInventoryActivity.class)));

        render();   // clean starting state while the catalog loads
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload every time the screen shows, so a new scan appears straight away.
        CollectibleController.loadAll(this, new CollectibleController.Callback() {
            @Override
            public void onSuccess(List<Collectible> collectibles) {
                catalog.clear();
                catalog.addAll(collectibles);
                render();
            }

            @Override
            public void onError(String message) {
                // Keep whatever was last shown; the catalog loads again next time.
                android.widget.Toast.makeText(VisitorDashboardActivity.this,
                        message, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void render() {
        int found = 0, flora = 0, architecture = 0;
        for (Collectible c : catalog) {
            if (!DiscoveryStore.isDiscovered(this, c.getId())) continue;
            found++;
            if ("Architecture".equals(c.getCategory())) architecture++;
            else flora++;
        }
        ((TextView) findViewById(R.id.tvDiscoveredCount)).setText(found + " of " + catalog.size());
        ProgressBar progress = findViewById(R.id.progressDiscovery);
        progress.setMax(Math.max(catalog.size(), 1));
        progress.setProgress(found);
        ((TextView) findViewById(R.id.tvFloraCount)).setText(String.valueOf(flora));
        ((TextView) findViewById(R.id.tvArchitectureCount)).setText(String.valueOf(architecture));

        Collectible recent = mostRecent();
        boolean has = recent != null;
        TextView name = findViewById(R.id.tvDiscoveryName);
        TextView subtitle = findViewById(R.id.tvCollectibleId);
        if (has) {
            name.setText(recent.getName());
            subtitle.setText("Collectible #" + recent.getId());
            ((TextView) findViewById(R.id.tvRarity)).setText(recent.getRarity());
            ((TextView) findViewById(R.id.tvUnlockedDate))
                    .setText(DiscoveryStore.getUnlockedAt(this, recent.getId()));
            ((TextView) findViewById(R.id.tvDiscoveryCategory)).setText(recent.getCategory());
            ((ImageView) findViewById(R.id.ivRecentIcon)).setImageResource(
                    "Architecture".equals(recent.getCategory()) ? R.drawable.ic_building : R.drawable.ic_leaf);
        } else {
            name.setText("No discoveries yet");
            subtitle.setText("Scan a plant or landmark to unlock your first card");
        }
        int details = has ? View.VISIBLE : View.GONE;
        findViewById(R.id.tvRarity).setVisibility(details);
        findViewById(R.id.dividerRecent).setVisibility(details);
        findViewById(R.id.rowRecentUnlocked).setVisibility(details);
        findViewById(R.id.rowRecentCategory).setVisibility(details);
    }

    /** The visitor's most recently unlocked collectible that's still in the catalog, or null. */
    private Collectible mostRecent() {
        String id = DiscoveryStore.getMostRecentlyDiscoveredId(this);
        if (id == null) return null;
        for (Collectible c : catalog) if (c.getId().equals(id)) return c;
        return null;
    }
}
