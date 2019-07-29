package com.test.bsdiff.activity;


import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.FragmentManager;
import android.app.KeyguardManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import com.test.bsdiff.R;
import com.test.bsdiff.fragment.FingerFragment;
import com.test.bsdiff.ndk.ApkExtract;
import com.test.bsdiff.ndk.PatchUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.security.KeyStore;


public class MainActivity extends AppCompatActivity {

    private static final String DEFAULT_KEY_NAME = "default_key";
    private KeyStore keyStore;
    private FragmentManager fragmentManager;
    private FingerprintManager fingerprintManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Switch mSwitch = findViewById(R.id.zhiwenkey);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    System.out.println("open");
                    if(!checkBuildVersion()){
                        mSwitch.setChecked(false);
                    }else{
                        initKey();
                        initCipher();
                    }
                }else{
                    System.out.println("close");
                }
            }
        });
    }

    /**
     * 生成并存放密钥
     */
    @TargetApi(23)
    private void initKey() {
        try {
            //产生密钥
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(DEFAULT_KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    //设置用户需要验证
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            keyGenerator.init(builder.build());
            //生成密钥
            keyGenerator.generateKey();
            //存放密钥
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 初始化Cipher对象，并将对象传递
     */
    @TargetApi(23)
    private void initCipher() {
        try {
            SecretKey key = (SecretKey) keyStore.getKey(DEFAULT_KEY_NAME, null);
            Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            showFingerFragment(cipher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 调用指纹识别
     * @param cipher
     */
    private void showFingerFragment(Cipher cipher) {
        FingerFragment fingerFragment = new FingerFragment();
        fingerFragment.setCipher(cipher);
        fingerFragment.setFingerprintManager(fingerprintManager);
        fragmentManager = getFragmentManager();
        fingerFragment.show(fragmentManager,"指纹识别窗口");
    }

    /**
     * 检测系统是否支持或开启指纹识别
     * @return
     */
    public boolean checkBuildVersion(){
        if (Build.VERSION.SDK_INT < 23) {
            Toast.makeText(this, "您的系统版本过低，不支持指纹功能", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
            fingerprintManager = getSystemService(FingerprintManager.class);
            if (!fingerprintManager.isHardwareDetected()) {
                Toast.makeText(this, "您的手机不支持指纹功能", Toast.LENGTH_SHORT).show();
                return false;
            } else if (!keyguardManager.isKeyguardSecure()) {
                Toast.makeText(this, "您还未设置锁屏，请先设置锁屏并添加一个指纹", Toast.LENGTH_SHORT).show();
                ComponentName cm = new ComponentName("com.android.settings","com.android.settings.ChooseLockGeneric");
                Intent intent = new Intent("/");
                intent.setComponent(cm);
                startActivityForResult(intent,0);
                return false;
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                Toast.makeText(this, "您至少需要在系统设置中添加一个指纹", Toast.LENGTH_SHORT).show();
                ComponentName cm = new ComponentName("com.android.settings","com.android.settings.fingerprint.FingerprintSettingsActivity");
                Intent intent = new Intent("/");
                intent.setComponent(cm);
                startActivityForResult(intent,0);
                return false;
            }
        }
        return true;
    }

    /**
     * 验证成功回调
     */
    public void onAuthenticated() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    public void update(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("版本更新");
        builder.setMessage("未发现新版本!");
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
//                    File file =  getCacheDir();
                    File file = Environment.getExternalStorageDirectory();
//                    String _thisapk = ApkExtract.extract(MainActivity.this).replace("base.apk","");
//                    File file = new File(_thisapk);
                    System.out.println(file.getAbsolutePath());
//                    request.setDestinationInExternalPublicDir(file.getAbsolutePath(), "patch.patch");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE | DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    File patch = new File(file, "patch.patch");
                    if(patch.exists()){
                        patch.delete();
                    }
                    //BuildConfig包不能导入V4的包不然会报错 导入APP自身的包
//                    Uri uri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".fileProvider",patch);
//                    request.setDestinationUri(uri);
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
//        final File destApk = new File(this.getCacheDir(), "dest.apk");
//        final File patch = new File(this.getCacheDir(), "patch.patch");        //一定要检查文件都存在
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

