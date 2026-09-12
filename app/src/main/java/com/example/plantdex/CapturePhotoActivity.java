package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CapturePhotoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_photo);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);
        ((android.widget.TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.VISITOR));

        ImageButton btnShutter = findViewById(R.id.btnShutter);
        btnShutter.setOnClickListener(v -> startActivity(new Intent(this, ConfirmPhotoActivity.class)));

        findViewById(R.id.btnGallery).setOnClickListener(v ->
                Toast.makeText(this, "Choose from gallery — prototype only.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnFlipCamera).setOnClickListener(v ->
                Toast.makeText(this, "Flip camera — prototype only.", Toast.LENGTH_SHORT).show());
    }
}
