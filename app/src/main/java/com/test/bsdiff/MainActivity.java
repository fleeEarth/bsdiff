package com.test.bsdiff;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.v4.BuildConfig;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import com.test.bsdiff.ndk.ApkExtract;
import com.test.bsdiff.ndk.PatchUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void update(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("版本更新");
        builder.setMessage("发现新版本V：1.6.5");
//        builder.setMessage("暂未发现新版本");
        builder.setPositiveButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton("更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                }else {
                    String url = "http://192.168.2.161:8080/myboot/downpatch?version=1.6.7";
                    final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    File file =  Environment.getExternalStorageDirectory();
//                    File file = getCacheDir();
//                    String _thisapk = ApkExtract.extract(MainActivity.this).replace("base.apk","");
//                    File file = new File(_thisapk);
                    System.out.println(file.getAbsolutePath());
//                    request.setDestinationInExternalPublicDir(file.getAbsolutePath(), "patch.patch");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE | DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    File patch = new File(file, "patch.patch");
                    if(patch.exists()){
                        patch.delete();
                    }
                    request.setDestinationUri(Uri.fromFile(patch));
                    request.setTitle("下载");
                    request.setDescription("差异包正在下载");
                    DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    final long id = manager.enqueue(request);
                    IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                    registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            long ID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                            if(ID==id){
                                doBspatch();
                            }
                        }
                    },intentFilter);
                }

            }
        });
        builder.create();
        builder.show();

    }


    private void doBspatch() {
        final File destApk = new File(Environment.getExternalStorageDirectory(), "dest.apk");
        final File patch = new File(Environment.getExternalStorageDirectory(), "patch.patch");        //一定要检查文件都存在
        final String _thisapk = ApkExtract.extract(this);
        System.out.println(patch.getAbsolutePath());
        System.out.println(patch.exists());
        int result = PatchUtils.bspatch(_thisapk,destApk.getAbsolutePath(),patch.getAbsolutePath());
        System.out.println(result);
        if(result==0){
            if (destApk.exists()) {
                ApkExtract.install(MainActivity.this, destApk.getAbsolutePath());
            }
        }



    }



}

