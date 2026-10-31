package com.example.plantdex.common.boundary;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.control.AuthController;

/**
 *Always creates a Visitor account — there's
 * no role picker here on purpose; anything else is created by a User Admin.
 */

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etFullName, etUsername, etPassword, etConfirmPassword;
    private Button btnCreateAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.etEmail);
        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        Button btnBackToSignIn = findViewById(R.id.btnBackToSignIn);

        btnCreateAccount.setOnClickListener(v -> attemptCreateAccount());
        btnBackToSignIn.setOnClickListener(v -> finish());
    }

    private void attemptCreateAccount() {
        if (isEmpty(etEmail) || isEmpty(etFullName) || isEmpty(etUsername)
                || isEmpty(etPassword) || isEmpty(etConfirmPassword)) {
            Toast.makeText(this, "Please fill in every field", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!etPassword.getText().toString().equals(etConfirmPassword.getText().toString())) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        String email = etEmail.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        btnCreateAccount.setEnabled(false);
        AuthController.register(email, fullName, username, password, new AuthController.SimpleCallback() {
            @Override
            public void onSuccess() {
                btnCreateAccount.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Account created. You can sign in now.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                btnCreateAccount.setEnabled(true);
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Small helper so the validation block above reads as a plain list of checks instead of five near-identical trim()/isEmpty() calls. */
    private boolean isEmpty(EditText editText) {
        return TextUtils.isEmpty(editText.getText().toString().trim());
    }
}
