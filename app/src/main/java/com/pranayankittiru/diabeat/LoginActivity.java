package com.pranayankittiru.diabeat;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// TODO progress bar hide/show
/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    // Progress Bar
    private ProgressBar mProgressBar;

    // Email and Password fields
    private EditText mEmailField;
    private EditText mPasswordField;

    // Firebase Auth
    private FirebaseAuth mAuth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Login setup
        mEmailField = findViewById(R.id.field_email);
        mPasswordField = findViewById(R.id.field_password);
        findViewById(R.id.email_sign_in_button).setOnClickListener((OnClickListener) this);

        mAuth = FirebaseAuth.getInstance();
    }

    // Check if user is already authenticated
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    // SignIn the user
    private void signIn(String email, String password) {
        Log.d("EmailPassword", "signIn" + email);
        if(!validateForm()) {
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            Log.d("EmailPassword", "SignInWithEmail: Success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        }
                        else {
                            Log.w("EmailPassword", "SignInWithEmail: Failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    // Validate form credentials
    private boolean validateForm() {
        boolean valid = true;
        String email = mEmailField.getText().toString();
        if(TextUtils.isEmpty(email) && email.contains("@")) {
            mEmailField.setError("Required");
            valid = false;
        }
        else {
            mEmailField.setError(null);
        }

        String password = mPasswordField.getText().toString();
        if(TextUtils.isEmpty(password) && password.length() > 8) {
            mPasswordField.setError("Required");
            valid = false;
        }
        else {
            mPasswordField.setError(null);
        }

        return valid;
    }

    // updateUI on successful or unsuccessful authentication
    private void updateUI(FirebaseUser user) {
        if(user != null) {
            Intent homeIntent = new Intent(this, HomeActivity.class);
            this.startActivity(homeIntent);
        }
    }

    // onClick function definition
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.email_sign_in_button) {
            signIn(mEmailField.getText().toString(), mPasswordField.getText().toString());
        }
    }

}

