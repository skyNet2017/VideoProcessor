package com.hss01248.videocompress.compare;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ReflectUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.hss01248.videocompress.CompressHepler;
import com.hss01248.videocompress.R;
import com.hss01248.videocompress.VideoCompressUtil;
import com.hss01248.videocompress.VideoInfo;
import com.hss01248.videocompress.mediacodec.MediaCodecCompressImpl;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class CompressCompareActivity extends AppCompatActivity {

    public static void start(Activity activity,String originalFile,String compressedFile,long startTime){
        Intent intent = new Intent(activity,CompressCompareActivity.class);
        intent.putExtra("originalFile",originalFile);
        intent.putExtra("compressedFile",compressedFile);
        intent.putExtra("startTime",startTime);
        activity.startActivity(intent);

    }

    String originalFile, compressedFile;
    ImageView iv1,iv2;
    TextView tv1,tv2,tv_title;
    VideoInfo info1,info2;
    long startTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        originalFile = getIntent().getStringExtra("originalFile");
        compressedFile = getIntent().getStringExtra("compressedFile");
        startTime = getIntent().getLongExtra("startTime",0);
        setContentView(R.layout.video_activity_compare);
        iv1 = findViewById(R.id.iv_1);
        iv2 = findViewById(R.id.iv_2);
        tv1 = findViewById(R.id.tv_1);
        tv2 = findViewById(R.id.tv_2);
        tv_title = findViewById(R.id.tv_title);
        tv_title.setText(tv_title.getText()+"\n压缩耗时:"+(System.currentTimeMillis() - startTime)/1000+"s");

        //显示缩略图
        showThumail();

        info1 = VideoInfo.getInfo(originalFile);
        info2 = VideoInfo.getInfo(compressedFile);

        tv1.setText(info1.toString());
        tv2.setText(info2.toString());
        if(!AppUtils.isAppDebug()){
            findViewById(R.id.btn_replace).setVisibility(View.GONE);
        }


    }

    private void showThumail() {
        try{
            {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(originalFile);
                Bitmap embeddedPicture = retriever.getFrameAtTime();
                if(embeddedPicture != null ){
                    iv1.setImageBitmap(embeddedPicture);

                }
            }

            {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(compressedFile);
                Bitmap embeddedPicture = retriever.getFrameAtTime();
                if(embeddedPicture != null ){
                    iv2.setImageBitmap(embeddedPicture);

                }
            }
        }catch (Throwable throwable){
            LogUtils.w(throwable);
        }



    }

    public void replace(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        //todo
                       // FileUtils.copy(new FileInputStream(compressedFile),new FileOutputStream(originalFile));
                    }

                    //String path,boolean canHaveUI, Observer<Boolean> callBack
                    ReflectUtils.reflect("com.hss01248.takephoto.demo.FileDeleteUtil")
                            .method("deleteImage", MediaCodecCompressImpl.uriMap.get(originalFile)+"",
                                    true, new Observer<Boolean>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onNext(Boolean aBoolean) {

                                    if(aBoolean){
                                        File file = new File(compressedFile);
                                        boolean b = VideoCopyer.copyVideoToXCompressed(getApplication(), file, file.getName());
                                        ToastUtils.showShort("保存到mediastore成功: "+b);
                                    }else {
                                        ToastUtils.showShort("替换失败");
                                    }
                                }

                                @Override
                                public void onError(Throwable e) {

                                }

                                @Override
                                public void onComplete() {

                                }
                            });


                    //new File(compressedFile).delete();
                  //  CompressHepler.refreshMediaCenter(getApplication(),originalFile);
                   // toast("覆盖成功");
                } catch (Exception e) {
                    e.printStackTrace();
                    toast("覆盖失败:"+e.getMessage());

                }

            }
        }).start();
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    public void cancel(View view) {
        new File(compressedFile).delete();
        toast("已删除压缩的文件");
    }

    public void keepBoth(View view) {
        File file = new File(compressedFile);
        boolean b = VideoCopyer.copyVideoToXCompressed(getApplication(), file, file.getName());
        ToastUtils.showShort("保存到mediastore成功: "+b);
        //CompressHepler.refreshMediaCenter(getApplication(),compressedFile);
        //toast("ok");
    }

    private void toast(String ok) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CompressCompareActivity.this,ok,Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void view1(View view) {
        if(VideoCompressUtil.iPreviewVideo != null){
            VideoCompressUtil.iPreviewVideo.preview(this,originalFile);
        }

    }

    public void view2(View view) {
        if(VideoCompressUtil.iPreviewVideo != null){
            VideoCompressUtil.iPreviewVideo.preview(this,compressedFile);
        }
    }
}
