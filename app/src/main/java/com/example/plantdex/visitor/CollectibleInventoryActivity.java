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


/** #VS-09 (view inventory / track progress) and #VS-10 (filter by type/rarity/discovery status).
 * Unlocked collectibles come first, then the locked ones still to discover. */
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

    /** The visitor's unlocked cards first, then the ones still to discover (shown as "???"),
     *  each section two per row. */
    private void renderItems() {
        gridRows.removeAllViews();

        List<Collectible> found = new ArrayList<>(), shownFound = new ArrayList<>();
        List<Collectible> locked = new ArrayList<>(), shownLocked = new ArrayList<>();
        for (Collectible item : allCollectibles) {
            boolean discovered = DiscoveryStore.isDiscovered(this, item.getId());
            (discovered ? found : locked).add(item);
            if (!filter.matches(item, discovered)) continue;
            if (discovered && matchesSearch(item)) shownFound.add(item);
            // A locked card's name is a secret, so searching by name only finds unlocked ones.
            if (!discovered && searchQuery.isEmpty()) shownLocked.add(item);
        }

        int total = allCollectibles.size();
        int percent = total == 0 ? 0 : Math.round(found.size() * 100f / total);
        tvProgress.setText(found.size() + " / " + total + " · " + percent + "%");
        progressBar.setMax(Math.max(total, 1));
        progressBar.setProgress(found.size());

        groupEmpty.setVisibility(View.GONE);
        groupNoResults.setVisibility(total > 0 && shownFound.isEmpty() && shownLocked.isEmpty()
                ? View.VISIBLE : View.GONE);
        if (total == 0 || (shownFound.isEmpty() && shownLocked.isEmpty())) return;

        boolean filtering = !searchQuery.isEmpty() || filter.isActive();
        if (!shownFound.isEmpty() || !filtering) {
            addSectionTitle("Discovered · " + found.size());
            if (found.isEmpty()) {
                addHint("Nothing yet. Scan a plant or landmark in the gardens to unlock its card.");
            }
            addGrid(shownFound, false);
        }
        if (!shownLocked.isEmpty()) {
            addSectionTitle("Still to discover · " + locked.size());
            addGrid(shownLocked, true);
        }
    }

    private boolean matchesSearch(Collectible item) {
        String scientific = item.getScientificName();
        return searchQuery.isEmpty()
                || item.getName().toLowerCase(Locale.ROOT).contains(searchQuery)
                || (scientific != null && scientific.toLowerCase(Locale.ROOT).contains(searchQuery));
    }

    private void addSectionTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(getColor(R.color.white));
        title.setTextSize(15);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        int top = Math.round(getResources().getDisplayMetrics().density * (gridRows.getChildCount() == 0 ? 4 : 18));
        int bottom = Math.round(getResources().getDisplayMetrics().density * 6);
        title.setPadding(0, top, 0, bottom);
        gridRows.addView(title);
    }

    private void addHint(String text) {
        TextView hint = new TextView(this);
        hint.setText(text);
        hint.setTextColor(getColor(R.color.white));
        hint.setAlpha(0.8f);
        hint.setTextSize(13);
        gridRows.addView(hint);
    }

    private void addGrid(List<Collectible> items, boolean locked) {
        LinearLayout row = null;
        for (int i = 0; i < items.size(); i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                gridRows.addView(row, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            row.addView(locked ? lockedTile(items.get(i), row) : discoveredTile(items.get(i), row));
        }
        // An odd last card keeps half the width instead of stretching across the row.
        if (row != null && items.size() % 2 != 0) {
            row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        }
    }

    private View discoveredTile(Collectible item, LinearLayout row) {
        View tile = LayoutInflater.from(this).inflate(R.layout.tile_collectible, row, false);
        int icon = "Architecture".equals(item.getCategory()) ? R.drawable.ic_building : R.drawable.ic_leaf;
        ((TextView) tile.findViewById(R.id.tileName)).setText(item.getName());
        ((TextView) tile.findViewById(R.id.tileRarity)).setText(item.getRarity());
        ((ImageView) tile.findViewById(R.id.tileIcon)).setImageResource(icon);
        tile.setOnClickListener(v -> startActivity(CollectibleCardActivity.discovered(this, item)));
        return tile;
    }

    private View lockedTile(Collectible item, LinearLayout row) {
        View tile = LayoutInflater.from(this).inflate(R.layout.tile_collectible, row, false);
        TextView name = tile.findViewById(R.id.tileName);
        TextView status = tile.findViewById(R.id.tileRarity);
        name.setText("???");
        status.setText("Undiscovered");
        name.setTextColor(getColor(R.color.plantdex_text_muted));
        status.setTextColor(getColor(R.color.plantdex_text_muted));
        ((ImageView) tile.findViewById(R.id.tileIcon)).setImageResource(R.drawable.ic_lock);
        tile.findViewById(R.id.tileIconFrame).setBackgroundColor(getColor(R.color.plantdex_common_bg));
        tile.setOnClickListener(v -> startActivity(CollectibleCardActivity.undiscovered(this, item)));
        return tile;
    }
}
