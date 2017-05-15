package com.harpe.harpemessenger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by anthony on 14/05/2017.
 */

public class SwitchCameraButtonView extends View {

    private static final String TAG = "HELog";
    private Paint paint;
    private int screenHeight;
    private int screenWidth;

    private int radius;
    RectF rectF;

    public SwitchCameraButtonView(Context context) {
        super(context);
        paint = new Paint();
    }

    public SwitchCameraButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;
        radius = screenWidth/15;
        int xPos = screenWidth - screenWidth/10;
        rectF = new RectF(xPos-radius/2,
                radius/2+ HomePageActivity.heightOfStatusBar/2,
                xPos+radius/2,
                3*radius/2+ HomePageActivity.heightOfStatusBar/2);
    }

    public SwitchCameraButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(15);
        paint.setAlpha(200);
        canvas.drawRoundRect(rectF, 10, 10, paint);
    }

    public void animateButton() {

    }
}