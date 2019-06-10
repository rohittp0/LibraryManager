package com.make.it.kit.librarymanager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class OverlayView extends SurfaceView
{

    private final Paint paint;
    private final Paint textPaint;
    private final SurfaceHolder mHolder;

    public OverlayView(Context context)
    {
        super(context);
        mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(6f);
        paint.setStyle(Paint.Style.STROKE);

        textPaint.setTextSize(30);
        textPaint.setColor(Color.BLUE);
    }

    void drawText(Rect rect, String text)
    {
        invalidate();
        if (mHolder.getSurface().isValid())
        {
            final Canvas canvas = mHolder.lockCanvas();
            if (canvas != null)
            {
                canvas.drawRect(rect, paint);
                canvas.drawText(text, rect.left + 3f, rect.bottom + 20f, textPaint);
            }
            mHolder.unlockCanvasAndPost(canvas);
        }
    }

    void clearCanvas()
    {
        invalidate();
        if (mHolder.getSurface().isValid())
        {
            final Canvas canvas = mHolder.lockCanvas();
            if (canvas != null)
            {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvas.drawColor(Color.TRANSPARENT);
            }
            mHolder.unlockCanvasAndPost(canvas);
        }
    }
}