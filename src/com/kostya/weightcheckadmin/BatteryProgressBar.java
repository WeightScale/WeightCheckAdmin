package com.kostya.weightcheckadmin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.support.annotation.NonNull;

/*
 * Created by Kostya on 16.11.2014.
 */
public class BatteryProgressBar extends ProgressBar {
    private String text = "";
    private int textColor = Color.BLACK;
    private float textSize = getResources().getDimension(R.dimen.text_micro);
    private final Paint textPaint;
    private final Rect bounds;

    public BatteryProgressBar(Context context) {
        super(context);
        textPaint = new Paint();
        bounds = new Rect();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(textSize);
    }

    public BatteryProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint = new Paint();
        bounds = new Rect();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(textSize);
    }

    public synchronized void updateProgress(int progress) {

        setProgress(0);

        if (progress > 15) {
            if (progress < 50)
                setTextColor(Color.WHITE);
            else
                setTextColor(Color.BLACK);
            setProgressDrawable(getResources().getDrawable(R.drawable.battery));
        } else {
            setTextColor(Color.WHITE);
            setProgressDrawable(getResources().getDrawable(R.drawable.battery_discharged));
        }
        setText(String.valueOf(progress));
        setProgress(progress);
        drawableStateChanged();
    }

    private void setText(String text) {
        this.text = text + '%';
        postInvalidate();
    }

    @Override
    protected synchronized void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        textPaint.setColor(textColor);
        //Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        int x = getWidth() / 2 - bounds.centerX();
        int y = getHeight() / 2 - bounds.centerY();
        canvas.drawText(text, x, y, textPaint);
    }

    public synchronized void setTextColor(int textColor) {
        this.textColor = textColor;
        postInvalidate();
    }

    /*private void setAttrs(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.TextProgressBar, 0, 0);
            setText(a.getString(R.styleable.TextProgressBar_text));
            setTextColor(a.getColor(R.styleable.TextProgressBar_textColor, Color.BLACK));
            setTextSize(a.getDimension(R.styleable.TextProgressBar_textSize, getResources().getDimension(R.dimen.text_micro)));
            a.recycle();
        }
    }*/

    public synchronized void setTextSize(float textSize) {
        this.textSize = textSize;
        postInvalidate();
    }

}
