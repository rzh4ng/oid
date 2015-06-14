package rz.remotesteer;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class VerticalSeekBar extends SeekBar {

    public VerticalSeekBar(Context context) {
        super(context);
    }


    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }


    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);
        super.onDraw(c);
    }

    @Override public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                super.onTouchEvent(event);
                break;
            case MotionEvent.ACTION_DOWN:
                //super.onTouchEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                int progress = getMax() - (int)(getMax() * (event.getY() / getHeight()));
                Log.d("remote steer: ", "Y=" + event.getY() + "; H=" + getHeight() + "; max=" + getMax() + "; Prog=" + progress);
                setProgress(progress);
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }

}

