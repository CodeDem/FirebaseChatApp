package com.codedem.chattest;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.Random;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private static final int MAX_LENGTH = 10;
    private CircleImageView mUserImage;
    private TextView mUserName;
    private TextView mUserStatus;
    private Button mChangeStatusBtn;
    private Button mChangeImageBtn;

    private DatabaseReference mUserDatabase;
    private FirebaseUser mCurrentUser;
    private StorageReference mImageStorage;

    private ProgressDialog mProgressDialog;

    public  static final int GALLERY_PICK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mImageStorage = FirebaseStorage.getInstance().getReference();

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        String current_uid = mCurrentUser.getUid().toString();
        mUserDatabase = FirebaseDatabase.getInstance().getReference("Users").child(current_uid);

        mUserName = findViewById(R.id.settings_display_name);
        mUserStatus = findViewById(R.id.settings_status);
        mUserImage = findViewById(R.id.settings_image);
        mChangeStatusBtn = findViewById(R.id.settings_change_status_btn);
        mChangeImageBtn = findViewById(R.id.settings_change_image);

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String name = dataSnapshot.child("name").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String thumbImage = dataSnapshot.child("thumb_image").toString();

                mUserName.setText(name);
                mUserStatus.setText(status);
                Picasso.get().load(image).into(mUserImage);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mChangeStatusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String status_value = mUserStatus.getText().toString();
                Intent changeStatus = new Intent(SettingsActivity.this, StatusActivity.class);
                changeStatus.putExtra("status_value", status_value);
                startActivity(changeStatus);
            }
        });

        mChangeImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);

//                CropImage.activity()
//                        .setGuidelines(CropImageView.Guidelines.ON)
//                        .start(SettingsActivity.this);

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK){

            Uri ImageUri =  data.getData();

            CropImage.activity(ImageUri)
                    .setAspectRatio(1, 1)
                    .start(this);


        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK){
                mProgressDialog = new ProgressDialog(SettingsActivity.this);
                mProgressDialog.setTitle("Uploading Image");
                mProgressDialog.setMessage("Kinldy allow a minute..");
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.show();
                Uri resultUri = result.getUri();
                String user_id = mCurrentUser.getUid().toString();
                StorageReference filepath = mImageStorage.child("profile_images").child(user_id+".jpg");
                filepath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()){
                            String downloadUrl =  task.getResult().getDownloadUrl().toString();
                            mUserDatabase.child("image").setValue(downloadUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        mProgressDialog.dismiss();
                                        Toast.makeText(SettingsActivity.this, "Success", Toast.LENGTH_LONG).show();
                                    } else{
                                        Toast.makeText(SettingsActivity.this, "Error while setting image to database", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });

                        } else {

                            Toast.makeText(SettingsActivity.this, "Error while uploading", Toast.LENGTH_LONG).show();
                        }
                    }
                });

            } else if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                Exception  error = result.getError();
            }
        }
    }

}
