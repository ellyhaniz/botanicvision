package com.example.plantdex.common.boundary;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.Roles;
import com.example.plantdex.common.RoleSplashActivity;
import com.example.plantdex.common.Session;
import com.example.plantdex.common.control.AuthController;
import com.example.plantdex.common.entity.User;

/**
 * Sign-in screen and the app's launcher Activity. Pure boundary class — it
 * only reads the form, shows loading/error state, and hands the actual
 * authentication off to AuthController.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnSignIn;
    private Button btnRegister;
    private TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnRegister = findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnSignIn.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class)));
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);
        AuthController.login(this, username, password, new AuthController.LoginCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);

                // This app is Visitor-only — User Admin, System Admin and Researcher
                // accounts now belong on the separate management website. AuthController
                // already saved the session before calling us, so undo that and bounce
                // rather than send a non-Visitor into the Visitor dashboard.
                if (!Roles.VISITOR.equals(user.getRole())) {
                    Session.clear(LoginActivity.this);
                    Toast.makeText(LoginActivity.this,
                            "This app is for Visitors only. Staff and admin accounts should sign in on the PlantDex management website.",
                            Toast.LENGTH_LONG).show();
                    etPassword.setText("");
                    return;
                }

                Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, RoleSplashActivity.class);
                intent.putExtra(RoleSplashActivity.EXTRA_ROLE, user.getRole());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                // Clear the password rather than the username — whatever they
                // typed was wrong, no reason to make them retype the username too.
                etPassword.setText("");
            }
        });
    }

    /** Disables the button and swaps its label while a login request is in flight, so a slow network can't be double-tapped into two requests. */
    private void setLoading(boolean loading) {
        btnSignIn.setEnabled(!loading);
        if (loading) {
            btnSignIn.setText("Signing in…");
        } else {
            btnSignIn.setText(R.string.sign_in);
        }
    }
}
