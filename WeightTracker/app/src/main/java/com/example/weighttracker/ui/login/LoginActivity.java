package com.example.weighttracker.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.weighttracker.ui.main.MainActivity;
import com.example.weighttracker.R;
import com.example.weighttracker.data.DatabaseHelper;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private TextInputLayout passwordLayout;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        initializeViews();
        setupPasswordValidation();
    }

    private void initializeViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        passwordLayout = findViewById(R.id.passwordLayout);
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnRegister = findViewById(R.id.btn_register);

        btnLogin.setOnClickListener(v -> handleLogin());
        btnRegister.setOnClickListener(v -> handleRegistration());
    }

    private void setupPasswordValidation() {
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();
                if (password.isEmpty()) {
                    passwordLayout.setError(null);
                    passwordLayout.setHelperText("Password must be 8+ chars with number/symbol");
                } else if (!DatabaseHelper.isPasswordValid(password)) {
                    passwordLayout.setError("Invalid format");
                } else {
                    passwordLayout.setError(null);
                    passwordLayout.setHelperText("Password strength: Good");
                }
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInputs(username, password)) return;

        try {
            if (dbHelper.validateUser(username, password)) {
                long userId = dbHelper.getUserId(username);
                startMainActivity(userId, username);
            } else {
                showToast("Invalid credentials");
            }
        } catch (Exception e) {
            showToast("Login error. Try again.");
        }
    }

    private void startMainActivity(long userId, String username) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("USERNAME", username);
        startActivity(intent);
        finish();
    }

    private void handleRegistration() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInputs(username, password)) return;

        try {
            if (dbHelper.addUser(username, password)) {
                showToast("Registration successful! Please login");
                etPassword.setText(""); // Clear password field
            } else {
                showToast("Registration failed. Username may exist");
            }
        } catch (Exception e) {
            showToast("Registration error: " + e.getMessage());
        }
    }

    private boolean validateInputs(String username, String password) {
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username required");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            return false;
        }
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}