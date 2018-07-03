package com.codedem.chattest;

import android.content.Context;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String mChatUser, mCurrentChatUsername;

    private Toolbar mChatToolbar;
    private TextView mTitleView;
    private TextView mlastSeenView;
    private CircleImageView mProfileImage;
    private FirebaseAuth mAuth;
    private String mCurrentUserId;

    //UI elements
    private ImageView mAddItemBtn;
    private ImageView mSendBtn;
    private EditText mChatMessageView;

    private RecyclerView mMessagesList;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private final List<Messages> messageList = new ArrayList<>();
    private LinearLayoutManager mLinearlayout;
    private MessageAdapter mAdapter;
    private DatabaseReference mRootRef;

    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private int mCurrentPage = 1;



    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_chat_activity);

            mChatToolbar = findViewById(R.id.chat_app_bar);
            setSupportActionBar(mChatToolbar);

            ActionBar actionBar = getSupportActionBar();

            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);


            mRootRef = FirebaseDatabase.getInstance().getReference();
            mAuth = FirebaseAuth.getInstance();
            mCurrentUserId = mAuth.getCurrentUser().getUid();

            mChatUser = getIntent().getStringExtra("userid");
            mCurrentChatUsername = getIntent().getStringExtra("username");
            getSupportActionBar().setTitle(mCurrentChatUsername);

            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View actionBarView = inflater.inflate(R.layout.chat_custom_bar, null);

            actionBar.setCustomView(actionBarView);

            mTitleView = findViewById(R.id.chat_custom_recipient_name);
            mlastSeenView = findViewById(R.id.chat_custom_lastseen);
            mProfileImage = findViewById(R.id.custom_bar_image);
            mAddItemBtn = findViewById(R.id.chat_add);
            mSendBtn = findViewById(R.id.chat_send);
            mChatMessageView = findViewById(R.id.chat_text_body);
            mAdapter = new MessageAdapter(messageList);
            mSwipeRefreshLayout = findViewById(R.id.message_swipe_layout);
            mMessagesList = findViewById(R.id.messages_lisr);

            mLinearlayout = new LinearLayoutManager(this);

            mMessagesList.setHasFixedSize(true);
            mMessagesList.setLayoutManager(mLinearlayout);
            mMessagesList.setAdapter(mAdapter);

            loadMessages();


            mTitleView.setText(mCurrentChatUsername);

            mRootRef.child("Users").child(mChatUser).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String online = dataSnapshot.child("online").getValue().toString();
                    String image = dataSnapshot.child("image").getValue().toString();

                    if (online.equals("true")){
                        mlastSeenView.setText("Online");
                    } else {
                        GetTimeAgo getTimeAgo = new GetTimeAgo();
                        long lastTime = Long.parseLong(online);
                        String lastSeenTime = getTimeAgo.getTimeAgo(    lastTime, getApplicationContext());
                        mlastSeenView.setText(lastSeenTime);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            mRootRef.child("chat").child(mCurrentUserId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.hasChild(mChatUser)){

                        Map chatAddMap = new HashMap();
                        chatAddMap.put("seen", false);
                        chatAddMap.put("timestamp", ServerValue.TIMESTAMP);

                        Map chatUserMap = new HashMap();
                        chatUserMap.put("chat/"+mCurrentUserId+"/"+mChatUser, chatAddMap);
                        chatUserMap.put("chat/"+mChatUser+"/"+mCurrentUserId, chatAddMap);

                        mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                if (databaseError != null ){
                                    Log.d("Chat_LOG", databaseError.getMessage().toString());
                                }
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            //Send Message
            mSendBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendMesssage();
                }
            });

            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    mCurrentPage++;
                    messageList.clear();
                    loadMoreMessages();
                }
            });


        }

        private void loadMoreMessages(){
            DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUserId).child(mChatUser);

            Query messageQuery = messageRef.orderByKey();
        }

        private void loadMessages(){

            DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUserId).child(mChatUser);

            Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);

//            mRootRef.child("messages").child(mCurrentUserId).child(mChatUser)

            messageQuery.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    Messages message = dataSnapshot.getValue(Messages.class);

                    messageList.add(message);
                    mAdapter.notifyDataSetChanged();
                    mMessagesList.scrollToPosition(messageList.size() - 1);

                    mSwipeRefreshLayout.setRefreshing(false);





                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    private void sendMesssage() {

        String message = mChatMessageView.getText().toString();
        if (!TextUtils.isEmpty(message)){

            String current_user_ref = "messages/"+mCurrentUserId+"/"+mChatUser;
            String chat_user_ref = "messages/"+mChatUser+"/"+mCurrentUserId;

            DatabaseReference user_message_push = mRootRef.child("messages").child(mCurrentUserId).child(mChatUser).push();

            String push_id = user_message_push.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message", message );
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUserId);

            Map mesageUserMap = new HashMap();
            mesageUserMap.put(current_user_ref + "/" + push_id, messageMap);
            mesageUserMap.put(chat_user_ref+"/"+push_id, messageMap);

            mChatMessageView.setText("");

            mRootRef.updateChildren(mesageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                    if (databaseError != null){
                        Log.d("Chat_LOG", databaseError.getMessage().toString());
                    }
                 }
            });


        }
    }
}
