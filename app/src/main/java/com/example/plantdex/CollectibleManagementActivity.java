package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CollectibleManagementActivity extends AppCompatActivity {

    private static class Item {
        final String name, id, category, rarity, linked;
        Item(String name, String id, String category, String rarity, String linked) {
            this.name = name; this.id = id; this.category = category; this.rarity = rarity; this.linked = linked;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_with_search);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.SYSTEM_ADMIN);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.SYSTEM_ADMIN));
        ((TextView) findViewById(R.id.tvScreenTitle)).setText("Collectible Management");

        android.widget.Button btnCreateNew = findViewById(R.id.btnCreateNew);
        btnCreateNew.setVisibility(View.VISIBLE);
        btnCreateNew.setText("+ Create collectible");
        btnCreateNew.setOnClickListener(v -> startActivity(new Intent(this, CreateCollectibleActivity.class)));

        Item[] items = {
                new Item("Tembusu Tree", "0087", "Flora", "Rare", "#0231 Tembusu Tree"),
                new Item("Bandstand", "0042", "Architecture", "Common", "#0198 Bandstand"),
                new Item("Vanda Miss Joaquim", "0055", "Flora", "Rare", "#0102 Vanda Miss Joaquim"),
        };

        LinearLayout container = findViewById(R.id.listContainer);
        for (Item item : items) {
            View tile = LayoutInflater.from(this).inflate(R.layout.tile_collectible_mgmt, container, false);
            ((TextView) tile.findViewById(R.id.collName)).setText(item.name);
            ((TextView) tile.findViewById(R.id.collSubtitle)).setText("Collectible #" + item.id);
            ((TextView) tile.findViewById(R.id.collCategory)).setText(item.category);
            ((TextView) tile.findViewById(R.id.collRarity)).setText(item.rarity);
            ((TextView) tile.findViewById(R.id.collLinkedRecord)).setText(item.linked);
            if ("Architecture".equals(item.category)) {
                ((android.widget.ImageView) tile.findViewById(R.id.collIcon)).setImageResource(R.drawable.ic_building);
            }
            tile.findViewById(R.id.btnEditColl).setOnClickListener(v ->
                    startActivity(new Intent(this, CreateCollectibleActivity.class)));
            container.addView(tile);
        }
    }
}
