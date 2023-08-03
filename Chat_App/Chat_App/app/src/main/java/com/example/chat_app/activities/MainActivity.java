package com.example.chat_app.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chat_app.databinding.ActivityMainBinding;
import com.example.chat_app.utilities.Constants;
import com.example.chat_app.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Initialize preference manager and load user details
            preferenceManager = new PreferenceManager(getApplicationContext());
            loadUserDetails();

            // Get the FCM token for push notifications
            getToken();

            // Set click listeners for sign out button and new chat button
            setListeners();
        } catch (Exception e) {
            Log.d("DEBUG", "Exception occurred: " + e.getMessage());
        }
    }

    private void setListeners() {
        try {
            // Set click listener for the sign out button
            binding.imageSignOut.setOnClickListener(v -> signOut());

            // Set click listener for the new chat button to start the UsersActivity
            binding.fabNewChat.setOnClickListener(v -> {
                startActivity(new Intent(getApplicationContext(), UsersActivity.class));
            });
        } catch (Exception e) {
            Log.d("DEBUG", "Exception occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadUserDetails() {
        // Set the user's name on the text view
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));

        // Decode the Base64 encoded image string into a byte array
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);

        // Decode the byte array into a Bitmap and set it on the profile image view
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void showToast(String message) {
        // Display a short toast message
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void getToken() {
        // Get the FCM token for the device to receive push notifications
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        // Update the FCM token in the Firestore document for the current user
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    private void signOut() {
        // Display a signing out message
        showToast("Signing out...");

        // Update the FCM token in the Firestore document to delete it
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    // Clear user preferences, start the SignInActivity, and finish this activity
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }
}
/////////////////////////////////////
//    private void fixLoad(String userId) {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        CollectionReference usersRef = db.collection("users");
//
//        Query query = usersRef.whereEqualTo("userId", userId);
//        query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//            @Override
//            public void onSuccess(QuerySnapshot querySnapshot) {
//                if (!querySnapshot.isEmpty()) {
//                    DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
//                    String name = documentSnapshot.getString("name");
//                    if (name != null) {
//                        binding.textName.setText(name);
//                    } else {
//                        // Handle case where "name" property is null
//                        Toast.makeText(MainActivity.this, "Name does not exist", Toast.LENGTH_SHORT).show();
//                    }
//                } else {
//                    // Handle case where the document doesn't exist
//                    Toast.makeText(MainActivity.this, "User not found", Toast.LENGTH_SHORT).show();
//                }
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(Exception e) {
//                // Handle any errors that occurred during the query
//                Toast.makeText(MainActivity.this, "Failed to retrieve user", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }


