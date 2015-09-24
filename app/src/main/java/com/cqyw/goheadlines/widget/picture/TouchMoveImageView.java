package com.cqyw.goheadlines.widget.picture;

import com.cqyw.goheadlines.util.Logger;
import com.cqyw.goheadlines.config.Constant;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * 可移动、缩放、旋转的ImageView--修改for 裁剪，去掉旋转，加边界判断
 * @ClassName: TouchImageView
 * @Description: TODO
 * @author
 * @date 2014年11月28日 下午4:52:44 
 *
 */
public class TouchMoveImageView extends ImageView {

    float x_down = 0;
    float y_down = 0;
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;
    float oldRotation = 0;
    Matrix matrix = new Matrix();
    Matrix matrix1 = new Matrix();

    Matrix matrixZoom = new Matrix();  //shark
    boolean matrixZoomCheck = false;  //shark
    float[] mf = new float[9];  		  //shark
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
//        gintama = BitmapFactory.decodeResource(getResources(), resId);  
        gintama = resBitmap;
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        widthScreen = dm.widthPixels;
        heightScreen = dm.heightPixels;

        matrix = new Matrix();
    }

    public void setImageBitmap(Bitmap resBitmap) {
        gintama = resBitmap;
        matrix = new Matrix();
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.drawBitmap(gintama, matrix, null);
        canvas.restore();
    }

    public boolean onTouchEvent(MotionEvent event) {
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
//            oldRotation = rotation(event);  //去掉旋转效果
                savedMatrix.set(matrix);
                midPoint(mid, event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == ZOOM) {

                    Logger.w("", "ACTION_MOVE  ZOOM");
                    matrix1.set(savedMatrix);
//                float rotation = rotation(event) - oldRotation;  //去掉旋转效果
                    float newDist = spacing(event);
                    float scale = newDist / oldDist;
                    matrix1.postScale(scale, scale, mid.x, mid.y);// 縮放
//                matrix1.postRotate(rotation, mid.x, mid.y);// 旋轉  //去掉旋转效果

                    //缩放进行检查，不能小于最小宽度，记录最小宽度时的matrixZoom，手指放开时回弹最小规定大小
                    matrixCheck = matrixZoomCheck();
                    if (matrixCheck == true && !matrixZoomCheck) {
                        Logger.w("", "ACTION_MOVE   matrixZoomCheck ----   true");
                        matrixZoomCheck = true;
                    }
                    if(!matrixZoomCheck){
                        //当达到最小规定大小时，记录第一个顶点位置，如果顶点位于裁剪区域中间，则回弹时平移至边界(0,0)点
                        matrix1.getValues(mf);
                        x1 = mf[0] * 0 + mf[1] * 0 + mf[2];
                        y1 = mf[3] * 0 + mf[4] * 0 + mf[5];
                        //记录最小宽度时的matrixZoom,手指放开时回弹至该大小
                        matrixZoom.set(savedMatrix);
                        matrixZoom.postScale(scale, scale, mid.x, mid.y);//记录最小宽度时的matrixZoom,手指放开时回弹至该大小
                    }
                    matrix.set(matrix1);
                    invalidate();

                } else if (mode == DRAG) {
                    //Constant.displayWidth  为裁剪区域宽高，正方形
                    //平移时分别判断x、y轴坐标是否符合要求，不能在 (0,Constant.displayWidth)之间
                    matrixDrag.set(savedMatrix);
                    matrixDrag.postTranslate(event.getX() - x_down, event.getY() - y_down);// 平移
                    Boolean xCheck = matrixDragXCheck(matrixDrag);
                    Boolean yCheck = matrixDragYCheck(matrixDrag);

                    Logger.w("", "ACTION_MOVE  DRAG  xCheck:" + xCheck + "--yCheck:" + yCheck);
                    if (xCheck == true && yCheck == false) {
                        Logger.w("", "ACTION_MOVE  DRAG 11");
                        matrix1.set(savedMatrix);
                        matrix1.postTranslate(xMovePosi, event.getY() - y_down);// 平移
                        matrix.set(matrix1);
                        invalidate();
                        yMovePosi = event.getY() - y_down;
                    } else if(xCheck == false && yCheck == true) {
                        Logger.w("", "ACTION_MOVE  DRAG 22");
                        matrix1.set(savedMatrix);
                        matrix1.postTranslate(event.getX() - x_down, yMovePosi);// 平移
                        matrix.set(matrix1);
                        invalidate();
                        xMovePosi = event.getX() - x_down;
                    } else if(xCheck == false && yCheck == false) {
                        Logger.w("", "ACTION_MOVE  DRAG 33");
                        matrix1.set(savedMatrix);
                        matrix1.postTranslate(event.getX() - x_down, event.getY() - y_down);// 平移
                        matrix.set(matrix1);
                        invalidate();
                        xMovePosi = event.getX() - x_down;
                        yMovePosi = event.getY() - y_down;
                    }
            	
                /*matrix1.set(savedMatrix);  
                matrix1.postTranslate(event.getX() - x_down, event.getY()  
                        - y_down);// 平移  
                matrixCheck = matrixDragCheck();  
//                matrixCheck = matrixCheck();  
                if (matrixCheck == false) {  
                    matrix.set(matrix1);  
                    invalidate();  
                }  */
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
                    matrix1.getValues(mf);
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
                    }
                    else if (x2 < Constant.displayWidth && y2 > 0) {
                        matrixZoom.postTranslate(Constant.displayWidth - x2, 0 - y2);// 平移
                    } else if (x2 < Constant.displayWidth && y2 < 0 && y4 > Constant.displayWidth) {
                        matrixZoom.postTranslate(Constant.displayWidth - x2, 0);// 平移
                    }
                    else if (x4 < Constant.displayWidth && y4 < Constant.displayWidth) {
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
        return true;
    }

    /**
     * 缩放时候的边界检查
     */
    private boolean matrixZoomCheck() {
        float[] f = new float[9];
        matrix1.getValues(f);
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
  
    /*private boolean matrixDragCheck() {  
        float[] f = new float[9];  
        matrix1.getValues(f);  
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
        // 缩放比率判断  
        if (width < widthScreen / 1 || width > widthScreen * 5) {  
        	Logger.w("", "ture--------------");
            return true;  
        }  
        // 出界判断  
        if ((x1 < widthScreen / 3 && x2 < widthScreen / 3  
                && x3 < widthScreen / 3 && x4 < widthScreen / 3)  
                || (x1 > widthScreen * 2 / 3 && x2 > widthScreen * 2 / 3  
                        && x3 > widthScreen * 2 / 3 && x4 > widthScreen * 2 / 3)  
                || (y1 < heightScreen / 3 && y2 < heightScreen / 3  
                        && y3 < heightScreen / 3 && y4 < heightScreen / 3)  
                || (y1 > heightScreen * 2 / 3 && y2 > heightScreen * 2 / 3  
                        && y3 > heightScreen * 2 / 3 && y4 > heightScreen * 2 / 3)) {  
            return true;  
        }  
        Logger.w("", "x1:" + x1 + "-x2:" + x2 + "-x3:" + x3 + "-x4:" + x4);
        Logger.w("", "y1:" + y1 + "-y2:" + y2 + "-y3:" + y3 + "-y4:" + y4);
        if (x1 > 0 || x2 < widthScreen || y1 > 0 || y3 < widthScreen) {

        	Logger.w("", "ture--------------");
        	return true;  
		}
        return false;  
    }  */

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
    public Bitmap creatNewPhoto() {
        int newWidth,newHeight;
        newWidth = getWidth();newHeight = getHeight();
        Bitmap bitmap = Bitmap.createBitmap(newWidth, newHeight,
                Config.ARGB_8888); // 背景图片  
        Canvas canvas = new Canvas(bitmap); // 新建画布  
        canvas.drawBitmap(gintama, matrix, null); // 画图片  
        canvas.save(Canvas.ALL_SAVE_FLAG); // 保存画布  
        canvas.restore();
        return bitmap;
    }


}
