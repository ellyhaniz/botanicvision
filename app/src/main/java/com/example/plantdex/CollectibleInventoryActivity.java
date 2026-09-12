package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CollectibleInventoryActivity extends AppCompatActivity {

    private static class Item {
        final String name, rarity, id;
        final boolean discovered;
        final int icon;

        Item(String name, String rarity, String id, boolean discovered, int icon) {
            this.name = name;
            this.rarity = rarity;
            this.id = id;
            this.discovered = discovered;
            this.icon = icon;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collectible_inventory);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.VISITOR));

        findViewById(R.id.btnFilter).setOnClickListener(v -> VisitorSheets.showFilterInventory(this));

        Item[] items = new Item[]{
                new Item("Tembusu Tree", "Rare", "0087", true, R.drawable.ic_leaf),
                new Item("Bandstand", "Common", "0042", true, R.drawable.ic_building),
                new Item("Vanda Miss Joaquim", "Rare", "0055", true, R.drawable.ic_leaf),
                new Item("???", "Undiscovered", "", false, R.drawable.ic_lock),
                new Item("???", "Undiscovered", "", false, R.drawable.ic_lock),
                new Item("???", "Undiscovered", "", false, R.drawable.ic_lock),
        };

        LinearLayout row1 = findViewById(R.id.row1);
        LinearLayout row2 = findViewById(R.id.row2);

        for (int i = 0; i < items.length; i++) {
            LinearLayout targetRow = (i < 3) ? row1 : row2;
            View tile = LayoutInflater.from(this).inflate(R.layout.tile_collectible, targetRow, false);

            Item item = items[i];
            ((TextView) tile.findViewById(R.id.tileName)).setText(item.name);
            ((TextView) tile.findViewById(R.id.tileRarity)).setText(item.rarity);
            ((ImageView) tile.findViewById(R.id.tileIcon)).setImageResource(item.icon);

            if (!item.discovered) {
                tile.findViewById(R.id.tileIconFrame).setBackgroundColor(getColor(R.color.plantdex_common_bg));
                ((TextView) tile.findViewById(R.id.tileName)).setTextColor(getColor(R.color.plantdex_text_muted));
                ((TextView) tile.findViewById(R.id.tileRarity)).setTextColor(getColor(R.color.plantdex_text_muted));
            } else {
                final String name = item.name;
                final String id = item.id;
                tile.setOnClickListener(v -> startActivity(CollectibleCardActivity.discovered(this, name, id)));
            }

            targetRow.addView(tile);
        }
    }
}
