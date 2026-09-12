package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ConfirmPhotoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_photo);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.VISITOR));

        findViewById(R.id.btnRetake).setOnClickListener(v -> finish());

        // Prototype: alternate between a successful match and a no-match result
        // so both alt-flows in the design can be exercised from the same button.
        findViewById(R.id.btnUse).setOnClickListener(v -> {
            boolean matched = Math.random() > 0.35;
            Intent intent = new Intent(this, IdentificationResultActivity.class);
            intent.putExtra(IdentificationResultActivity.EXTRA_MATCHED, matched);
            startActivity(intent);
        });
    }
}
