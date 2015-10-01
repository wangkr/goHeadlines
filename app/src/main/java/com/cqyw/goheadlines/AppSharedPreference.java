package com.cqyw.goheadlines;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.preference.PreferenceManager;

import com.cqyw.goheadlines.camera.CameraHelper;
import com.cqyw.goheadlines.config.Constant;

/**
 * Created by Kairong on 2015/9/29.
 * mail:wangkrhust@gmail.com
 */
public class AppSharedPreference {
    private static Context mContext;

    public AppSharedPreference(){
    }
    public void init(Context mContext){
        this.mContext = mContext;
    }
    public static void setCameraPos(int cameraPos){
        SharedPreferences lastCamPos = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = lastCamPos.edit();
        editor.putInt(Constant.defCamPosKey,cameraPos);
        editor.apply();
    }
    public static int getCameraPos(){
        SharedPreferences camPos = PreferenceManager.getDefaultSharedPreferences(mContext);
        return camPos.getInt(Constant.defCamPosKey, Camera.CameraInfo.CAMERA_FACING_FRONT);
    }
    public static void setFlashLight(int mode){
        // 写入用户闪光灯首选项
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constant.defFlashLightKey,mode);
        editor.apply();
    }
    public static int getFlashLight(){
        SharedPreferences camPos = PreferenceManager.getDefaultSharedPreferences(mContext);
        return camPos.getInt(Constant.defFlashLightKey, CameraHelper.FLIGHT_OFF);
    }
}
