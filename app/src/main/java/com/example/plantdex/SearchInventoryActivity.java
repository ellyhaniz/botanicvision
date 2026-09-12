package com.example.plantdex;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchInventoryActivity extends AppCompatActivity {

    private static class Entry {
        final String name, subtitle, unlocked;
        final boolean discovered;

        Entry(String name, String subtitle, String unlocked, boolean discovered) {
            this.name = name;
            this.subtitle = subtitle;
            this.unlocked = unlocked;
            this.discovered = discovered;
        }
    }

    private final List<Entry> allEntries = new ArrayList<>();
    private LinearLayout resultsContainer;
    private TextView tvResultCount, tvNoMatchQuery;
    private LinearLayout groupResults, groupNoResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_inventory);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.VISITOR));

        allEntries.add(new Entry("Vanda Miss Joaquim", "Collectible #0055 · Flora", "03 Dec 2026, 11:10:05", true));
        allEntries.add(new Entry("Vanda Tan Chay Yan", "Collectible #0061 · Flora", "—", false));
        allEntries.add(new Entry("Tembusu Tree", "Collectible #0087 · Flora", "07 Dec 2026, 18:14:05", true));
        allEntries.add(new Entry("Bandstand", "Collectible #0042 · Architecture", "05 Dec 2026, 14:13:09", true));

        resultsContainer = findViewById(R.id.resultsContainer);
        tvResultCount = findViewById(R.id.tvResultCount);
        tvNoMatchQuery = findViewById(R.id.tvNoMatchQuery);
        groupResults = findViewById(R.id.groupResults);
        groupNoResults = findViewById(R.id.groupNoResults);

        findViewById(R.id.btnFilter).setOnClickListener(v -> VisitorSheets.showFilterInventory(this));

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.setText("Vanda");
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { runSearch(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        View.OnClickListener clearSearch = v -> etSearch.setText("");
        findViewById(R.id.tvClearSearch).setOnClickListener(clearSearch);
        findViewById(R.id.btnClearSearchNoResults).setOnClickListener(clearSearch);

        runSearch("Vanda");
    }

    private void runSearch(String query) {
        resultsContainer.removeAllViews();
        String q = query.trim().toLowerCase(Locale.ROOT);

        if (q.isEmpty()) {
            groupResults.setVisibility(View.VISIBLE);
            groupNoResults.setVisibility(View.GONE);
            tvResultCount.setText(allEntries.size() + " results");
            for (Entry e : allEntries) addResultTile(e);
            return;
        }

        List<Entry> matches = new ArrayList<>();
        for (Entry e : allEntries) {
            if (e.name.toLowerCase(Locale.ROOT).contains(q)) matches.add(e);
        }

        if (matches.isEmpty()) {
            groupResults.setVisibility(View.GONE);
            groupNoResults.setVisibility(View.VISIBLE);
            tvNoMatchQuery.setText("Nothing matches \"" + query.trim() + "\"");
        } else {
            groupResults.setVisibility(View.VISIBLE);
            groupNoResults.setVisibility(View.GONE);
            tvResultCount.setText(matches.size() + " results");
            for (Entry e : matches) addResultTile(e);
        }
    }

    private void addResultTile(Entry entry) {
        View tile = LayoutInflater.from(this).inflate(R.layout.tile_search_result, resultsContainer, false);
        ((TextView) tile.findViewById(R.id.resultName)).setText(entry.name);
        ((TextView) tile.findViewById(R.id.resultSubtitle)).setText(entry.subtitle);
        ((TextView) tile.findViewById(R.id.resultUnlocked)).setText(entry.unlocked);

        TextView status = tile.findViewById(R.id.resultStatus);
        if (entry.discovered) {
            status.setText("Discovered");
            status.setTextColor(getColor(R.color.plantdex_success_text));
            status.setBackgroundResource(R.drawable.bg_pill);
        } else {
            status.setText("Undiscovered");
            status.setTextColor(getColor(R.color.plantdex_text_muted));
            status.setBackgroundResource(R.drawable.bg_chip_unselected);
            tile.findViewById(R.id.resultIcon).setAlpha(0.4f);
        }

        tile.setOnClickListener(v -> {
            if (entry.discovered) {
                startActivity(CollectibleCardActivity.discovered(this, entry.name, "0055"));
            } else {
                startActivity(CollectibleCardActivity.undiscovered(this, "0061"));
            }
        });

        resultsContainer.addView(tile);
    }
}
