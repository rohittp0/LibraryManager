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
    private final SurfaceHolder mHolder;

    public OverlayView(Context context)
    {
        super(context);
        mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(10f);
        paint.setStyle(Paint.Style.STROKE);
    }

    void drawRect(Rect rect, String label)
    {
        invalidate();
        if (mHolder.getSurface().isValid())
        {
            final Canvas canvas = mHolder.lockCanvas();
            float x = rect.left + 10f;
            float y = rect.bottom + 20f;
            if (canvas != null)
            {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvas.drawColor(Color.TRANSPARENT);
                canvas.drawRect(rect, paint);
                canvas.drawText(label, x, y, paint);
            }
        }
    }
}