package com.android.cs_capstone_design_otp;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayoutStates;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView textView1;
    private GoogleMap mMap;
    private Double latitude;
    private Double longitude;

    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final String VIDEO_URL = "http://43.201.200.119/fakelockscreen_test/fakelockscreen-main/uploads/webcam_capture.png";
    // Initialize Firestore
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView1 = (TextView) findViewById(R.id.textView1);

        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 버튼 클릭 리스너 등록
        Button downloadButton = findViewById(R.id.download_button);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 권한 확인 후 다운로드 실행
                if (checkPermission()) {
                    new DownloadImageTask(MainActivity.this).execute(VIDEO_URL);
                } else {
                    requestPermission();
                }
            }
        });

        Button alertmessage = findViewById(R.id.Alert);
        alertmessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Map<String, Object> user_state = new HashMap<>();
                user_state.put("authorized", false);

                db.collection("data")
                        .document("user_state")
                        .set(user_state)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "user_state value sending successful");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "user_sate sending error", e);
                            }
                        });
            }
        });

        DocumentReference docRef = db.collection("data").document("GPS");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        latitude = document.getDouble("lat");
                        longitude = document.getDouble("long");

                        textView1.setText("Lat: " + latitude + "\nLong: " + longitude);
//                        textView1.setText(document.getData().toString());
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());

                        updateMapLocation();

                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(ConstraintLayoutStates.TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // FCM token 값 받기
                        String token = task.getResult();

                        // log, toast, firestore upload
                        Log.d(ConstraintLayoutStates.TAG, "token value " + token);
                        Toast.makeText(MainActivity.this, token, Toast.LENGTH_SHORT).show();

                        Map<String, Object> data = new HashMap<>();
                        data.put("token_value", token);

                        db.collection("data")
                                .document("token")
                                .set(data)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "token_value set successful");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error adding token_value", e);
                                    }
                                });

                    }
                });

        Button button = findViewById(R.id.OTP);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String randomNumber = generateRandomNumber();

                // Create a new document with a generated ID
                Map<String, Object> data = new HashMap<>();
                data.put("OTPnum", randomNumber);

                db.collection("data")
                        .document("OTP")
                        .set(data)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "OTPnum set successful");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error adding OTPnum", e);
                            }
                        });
            }
        });
    }

    // 저장 권한 확인
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    // 권한 요청
    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new DownloadImageTask(MainActivity.this).execute(VIDEO_URL);
            } else {
                Toast.makeText(this, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //    ramdom otp generator
    public String generateRandomNumber() {
        int number = (int)(Math.random() * 900000) + 100000;  // This will generate a random number with 6 digits
        return String.valueOf(number);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }

    private void updateMapLocation() {
        if (mMap != null && latitude != null && longitude != null) {
            LatLng Position = new LatLng(latitude, longitude);

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(Position);
            markerOptions.title("current position");

            mMap.addMarker(markerOptions);

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Position, 15));
            mMap.getUiSettings().setZoomControlsEnabled(true);
        }
    }

    // 비디오가 아닌 이미지를 다운로드 및 저장 작업을 수행하는 AsyncTask 클래스
    private static class DownloadImageTask extends AsyncTask<String, Void, String> {

        private Context context;

        public DownloadImageTask(Context context) {
            this.context = context;
        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        protected String doInBackground(String... strings) {
            InputStream inputStream = null;
            HttpURLConnection connection = null;
            FileOutputStream fileOutputStream = null;
            try {
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // 서버 연결 실패 처리
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "서버 연결에 실패했습니다.";
                }

                int fileLength = connection.getContentLength();
                inputStream = new BufferedInputStream(connection.getInputStream());

                // 콘텐츠 정보 설정 및 Uri 생성
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png"); // 수정: 이미지 MIME 타입으로 변경
                contentValues.put(MediaStore.MediaColumns.SIZE, fileLength);
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES); // 수정: 이미지를 사진 디렉터리에 저장

                // 갤러리에 이미지를 저장하기 위한 Uri 생성
                Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues); // 수정: 이미지 저장 경로 변경
                fileOutputStream = (FileOutputStream) context.getContentResolver().openOutputStream(uri);

                // 다운로드 받은 데이터를 저장할 버퍼 설정
                byte[] buffer = new byte[4096];
                int bytesRead;

                // 서버에서 데이터를 읽어와 갤러리에 저장
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                return e.getMessage();
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return "이미지 다운로드 완료!"; // 수정: 결과 메시지 변경
        }

        // 작업 완료 후 결과 표시
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    }
}