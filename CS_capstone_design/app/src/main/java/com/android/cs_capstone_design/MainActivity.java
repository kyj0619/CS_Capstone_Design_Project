package com.android.cs_capstone_design;

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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final String VIDEO_URL = "http://43.201.200.119//youtube.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 버튼 클릭 리스너 등록
        Button downloadButton = findViewById(R.id.download_button);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 권한 확인 후 다운로드 실행
                if (checkPermission()) {
                    new DownloadVideoTask(MainActivity.this).execute(VIDEO_URL);
                } else {
                    requestPermission();
                }
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
                new DownloadVideoTask(MainActivity.this).execute(VIDEO_URL);
            } else {
                Toast.makeText(this, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 비디오 다운로드 및 저장 작업을 수행하는 AsyncTask 클래스
    private static class DownloadVideoTask extends AsyncTask<String, Void, String> {

        private Context context;

        public DownloadVideoTask(Context context) {
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
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
                contentValues.put(MediaStore.MediaColumns.SIZE, fileLength);
                contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES);

                // 갤러리에 영상을 저장하기 위한 Uri 생성
                Uri uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
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
            return "영상 다운로드 완료!";
        }

        // 작업 완료 후 결과 표시
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    }
}
