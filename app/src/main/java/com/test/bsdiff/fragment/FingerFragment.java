package com.test.bsdiff.fragment;

import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.os.CancellationSignal;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.test.bsdiff.R;
import com.test.bsdiff.activity.MainActivity;

import javax.crypto.Cipher;

public class FingerFragment extends DialogFragment {
    private FingerprintManager fingerprintManager ;
    private CancellationSignal mCancellationSignal;
    private Cipher mCipher;
    private MainActivity mActivity;
    private TextView errorMsg;
    /**
     * 标识是否是用户主动取消的认证。
     */
    private boolean isSelfCancelled;

    public void setCipher(Cipher cipher) {
        mCipher = cipher;
    }

    public void setFingerprintManager(FingerprintManager fingerprintManager){
        this.fingerprintManager = fingerprintManager;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (MainActivity) getActivity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL,android.R.style.Theme_Material_Light_Dialog);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_finger, container, false);
        errorMsg =  v.findViewById(R.id.cancel);
        TextView cancel = v.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                stopListening();
            }
        });
        return v;
    }


    @Override
    public void onResume() {
        super.onResume(); // 开始指纹认证监听
        startListening(mCipher);
    }

    @Override
    public void onPause() {
        super.onPause(); // 停止指纹认证监听
        stopListening();
    }


    @TargetApi(23)
    private void startListening(Cipher cipher) {

        isSelfCancelled = false;
        mCancellationSignal = new CancellationSignal();
        fingerprintManager.authenticate(new FingerprintManager.CryptoObject(cipher), mCancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
            int i = 5;
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                if (!isSelfCancelled) {
                    errorMsg.setText(errString);
                    if (errorCode == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT) {
                        Toast.makeText(mActivity, errString, Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                }
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                errorMsg.setText(helpString);
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                Toast.makeText(mActivity, "指纹认证成功", Toast.LENGTH_SHORT).show();
                mActivity.onAuthenticated();
            }

            @Override
            public void onAuthenticationFailed() {
            errorMsg.setText("指纹认证失败，还剩"+i+"次机会");
            i--;
        }
    },null);
    }


    private void stopListening() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
            isSelfCancelled = true;
        }
    }
}
