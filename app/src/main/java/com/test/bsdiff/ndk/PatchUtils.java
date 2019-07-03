package com.test.bsdiff.ndk;

public class PatchUtils {

    private static PatchUtils instance;

    static {
        System.loadLibrary("native-lib");
    }

    public static PatchUtils getInstance(){
        if(instance==null){
            instance = new PatchUtils();
        }
        return instance;
    }

    /**
     * native方法 使用路径为oldApkPath的Apk与路径为patchPath的补丁包合成一个新的apk存储与newApkPath
     * 返回 0 成功
     * @param oldApkPath   示例：/usr/old/old.apk
     * @param newApkPath   示例：/usr/new/new.apk
     * @param patchPath    示例：/usr/patch/patch_1.patch
     * @return
     */
    public static native int bspatch(String oldApkPath,String newApkPath,String patchPath);


}
