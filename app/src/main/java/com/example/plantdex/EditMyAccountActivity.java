package com.example.plantdex;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditMyAccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_my_account);

        String role = getIntent().getStringExtra(MyAccountActivity.EXTRA_ROLE);
        if (role == null) role = Roles.VISITOR;
        final String finalRole = role;

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), role);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(role));

        EditText etEmail = findViewById(R.id.etEditEmail);
        TextView tvEmailError = findViewById(R.id.tvEmailError);
        View layoutSaveError = findViewById(R.id.layoutSaveError);

        findViewById(R.id.btnSaveAccount).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            boolean validEmail = email.contains("@") && email.contains(".");

            if (!validEmail) {
                etEmail.setBackgroundResource(R.drawable.bg_input_box_error);
                tvEmailError.setText("Enter a valid email address");
                tvEmailError.setVisibility(View.VISIBLE);
                layoutSaveError.setVisibility(View.VISIBLE);
                return;
            }

            etEmail.setBackgroundResource(R.drawable.bg_input_box);
            tvEmailError.setVisibility(View.GONE);
            layoutSaveError.setVisibility(View.GONE);
            Toast.makeText(this, "Account updated.", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btnCancelEditAccount).setOnClickListener(v -> finish());
    }
}
