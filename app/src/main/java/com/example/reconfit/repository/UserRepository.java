package com.example.reconfit.repository;

import com.example.reconfit.model.User;
import com.example.reconfit.model.Zone;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {
    private final FirebaseFirestore db;
    private FirebaseAuth auth;
    private static final String USERS_COLLECTION = "users";
    private final String userId;

    public UserRepository(){
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "invitado";
    }

    public Task<DocumentReference> saveUser(User user){
        CollectionReference usersRef = db.collection("users");
        return usersRef.add(user);
    }

}
