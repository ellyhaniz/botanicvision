package com.example.plantdex.visitor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;
import com.example.plantdex.common.entity.Collectible;
import com.example.plantdex.visitor.control.CollectibleController;

import java.util.ArrayList;
import java.util.List;

//import for inventory search work
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import java.util.Locale;


/** #VS-09 (view inventory / track progress) and #VS-10 (filter by type/rarity/discovery status). */
public class CollectibleInventoryActivity extends AppCompatActivity {

    private final List<Collectible> allCollectibles = new ArrayList<>();
    private InventoryFilter filter = new InventoryFilter();

    //for the search bar
    private String searchQuery = "";

    private LinearLayout row1, row2, groupNoResults;
    private TextView tvProgress;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collectible_inventory);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.VISITOR));

        DiscoveryStore.seedDefaultsIfFirstRun(this);

        row1 = findViewById(R.id.row1);
        row2 = findViewById(R.id.row2);
        groupNoResults = findViewById(R.id.groupNoResults);
        tvProgress = findViewById(R.id.tvProgress);
        progressBar = findViewById(R.id.progressBar);

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                renderItems();
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnFilter).setOnClickListener(v ->
                VisitorSheets.showFilterInventory(this, filter, applied -> {
                    filter = applied;
                    renderItems();
                }));
        //new listener
        findViewById(R.id.btnClearFilterFromEmpty).setOnClickListener(v -> {
            filter = new InventoryFilter();
            etSearch.setText("");
            renderItems();
        });



        loadCollectibles();
    }

    //This uses the existing search and filters for scanned plants. A scanned tile opens its collectible card, where the saved Pl@ntNet image is used.

    @Override
    protected void onResume() {
        super.onResume();
        if (row1 != null) renderItems();
    }

    private void loadCollectibles() {
        CollectibleController.loadAll(new CollectibleController.Callback() {
            @Override
            public void onSuccess(List<Collectible> collectibles) {
                allCollectibles.clear();
                allCollectibles.addAll(collectibles);
                renderItems();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CollectibleInventoryActivity.this,
                        message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderItems() {
        LinearLayout scannedRows = findViewById(R.id.scannedRows);
        row1.removeAllViews();
        row2.removeAllViews();
        scannedRows.removeAllViews();

        List<Collectible> visible = new ArrayList<>();
        for (Collectible item : allCollectibles) {
            boolean discovered = DiscoveryStore.isDiscovered(this, item.getId());
            String scientific = item.getScientificName();
            boolean matchesName = searchQuery.isEmpty()
                    || item.getName().toLowerCase(Locale.ROOT).contains(searchQuery)
                    || (scientific != null
                    && scientific.toLowerCase(Locale.ROOT).contains(searchQuery));

            if (filter.matches(item, discovered) && matchesName) {
                visible.add(item);
            }
        }

        List<org.json.JSONObject> scanned = ScannedCollectibleStore.getAll(this);
        List<org.json.JSONObject> visibleScanned = new ArrayList<>();

        for (org.json.JSONObject scan : scanned) {
            String name = scan.optString("name", "");
            String scientific = scan.optString("scientificName", "");
            Collectible filterItem = new Collectible(
                    "scan", name, "Flora", "Rare", scientific, "", "", "");

            boolean matchesName = searchQuery.isEmpty()
                    || name.toLowerCase(Locale.ROOT).contains(searchQuery)
                    || scientific.toLowerCase(Locale.ROOT).contains(searchQuery);

            if (filter.matches(filterItem, true) && matchesName) {
                visibleScanned.add(scan);
            }
        }
        visibleScanned.sort((a, b) -> a.optString("name", "")
                .compareToIgnoreCase(b.optString("name", "")));

        int discoveredCount = scanned.size();
        for (Collectible item : allCollectibles) {
            if (DiscoveryStore.isDiscovered(this, item.getId())) {
                discoveredCount++;
            }
        }
        int total = allCollectibles.size() + scanned.size();
        int percent = total == 0 ? 0 : Math.round(discoveredCount * 100f / total);
        tvProgress.setText(discoveredCount + " / " + total + " · " + percent + "%");
        progressBar.setMax(Math.max(total, 1));
        progressBar.setProgress(discoveredCount);

        boolean noResults = visible.isEmpty()
                && visibleScanned.isEmpty()
                && total > 0;
        groupNoResults.setVisibility(noResults ? View.VISIBLE : View.GONE);
        row1.setVisibility(visible.isEmpty() ? View.GONE : View.VISIBLE);
        row2.setVisibility(visible.isEmpty() ? View.GONE : View.VISIBLE);
        scannedRows.setVisibility(
                visibleScanned.isEmpty() ? View.GONE : View.VISIBLE);

        // Existing example collectibles
        for (int i = 0; i < visible.size(); i++) {
            Collectible item = visible.get(i);
            LinearLayout targetRow = (i % 2 == 0) ? row1 : row2;
            View tile = LayoutInflater.from(this)
                    .inflate(R.layout.tile_collectible, targetRow, false);

            boolean discovered = DiscoveryStore.isDiscovered(this, item.getId());
            int icon = "Architecture".equals(item.getCategory())
                    ? R.drawable.ic_building : R.drawable.ic_leaf;

            if (discovered) {
                ((TextView) tile.findViewById(R.id.tileName))
                        .setText(item.getName());
                ((TextView) tile.findViewById(R.id.tileRarity))
                        .setText("Discovered · " + item.getRarity());
                ((ImageView) tile.findViewById(R.id.tileIcon))
                        .setImageResource(icon);
                tile.setOnClickListener(v -> startActivity(
                        CollectibleCardActivity.discovered(this, item)));
            } else {
                ((TextView) tile.findViewById(R.id.tileName))
                        .setText(item.getName());
                ((TextView) tile.findViewById(R.id.tileRarity))
                        .setText("Undiscovered");
                ((ImageView) tile.findViewById(R.id.tileIcon))
                        .setImageResource(R.drawable.ic_lock);
                tile.findViewById(R.id.tileIconFrame)
                        .setBackgroundColor(getColor(R.color.plantdex_common_bg));
                ((TextView) tile.findViewById(R.id.tileName))
                        .setTextColor(getColor(R.color.plantdex_text_muted));
                ((TextView) tile.findViewById(R.id.tileRarity))
                        .setTextColor(getColor(R.color.plantdex_text_muted));
                tile.setOnClickListener(v -> startActivity(
                        CollectibleCardActivity.undiscovered(this, item)));
            }
            targetRow.addView(tile);
        }

        // Plants identified from the visitor's scans
        for (int i = 0; i < visibleScanned.size(); i++) {
            if (i % 2 == 0) {
                LinearLayout newRow = new LinearLayout(this);
                newRow.setOrientation(LinearLayout.HORIZONTAL);
                newRow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                scannedRows.addView(newRow);
            }

            LinearLayout targetRow = (LinearLayout) scannedRows.getChildAt(
                    scannedRows.getChildCount() - 1);
            org.json.JSONObject scan = visibleScanned.get(i);
            String name = scan.optString("name", "");
            String scientific = scan.optString("scientificName", "");

            View tile = LayoutInflater.from(this)
                    .inflate(R.layout.tile_collectible, targetRow, false);
            ((TextView) tile.findViewById(R.id.tileName)).setText(name);
            ((TextView) tile.findViewById(R.id.tileRarity))
                    .setText("Discovered · Rare");
            ((ImageView) tile.findViewById(R.id.tileIcon))
                    .setImageResource(R.drawable.ic_leaf);

            tile.setOnClickListener(v -> startActivity(
                    CollectibleCardActivity.fromScan(
                            CollectibleInventoryActivity.this,
                            name,
                            scientific,
                            scan.optDouble("score", 0),
                            scan.optString("scannedAt", ""),
                            scan.optString("imageUrl", ""),
                            scan.optString("imageCredit", "")
                    )));
            targetRow.addView(tile);
        }

        if (visibleScanned.size() % 2 != 0) {
            LinearLayout lastRow = (LinearLayout) scannedRows.getChildAt(
                    scannedRows.getChildCount() - 1);
            View spacer = new View(this);
            lastRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        }
    }
}
