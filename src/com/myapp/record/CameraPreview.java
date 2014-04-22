package com.myapp.record;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;


public class CameraPreview extends SurfaceView {
	private int previewWidth = 16, previewHeight = 9;
	
	public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(heightMeasureSpec*previewWidth/previewHeight, 
								heightMeasureSpec);
	}
	
	public void setPreviewRatio(int width, int height) {
		previewWidth = width;
		previewHeight = height;
		
		requestLayout();
	}
}
