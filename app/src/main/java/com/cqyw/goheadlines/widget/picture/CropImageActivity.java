package com.cqyw.goheadlines.widget.picture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.cqyw.goheadlines.R;
import com.cqyw.goheadlines.ShareActivity;
import com.cqyw.goheadlines.util.Logger;
import com.cqyw.goheadlines.config.Constant;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 *
 * @ClassName: CropActivity
 * @Description: The activity can crop specific region of interest from an image.图片裁剪界面
 * 					裁剪方式是移动缩放图片
 * @author shark
 * @date 2014年11月26日 上午10:53:25
 *
 */
public class CropImageActivity extends MonitoredActivity {

    private String TAG = "CropImageActivity";

    public final static int REQ_CODE = 211;

    public Boolean mSaving = false;

    private int mAspectX, mAspectY;

    private final Handler mHandler = new Handler();

    private boolean mCircleCrop = false;

    private int rotateDeg = 0;

    private int coverIdx;

    private Bitmap mBitmap;

    private TouchMoveImageView mImageView;

    private ImageView mWaterMark;

    //private RotateBitmap rotateBitmap;
    HighlightView mCrop;

    Uri targetUri ;

    HighlightView hv;

    private ContentResolver mContentResolver ;

    private static final int DEFAULT_WIDTH = 720;
    private static final int DEFAULT_HEIGHT = 960;

    private int width ;
    private int height;
    private int sampleSize = 1;

    private RelativeLayout mCrViewLayout;
    private Button mSaveBtn;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
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
        mImageView = new TouchMoveImageView(this,mBitmap);
        mWaterMark = new ImageView(this);
        coverIdx = getIntent().getIntExtra(Constant.COVER_INDEX, 0);
        mWaterMark.setImageResource(Constant.coverResIds[coverIdx]);
        mCrViewLayout.addView(mImageView, params);
        mCrViewLayout.addView(mWaterMark, params);
    }
    /**
     * 此处写方法描述
     * @Title: initViews
     * @return void
     * @date 2012-12-14 上午10:41:23
     */
    private void initViews(){
        findViewById(R.id.crop_bar_cancell).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        //取消
                        finish();
                    }
                });
        findViewById(R.id.crop_bar_rotate).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //旋转
                onRotateClicked();
            }
        });

        findViewById(R.id.crop_bar_ok).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //下一步  先保存图片
                onSaveClicked();
            }
        });


        int barrier_height = Constant.displayHeight-Constant.picHeight;
        mCrViewLayout = (RelativeLayout)findViewById(R.id.crop_CropImage_view);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCrViewLayout.getLayoutParams());
        int margin = barrier_height/2;
        params.setMargins(0, margin, 0, margin);
        mCrViewLayout.setLayoutParams(params);

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
            while ((width / sampleSize > DEFAULT_WIDTH * 2) || (height / sampleSize > DEFAULT_HEIGHT * 2)) {
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
        Cursor cursor = mContentResolver.query(mUri, null, null,null, null);
        cursor.moveToFirst();
        imgPath = cursor.getString(1); // 图片文件路径
        return imgPath;
    }
    /**
     * 此处写方法描述
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
            MonitoredActivity.LifeCycleAdapter implements Runnable {

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
     * 点击保存的处理，这里保存成功回传的是一个Uri，系统默认传回的是一个bitmap图，
     * 如果传回的bitmap图比较大的话就会引起系统出错。会报这样一个异常：
     * android.os.transactiontoolargeexception。为了规避这个异常，
     * 采取了传回Uri的方法。
     * @Title: onSaveClicked
     * @return void
     * @date 2012-12-14 上午10:32:38
     */
    private void onSaveClicked() {
        final Bitmap croppedImage = mImageView.creatNewPhoto();
        // 水印Bitmap
        final Bitmap waterMark = BitmapFactory.decodeResource(getResources(),Constant.coverResIds[coverIdx]);

        Logger.w(TAG, "croppedImage ww:" + croppedImage.getWidth());
        Logger.w(TAG, "croppedImage wh:" + croppedImage.getHeight());

        String imgPath = getFilePath(targetUri);
        final String cropPath = Constant.CACHEPATH + "/" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        Logger.w("onSaveClicked", "cropPath:" + cropPath);

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
        mBitmap.recycle();
        mBitmap = null;
    }


}
