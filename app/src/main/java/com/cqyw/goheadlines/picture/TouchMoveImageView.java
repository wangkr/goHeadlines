package com.cqyw.goheadlines.picture;

import com.cqyw.goheadlines.AppSharedPreference;
import com.cqyw.goheadlines.picture.blur.FastBlur;
import com.cqyw.goheadlines.util.Logger;
import com.cqyw.goheadlines.config.Constant;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * 可移动、缩放、旋转的ImageView--修改for 裁剪，去掉旋转，加边界判断
 * @ClassName: TouchImageView
 * @Description: TODO
 * @author
 * @date 2014年11月28日 下午4:52:44 
 * @modified Kairong Wang
 * @modifyTime 2015年10月1日
 */
public class TouchMoveImageView extends ImageView {

    private boolean isPressed = false;
    public boolean isEdited = false;
    public boolean isBluring = false;
    private boolean hasBeenBlured = false;
    private int radius;                     // 模糊半径
    private int penSize;                    // 画笔半径

    private Bitmap bluredBitmap = null;     // 模糊图
    private Bitmap cursorBitmap = null;     // 光标图
    private Canvas blurCanvas = null;       // 模糊画布
    private Canvas cursorCanvas = null;     // 光标画布
    private BlurMaskFilter blurMaskFilter;  // 模糊面具
    private Path path;
    private Paint paint;
    private Paint cursorPaint;
    private Paint clearPaint;

    float x_down = 0;
    float y_down = 0;
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;
    float oldRotation = 0;
    float cursorRadius = 0;
    Matrix matrix = new Matrix();
    Matrix matrixTmp = new Matrix();
    Matrix matrixZoom = new Matrix();
    Matrix mDown = new Matrix();        // 图像缩小
    Matrix mUp = new Matrix();          // 图像放大
    boolean matrixZoomCheck = false;
    float[] mf = new float[9];
    float x1 = 0, x2 = 0, x3 = 0, x4 = 0;
    float y1 = 0, y2 = 0, y3 = 0, y4 = 0;
    Matrix matrixDrag = new Matrix();  //shark
    float xMovePosi = 0;  	//记录移动到边界的距离
    float yMovePosi = 0;

    Matrix savedMatrix = new Matrix();

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    int mode = NONE;

    boolean matrixCheck = false;

    int widthScreen;
    int heightScreen;

    Bitmap gintama;

    public TouchMoveImageView(Activity activity, Bitmap resBitmap) {
        super(activity);
        radius = Constant.defRadiuSize;
        penSize = Constant.defPenSize;
        cursorRadius = penSize*0.6f;
        initDrawTools();
        isBluring = false;
        gintama = resBitmap;
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        widthScreen = dm.widthPixels;
        heightScreen = dm.heightPixels;

        matrix = new Matrix();
        mDown.setScale(1f / Constant.blurScaleFactor, 1f / Constant.blurScaleFactor);
        mUp.setScale(Constant.blurScaleFactor, Constant.blurScaleFactor);
    }

    private Bitmap createARG(Bitmap resBitmap, Config config){
        Bitmap bitmap = Bitmap.createBitmap(resBitmap.getWidth(),resBitmap.getHeight(),config);
        Canvas canvas = new Canvas(bitmap);
        Paint cursorPaint = new Paint();
        cursorPaint.setFlags(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(resBitmap, 0, 0, cursorPaint);
        return  bitmap;
    }

    public void setPenSize(int penSize) {
        this.penSize = Math.max(penSize,4);
        cursorRadius = this.penSize*0.6f;
        if(paint != null){
            paint.setStrokeWidth(penSize);
            blurMaskFilter = new BlurMaskFilter(Math.max((float) penSize * 0.1f, 2f), BlurMaskFilter.Blur.SOLID);
            paint.setMaskFilter(blurMaskFilter);
        }
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public float getRadius(){
        return radius;
    }

    public int getPenSize(){
        return penSize;
    }

    public void setBluring(boolean isBluring){
        this.isBluring = isBluring;
        if(isBluring){
            if (!hasBeenBlured) {
                EraseBitmp();
                invalidate();
            }
        } else {
            recycleBlurBitmap();
            path.reset();
            hasBeenBlured = false;
            isEdited = false;
            invalidate();
        }
    }
    public void refreshCanvas(){
        if(blurCanvas!=null){
            recycleBlurBitmap();
            blurCanvas = null;
            path.reset();
            EraseBitmp();
            invalidate();
        }
    }
    private void recycleBlurBitmap(){
        if(bluredBitmap!=null){
            bluredBitmap.recycle();
            bluredBitmap = null;
            System.gc();
        }
    }

    public void setImageBitmap(Bitmap resBitmap) {
        gintama = resBitmap;
        matrix = new Matrix();
        invalidate();
    }

    /**
     * 水平翻转
     */
    public void flip_Horizon(){
        Matrix m = new Matrix();
        m.postScale(-1, 1);
        gintama = Bitmap.createBitmap(gintama, 0, 0, gintama.getWidth(), gintama.getHeight(), m, true);
        invalidate();
    }

    /**
     * 旋转
     * @param degree 旋转角度
     */
    public void rotate(int degree){
        Matrix m = new Matrix();
        m.setRotate(degree);
        gintama = Bitmap.createBitmap(gintama, 0, 0, gintama.getWidth(), gintama.getHeight(), m, true);
        invalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        if(isBluring && radius > 0) {
            if (blurCanvas == null) {
                EraseBitmp();
            }
            blurCanvas.drawPath(path, paint);
        }
        canvas.save();
        canvas.drawBitmap(gintama, matrix, null);
        if(hasBeenBlured && bluredBitmap!=null) {
            canvas.drawBitmap(bluredBitmap, 0, 0, null);
            if(isPressed){
                clearCanvas(cursorCanvas);
                cursorCanvas.drawCircle(x_down, y_down, cursorRadius, cursorPaint);
                canvas.drawBitmap(cursorBitmap, 0, 0, null);
            } else {
                clearCanvas(cursorCanvas);
                canvas.drawBitmap(cursorBitmap, 0, 0, null);
            }
        }
        canvas.restore();
    }

    private void initDrawTools(){
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(penSize);
        
        cursorPaint = new Paint();
        cursorPaint.setStyle(Paint.Style.FILL);
        cursorPaint.setColor(Color.argb(0x88, 0xff, 0xff, 0xff));

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        blurMaskFilter = new BlurMaskFilter(Math.max((float)penSize*0.1f, 2f), BlurMaskFilter.Blur.SOLID);
        paint.setMaskFilter(blurMaskFilter);

        path = new Path();
    }

    /**
     * 清除画布
     * @param canvas
     */
    private void clearCanvas(Canvas canvas){
        canvas.drawPaint(clearPaint);
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void EraseBitmp() {
        if(radius == 0){
            hasBeenBlured = false;
            recycleBlurBitmap();
            return;
        }
        bluredBitmap = createNewBkPhoto();
        cursorBitmap = createARG(bluredBitmap, Config.ARGB_8888);

        // 采用图像金字塔处理
        bluredBitmap = Bitmap.createBitmap(bluredBitmap, 0, 0, bluredBitmap.getWidth(), bluredBitmap.getHeight(), mDown, true);
        bluredBitmap = FastBlur.doBlurJniArray(bluredBitmap, radius, true);
        bluredBitmap = Bitmap.createBitmap(bluredBitmap, 0, 0, bluredBitmap.getWidth(), bluredBitmap.getHeight(), mUp, true);

        blurCanvas = new Canvas(bluredBitmap);
        cursorCanvas = new Canvas(cursorBitmap);
        hasBeenBlured = true;
        System.gc();
    }

    public boolean onTouchEvent(MotionEvent event) {
        if(isBluring){
            x_down = event.getX();
            y_down = event.getY();
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    isPressed = true;
                    path.reset();
                    path.moveTo(x_down, y_down);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    isPressed = true;
                    path.lineTo(x_down, y_down);
                    isEdited = true;
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    isPressed = false;
                    invalidate();
                    return true;
            }
            return super.onTouchEvent(event);
        } else {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    mode = DRAG;
                    x_down = event.getX();
                    y_down = event.getY();
                    savedMatrix.set(matrix);
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mode = ZOOM;
                    oldDist = spacing(event);
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == ZOOM) {

                        Logger.w("", "ACTION_MOVE  ZOOM");
                        matrixTmp.set(savedMatrix);
                        float newDist = spacing(event);
                        float scale = newDist / oldDist;
                        matrixTmp.postScale(scale, scale, mid.x, mid.y);// 縮放

                        //缩放进行检查，不能小于最小宽度，记录最小宽度时的matrixZoom，手指放开时回弹最小规定大小
                        matrixCheck = matrixZoomCheck();
                        if (matrixCheck && !matrixZoomCheck) {
                            Logger.w("", "ACTION_MOVE   matrixZoomCheck ----  true");
                            matrixZoomCheck = true;
                        }
                        if (!matrixZoomCheck) {
                            //当达到最小规定大小时，记录第一个顶点位置，如果顶点位于裁剪区域中间，则回弹时平移至边界(0,0)点
                            matrixTmp.getValues(mf);
                            x1 = mf[0] * 0 + mf[1] * 0 + mf[2];
                            y1 = mf[3] * 0 + mf[4] * 0 + mf[5];
                            //记录最小宽度时的matrixZoom,手指放开时回弹至该大小
                            matrixZoom.set(savedMatrix);
                            matrixZoom.postScale(scale, scale, mid.x, mid.y);//记录最小宽度时的matrixZoom,手指放开时回弹至该大小
                        }
                        matrix.set(matrixTmp);
                        invalidate();

                    } else if (mode == DRAG) {
                        //Constant.displayWidth  为裁剪区域宽高，正方形
                        //平移时分别判断x、y轴坐标是否符合要求，不能在 (0,Constant.displayWidth)之间
                        matrixDrag.set(savedMatrix);
                        matrixDrag.postTranslate(event.getX() - x_down, event.getY() - y_down);// 平移
                        Boolean xCheck = matrixDragXCheck(matrixDrag);
                        Boolean yCheck = matrixDragYCheck(matrixDrag);

                        Logger.w("", "ACTION_MOVE  DRAG  xCheck:" + xCheck + "--yCheck:" + yCheck);
                        if (xCheck && !yCheck) {
                            Logger.w("", "ACTION_MOVE  DRAG 11");
                            matrixTmp.set(savedMatrix);
                            matrixTmp.postTranslate(xMovePosi, event.getY() - y_down);// 平移
                            matrix.set(matrixTmp);
                            invalidate();
                            yMovePosi = event.getY() - y_down;
                        } else if (!xCheck && yCheck) {
                            Logger.w("", "ACTION_MOVE  DRAG 22");
                            matrixTmp.set(savedMatrix);
                            matrixTmp.postTranslate(event.getX() - x_down, yMovePosi);// 平移
                            matrix.set(matrixTmp);
                            invalidate();
                            xMovePosi = event.getX() - x_down;
                        } else if (!xCheck && !yCheck) {
                            Logger.w("", "ACTION_MOVE  DRAG 33");
                            matrixTmp.set(savedMatrix);
                            matrixTmp.postTranslate(event.getX() - x_down, event.getY() - y_down);// 平移
                            matrix.set(matrixTmp);
                            invalidate();
                            xMovePosi = event.getX() - x_down;
                            yMovePosi = event.getY() - y_down;
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    if (mode == ZOOM && matrixZoomCheck) {
                        //缩放时，如果缩放大小小于最小范围则回弹至最小范围
                        Logger.w("", "ACTION_POINTER_UP   matrixZoomCheck ----   **");
                        matrixZoom.postTranslate(0 - x1, 0 - y1);// 平移至(0,0)点
                        matrix.set(matrixZoom);
                        invalidate();
                        matrixZoomCheck = false;
                    } else if (mode == ZOOM) {
                        Logger.w("", "ACTION_POINTER_UP   ZOOM ----   **");
                        //获取顶点位置，如果顶点不在规定区域，则平移至边界
                        //Constant.displayWidth  为裁剪区域宽高，正方形
                        matrixTmp.getValues(mf);
                        // 图片4个顶点的坐标
                        x1 = mf[0] * 0 + mf[1] * 0 + mf[2];
                        y1 = mf[3] * 0 + mf[4] * 0 + mf[5];
                        x2 = mf[0] * gintama.getWidth() + mf[1] * 0 + mf[2];
                        y2 = mf[3] * gintama.getWidth() + mf[4] * 0 + mf[5];
                        x3 = mf[0] * 0 + mf[1] * gintama.getHeight() + mf[2];
                        y3 = mf[3] * 0 + mf[4] * gintama.getHeight() + mf[5];
                        x4 = mf[0] * gintama.getWidth() + mf[1] * gintama.getHeight() + mf[2];
                        y4 = mf[3] * gintama.getWidth() + mf[4] * gintama.getHeight() + mf[5];
                        if (x1 > 0 && y1 > 0) {
                            matrixZoom.postTranslate(0 - x1, 0 - y1);// 平移
                        } else if (x3 > 0 && y3 < Constant.displayWidth) {
                            matrixZoom.postTranslate(0 - x3, Constant.displayWidth - y3);// 平移
                        } else if (x1 > 0 && y1 < 0 && y3 > Constant.displayWidth) {
                            matrixZoom.postTranslate(0 - x1, 0);// 平移
                        } else if (x1 < 0 && y1 > 0 && x2 > Constant.displayWidth) {
                            matrixZoom.postTranslate(0, 0 - y1);// 平移
                        } else if (x2 < Constant.displayWidth && y2 > 0) {
                            matrixZoom.postTranslate(Constant.displayWidth - x2, 0 - y2);// 平移
                        } else if (x2 < Constant.displayWidth && y2 < 0 && y4 > Constant.displayWidth) {
                            matrixZoom.postTranslate(Constant.displayWidth - x2, 0);// 平移
                        } else if (x4 < Constant.displayWidth && y4 < Constant.displayWidth) {
                            matrixZoom.postTranslate(Constant.displayWidth - x4, Constant.displayWidth - y4);// 平移
                        } else if (x4 > Constant.displayWidth && y4 < Constant.displayWidth && x3 < Constant.displayWidth) {
                            matrixZoom.postTranslate(0, Constant.displayWidth - y4);// 平移
                        }
                        matrix.set(matrixZoom);
                        invalidate();
                    }
                    mode = NONE;
                    xMovePosi = 0;
                    yMovePosi = 0;
                    break;
            }
        }
        return true;
    }

    /**
     * 缩放时候的边界检查
     */
    private boolean matrixZoomCheck() {
        float[] f = new float[9];
        matrixTmp.getValues(f);
        // 图片4个顶点的坐标  
        float x1 = f[0] * 0 + f[1] * 0 + f[2];
        float y1 = f[3] * 0 + f[4] * 0 + f[5];
        float x2 = f[0] * gintama.getWidth() + f[1] * 0 + f[2];
        float y2 = f[3] * gintama.getWidth() + f[4] * 0 + f[5];
        float x3 = f[0] * 0 + f[1] * gintama.getHeight() + f[2];
        float y3 = f[3] * 0 + f[4] * gintama.getHeight() + f[5];
        float x4 = f[0] * gintama.getWidth() + f[1] * gintama.getHeight() + f[2];
        float y4 = f[3] * gintama.getWidth() + f[4] * gintama.getHeight() + f[5];
        // 图片现宽度  
        double width = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
        // 图片现高度  
        double height = Math.sqrt((x2 - x4) * (x2 - x4) + (y2 - y4) * (y2 - y4));
        double minW = Math.min(width, height);

        Logger.w("", "matrixZoomCheck----width:" + width + "--height:" + height + "--minW:" + minW);
        // 缩放比率判断  
        if (minW < widthScreen / 1 || minW > widthScreen * 5) {
            Logger.w("", "matrixZoomCheck  ture--------------");
            return true;
        }

        return false;
    }

    /**
     * 平移时候的边界检查  X轴检查
     */
    private boolean matrixDragXCheck(Matrix matrixD) {
        float[] f = new float[9];
        matrixD.getValues(f);
        // 图片4个顶点的坐标  
        float x1 = f[0] * 0 + f[1] * 0 + f[2];
        float y1 = f[3] * 0 + f[4] * 0 + f[5];
        float x2 = f[0] * gintama.getWidth() + f[1] * 0 + f[2];
        float y2 = f[3] * gintama.getWidth() + f[4] * 0 + f[5];
        float x3 = f[0] * 0 + f[1] * gintama.getHeight() + f[2];
        float y3 = f[3] * 0 + f[4] * gintama.getHeight() + f[5];
        float x4 = f[0] * gintama.getWidth() + f[1] * gintama.getHeight() + f[2];
        float y4 = f[3] * gintama.getWidth() + f[4] * gintama.getHeight() + f[5];

        Logger.w("", "x1:" + x1 + "-x2:" + x2 + "-x3:" + x3 + "-x4:" + x4);
        Logger.w("", "y1:" + y1 + "-y2:" + y2 + "-y3:" + y3 + "-y4:" + y4);
        if (x1 > 0 || x2 < widthScreen ) {

            Logger.w("", "matrixDragXCheck xx ture--------------");
            return true;
        }
        return false;
    }

    /**
     * 平移时候的边界检查  Y轴检查
     */
    private boolean matrixDragYCheck(Matrix matrixD) {
        float[] f = new float[9];
        matrixD.getValues(f);
        // 图片4个顶点的坐标  
        float x1 = f[0] * 0 + f[1] * 0 + f[2];
        float y1 = f[3] * 0 + f[4] * 0 + f[5];
        float x2 = f[0] * gintama.getWidth() + f[1] * 0 + f[2];
        float y2 = f[3] * gintama.getWidth() + f[4] * 0 + f[5];
        float x3 = f[0] * 0 + f[1] * gintama.getHeight() + f[2];
        float y3 = f[3] * 0 + f[4] * gintama.getHeight() + f[5];
        float x4 = f[0] * gintama.getWidth() + f[1] * gintama.getHeight() + f[2];
        float y4 = f[3] * gintama.getWidth() + f[4] * gintama.getHeight() + f[5];

        Logger.w("", "x1:" + x1 + "-x2:" + x2 + "-x3:" + x3 + "-x4:" + x4);
        Logger.w("", "y1:" + y1 + "-y2:" + y2 + "-y3:" + y3 + "-y4:" + y4);
        if (y1 > 0 || y3 < widthScreen) {

            Logger.w("", "matrixDragYCheck yy ture--------------");
            return true;
        }
        return false;
    }

    // 触碰两点间距离  
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    // 取手势中心点  
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    // 取旋转角度  
    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    // 将移动，缩放以及旋转后的图层保存为新图片  
    // 本例中沒有用到該方法，需要保存圖片的可以參考  
    public Bitmap createNewPhoto() {
        int newWidth,newHeight;
        newWidth = getWidth();newHeight = getHeight();
        Bitmap bitmap = Bitmap.createBitmap(newWidth, newHeight,
                Config.ARGB_8888); // 背景图片  
        Canvas canvas = new Canvas(bitmap); // 新建画布  
        canvas.drawBitmap(gintama, matrix, null);       // 画背景图
        if(hasBeenBlured) {
            canvas.drawBitmap(bluredBitmap, 0, 0, null);  // 画模糊图
        }
        canvas.save(Canvas.ALL_SAVE_FLAG); // 保存画布  
        canvas.restore();
        return bitmap;
    }

    // 将移动，缩放以及旋转后的背景图层保存为新图片  
    public Bitmap createNewBkPhoto() {
        int newWidth,newHeight;
        newWidth = getWidth();newHeight = getHeight();
        Bitmap bitmap = Bitmap.createBitmap(newWidth, newHeight,
                Config.ARGB_8888); // 背景图片  
        Canvas canvas = new Canvas(bitmap); // 新建画布
        canvas.drawBitmap(gintama, matrix, null);  // 画背景图
        return bitmap;
    }

    public void recyle(){
        if(bluredBitmap!=null && !bluredBitmap.isRecycled()){
            bluredBitmap.recycle();
            bluredBitmap = null;
        }
        if(cursorBitmap!=null&& !cursorBitmap.isRecycled()){
            cursorBitmap.recycle();
            cursorBitmap = null;
        }
        blurCanvas = null;
        cursorCanvas = null;
        cursorPaint = null;
        paint = null;
        clearPaint = null;
        System.gc();
    }

}
