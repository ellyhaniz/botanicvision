package com.example.plantdex.common.boundary;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;
import com.example.plantdex.common.Session;
import com.example.plantdex.common.control.AuthController;
import com.example.plantdex.common.control.ProfileController;

/**
 * Lets a signed-in user edit their own full name and email. Username stays
 * fixed (it's the login key) and password changes go through the separate
 * Reset Password screen rather than a plain-text field here.
 */
public class EditMyAccountActivity extends AppCompatActivity {

    private EditText etEditFullName, etEditEmail;
    private View layoutSaveError;
    private TextView tvSaveError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_my_account);

        String role = getIntent().getStringExtra(MyAccountActivity.EXTRA_ROLE);
        if (role == null) role = Session.getRole(this);
        if (role == null) role = Roles.VISITOR;

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), role);
        ((TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(role));

        // Username field is disabled in the layout itself (android:enabled="false") —
        // it's only here so the field grid reads the same as the admin's Edit Account screen.
        EditText etEditUsername = findViewById(R.id.etEditUsername);
        etEditFullName = findViewById(R.id.etEditFullName);
        etEditEmail = findViewById(R.id.etEditEmail);
        layoutSaveError = findViewById(R.id.layoutSaveError);
        tvSaveError = findViewById(R.id.tvSaveError);

        // Real data from the current session — not placeholder content.
        etEditUsername.setText(Session.getUsername(this));
        etEditFullName.setText(Session.getFullName(this));
        etEditEmail.setText(Session.getEmail(this));

        findViewById(R.id.btnGoToResetPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));

        findViewById(R.id.btnSaveAccount).setOnClickListener(v -> attemptSave());
        findViewById(R.id.btnCancelEditAccount).setOnClickListener(v -> finish());
    }

    private void attemptSave() {
        String fullName = etEditFullName.getText().toString().trim();
        String email = etEditEmail.getText().toString().trim();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email)) {
            showError("Please fill in every field");
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            showError("Enter a valid email address");
            return;
        }

        layoutSaveError.setVisibility(View.GONE);
        int userId = Session.getUserId(this);
        ProfileController.updateMyProfile(this, userId, fullName, email, new AuthController.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EditMyAccountActivity.this, "Account updated.", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void showError(String message) {
        tvSaveError.setText("⚠ " + message);
        layoutSaveError.setVisibility(View.VISIBLE);
    }
}
