package com.example.plantdex.visitor;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.Locale;

/** #VS-11 (search inventory by name), also supports the #VS-10 filter. */
public class SearchInventoryActivity extends AppCompatActivity {

    private final List<Collectible> allCollectibles = new ArrayList<>();
    private InventoryFilter filter = new InventoryFilter();
    private String currentQuery = "";

    private LinearLayout resultsContainer;
    private TextView tvResultCount, tvNoMatchQuery;
    private LinearLayout groupResults, groupNoResults;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_inventory);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.VISITOR));

        DiscoveryStore.seedDefaultsIfFirstRun(this);

        resultsContainer = findViewById(R.id.resultsContainer);
        tvResultCount = findViewById(R.id.tvResultCount);
        tvNoMatchQuery = findViewById(R.id.tvNoMatchQuery);
        groupResults = findViewById(R.id.groupResults);
        groupNoResults = findViewById(R.id.groupNoResults);
        etSearch = findViewById(R.id.etSearch);

        findViewById(R.id.btnFilter).setOnClickListener(v ->
                VisitorSheets.showFilterInventory(this, filter, applied -> {
                    filter = applied;
                    runSearch(currentQuery);
                }));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { runSearch(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.tvClearSearch).setOnClickListener(v -> etSearch.setText(""));

        // The empty-state "Clear search" also drops the type/rarity/status filter —
        // the empty result could be caused by either, so this is the one button
        // guaranteed to get back to "show everything".
        findViewById(R.id.btnClearSearchNoResults).setOnClickListener(v -> {
            filter = new InventoryFilter();
            etSearch.setText(""); // no-op if already empty, so force a re-run either way
            runSearch("");
        });

        loadCollectibles();
    }

    private void loadCollectibles() {
        CollectibleController.loadAll(new CollectibleController.Callback() {
            @Override
            public void onSuccess(List<Collectible> collectibles) {
                allCollectibles.clear();
                allCollectibles.addAll(collectibles);
                runSearch(etSearch.getText().toString());
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SearchInventoryActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void runSearch(String query) {
        currentQuery = query;
        resultsContainer.removeAllViews();
        String q = query.trim().toLowerCase(Locale.ROOT);

        List<Collectible> matches = new ArrayList<>();
        for (Collectible c : allCollectibles) {
            boolean discovered = DiscoveryStore.isDiscovered(this, c.getId());
            boolean filterOk = filter.matches(c, discovered);
            // Searchable by real name whether or not it's discovered yet — that's how you
            // check "have I found the Rafflesia yet?" without already knowing you have.
            boolean nameOk = q.isEmpty() || c.getName().toLowerCase(Locale.ROOT).contains(q);
            if (filterOk && nameOk) matches.add(c);
        }

        boolean nothingLoadedYet = allCollectibles.isEmpty();
        if (matches.isEmpty() && !nothingLoadedYet) {
            groupResults.setVisibility(View.GONE);
            groupNoResults.setVisibility(View.VISIBLE);
            if (!q.isEmpty()) {
                tvNoMatchQuery.setText("Nothing matches \"" + query.trim() + "\"");
            } else {
                tvNoMatchQuery.setText("No collectibles match this filter");
            }
            ((TextView) findViewById(R.id.tvNoResultsHint)).setText(
                    "Check the spelling, or clear the search to see all " + allCollectibles.size() + " collectibles.");
        } else {
            groupResults.setVisibility(View.VISIBLE);
            groupNoResults.setVisibility(View.GONE);
            tvResultCount.setText(matches.size() + " results");
            for (Collectible c : matches) addResultTile(c);
        }
    }

    private void addResultTile(Collectible item) {
        View tile = LayoutInflater.from(this).inflate(R.layout.tile_search_result, resultsContainer, false);
        boolean discovered = DiscoveryStore.isDiscovered(this, item.getId());

        ((TextView) tile.findViewById(R.id.resultName)).setText(item.getName());
        ((TextView) tile.findViewById(R.id.resultSubtitle)).setText(
                "Collectible #" + item.getId() + " · " + item.getCategory());
        ((ImageView) tile.findViewById(R.id.resultIcon)).setImageResource(
                "Architecture".equals(item.getCategory()) ? R.drawable.ic_building : R.drawable.ic_leaf);

        TextView status = tile.findViewById(R.id.resultStatus);
        if (discovered) {
            status.setText("Discovered");
            status.setTextColor(getColor(R.color.plantdex_success_text));
            status.setBackgroundResource(R.drawable.bg_pill);
            ((TextView) tile.findViewById(R.id.resultUnlocked)).setText(
                    DiscoveryStore.getUnlockedAt(this, item.getId()));
        } else {
            status.setText("Undiscovered");
            status.setTextColor(getColor(R.color.plantdex_text_muted));
            status.setBackgroundResource(R.drawable.bg_chip_unselected);
            tile.findViewById(R.id.resultIcon).setAlpha(0.4f);
            ((TextView) tile.findViewById(R.id.resultUnlocked)).setText("—");
        }

        tile.setOnClickListener(v -> {
            if (discovered) {
                startActivity(CollectibleCardActivity.discovered(this, item));
            } else {
                startActivity(CollectibleCardActivity.undiscovered(this, item));
            }
        });

        resultsContainer.addView(tile);
    }
}
