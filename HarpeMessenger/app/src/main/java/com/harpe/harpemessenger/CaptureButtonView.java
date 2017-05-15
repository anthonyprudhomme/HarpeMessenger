package com.harpe.harpemessenger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by anthony on 14/05/2017.
 */

public class CaptureButtonView extends View {

    private static final String TAG = "HELog";
    private Paint paint;
    private int screenHeight;
    private int screenWidth;

    private int radius = 100;

    public CaptureButtonView(Context context) {
        super(context);
        paint = new Paint();
    }

    public CaptureButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;
    }

    public CaptureButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(15);
        paint.setAlpha(200);
        int xPos = screenWidth/2;
        int yPos = (int) (screenHeight - 3*radius);
        canvas.drawCircle(xPos,yPos,radius,paint);
    }

    public void animateButton(){

    }
}