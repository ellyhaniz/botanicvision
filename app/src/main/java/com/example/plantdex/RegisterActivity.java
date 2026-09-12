package com.example.plantdex;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etFullName, etUsername, etPassword, etConfirmPassword, etDob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.etEmail);
        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etDob = findViewById(R.id.etDob);

        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);
        Button btnBackToSignIn = findViewById(R.id.btnBackToSignIn);

        btnCreateAccount.setOnClickListener(v -> attemptCreateAccount());
        btnBackToSignIn.setOnClickListener(v -> finish());
    }

    private void attemptCreateAccount() {
        if (isEmpty(etEmail) || isEmpty(etFullName) || isEmpty(etUsername)
                || isEmpty(etPassword) || isEmpty(etConfirmPassword) || isEmpty(etDob)) {
            Toast.makeText(this, "Please fill in every field", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!etPassword.getText().toString().equals(etConfirmPassword.getText().toString())) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // Prototype only. Replace with POST /api/auth/register.
        Toast.makeText(this, "Account created. You can sign in now.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isEmpty(EditText editText) {
        return TextUtils.isEmpty(editText.getText().toString().trim());
    }
}
