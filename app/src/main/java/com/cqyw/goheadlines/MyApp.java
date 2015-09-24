package com.cqyw.goheadlines;

import android.app.Application;
import android.os.Environment;

import java.io.File;

/**
 * Created by Kairong on 2015/9/20.
 * mail:wangkrhust@gmail.com
 */
public class MyApp extends Application {

    /*屏幕宽*/
    private int screenWidth;
    /*屏幕高*/
    private int screenHeight;
    /*应用存放根目录 /mnt/extSdCard/VisionIntuition/ */
    private String rootFileDir;
    /*应用临时文件存放目录 $rootFileDir/temp/ */
    private String tempFileDir;
    /*应用存放保存照片的目录 $rootFileDir/photo/ */
    private String savedPhotoDir;

    public static MyApp getInstance(){
        return myApp;
    }

    public boolean initDirPath(){
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)&&
                !Environment.getExternalStorageState().equals(Environment.MEDIA_SHARED)){
            return false;
        }
        File dir = new File(Environment.getExternalStorageDirectory(), getResources().getString(R.string.app_name));
        if (!dir.exists()) {
            dir.mkdir();
        }
        rootFileDir = dir.getPath();
        File tempdir = new File(dir,"temp");
        if(!tempdir.exists()){
            tempdir.mkdir();
        }
        tempFileDir = tempdir.getPath();
        File photodir = new File(dir,"photo");
        if(!photodir.exists()){
            photodir.mkdir();
        }
        savedPhotoDir = photodir.getPath();

        return true;
    }

    public void setScreenWidth(int screenWidth){
        this.screenWidth = screenWidth;
    }
    public void setScreenHeight(int screenHeight){
        this.screenHeight = screenHeight;
    }

    public int getScreenWidth(){
        return this.screenWidth;
    }
    public int getScreenHeight(){
        return this.screenHeight;
    }
    public String getRootFileDir(){
        return rootFileDir;
    }

    public String getSavedPhotoDir() {
        return savedPhotoDir;
    }

    public String getTempFileDir() {
        return tempFileDir;
    }

    public static void initViApp(Application app){
        if(myApp == null){
            myApp = (MyApp)app;
        }
    }

    private static MyApp myApp = null;
}
