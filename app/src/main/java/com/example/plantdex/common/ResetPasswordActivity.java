package com.example.plantdex.common;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etUsername, etNewPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etNewPassword = findViewById(R.id.etNewPassword);

        Button btnResetPassword = findViewById(R.id.btnResetPassword);
        Button btnBackToSignIn = findViewById(R.id.btnBackToSignIn);

        btnResetPassword.setOnClickListener(v -> attemptReset());
        btnBackToSignIn.setOnClickListener(v -> finish());
    }

    private void attemptReset() {
        if (TextUtils.isEmpty(etEmail.getText().toString().trim())
                || TextUtils.isEmpty(etUsername.getText().toString().trim())
                || TextUtils.isEmpty(etNewPassword.getText().toString().trim())) {
            Toast.makeText(this, "Please fill in every field", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prototype only. Replace with POST /api/auth/reset-password.
        Toast.makeText(this, "Password reset. Please sign in.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
