package com.codedem.chattest;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private Button mButton;
    private ProgressDialog mRegProgress;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        mRegProgress = new ProgressDialog(this);

        mToolbar = (Toolbar) findViewById(R.id.login_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Login");

        mEmail = findViewById(R.id.login_email);
        mPassword = findViewById(R.id.login_password);
        mButton = findViewById(R.id.login_btn);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmail.getEditText().getText().toString();
                String password = mPassword.getEditText().getText().toString();

                mRegProgress.setTitle("Logging in");
                if (!TextUtils.isEmpty(email) || !TextUtils.isEmpty(password)){
                    mRegProgress.setMessage("Kinldy allow one minute");
                    mRegProgress.setCanceledOnTouchOutside(false);
                    mRegProgress.show();
                    loginUser(email, password);
                }
            }
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                 if (task.isSuccessful()){
                     mRegProgress.dismiss();
                     Intent loginIntent = new Intent(LoginActivity.this, MainActivity.class);
                     loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                     startActivity(loginIntent);
                     finish();

                 } else{
                     mRegProgress.hide();
                     Toast.makeText(LoginActivity.this, "You got some error", Toast.LENGTH_LONG).show();
                 }
            }
        });

    }

}
