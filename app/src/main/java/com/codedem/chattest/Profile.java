package com.codedem.chattest;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class Profile extends AppCompatActivity {

    private CircleImageView mProfileImage;
    private TextView mProfileName, mProfileStatus, mProfilefriendsCount;
    private Button mProfileSendReqBtn;

    private DatabaseReference mUserDatabase;
    private DatabaseReference mFriendRequestDatabase;
    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mNotificationDatabase;
    private DatabaseReference mRootRef;


    private FirebaseUser mCurrentUser;

    private ProgressDialog mProgressDialog;
    private int mCurrent_state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        final String user_id = getIntent().getStringExtra("userid");

        mProfileImage = findViewById(R.id.profile_user_image);
        mProfileName = findViewById(R.id.profile_dislplay_name);
        mProfileStatus = findViewById(R.id.profile_user_status);
        mProfilefriendsCount = findViewById(R.id.profiel_friends_count);
        mProfileSendReqBtn = findViewById(R.id.profile_send_friend_request);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();



        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading user");
        mProgressDialog.setMessage("Kindly allow few seconds");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        mCurrent_state = 0;

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendRequestDatabase = FirebaseDatabase.getInstance().getReference().child("friend_req");
        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("friends");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("notifications");
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String display_name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                mProfileName.setText(display_name);
                mProfileStatus.setText(status);

                Picasso.get().load(image).placeholder(R.drawable.dhiraj).into(mProfileImage);


                /**
                 * Friend list / Request Feature
                 */

                mFriendRequestDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.hasChild(user_id)){
                            String request_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();
                            if (request_type.equals("received")){
                                mCurrent_state = 2;
                                mProfileSendReqBtn.setText("Accept friend Request");
                            } else if(request_type.equals("sent")){
                                mCurrent_state = 1;
                                mProfileSendReqBtn.setText("Cancel Friend Request");
                            }

                            mProgressDialog.dismiss();

                        } else {

                            mFriendsDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(user_id)){
                                        mCurrent_state = 4;
                                        mProfileSendReqBtn.setText("UnFriend");
                                    }
                                    mProgressDialog.dismiss();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    mProgressDialog.dismiss();
                                }
                            });

                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                mProfileSendReqBtn.setEnabled(false);

                /**
                 * If the profile is not a friend
                 */
                if (mCurrent_state==0){

                    DatabaseReference newNotificationRef = mRootRef.child("notifications").child(user_id).push();
                    String notificationId = newNotificationRef.getKey();

                    HashMap<String, String> notificationData = new HashMap<>();
                    notificationData.put("from", mCurrentUser.getUid());
                    notificationData.put("type", "request");

                   Map requestMap = new HashMap<>();
                   requestMap.put("friend_req/"+mCurrentUser.getUid()+"/"+user_id+ "/request_type", "sent");
                   requestMap.put("friend_req/"+user_id+"/"+mCurrentUser.getUid()+ "/request_type", "received");
                   requestMap.put("notifications/"+user_id+"/"+ notificationId, notificationData);
                   mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                       @Override
                       public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                           if (databaseError != null){
                               Toast.makeText(Profile.this, "There was some error in sending request", Toast.LENGTH_LONG).show();
                           }
                           mCurrent_state = 1;
                           mProfileSendReqBtn.setText("Cancel Friend Request");
                           mProfileSendReqBtn.setEnabled(true);
                       }
                   });


                }

                /**
                 * Cancel Sent reuquest
                 */
                if (mCurrent_state==1){

                    mFriendRequestDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendRequestDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mProfileSendReqBtn.setEnabled(true);
                                    mCurrent_state = 0;
                                    mProfileSendReqBtn.setText("Send friend Request");

                                }
                            });

                        }
                    });

                }

                /**
                 * Request received state
                 */
                if (mCurrent_state==2){

                    final String currentDate = DateFormat.getDateInstance().format(new Date());

                    mFriendsDatabase.child(mCurrentUser.getUid()).child(user_id).child("date").setValue(currentDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendsDatabase.child(user_id).child(mCurrentUser.getUid()).child("date").setValue(currentDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mFriendRequestDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {

                                            mFriendRequestDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    mProfileSendReqBtn.setEnabled(true);
                                                    mCurrent_state = 4;
                                                    mProfileSendReqBtn.setText("UnFriend");

                                                }
                                            });

                                        }
                                    });


                                }
                            });

                        }
                    });

                }

                if (mCurrent_state == 3){

                    Map unfriendMap = new HashMap();
                    unfriendMap.put("friends/"+mCurrentUser.getUid()+"/"+user_id, null);
                    unfriendMap.put("friends/"+user_id+"/"+mCurrentUser.getUid(), null);
                    mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            if (databaseError != null){

                                mProfileSendReqBtn.setEnabled(true);
                                mCurrent_state = 0;
                                mProfileSendReqBtn.setText("Send friend request");

                            }
                        }
                    });


                }
            }
        });





    }
}
