package com.cqyw.goheadlines.picture.crop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.cqyw.goheadlines.AppSharedPreference;
import com.cqyw.goheadlines.R;
import com.cqyw.goheadlines.MonitoredActivity;
import com.cqyw.goheadlines.picture.TouchMoveImageView;
import com.cqyw.goheadlines.share.ShareActivity;
import com.cqyw.goheadlines.util.Logger;
import com.cqyw.goheadlines.config.Constant;
import com.umeng.analytics.MobclickAgent;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

/**
 *
 * @ClassName: CropImageActivity
 * @Description: The activity can crop specific region of interest from an image.图片裁剪界面
 * 					裁剪方式是移动缩放图片
 * @author shark
 * @date 2014年11月26日 上午10:53:25
 * @mail wangkrhust@gmail.com
 * @modified Kairong Wang
 *
 */
public class CropImageActivity extends MonitoredActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener{

    private String TAG = "CropImageActivity";

    public boolean mSaving = false;
    public boolean isRadiusChanged = false;
    private boolean showBlurMenu = false;
    private boolean showRotateMenu = false;
    private boolean isAnimating = false;

    private final Handler mHandler = new Handler();

    private int rotateDeg = 0;
    private int coverIdx;

    private Bitmap mBitmap;
    private TouchMoveImageView mImageView;
    private SeekBar blurRadiusBar;
    private SeekBar blurPenBar;

    private LinearLayout crop_child_menu_blur_ll;
    private LinearLayout crop_child_menu_rotate_ll;
    private LinearLayout pop_dialog_blur_radius_ll;
    private RelativeLayout pop_dialog_blurPenBar_rl;

    Uri targetUri ;

    private ContentResolver mContentResolver ;

    private int width ;
    private int height;
    private int sampleSize = 1;

    private RelativeLayout mCrViewLayout;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_crop_image);

        initViews();

        Intent intent = getIntent();
        targetUri = intent.getParcelableExtra(Constant.IMAGE_URI);
        mContentResolver = getContentResolver();

        if (mBitmap == null) {
            String path = getFilePath(targetUri);
            Logger.d(TAG,"CropImageActivity recieve file:"+path);
            //判断图片是不是旋转，是的话就进行纠正。
             rotateDeg = Constant.getExifRotation(new File(path));
            getBitmapSize();
            getBitmap();
        }

        if (mBitmap == null) {
            finish();
            return;
        }
        // 设置图片显示宽为屏幕宽度,高为宽的4/3
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(Constant.picWidth,Constant.picHeight);
        ImageView mWaterMark = new ImageView(this);
        mImageView = new TouchMoveImageView(this,mBitmap);
        mCrViewLayout.addView(mImageView, params);
        mCrViewLayout.addView(mWaterMark, params);
        // 设置默认封面
        coverIdx = getIntent().getIntExtra(Constant.COVER_INDEX, 0);
        mWaterMark.setBackgroundResource(Constant.coverResIds[coverIdx]);
        // 设置触摸监听
        mImageView.setOnTouchListener(touchViewListener);

        // 初始化seekbar
        blurRadiusBar.setMax(Constant.maxRadiusSize);
        blurPenBar.setMax(Constant.maxPenSize);
        blurRadiusBar.setProgress(Constant.defRadiuSize);
        blurPenBar.setProgress(AppSharedPreference.getPenSizePref());
    }
    // 触摸mImageView弹出菜单消失
    View.OnTouchListener touchViewListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Rect viewRect = new Rect();
            v.getDrawingRect(viewRect);
            int crop_bar_height = (int) (getResources().getDimension(R.dimen.crop_bar_height));
            viewRect.top += crop_bar_height;
            viewRect.bottom -= crop_bar_height;
            float m_X = event.getX(0);
            float m_Y = event.getY(0);
            if ( viewRect.contains((int) m_X, (int) m_Y)) {
                pop_dialog_blur_radius_ll.setVisibility(View.GONE);
                pop_dialog_blurPenBar_rl.setVisibility(View.GONE);
            }
            return false;
        }
    };
    /**
     * 初始化视图
     * @Title: initViews
     * @return void
     * @date 2012-12-14 上午10:41:23
     */
    private void initViews(){
        findViewById(R.id.crop_bar_cancell).setOnClickListener(this);
        findViewById(R.id.crop_bar_ok).setOnClickListener(this);
        findViewById(R.id.crop_bar_rotate_menu).setOnClickListener(this);
        findViewById(R.id.crop_bar_blur_menu).setOnClickListener(this);
        findViewById(R.id.crop_child_menu_blur_back).setOnClickListener(this);
        findViewById(R.id.crop_child_menu_pen_size).setOnClickListener(this);
        findViewById(R.id.crop_child_menu_blur_size).setOnClickListener(this);
        findViewById(R.id.crop_child_menu_rotate_back).setOnClickListener(this);
        findViewById(R.id.crop_child_menu_rotate).setOnClickListener(this);
        findViewById(R.id.crop_child_menu_fanzhuan).setOnClickListener(this);

        pop_dialog_blur_radius_ll = (LinearLayout)findViewById(R.id.crop_pop_dialg_blurradius);
        pop_dialog_blurPenBar_rl = (RelativeLayout)findViewById(R.id.crop_pop_dialg_pensize);
        crop_child_menu_blur_ll = (LinearLayout)findViewById(R.id.crop_blur_child_menu_ll);
        crop_child_menu_rotate_ll = (LinearLayout)findViewById(R.id.crop_rotate_child_menu_ll);

        blurRadiusBar = (SeekBar)findViewById(R.id.seekBar_blur_radius);
        blurPenBar = (SeekBar)findViewById(R.id.seekBar_pen_size);

        blurRadiusBar.setOnSeekBarChangeListener(this);
        blurPenBar.setOnSeekBarChangeListener(this);

        pop_dialog_blur_radius_ll.setVisibility(View.GONE);
        pop_dialog_blurPenBar_rl.setVisibility(View.GONE);

        int barrier_height = Constant.displayHeight-Constant.picHeight;
        mCrViewLayout = (RelativeLayout)findViewById(R.id.crop_CropImage_view);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCrViewLayout.getLayoutParams());
        int margin = barrier_height/2;
        params.setMargins(0, margin, 0, margin);
        mCrViewLayout.setLayoutParams(params);

        initShowChildMenuAnimation();

        if(!Constant.blur_menu_shown){
            findViewById(R.id.crop_menu_ll).setVisibility(View.GONE);
            findViewById(R.id.crop_rotate_child_menu_ll).setVisibility(View.VISIBLE);
            findViewById(R.id.crop_child_menu_rotate_back).setVisibility(View.GONE);
        }

    }

    // 监听点击事件
    @Override
    public void onClick(View v) {
        if(isAnimating){
            return;
        }
        switch (v.getId()){
            case R.id.crop_bar_cancell:
                // 取消
                finish();
                break;
            case R.id.crop_bar_ok:
                // 完成
                onSaveClicked();
                break;
            case R.id.crop_bar_rotate_menu:
            case R.id.crop_bar_blur_menu:
            case R.id.crop_child_menu_blur_back:
            case R.id.crop_child_menu_rotate_back:
                showChildMenu(v.getId());
                break;
            case R.id.crop_child_menu_pen_size:
                int blurPenBar_visi = pop_dialog_blurPenBar_rl.getVisibility()==View.VISIBLE?View.GONE: View.VISIBLE;
                pop_dialog_blurPenBar_rl.setVisibility(blurPenBar_visi);
                pop_dialog_blur_radius_ll.setVisibility(View.GONE);
                break;
            case R.id.crop_child_menu_blur_size:
                int blur_radius_visi = pop_dialog_blur_radius_ll.getVisibility()==View.VISIBLE?View.GONE: View.VISIBLE;
                pop_dialog_blurPenBar_rl.setVisibility(View.GONE);
                pop_dialog_blur_radius_ll.setVisibility(blur_radius_visi);
                break;
            case R.id.crop_child_menu_rotate:
                mImageView.rotate(90);
                // 旋转功能使用统计
                MobclickAgent.onEvent(this,Constant.stat_xuanzhuan);
                break;
            case R.id.crop_child_menu_fanzhuan:
                mImageView.flip_Horizon();
                // 翻转功能使用统计
                MobclickAgent.onEvent(this,Constant.stat_fanzhuan);
                break;
            default:

        }
    }

    TranslateAnimation showAni;
    TranslateAnimation hideAni;

    /**
     * 显示子菜单
     * @param Id 子菜单资源id
     */
    private void showChildMenu(int Id){
        switch (Id){
            case R.id.crop_bar_blur_menu:
                showBlurMenu = true;
                crop_child_menu_blur_ll.startAnimation(showAni);
                isAnimating = true;
                startBackgroundJob(CropImageActivity.this, null, "正在处理...",
                        new Runnable() {
                            public void run() {
                                mHandler.post(new Runnable() {
                                    public void run() {
                                        mImageView.setBluring(true);
                                    }
                                });
                            }
                        }, mHandler
                );
                if(AppSharedPreference.isFirstUseBlur()){
                    AlertDialog tip = new AlertDialog.Builder(CropImageActivity.this)
                            .setTitle("提示")
                            .setMessage("请用手指擦除不需要模糊的地方")
                            .setPositiveButton("确定",null)
                            .setCancelable(false)
                            .create();
                    tip.show();
                    AppSharedPreference.setFirstUseBlur();
                }
                break;
            case R.id.crop_child_menu_blur_back:
                pop_dialog_blurPenBar_rl.setVisibility(View.GONE);
                pop_dialog_blur_radius_ll.setVisibility(View.GONE);
                // 如果进行了编辑就提示是否返回
                if(mImageView.isEdited) {
                    AlertDialog alertDialog = new AlertDialog.Builder(this)
                            .setTitle("确定返回?")
                            .setMessage("模糊效果将不被保存,确定返回?")
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    crop_child_menu_blur_ll.startAnimation(hideAni);
                                    isAnimating = true;
                                    /// 取消模糊化效果
                                    mImageView.setBluring(false);
                                }
                            })
                            .setNegativeButton("取消",null)
                            .create();
                    alertDialog.show();
                } else {
                    crop_child_menu_blur_ll.startAnimation(hideAni);
                    isAnimating = true;
                    /// 取消模糊化效果
                    mImageView.setBluring(false);
                }
                break;
            case R.id.crop_bar_rotate_menu:
                showRotateMenu = true;
                crop_child_menu_rotate_ll.startAnimation(showAni);
                isAnimating = true;
                break;
            case R.id.crop_child_menu_rotate_back:
                crop_child_menu_rotate_ll.startAnimation(hideAni);
                isAnimating = true;
                break;

        }
    }

    // 初始化子菜单弹出动画
    private void initShowChildMenuAnimation(){
        showAni = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF,0);
        hideAni = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF,0);
        showAni.setDuration(600);
        hideAni.setDuration(600);
        showAni.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (showBlurMenu) {
                    crop_child_menu_blur_ll.clearAnimation();
                    crop_child_menu_blur_ll.setVisibility(View.VISIBLE);
                }
                if (showRotateMenu) {
                    crop_child_menu_rotate_ll.clearAnimation();
                    crop_child_menu_rotate_ll.setVisibility(View.VISIBLE);
                }
                isAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        hideAni.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if(showBlurMenu){
                    crop_child_menu_blur_ll.clearAnimation();
                    crop_child_menu_blur_ll.setVisibility(View.GONE);
                    showBlurMenu = false;
                }
                if(showRotateMenu){
                    crop_child_menu_rotate_ll.clearAnimation();
                    crop_child_menu_rotate_ll.setVisibility(View.GONE);
                    showRotateMenu = false;
                }
                isAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }


    // seekbar监听
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()){
            case R.id.seekBar_blur_radius:
                // 模糊半径改变监听
                if(isRadiusChanged){
                    startBackgroundJob(CropImageActivity.this, null, "正在处理...",
                            new Runnable() {
                                public void run() {
                                    mHandler.post(new Runnable() {
                                        public void run() {
                                            mImageView.refreshCanvas();
                                        }
                                    });
                                }
                            }, mHandler
                    );
                    isRadiusChanged = false;
                }
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()){
            case R.id.seekBar_blur_radius:
                // 模糊半径改变监听,阶梯式设置
                int newprocess;
                if(progress<=Constant.radius_stage1){
                    newprocess = Math.round(progress*Constant.radius_rate[0]);
                } else if(progress <= Constant.radius_stage2){
                    newprocess = Math.round(progress*Constant.radius_rate[1]);
                } else if(progress <= Constant.radius_stage3){
                    newprocess = Math.round(progress*Constant.radius_rate[2]);
                } else {
                    newprocess = Math.round(progress*Constant.radius_rate[3]);
                }
                mImageView.setRadius(newprocess==0?1:newprocess);
                isRadiusChanged = true;
                break;
            case R.id.seekBar_pen_size:
                // 模糊画笔大小改变监听
                mImageView.setPenSize(progress);
                float scale = Math.max((float)progress*1.2f,4f)/100;
                ((ImageView)findViewById(R.id.pen_size_view)).setScaleY(scale);
                ((ImageView)findViewById(R.id.pen_size_view)).setScaleX(scale);
                break;
        }
    }


    /**
     * 获取Bitmap分辨率，太大了就进行压缩
     * @Title: getBitmapSize
     * @return void
     * @date 2012-12-14 上午8:32:13
     */
    private void getBitmapSize(){
        InputStream is = null;
        try {

            is = getInputStream(targetUri);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);

            switch (rotateDeg){
                case 180:case 0:
                    width = options.outWidth;
                    height = options.outHeight;
                    break;
                case 90:case 270:
                    width = options.outHeight;
                    height = options.outWidth;
                    break;
                default:
                    width = options.outWidth;
                    height = options.outHeight;
            }
        }catch(IOException e) {
            e.printStackTrace();
        }finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 此处写方法描述
     * @Title: getBitmap
     * @return void
     * @date 2012-12-13 下午8:22:23
     */
    private void getBitmap(){
        InputStream is = null;
        try {

            try {
                is = getInputStream(targetUri);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            //shark 如果图片太大的话，压缩
            while ((width / sampleSize > Constant.displayWidth*2) || (height / sampleSize > Constant.displayHeight*2)) {
                sampleSize *= 2;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;

            mBitmap = BitmapFactory.decodeStream(is, null, options);
            // 进行旋转
            if(rotateDeg>0){
                Matrix m = new Matrix();
                m.setRotate(rotateDeg);
                mBitmap = Bitmap.createBitmap(mBitmap,0,0,mBitmap.getWidth(),mBitmap.getHeight(),m,false);
            }

            // 缩放图片的尺寸
            int ww = mBitmap.getWidth();
            int wh = mBitmap.getHeight();
            float scale = 0;
            if(ww*1f/wh>Constant.pictureRatio){
                scale = (float) Constant.picHeight / wh;
            }else{
                scale = (float) Constant.picWidth / ww;
            }

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            // 产生缩放后的Bitmap对象
            mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, ww, wh, matrix, false);
            Logger.w(TAG, "Width:" + mBitmap.getWidth());
            Logger.w(TAG, "Height:" + mBitmap.getHeight());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 此处写方法描述
     * @Title: rotateImage
     * @param path
     * @return void
     * @date 2012-12-14 上午10:58:26
     */
    private boolean isRotateImage(String path){

        try {
            ExifInterface exifInterface = new ExifInterface(path);

            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            if(orientation == ExifInterface.ORIENTATION_ROTATE_90 ){
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取输入流
     * @Title: getInputStream
     * @param mUri
     * @return
     * @return InputStream
     * @date 2012-12-14 上午9:00:31
     */
    private InputStream getInputStream(Uri mUri) throws IOException{
        try {
            if (mUri.getScheme().equals("file")) {
                return new java.io.FileInputStream(mUri.getPath());
            } else {
                return mContentResolver.openInputStream(mUri);
            }
        } catch (FileNotFoundException ex) {
            return null;
        }
    }
    /**
     * 根据Uri返回文件路径
     * @Title: getInputString
     * @param mUri
     * @return
     * @return String
     * @date 2012-12-14 上午9:14:19
     */
    private String getFilePath(Uri mUri){
        try {
            if (mUri.getScheme().equals("file")) {
                return mUri.getPath();
            } else {
                return getFilePathByUri(mUri);
            }
        } catch (FileNotFoundException ex) {
            return null;
        }
    }
    /**
     * 此处写方法描述
     * @Title: getFilePathByUri
     * @param mUri
     * @return
     * @return String
     * @date 2012-12-14 上午9:16:33
     */
    private String getFilePathByUri(Uri mUri) throws FileNotFoundException{
        String imgPath ;
        Cursor cursor = mContentResolver.query(mUri, null, null, null, null);
        cursor.moveToFirst();
        imgPath = cursor.getString(1); // 图片文件路径
        return imgPath;
    }
    /**
     * 旋转图片
     * @Title: rotateBitmap
     * @param isRotate 是否旋转图片
     * @return void
     * @date 2012-12-14 上午10:38:29
     */
    private void rotateBitmap(final boolean isRotate) {

        Logger.d(TAG, "rotateBitmap  isRotate:" + isRotate);
        if (isFinishing()) {
            return;
        }
        if(isRotate){
            bitmapRotate(90);
        }

        mImageView.setImageBitmap(mBitmap);

    }

    /**
     * 水平翻转原图
     */
    private void bitmapHFlip(){
        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1); // 镜像水平翻转
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        Logger.w(TAG, "bitmapFlip  width:" + width);
        Logger.w(TAG, "bitmapFlip  height:" + height);

        try{
            mBitmap = Bitmap.createBitmap(mBitmap,0, 0, width, height, matrix, true);
        }catch(OutOfMemoryError ooe){

            matrix.postScale((float)1/sampleSize,(float) 1/sampleSize);
            mBitmap = Bitmap.createBitmap(mBitmap,0, 0, width, height, matrix, true);

        }
    }

    /**
     * 旋转原图
     * @Title: bitmapRotate
     * @return void
     * @date 2012-12-13 下午5:37:15
     */
    private void bitmapRotate(int degree){
        Matrix m = new Matrix();
        m.setRotate(degree);
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        Logger.w(TAG, "bitmapRotate  width:" + width);
        Logger.w(TAG, "bitmapRotate  height:" + height);

        try{
            mBitmap = Bitmap.createBitmap(mBitmap,0, 0, width, height, m, true);
        }catch(OutOfMemoryError ooe){

            m.postScale((float)1/sampleSize,(float) 1/sampleSize);
            mBitmap = Bitmap.createBitmap(mBitmap,0, 0, width, height, m, true);

        }

    }

    private static class BackgroundJob extends
            LifeCycleAdapter implements Runnable {

        private final MonitoredActivity mActivity;
        private final ProgressDialog mDialog;
        private final Runnable mJob;
        private final Handler mHandler;
        private final Runnable mCleanupRunner = new Runnable() {
            public void run() {
                mActivity.removeLifeCycleListener(BackgroundJob.this);
                if (mDialog.getWindow() != null)
                    mDialog.dismiss();
            }
        };

        public BackgroundJob(MonitoredActivity activity, Runnable job,
                             ProgressDialog dialog, Handler handler) {
            mActivity = activity;
            mDialog = dialog;
            mJob = job;
            mActivity.addLifeCycleListener(this);
            mHandler = handler;
        }

        public void run() {
            try {
                mJob.run();
            } finally {
                mHandler.post(mCleanupRunner);
            }
        }

        @Override
        public void onActivityDestroyed(MonitoredActivity activity) {
            // We get here only when the onDestroyed being called before
            // the mCleanupRunner. So, run it now and remove it from the queue
            mCleanupRunner.run();
            mHandler.removeCallbacks(mCleanupRunner);
        }

        @Override
        public void onActivityStopped(MonitoredActivity activity) {
            mDialog.hide();
        }

        @Override
        public void onActivityStarted(MonitoredActivity activity) {
            mDialog.show();
        }
    }

    private static void startBackgroundJob(MonitoredActivity activity,
                                           String title, String message, Runnable job, Handler handler) {
        // Make the progress dialog uncancelable, so that we can gurantee
        // the thread will be done before the activity getting destroyed.
        ProgressDialog dialog = ProgressDialog.show(activity, title, message,
                true, false);
        new Thread(new BackgroundJob(activity, job, dialog, handler)).start();
    }

    /**
     * 旋转图片，每次以90度为单位
     * @Title: onRotateClicked
     * @return void
     * @date 2012-12-12 下午5:19:21
     */
    private void onRotateClicked(){

        rotateBitmap(true);

    }

    /**
     * 水平翻转图片
     */
    private void onFlipClicked(){
        bitmapHFlip();
        mImageView.setImageBitmap(mBitmap);
    }

    // 统计用户使用模糊功能事件的属性
    private void statBlurEvent(){
        // 模糊半径
        if(mImageView.getRadius()<=25){
            MobclickAgent.onEvent(this,Constant.stat_blur_radius,"0~25");
        } else if(mImageView.getRadius() <= 45){
            MobclickAgent.onEvent(this,Constant.stat_blur_radius,"28~40");
        } else if(mImageView.getRadius() <= 60){
            MobclickAgent.onEvent(this,Constant.stat_blur_radius,"46~60");
        } else {
            MobclickAgent.onEvent(this,Constant.stat_blur_radius,"80~91");
        }

        // 使用画笔大小
        if(mImageView.getPenSize()<=100){
            MobclickAgent.onEvent(this,Constant.stat_pen_size,"0~100");
        } else if(mImageView.getPenSize() <= 200){
            MobclickAgent.onEvent(this,Constant.stat_pen_size,"101~200");
        } else if(mImageView.getPenSize() <= 300){
            MobclickAgent.onEvent(this,Constant.stat_pen_size,"201~300");
        } else {
            MobclickAgent.onEvent(this,Constant.stat_pen_size,"301~500");
        }

        // 保存次数
        MobclickAgent.onEvent(this,Constant.stat_blur_save_times);
    }


    /**
     * 点击保存的处理，这里保存成功回传的是一个Uri，系统默认传回的是一个bitmap图，
     * 如果传回的bitmap图比较大的话就会引起系统出错。会报这样一个异常：
     * android.os.transactiontoolargeexception。为了规避这个异常，
     * 采取了传回Uri的方法。
     * @Title: onSaveClicked
     * @return void
     * @date 2012-12-14 上午10:32:38
     */
    private void onSaveClicked() {
        // 统计【编辑原图片】功能使用属性
        if(mImageView.isBluring) {
            statBlurEvent();
        }
        final Bitmap croppedImage = mImageView.createNewPhoto();
        // 水印Bitmap
        final Bitmap waterMark = BitmapFactory.decodeResource(getResources(),Constant.coverResIds[coverIdx]);

        Logger.w(TAG, "croppedImage ww:" + croppedImage.getWidth());
        Logger.w(TAG, "croppedImage wh:" + croppedImage.getHeight());

        String imgPath = getFilePath(targetUri);
        final String cropPath = Constant.CACHEPATH + "/" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())+".jpg";
        Logger.w("onSaveClicked", "cropPath:" + cropPath);

        startBackgroundJob(CropImageActivity.this, null, "处理中,请稍候...", new Runnable() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Bitmap finalBitmap = createBitmap(croppedImage, waterMark);
                        saveDrawableToCache(finalBitmap, cropPath);
                        croppedImage.recycle();
                        waterMark.recycle();
                        croppedImage.recycle();
                    }

                });
            }
        }, mHandler);

        Uri cropUri = Uri.fromFile(new File(cropPath));
        Logger.d("onSaveClicked", "cropPath:" + cropPath);
        Logger.d("onSaveClicked", "cropUri:" + cropUri);

        Intent intent = new Intent(this, ShareActivity.class);
        intent.putExtra(Constant.WM_IMAGE_URI, cropUri);
        startActivity(intent);
    }
    /**
     * 将Bitmap放入缓存，
     * @Title: saveDrawableToCache
     * @param bitmap
     * @param filePath
     * @return void
     * @date 2012-12-14 上午9:27:38
     */
    private void saveDrawableToCache(Bitmap bitmap, String filePath){

        try {
            File file = new File(filePath);

            file.createNewFile();

            OutputStream outStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    /**
     *
     * @param src
     * @param watermark
     * @return
     */
    private Bitmap createBitmap(Bitmap src, Bitmap watermark){
        if(src == null) {
            return null;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        Logger.d(TAG, "createBitmap w:" + w);
        Logger.d(TAG, "createBitmap h:" + h);
        int ww = watermark.getWidth();
        int wh = watermark.getHeight();
        Logger.d(TAG, "watermark ww:" + ww);
        Logger.d(TAG, "watermark wh:" + wh);

        // 缩放图片的尺寸
        float scaleWidth = (float) w / ww;
        float scaleHeight = (float) h / wh;
        Logger.d(TAG, "watermark scaleWidth:" + scaleWidth);
        Logger.d(TAG, "watermark scaleHeight:" + scaleHeight);

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 产生缩放后的Bitmap对象
        Bitmap resizeBitmap = Bitmap.createBitmap(watermark, 0, 0, ww, wh, matrix, false);
        Logger.d(TAG, "resizeBitmap ww:" + resizeBitmap.getWidth());
        Logger.d(TAG, "resizeBitmap wh:" + resizeBitmap.getHeight());


        //create the new blank bitmap
        Bitmap newb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        //创建一个新的和SRC长度宽度一样的位图
        Canvas cv = new Canvas(newb);
        //draw src into
        cv.drawBitmap(src, 0, 0, null);//在 0，0坐标开始画入src
        //draw watermark into
        cv.drawBitmap(resizeBitmap, 0, 0, null);//在src的右下角画入水印
        // save all clip
        cv.save(Canvas.ALL_SAVE_FLAG);//保存
        //store
        cv.restore();//存储

        resizeBitmap.recycle();
        return newb;
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageView.recyle();
        mBitmap.recycle();
        mBitmap = null;
    }

    @Override
    public void finish() {
        AppSharedPreference.setPenSizePref(mImageView.getPenSize());
        super.finish();
    }
}
