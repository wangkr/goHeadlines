package com.cqyw.goheadlines.picture.blur;

/**
 * Created by Kairong on 2015/9/29.
 * mail:wangkrhust@gmail.com
 */
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class EraseBlurView extends View {
    private boolean isMove = false;
    private boolean isBlur = false;
    private int radius;                     // 模糊半径
    private Bitmap originBitmap = null;     // 原图
    private Bitmap bluredBitmap = null;     // 模糊图
    private Path path;
    private Canvas mCanvas;
    private Paint paint;


    public EraseBlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.radius = 20;
        this.isBlur = false;
    }

    public EraseBlurView(Context context, AttributeSet atts, int radius){
        this(context, atts);
        this.radius = radius;
        this.isBlur = false;
    }

    public EraseBlurView(Context context, AttributeSet attrs, int radius, boolean isBlur){
        this(context, attrs, radius);
        this.isBlur = isBlur;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setBlur(boolean isBlur){
        this.isBlur = isBlur;
    }

    public void setOriginBitmap(Bitmap originBitmap) {
        this.originBitmap = originBitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (mCanvas == null) {
            EraseBitmp();
        }
        canvas.drawBitmap(originBitmap, 0, 0, null);
        if(isBlur) {
            mCanvas.drawPath(path, paint);
        }
        super.onDraw(canvas);
    }

    public void EraseBitmp() {
        bluredBitmap = FastBlur.doBlurJniArray(originBitmap,radius,true);

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(20);

        path = new Path();

        mCanvas = new Canvas(originBitmap);
        if(isBlur) {
            mCanvas.drawBitmap(bluredBitmap, 0, 0, null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub

        float ax = event.getX();
        float ay = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            isMove = false;
            path.reset();
            path.moveTo(ax, ay);
            invalidate();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            isMove = true;
            path.lineTo(ax,ay);
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }
}
