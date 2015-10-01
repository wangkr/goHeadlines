package com.cqyw.goheadlines.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.MotionEvent;
import android.widget.ImageView;


/**
 * Created by Kairong on 2015/10/1.
 * mail:wangkrhust@gmail.com
 */
public class TouchCursorView extends ImageView {
    private Context mContext;

    private boolean isTracking;
    private boolean isPressing;
    private int touchCursorSize;
    private Paint paint;
    private float ax = 0,ay = 0;

    public TouchCursorView(Context context) {
        super(context);
        this.mContext = context;
        this.touchCursorSize = 10;
        this.isTracking = false;
        this.isPressing = false;
        initDrawTools();
    }

    private void initDrawTools(){
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if(isTracking) {
            if(isPressing) {
                // 首先清除画布
                clearCanvas(canvas);
                paint.setColor(Color.argb(0xff, 0xff, 0xff, 0xff));
                canvas.drawCircle(ax, ay, touchCursorSize / 2, paint);
            } else {
                clearCanvas(canvas);
            }
        }
        super.onDraw(canvas);
    }

    private void clearCanvas(Canvas canvas){
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
    }

    public void setIsTracking(boolean isTracking) {
        this.isTracking = isTracking;
    }

    public void setTouchCursorSize(int touchCursorSize) {
        this.touchCursorSize = touchCursorSize;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isTracking){
            return super.onTouchEvent(event);
        }
        ax = event.getX();
        ay = event.getY();
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                isPressing = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                isPressing = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                isPressing = false;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
}
