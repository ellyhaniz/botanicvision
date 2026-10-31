package com.example.plantdex.common.boundary;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.Session;
import com.example.plantdex.common.control.AuthController;
import com.example.plantdex.common.control.ProfileController;

/**
 reset password function
 */
public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etUsername, etNewPassword;
    private Button btnResetPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etNewPassword = findViewById(R.id.etNewPassword);

        btnResetPassword = findViewById(R.id.btnResetPassword);
        Button btnBackToSignIn = findViewById(R.id.btnBackToSignIn);

        btnResetPassword.setOnClickListener(v -> attemptReset());
        btnBackToSignIn.setOnClickListener(v -> finish());
    }

    private void attemptReset() {
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(username) || TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "Please fill in every field", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPassword.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        btnResetPassword.setEnabled(false);
        ProfileController.resetPasswordSelf(email, username, newPassword, new AuthController.SimpleCallback() {
            @Override
            public void onSuccess() {
                btnResetPassword.setEnabled(true);
                Toast.makeText(ResetPasswordActivity.this, "Password reset. Please sign in.", Toast.LENGTH_LONG).show();
                // Whether this was reached from "forgot password" (not logged in) or from
                // Edit My Account (logged in), force a fresh sign-in with the new password.
                Session.clear(ResetPasswordActivity.this);
                Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                btnResetPassword.setEnabled(true);
                Toast.makeText(ResetPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
