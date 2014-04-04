package com.myapp.record;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;


public class CameraPreview extends SurfaceView {
	//public static float PICTURE_RATIO_16_9 = 16/9;
	//public static float PICTURE_RATIO_4_3 = 4/3;
	
	public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(heightMeasureSpec*16/9, heightMeasureSpec);
	}
}
