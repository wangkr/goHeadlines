package com.cqyw.goheadlines;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Display;

import com.cqyw.goheadlines.camera.CameraUtil;
import com.cqyw.goheadlines.config.Constant;
import com.cqyw.goheadlines.util.FileUtils;
import com.cqyw.goheadlines.util.Logger;


/**
 * Created by Kairong on 2015/9/23.
 * mail:wangkrhust@gmail.com
 */
public class WelcomeActivity extends MonitoredActivity {

    private String TAG = "WelcomeActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        // 初始化用户首选项参数
        new AppSharedPreference().init(getApplicationContext());
        // 删除App缓存目录
        FileUtils fileUtils = new FileUtils(getApplicationContext());
        fileUtils.deleteCacheDir();
        // 创建App缓存文件夹
        Constant.CACHEPATH = fileUtils.makeAppDir();
        Logger.d(TAG, "Constant.CACHEPATH = " + Constant.CACHEPATH);

        Display display = getWindowManager().getDefaultDisplay();
        Constant.displayWidth = display.getWidth();
        Constant.displayHeight = display.getHeight();
        Logger.d(TAG, "Constant.displayWidth = " + Constant.displayWidth + "-- Constant.displayHeight = " + Constant.displayHeight);
        Constant.scale = getResources().getDisplayMetrics().density;
        Logger.d(TAG, "scale = " + Constant.scale);

        Constant.picWidth = Constant.displayWidth;
        Constant.picHeight = Math.round((float) Constant.picWidth / Constant.pictureRatio);

        Constant.hasFlashLight = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        Constant.canAutoFocus = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);

        CameraUtil cameraUtil = CameraUtil.getCameraUtil();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Thread.sleep(500);
                    Intent start = new Intent(WelcomeActivity.this,MainActivity.class);
                    startActivity(start);
                    finish();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
