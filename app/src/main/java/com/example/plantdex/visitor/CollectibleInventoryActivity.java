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


/** #VS-09 (view inventory / track progress) and #VS-10 (filter by type/rarity). Shows only the
 * collectibles this visitor has unlocked by scanning; the progress bar counts them against all cards. */
public class CollectibleInventoryActivity extends AppCompatActivity {

    private final List<Collectible> allCollectibles = new ArrayList<>();
    private InventoryFilter filter = new InventoryFilter();

    //for the search bar
    private String searchQuery = "";

    private LinearLayout gridRows, groupNoResults, groupEmpty;
    private TextView tvProgress;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collectible_inventory);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.VISITOR));


        gridRows = findViewById(R.id.gridRows);
        groupEmpty = findViewById(R.id.groupEmpty);
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



        // Plants identified only by Pl@ntNet used to be kept on the phone as extra cards.
        // Cards now come only from the portal, so remove that old local list.
        deleteSharedPreferences("plantdex_scanned_collectibles");

        loadCollectibles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gridRows != null) renderItems();
    }

    private void loadCollectibles() {
        CollectibleController.loadAll(this, new CollectibleController.Callback() {
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

    /** Only the cards this visitor has unlocked by scanning, two per row. */
    /** Only the cards this visitor has unlocked by scanning, two per row. */
    private void renderItems() {
        gridRows.removeAllViews();

        List<Collectible> discovered = new ArrayList<>();
        List<Collectible> visible = new ArrayList<>();
        for (Collectible item : allCollectibles) {
            if (!DiscoveryStore.isDiscovered(this, item.getId())) continue;
            discovered.add(item);
            String scientific = item.getScientificName();
            boolean matchesName = searchQuery.isEmpty()
                    || item.getName().toLowerCase(Locale.ROOT).contains(searchQuery)
                    || (scientific != null
                    && scientific.toLowerCase(Locale.ROOT).contains(searchQuery));
            if (filter.matches(item, true) && matchesName) visible.add(item);
        }

        int total = allCollectibles.size();
        int found = discovered.size();
        int percent = total == 0 ? 0 : Math.round(found * 100f / total);
        tvProgress.setText(found + " / " + total + " · " + percent + "%");
        progressBar.setMax(Math.max(total, 1));
        progressBar.setProgress(found);

        // Nothing scanned yet: say so instead of showing an empty grid.
        groupEmpty.setVisibility(found == 0 && !allCollectibles.isEmpty() ? View.VISIBLE : View.GONE);
        groupNoResults.setVisibility(found > 0 && visible.isEmpty() ? View.VISIBLE : View.GONE);

        LinearLayout row = null;
        for (int i = 0; i < visible.size(); i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                gridRows.addView(row, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            Collectible item = visible.get(i);
            View tile = LayoutInflater.from(this).inflate(R.layout.tile_collectible, row, false);
            int icon = "Architecture".equals(item.getCategory())
                    ? R.drawable.ic_building : R.drawable.ic_leaf;
            ((TextView) tile.findViewById(R.id.tileName)).setText(item.getName());
            ((TextView) tile.findViewById(R.id.tileRarity)).setText(item.getRarity());
            ((ImageView) tile.findViewById(R.id.tileIcon)).setImageResource(icon);
            tile.setOnClickListener(v -> startActivity(CollectibleCardActivity.discovered(this, item)));
            row.addView(tile);
        }
        // An odd last card keeps half the width instead of stretching across the row.
        if (row != null && visible.size() % 2 != 0) {
            row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        }
    }
}
