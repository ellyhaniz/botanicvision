package com.example.plantdex.visitor;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;

public class CollectibleCardLockedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collectible_card_locked);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);

        View.OnClickListener comingSoon = v ->
                Toast.makeText(this, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();

        findViewById(R.id.btnProfile).setOnClickListener(comingSoon);

        Button btnStartIdentifying = findViewById(R.id.btnStartIdentifying);
        btnStartIdentifying.setOnClickListener(comingSoon);
    }
}
