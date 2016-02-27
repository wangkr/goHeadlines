package com.cqyw.goheadlines;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.cqyw.goheadlines.camera.CameraHelper;
import com.cqyw.goheadlines.config.Constant;
import com.cqyw.goheadlines.config.Utils;
import com.cqyw.goheadlines.picture.crop.CropImageActivity;
import com.cqyw.goheadlines.util.Logger;
import com.cqyw.goheadlines.widget.horizonListView.*;
import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.sharesdk.framework.ShareSDK;

/**
 * Created by Kairong on 2015/9/20.
 * mail:wangkrhust@gmail.com
 */
public class MainActivity extends MonitoredActivity implements SurfaceHolder.Callback,View.OnClickListener{

    private CameraHelper cameraHelper;
    private SurfaceView surface;
    private ImageView focus_view;                // 显示对焦光标
    private ImageView flash_light;               // 闪光灯
    private ImageView switch_camera;             // 切换摄像头
    private ImageView cover_view;                // 封面图片
    private ImageView expand_button;             // 扩展按钮
    private ImageView gallery_button;            // 本地相册按钮
    private SeekBar camera_zoom_bar;             // 相机缩放
    // 手势控制
    private ScaleGestureDetector scaleGestureDetector;
    private View.OnTouchListener onTouchListener;

    /*显示封面选项菜单*/
    private boolean ifCoverMenuShown = true;
    private String[] stat_cover_items;

    enum ANIM_TYPE{SCALE,HIDE};

    private int barrier_height = 0;     // 拍照的遮幅高度
    private int curCoverIndex = 0;      // 当前选择的封面

    private String TAG = "MainActivity";
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        ShareSDK.initSDK(this);
        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera);
        initCameraHelper();
        // 初始化视图
        initViews();

        onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    Rect surfaceRect = new Rect();
                    view.getDrawingRect(surfaceRect);
                    int focus_view_size = getResources().getDimensionPixelSize(R.dimen.focus_view_size);
                    int photo_bar_height = getResources().getDimensionPixelSize(R.dimen.camera_top_bar_height) + Math.round(focus_view_size / 2);
                    int bottom_bar_height = getResources().getDimensionPixelSize(R.dimen.camera_bottom_bar_height) + Math.round(focus_view_size / 2);
                    surfaceRect.top += photo_bar_height;
                    surfaceRect.bottom -= bottom_bar_height;
                    surfaceRect.left += Math.round(focus_view_size / 2);
                    surfaceRect.right -= Math.round(focus_view_size / 2);
                    float m_X = motionEvent.getX(0);
                    float m_Y = motionEvent.getY(0);
                    if (surfaceRect.contains((int) m_X, (int) m_Y)) {
                        if (cameraHelper.isPreviewing
                                && !cameraHelper.focusing
                                && cameraHelper.camera_position == Camera.CameraInfo.CAMERA_FACING_BACK
                                ) {
                            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(focus_view.getLayoutParams());
                            // 将focus_view中心移动到点击的地方
                            lp.setMargins((int) (m_X - focus_view_size / 2), (int) (m_Y - focus_view_size / 2), 0, 0);
                            focus_view.setLayoutParams(lp);
                            cameraHelper.autoFocus(false, false);
                        }
                    } else {
                        return scaleGestureDetector.onTouchEvent(motionEvent);
                    }
                }
                return true;
            }
        };

        // 设置“点击聚焦”和拍照模式菜单弹回
        surface.setOnTouchListener(onTouchListener);
    }

    // 初始化cameraHelper
    private void initCameraHelper(){
        SurfaceHolder holder;
        surface = (SurfaceView)findViewById(R.id.surfaceview);
        holder = surface.getHolder();   //获得句柄
        holder.addCallback(this);       //添加回调
        // surfaceview不维护自己的缓冲区，等待屏幕渲染引擎将内容推送到用户面前
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        cameraHelper = new CameraHelper(this,holder,handler);
    }

    // 初始化视图资源
    private void initViews(){
        // 设置控件资源ID
        HorizontalListView coverList = (HorizontalListView)findViewById(R.id.cover_icon_list);
        LinearLayout previewing_barrier = (LinearLayout)findViewById(R.id.barrier);

        focus_view = (ImageView)findViewById(R.id.focus_view);
        cover_view = (ImageView)findViewById(R.id.cover);
        camera_zoom_bar = (SeekBar)findViewById(R.id.camera_zoom_bar);
        switch_camera = (ImageView)findViewById(R.id.switch_camera);
        expand_button = (ImageView)findViewById(R.id.expand_button);
        gallery_button = (ImageView)findViewById(R.id.gallery_button);
        flash_light = (ImageView)findViewById(R.id.flash_light);

        // 设置监听
        expand_button.setOnClickListener(this);
        gallery_button.setOnClickListener(this);
        flash_light.setOnClickListener(this);
        switch_camera.setOnClickListener(this);
        findViewById(R.id.shutter).setOnClickListener(this);


        int camera_top_bar_height = getResources().getDimensionPixelSize(R.dimen.camera_top_bar_height);
        int barrier_margin_top = Constant.picHeight + camera_top_bar_height;
        // 初始化遮幅高度
        barrier_height = Constant.displayHeight - barrier_margin_top;
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(Constant.displayWidth,barrier_height);
        View v = new View(this);
        previewing_barrier.addView(v, lp);
        // 设置封面
        cover_view.setImageResource(Constant.coverResIds[0]);

        // 初始化封面选项菜单
        String[] coverTitles = getResources().getStringArray(R.array.cover_title);
        HorizontalListViewAdapter coverListAdater = new HorizontalListViewAdapter(this,Constant.coverIcnIds,coverTitles);
        coverList.setAdapter(coverListAdater);
        coverList.setOnItemClickListener(onItemClickListener_coverList);

        // 闪光灯图标的显示
        if(!Constant.hasFlashLight){
            cameraHelper.flashLightMode = CameraHelper.FLIGHT_NONE;
            flash_light.setVisibility(View.GONE);
        }else {
            // 读取闪光灯默认选项
            cameraHelper.flashLightMode = AppSharedPreference.getFlashLight();
            setFlashLightView(AppSharedPreference.getFlashLight());
        }
        // 切换摄像头图标的显示
        findViewById(R.id.switch_camera).setVisibility(cameraHelper.cameraCount < 2?View.GONE:View.VISIBLE);

        stat_cover_items = getResources().getStringArray(R.array.stat_cover_item);

        camera_zoom_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (cameraHelper.isPreviewing && !cameraHelper.focusing) {
                    float scaleFactor = progress*1f / 100 + 1;
                    cameraHelper.setCameraZoom(scaleFactor);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        camera_zoom_bar.setMax(100);

    }
    private AdapterView.OnItemClickListener onItemClickListener_coverList = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(position != curCoverIndex){
                cover_view.setImageResource(Constant.coverResIds[position]);
                curCoverIndex = position;
            }
        }
    };

    public void startFocusViewAnimation(final ANIM_TYPE type){
        Animation.AnimationListener AniListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if(type == ANIM_TYPE.HIDE){
                    focus_view.setVisibility(View.INVISIBLE);
                }
                focus_view.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        if(type == ANIM_TYPE.SCALE) {
            ScaleAnimation scaleAnimation = new ScaleAnimation(2f, 1f, 2f, 1f, Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5f);
            scaleAnimation.setDuration(1000);
            scaleAnimation.setAnimationListener(AniListener);
            focus_view.startAnimation(scaleAnimation);
        }else if(type == ANIM_TYPE.HIDE){
            AlphaAnimation alphaAnimation = new AlphaAnimation(1f,1f);
            alphaAnimation.setDuration(1000);
            alphaAnimation.setAnimationListener(AniListener);
            focus_view.startAnimation(alphaAnimation);
        }
    }

    /**
     * 为了防止系统gc时发生内存泄露
     * 自定义的一个Handler静态类
     */
    static class viCmHandler extends Handler {
        WeakReference<MainActivity> mActivity;
        viCmHandler(MainActivity activity){
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity theActivity = mActivity.get();
            switch (msg.what) {
                case CameraHelper.MSG_FOCUSING:
                    theActivity.focus_view.setImageResource(R.mipmap.focus_icn);
                    theActivity.focus_view.setVisibility(View.VISIBLE);
                    theActivity.startFocusViewAnimation(ANIM_TYPE.SCALE);
                    break;
                case CameraHelper.MSG_FOCUSED:
                    if (theActivity.cameraHelper.camera_position == Camera.CameraInfo.CAMERA_FACING_BACK
                            &&theActivity.cameraHelper.isPreviewing) {
                        theActivity.focus_view.setImageResource(R.mipmap.focus_success_icn);
                        theActivity.focus_view.setVisibility(View.VISIBLE);
                        theActivity.startFocusViewAnimation(ANIM_TYPE.HIDE);
                    }
                    break;
                case CameraHelper.MSG_FOCUS_FAILED:
                    if (theActivity.cameraHelper.camera_position == Camera.CameraInfo.CAMERA_FACING_BACK
                            &&theActivity.cameraHelper.isPreviewing) {
                        theActivity.focus_view.setImageResource(R.mipmap.focus_failed_icn);
                        theActivity.focus_view.setVisibility(View.VISIBLE);
                        theActivity.startFocusViewAnimation(ANIM_TYPE.HIDE);
                    }
                    break;
                case CameraHelper.SAVE_PICTURE_DONE:
                    Bundle childMsgBundle = msg.getData();
                    String savedPath = childMsgBundle.getString(Constant.childMsgKey);
                    Logger.d(theActivity.TAG, "send file:" + savedPath);

                    if(savedPath==null) {
                        Toast.makeText(theActivity,"保存图片出错!",Toast.LENGTH_SHORT).show();
                        break;
                    }
                    Intent intent = new Intent(theActivity,CropImageActivity.class);
                    intent.putExtra(Constant.IMAGE_URI,Uri.fromFile(new File(savedPath)));
                    intent.putExtra(Constant.COVER_INDEX, theActivity.curCoverIndex);
                    theActivity.startActivity(intent);
                    break;
                case CameraHelper.SAVED_ERROR:
                    theActivity.cameraHelper.restartPreview();
                    Toast.makeText(theActivity,"发生错误,请重新拍照!",Toast.LENGTH_SHORT).show();
                    break;
                case CameraHelper.MSG_TAKE_PICTURE:
                    theActivity.takePhoto();
                    break;
                case CameraHelper.MSG_EXIT_APP:
                    theActivity.finish();
                    break;
            }
            super.handleMessage(msg);
        }
    }
    private viCmHandler handler = new viCmHandler(this);

    // 全局控件点击事件监听
    public void onClick(View v) {

        switch (v.getId())
        {
            case R.id.switch_camera:
                // 切换前后摄像头
                switchCamera();
                break;
            case R.id.shutter:
                // 拍照
                takePhoto();
                break;
            case R.id.flash_light:
                // 闪光灯
                switchFlashLightMode();
                break;
            case R.id.expand_button:
                // 显示封面选项
                showCoverList();
                break;
            case R.id.gallery_button:
                // 显示本地相册
                showLocalGallery();
                break;
        }
    }

    /**
     * 切换摄像头
     */
    private void switchCamera(){
        cameraHelper.switchCamera();
        AppSharedPreference.setCameraPos(cameraHelper.camera_position);
        if(cameraHelper.camera_position == Camera.CameraInfo.CAMERA_FACING_BACK){
            switch_camera.setImageResource(R.mipmap.ic_camera_front_white_24dp);
            flash_light.setVisibility(Constant.hasFlashLight?View.VISIBLE:View.GONE);
            cameraHelper.autoFocus(true,false);
        }else{
            switch_camera.setImageResource(R.mipmap.ic_camera_rear_white_24dp);
            flash_light.setVisibility(View.GONE);
            focus_view.setVisibility(View.GONE);
        }
    }
    /**
     * 显示/隐藏 封面选项面板
     */
    private void showCoverList(){
        final LinearLayout cover_icn_rl = (LinearLayout)findViewById(R.id.cover_icn_rl);
        if(ifCoverMenuShown){
            expand_button.setImageResource(R.mipmap.expand_icn);
            cover_icn_rl.setVisibility(View.INVISIBLE);
            ifCoverMenuShown = false;
        }else{
            expand_button.setImageResource(R.mipmap.collapse_icn);
            cover_icn_rl.setVisibility(View.VISIBLE);
            ifCoverMenuShown = true;
        }
    }
    /**
     * 加载本地相册
     */
    private void showLocalGallery(){
        cameraHelper.stop();
        // 打开本地相册
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 0);
            }
        },100);
    }

    /**
     * 切换闪光灯状态
     */
    public void switchFlashLightMode(){
        switch (cameraHelper.flashLightMode){
            case CameraHelper.FLIGHT_OFF:
                cameraHelper.switchFlashLightMode();
                setFlashLightView(CameraHelper.FLIGHT_ON);
                break;
            case CameraHelper.FLIGHT_ON:
                cameraHelper.switchFlashLightMode();
                setFlashLightView(CameraHelper.FLIGHT_AUTO);
                break;
            case CameraHelper.FLIGHT_AUTO:
                cameraHelper.switchFlashLightMode();
                setFlashLightView(CameraHelper.FLIGHT_OFF);
                break;
            default:
                break;
        }
    }
    /**
     * 设置闪光灯状态
     */
    private void setFlashLightView(int mode){
        switch(mode){
            case CameraHelper.FLIGHT_OFF:
                flash_light.setImageResource(R.mipmap.ic_flash_off_white_24dp);
                break;
            case CameraHelper.FLIGHT_ON:
                flash_light.setImageResource(R.mipmap.ic_flash_on_white_24dp);
                break;
            case CameraHelper.FLIGHT_AUTO:
                flash_light.setImageResource(R.mipmap.ic_flash_auto_white_24dp);
                break;
            default:
                break;
        }
        // 写入用户闪光灯首选项
        AppSharedPreference.setFlashLight(mode);
    }
    /**
     * 得到裁剪宽高比选项菜单内容
     * @param vlist_image:图片资源id
     * @param vlist_text:描述字符串
     * @return
     */
    private List<Map<String,Object>> getData(int[] vlist_image,String[] vlist_text){
        List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();

        Map<String,Object> map = null;
        for(int i = 0;i < vlist_image.length;i++){
            map = new HashMap<String,Object>();
            map.put("vlist_image",vlist_image[i]);
            map.put("vlist_text",vlist_text[i]);
            list.add(map);
        }

        return list;

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder,int format, int width, int height){

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // 如果只有一个摄像头，则默认打开后置摄像头，否则打开前置摄像头
        camera_zoom_bar.setProgress(0);
        if(cameraHelper.cameraCount == 1) {
            cameraHelper.open();
            switch_camera.setVisibility(View.GONE);
        } else if(cameraHelper.cameraCount >=2){
            int defCamPos = AppSharedPreference.getCameraPos();
            if (defCamPos == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                flash_light.setVisibility(View.GONE);
                switch_camera.setImageResource(R.mipmap.ic_camera_rear_white_24dp);
            } else {
                switch_camera.setImageResource(R.mipmap.ic_camera_front_white_24dp);
            }
            cameraHelper.open(defCamPos);
        }
//
//        screenOrnDetector = new ScreenOrnDetector(this);
//
//        screenOrnDetector.registerOnShakeListener(onSrcnListener);
//        // 启动监听
//        screenOrnDetector.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // 置预览回调为空，再关闭预览
        cameraHelper.stop();
//        screenOrnDetector.stop();
        surface = null;
    }

    /**
     * 根据屏幕方向对按钮进行旋转
     */
    private void Btns_Rotate(){
        // 布局默认为竖屏布局方式
    }
    /**
     * 带自动对焦功能的拍照
     */
    private void takePhoto(){
        // 后置摄像头拍照
        if(cameraHelper.camera_position == Camera.CameraInfo.CAMERA_FACING_BACK) {
//            picTakenScreenOrn = screenOrn;
            if (!cameraHelper.focusing) {
                // 当前为对焦完成状态则快速拍照
                if(cameraHelper.focuseState)
                    takePhotoThread();
                else{
                    // 否则自动对焦一次，等到对焦完成，再拍照
                    cameraHelper.autoFocus(true,true);
                }
            }
        }else if(cameraHelper.camera_position == Camera.CameraInfo.CAMERA_FACING_FRONT){// 前置摄像头拍照
//            picTakenScreenOrn = screenOrn;
            takePhotoThread();
        }
    }

    /**
     * 拍照线程
     */
    private void takePhotoThread(){
        // 统计拍照的摄像头
        if(cameraHelper.camera_position == Camera.CameraInfo.CAMERA_FACING_FRONT){
            MobclickAgent.onEvent(this,Constant.stat_camera_type_front);
        } else {
            MobclickAgent.onEvent(this,Constant.stat_camera_type_back);
        }
        // 统计封面的种类使用
        MobclickAgent.onEvent(this,stat_cover_items[curCoverIndex]);

        Utils.startBackgroundJob(MainActivity.this, null, "正在处理...",
                new Runnable() {
                    public void run() {
                        cameraHelper.takePhoto(/*picTakenScreenOrn, barrier_height*/);
                    }
                }, handler
        );
    }

    /**
     * 设置屏幕高亮
     */
    private void setScreen(){
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams lp = this.getWindow().getAttributes();
        lp.screenBrightness = 1;
        this.getWindow().setAttributes(lp);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode!=RESULT_OK) {
            Toast.makeText(getApplicationContext(),"没有选择任何图片!",Toast.LENGTH_SHORT).show();
            // 如果只有一个摄像头，则默认打开后置摄像头，否则打开前置摄像头
            if(cameraHelper.cameraCount == 1) {
                cameraHelper.open();
            } else if(cameraHelper.cameraCount >=2){
                int defCamPos = AppSharedPreference.getCameraPos();
                if (defCamPos == Camera.CameraInfo.CAMERA_FACING_FRONT)
                    flash_light.setVisibility(View.GONE);
                cameraHelper.open(defCamPos);
            }
            return;
        }
        switch (requestCode) {
            case 0:
                Uri uri = data.getData();
                Intent intent = new Intent(MainActivity.this, CropImageActivity.class);
                intent.putExtra(Constant.IMAGE_URI,uri);
                intent.putExtra(Constant.COVER_INDEX, curCoverIndex);
                startActivity(intent);
                break;
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setScreen();
    }


    //无意中按返回键时要释放内存
    @Override
    public void onBackPressed() {
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
