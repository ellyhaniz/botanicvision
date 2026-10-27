package com.example.plantdex.visitor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.DialogHelper;
import com.example.plantdex.common.Roles;

public class CollectibleCardUnlockedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collectible_card_unlocked);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);

        View.OnClickListener comingSoon = v ->
                Toast.makeText(this, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();

        findViewById(R.id.btnProfile).setOnClickListener(comingSoon);

        Button btnMoreInfo = findViewById(R.id.btnMoreInfo);
        Button btnShare = findViewById(R.id.btnShare);

        btnMoreInfo.setOnClickListener(v -> DialogHelper.showPlantInfo(
                this,
                "Tembusu Tree",
                "Cyrtophyllum fragrans",
                "Gentianaceae",
                "Dark fissured bark, small cream flowers, spreading crown.",
                "Appears on the reverse of the Singapore five-dollar note."));

        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "PlantDex Collectible: Tembusu Tree");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "I just discovered the Tembusu Tree (Cyrtophyllum fragrans) on PlantDex! A heritage tree native to Southeast Asia.");
            startActivity(Intent.createChooser(shareIntent, "Share collectible"));
        });
    }
}
