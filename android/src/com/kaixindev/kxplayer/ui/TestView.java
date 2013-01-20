package com.kaixindev.kxplayer.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.kaixindev.android.Log;

public class TestView extends View {
	
	private float mProgress = 1f;

	public TestView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			Log.d("x:" + event.getX());
			updateProgress(event.getX());
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			Log.d("action up.");
			break;
		}
		return true;
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Paint paint = new Paint();
		paint.setColor(Color.GREEN);
		int width = getWidth() - getPaddingLeft() - getPaddingRight();
		int bottom = getHeight() - getPaddingBottom();
		int right = (int)(mProgress*width) + getPaddingLeft();
		Log.d("right:" + right + ", bottom:" + bottom + ", width:" + width);
		canvas.drawRect(getPaddingLeft(), getPaddingTop(), right, bottom, paint);
	}
	
	private void updateProgress(float x) {
		int width = getWidth() - getPaddingLeft() - getPaddingRight();
		if (x >= getWidth() - getPaddingRight()) {
			mProgress = 1;
		} else if (x <= getPaddingLeft()) {
			mProgress = 0;
		} else {
			mProgress = (x - getPaddingLeft())/width;
		}
	}
	
}
