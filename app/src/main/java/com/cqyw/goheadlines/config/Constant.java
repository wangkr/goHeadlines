package com.cqyw.goheadlines.config;

import android.content.Context;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;

import com.cqyw.goheadlines.R;
import com.cqyw.goheadlines.util.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Kairong on 2015/9/22.
 * mail:wangkrhust@gmail.com
 */
public class Constant {
    /**
     * 设备屏幕宽度高度，MainActivity里面设置
     */
    public static int displayWidth = 0;
    public static int displayHeight = 0;
    public static int picWidth = 0;
    public static int picHeight = 0;
    public static int statusBarHeight = 0;
    public static float scale = 0;					//屏幕密度
    public static boolean hasFlashLight = false;
    public static boolean canAutoFocus = false;

    public final static float pictureRatio = 0.75f;

    public final static String childMsgKey = "savedPicturePath";
    // 默认摄像头选项
    public final static String defCamPosKey = "cameraPositionKey";
    // 默认闪光灯选项
    public final static String defFlashLightKey = "flashLightKey";

    /**
     * 指定发布图片的宽度,单位px
     */
    public static int IMAGEWIDTH = 640;

    /**
     * 图片路径
     */
    public static String IMAGE_URI = "image_uri";
    /**
     * 封面水印下标
     */
    public static String COVER_INDEX = "cover_index";
    /**
     * 加封面水印的图片路径
     */
    public static String WM_IMAGE_URI = "watermaker_image_uri";
    /**
     * 封面水印文件资源id
     */
    public final static int[] coverResIds = {R.drawable.nansheng,R.drawable.yizhou,R.drawable.science,R.drawable.nature
    ,R.drawable.kantianxia,R.drawable.jingjixueren,R.drawable.fubusi,R.drawable.time,R.drawable.easy
    ,R.drawable.ruili,R.drawable.playboy,R.drawable.erciyuan,R.drawable.yilin,R.drawable.nba
    ,R.drawable.nanfangzhoumo,R.drawable.shangtoutiao};
    /**
     * 封面水印文件ICON
     */
    public final static int[] coverIcnIds = {R.mipmap.nanshengnvsheng,R.mipmap.yizhou,R.mipmap.science,R.mipmap.nature
            ,R.mipmap.kantianxia,R.mipmap.jingjixueren,R.mipmap.fubusi,R.mipmap.time,R.mipmap.easy
            ,R.mipmap.ruili,R.mipmap.playboy,R.mipmap.erciyuan,R.mipmap.yilin,R.mipmap.nba
            ,R.mipmap.nanfangzhoumo,R.mipmap.shangtoutiao};

    /**
     * 刷新相册
     */
    public static void refreshGallery(Context mContext,File imageFile){
        Uri localUri = Uri.fromFile(imageFile);
        Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,localUri);
        mContext.sendBroadcast(localIntent);
    }
    /**
     * App缓存文件路径
     */
    public static String CACHEPATH = "";

    public static int getExifRotation(File imageFile) {
        if (imageFile == null) return -1;
        try {
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            // We only recognize a subset of orientation tag values
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return ExifInterface.ORIENTATION_UNDEFINED;
            }
        } catch (IOException e) {
            Logger.e("Error getting Exif data", e);
            return -1;
        }
    }

    public static boolean copyExifRotation(File sourceFile, File destFile) {
        if (sourceFile == null || destFile == null) return false;
        try {
            ExifInterface exifSource = new ExifInterface(sourceFile.getAbsolutePath());
            ExifInterface exifDest = new ExifInterface(destFile.getAbsolutePath());
            exifDest.setAttribute(ExifInterface.TAG_ORIENTATION, exifSource.getAttribute(ExifInterface.TAG_ORIENTATION));
            exifDest.saveAttributes();
            return true;
        } catch (IOException e) {
            Logger.e("Error copying Exif data", e);
            return false;
        }
    }

    /**
     * 将字符串转成16 位MD5值
     *
     * @param string
     * @return
     */
    public static String MD5(String string) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(
                    string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10)
                hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }
//        return hex.toString();// 32位
        return hex.toString().substring(8, 24);// 16位
    }

}
