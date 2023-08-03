package com.example.chat_app.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chat_app.adapters.ChatAdapter;
import com.example.chat_app.databinding.ActivityChatBinding;
import com.example.chat_app.models.ChatMessage;
import com.example.chat_app.models.User;
import com.example.chat_app.utilities.Constants;
import com.example.chat_app.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set click listeners and initialize chat activity
        setListeners();
        loadReceiverDetails();
        init();
        listenMessages();
    }

    private void init() {
        // Initialize preference manager, chat messages list, and chat adapter
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages, // List<ChatMessage> chatMessages
                getBitmapFromEncodedString(receiverUser.image), // Bitmap reciverProfileImage
                preferenceManager.getString(Constants.KEY_USER_ID)// String senderID
        );

        // Set the chat adapter on the RecyclerView
        binding.chatRecyclerView.setAdapter(chatAdapter);

        // Get an instance of Firebase Firestore
        database = FirebaseFirestore.getInstance();
    }

    private void sendMessage() {
        // Create a HashMap to store the message data
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());

        // Add the message to the chat collection in Firestore
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        // Clear the input text field
        binding.inputMessage.setText(null);
    }

    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        // Check for error
        if (error != null) {
            return; // Exit the method if there is an error
        }

        // Check if there is a value
        if (value != null) {
            int count = chatMessages.size(); // Get the initial size of the chatMessages list

            // Iterate over the document changes
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                // Check if the document change is of type ADDED
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();

                    // Set the properties of the chat message from the document
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage); // Add the chat message to the list
                }
            }

            // Sort the chat messages based on the dateObject
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateTime.compareTo(obj2.dateTime));


            // Check the count to determine if the adapter needs to be notified or items inserted
            if (count == 0) {
                chatAdapter.notifyDataSetChanged(); // Notify the adapter that the data has changed
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size()); // Insert new items in the adapter
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1); // Scroll to the last item in the RecyclerView
            }

            binding.chatRecyclerView.setVisibility(View.VISIBLE); // Make the RecyclerView visible
        }

        binding.progressBar.setVisibility(View.GONE); // Hide the progress bar
    };

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        // Decode the Base64 encoded string into a byte array
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);

        // Decode the byte array into a Bitmap
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void loadReceiverDetails() {
        // Retrieve the receiverUser object from the intent extras
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);

        // Set the receiver's name on the text view
        binding.textName.setText(receiverUser.name);
    }

    private void setListeners() {
        // Set click listener for the back button to go back to the previous activity
        binding.imageBack.setOnClickListener(v -> onBackPressed());

        // Set click listener for the send button to send a message
        binding.layoutSend.setOnClickListener(v -> sendMessage());
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }
}
