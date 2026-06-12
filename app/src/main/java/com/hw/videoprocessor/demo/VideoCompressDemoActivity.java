package com.hw.videoprocessor.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.hss01248.videocompress.CompressType;
import com.hss01248.videocompress.VideoCompressUtil;
import com.hss01248.videocompress.listener.ICompressListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VideoCompressDemoActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_VIDEO = 200;
    private static final int REQUEST_PICK_VIDEO = 201;
    private static final int PERMISSION_REQUEST_CODE = 202;

    private SwitchCompat switchShowProgress;
    private SwitchCompat switchShowCompare;
    private TextView tvStatus;
    private int pendingAction = -1;
    private Uri recordVideoUri;
    private String recordVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_compress_demo);

        switchShowProgress = findViewById(R.id.switchShowProgress);
        switchShowCompare = findViewById(R.id.switchShowCompare);
        tvStatus = findViewById(R.id.tvStatus);
        Button btnRecord = findViewById(R.id.btnRecordVideo);
        Button btnPick = findViewById(R.id.btnPickVideo);

        VideoCompressUtil.setiPreviewVideo((context, path) -> {
            File file = new File(path);
            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);
            Intent playIntent = new Intent(Intent.ACTION_VIEW);
            playIntent.setDataAndType(uri, "video/*");
            playIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(playIntent);
        });

        btnRecord.setOnClickListener(v -> {
            if (ensurePermissions()) {
                launchRecordVideo();
            } else {
                pendingAction = REQUEST_RECORD_VIDEO;
            }
        });

        btnPick.setOnClickListener(v -> {
            if (ensurePermissions()) {
                launchPickVideo();
            } else {
                pendingAction = REQUEST_PICK_VIDEO;
            }
        });
    }

    private boolean ensurePermissions() {
        if (Build.VERSION.SDK_INT < 23) return true;

        List<String> needed = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT < 33) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (needed.isEmpty()) return true;
        ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingAction == REQUEST_RECORD_VIDEO) launchRecordVideo();
            else if (pendingAction == REQUEST_PICK_VIDEO) launchPickVideo();
        }
        pendingAction = -1;
    }

    private void launchRecordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File moviesDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            if (moviesDir != null && !moviesDir.exists()) moviesDir.mkdirs();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File videoFile = new File(moviesDir, "VID_" + timestamp + ".mp4");
            recordVideoPath = videoFile.getAbsolutePath();
            recordVideoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", videoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, recordVideoUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_RECORD_VIDEO);
        } else {
            Toast.makeText(this, "未找到相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchPickVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(Intent.createChooser(intent, "选择视频"), REQUEST_PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_RECORD_VIDEO) {
            if (recordVideoPath != null && new File(recordVideoPath).exists()) {
                startCompress(recordVideoPath);
            } else {
                tvStatus.setText("拍摄的视频文件不存在");
            }
            return;
        }

        if (requestCode == REQUEST_PICK_VIDEO) {
            Uri videoUri = (data != null) ? data.getData() : null;
            if (videoUri == null) {
                tvStatus.setText("获取视频失败");
                return;
            }
            tvStatus.setText("正在拷贝视频到私有目录...");
            String inputPath = copyUriToPrivateDir(videoUri);
            if (inputPath == null) {
                tvStatus.setText("无法拷贝视频文件");
                return;
            }
            startCompress(inputPath);
        }
    }

    private void startCompress(final String inputPath) {
        boolean showProgress = switchShowProgress.isChecked();
        boolean showCompare = switchShowCompare.isChecked();

        VideoCompressUtil.init(this, showProgress, showCompare);

        tvStatus.setText("开始压缩: " + new File(inputPath).getName());

        VideoCompressUtil.doCompressAsync(inputPath, null, CompressType.TYPE_SDR_720P,
                new ICompressListener() {
                    @Override
                    public void onStart(String input, String outPath) {
                        runOnUiThread(() -> tvStatus.setText(
                                "压缩中...\n输入: " + new File(input).getName()
                                        + "\n输出: " + new File(outPath).getName()));
                    }

                    @Override
                    public void onProgress(int progress, long progressTime) {
                        runOnUiThread(() -> tvStatus.setText("压缩进度: " + progress + "%"));
                    }

                    @Override
                    public void onFinish(String outputFilePath) {
                        runOnUiThread(() -> {
                            File input = new File(inputPath);
                            File output = new File(outputFilePath);
                            long inputLen = input.length();
                            long outputLen = output.length();
                            String ratio = inputLen > 0
                                    ? (outputLen * 100 / inputLen) + "%"
                                    : "N/A";
                            tvStatus.setText("压缩完成!\n"
                                    + "原始大小: " + formatSize(inputLen) + "\n"
                                    + "压缩后大小: " + formatSize(outputLen) + "\n"
                                    + "压缩率: " + ratio);
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> tvStatus.setText("压缩失败: " + message));
                    }

                    @Override
                    public void onCancel() {
                        runOnUiThread(() -> tvStatus.setText("压缩已取消"));
                    }
                });
    }

    private String copyUriToPrivateDir(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            File moviesDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            if (moviesDir != null && !moviesDir.exists()) moviesDir.mkdirs();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File dest = new File(moviesDir, "PICK_" + timestamp + ".mp4");
            try (FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
            return dest.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024f);
        return String.format("%.2f MB", bytes / (1024f * 1024f));
    }
}
