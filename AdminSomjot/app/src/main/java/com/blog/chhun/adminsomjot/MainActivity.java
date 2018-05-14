package com.blog.chhun.adminsomjot;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {
    Button rescan_btn;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private TextView balance;
    private String user_id;
    public static Double amount;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        balance = findViewById(R.id.balance);

        user_id = firebaseAuth.getCurrentUser().getUid();

        rescan_btn = findViewById(R.id.button2);
        startScan();
        rescan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });

        firebaseFirestore.collection("users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        balance.setText(task.getResult().get("balance").toString().trim());
                    } else {
                        Toast.makeText(MainActivity.this, "Data does not exists", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    String error = task.getException(). getMessage();
                    Toast.makeText(MainActivity.this, "(FIRESTORE Retrieve Error) : "+error , Toast.LENGTH_SHORT).show();
                }
            }
        });

//        DocumentReference docRef = firebaseFirestore.collection("users").document("mTb3aw1JCQbaDJjeF76Xlqn0Mzy1");
//        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                if (task.isSuccessful()) {
//                    DocumentSnapshot document = task.getResult();
//                    if (document.exists()) {
//                        String a = document.getData().get("balance").toString();
//                        Toast.makeText(MainActivity.this, a, Toast.LENGTH_SHORT).show();
////                        balance.setText(a);
//                    } else {
//                        Toast.makeText(MainActivity.this, "No such document", Toast.LENGTH_SHORT).show();
//                    }
//                } else {
//                    Toast.makeText(MainActivity.this, "get failed with " +
//                            task.getException(), Toast.LENGTH_SHORT).show();
//                }
//            }
//        });

//        firebaseFirestore.collection("users")
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (DocumentSnapshot document : task.getResult()) {
//                                String a = document.getData().get("balance").toString();
//                                Toast.makeText(MainActivity.this, a, Toast.LENGTH_SHORT).show();
//                            }
//                        } else {
//                            Toast.makeText(MainActivity.this, "(FIRESTORE Error) : "
//                                    + task.getException(), Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });

//        // Create a new user with a first, middle, and last name
//        Map<String, Float> user = new HashMap<>();
//        float amt = 20555;
//        user.put("balance", amt);
//
//        // Add a new document with a generated ID
//        firebaseFirestore.collection("users")
//                .add(user)
//                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                    @Override
//                    public void onSuccess(DocumentReference documentReference) {
//                        Toast.makeText(MainActivity.this, "DocumentSnapshot added with ID: " +
//                                documentReference.getId(), Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(MainActivity.this, "Error adding document " +
//                                e, Toast.LENGTH_SHORT).show();
//                    }
//                });

    }

    public void startScan () {
        final Activity activity = this;
        IntentIntegrator integrator = new IntentIntegrator(activity);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("Put camera on barcode");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(true);
        integrator.setCaptureActivity(CaptureActivityPortrait.class);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null){
            if(result.getContents()==null){
                Toast.makeText(this, "You cancelled the scanning", Toast.LENGTH_LONG).show();
            }
            else {
//                Toast.makeText(this, result.getContents(),Toast.LENGTH_LONG).show();
                String key = "ThisIsSomjotAppp"; // 128 bit key
                String initVector = "RandomInitVector"; // 16 bytes IV
                String cipher = result.getContents();
                String plain = decrypt(key, initVector, cipher);
                Toast.makeText(MainActivity.this, plain, Toast.LENGTH_SHORT).show();
//                String cipher = encrypt(key, initVector, "ThisAESForSomjot");
//                String plain = decrypt(key, initVector, cipher);
                String[] part = plain.split("--somjot--");
                debit(part[3]);
                credit();

            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void credit() {
        firebaseFirestore.collection("users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
//                        balance.setText(task.getResult().get("balance").toString().trim());
                        amount = (Double) task.getResult().get("balance");
                        Map<String, Double> userMap = new HashMap<>();
                        userMap.put("balance", amount + 100);

                        firebaseFirestore.collection("users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "Account info is updated.", Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    String error = task.getException().getMessage();
                                    Toast.makeText(MainActivity.this, "(FIRESTORE Error) : "+error , Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(MainActivity.this, "Data does not exists", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    String error = task.getException().getMessage();
                    Toast.makeText(MainActivity.this, "(FIRESTORE Retrieve Error) : " + error , Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void debit(final String docRef) {
        firebaseFirestore.collection("users").document(docRef).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
//                        balance.setText(task.getResult().get("balance").toString().trim());
                        amount = (Double) task.getResult().get("balance");
                        Map<String, Double> userMap = new HashMap<>();
                        userMap.put("balance", amount - 100);

                        firebaseFirestore.collection("users").document(docRef).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "Account info is updated.", Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    String error = task.getException().getMessage();
                                    Toast.makeText(MainActivity.this, "(FIRESTORE Error) : "+error , Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(MainActivity.this, "Data does not exists", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    String error = task.getException().getMessage();
                    Toast.makeText(MainActivity.this, "(FIRESTORE Retrieve Error) : " + error , Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

//    public static String encrypt(String key, String initVector, String value) {
//        try {
//            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
//            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
//
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
//            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
//
//            byte[] encrypted = cipher.doFinal(value.getBytes());
//            System.out.println("encrypted string: "
//                    + Base64.encode(encrypted, Base64.DEFAULT));
//
//            return new String(Base64.encode(encrypted, Base64.DEFAULT));
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        return null;
//    }

    public static String decrypt(String key, String initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            sentToLogin();
        }
    }

    private void sentToLogin() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }
}
