package com.test.bsdiff;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.test.bsdiff.ndk.ApkExtract;
import com.test.bsdiff.ndk.PatchUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void update(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("版本更新");
        builder.setMessage("发现新版本V：1.6.3");
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
                } else {
                    String url = "http://192.168.2.161:8080/myboot/downpatch?version=1.6.7";
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
//                    File file = getFilesDir();
                    File file = Environment.getExternalStorageDirectory();
//                    File file = Environment.getExternalStorageDirectory();
                    request.setDestinationInExternalPublicDir(file.getAbsolutePath(), "differential.patch");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE | DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    final long id = manager.enqueue(request);
                    IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                    registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            long ID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                            if(ID==id){
                                doBspatch(MainActivity.this);
                            }
                        }
                    },intentFilter);
                }

            }
        });
        builder.create();
        builder.show();

    }


    private void doBspatch(Context context) {
//        final File destApk = new File(context.getFilesDir().getAbsolutePath(), "Synthesis.apk");
//        final File patch = new File(context.getFilesDir().getAbsolutePath(), "differential.patch");        //一定要检查文件都存在
        final File destApk = new File(Environment.getExternalStorageDirectory(), "dest.apk");
        final File patch = new File(Environment.getExternalStorageDirectory(), "differential.patch");        //一定要检查文件都存在
        final String _thisapk = ApkExtract.extract(this);
        System.out.println(destApk);
        System.out.println(patch);
        System.out.println(_thisapk);
        int result = PatchUtils.bspatch(_thisapk,destApk.getAbsolutePath(),patch.getAbsolutePath());
        System.out.println(result);
        if (destApk.exists()) {
            ApkExtract.install(this, destApk.getAbsolutePath());
        }
    }

}
