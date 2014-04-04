package com.myapp.record;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OutputFormat;
import android.media.MediaRecorder.VideoEncoder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.Toast;


public class RecordService extends Service implements 
							SurfaceHolder.Callback, 
							MediaRecorder.OnInfoListener,
							MediaRecorder.OnErrorListener, 
							View.OnClickListener {
	
	private static final String TAG = "RecordService";
	public static final String SAVE_FILE_ACTION = "com.myapp.action.SAVE_FILE";
	public static final String UNSAVE_FILE_ACTION = "com.myapp.action.UNSAVE_FILE";
	public static final String PREVIEW_ACTION = "com.myapp.action.PREVIEW";
	public static final String CLOSE_PREVIEW_ACTION = "com.myapp.action.CLOSE_PREVIEW";
	    
    private static final int NOTIFICATION_ID = 1;
    private static final int VIDEO_WIDTH = 1280, VIDEO_HEIGHT = 720;
    private static final int REC_DURATION = 3*60*1000;
    private static final int FILE_COUNT = 40; 
    
    private MediaRecorder mMr = null;
    private Camera mCam = null;
    private WindowManager windowManager;
    private CameraPreview previewSurface;
    private FrameLayout previewLayout;
    private SurfaceHolder mHolder;
    private LayoutParams mlayoutParams;
    private boolean mSaveFileFlag = false, mSurfaceCreated = false;
    private String mCurrentFileName;
    
    
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if (action != null) {
			if (action.equals(SAVE_FILE_ACTION)) 
				mSaveFileFlag = true;
			else if (action.equals(UNSAVE_FILE_ACTION))
				mSaveFileFlag = false;
			else if (action.equals(PREVIEW_ACTION)) {
				if (mSurfaceCreated) {
					mlayoutParams.width = LayoutParams.MATCH_PARENT;
		    		mlayoutParams.height = LayoutParams.MATCH_PARENT;
		    		windowManager.updateViewLayout(previewLayout, mlayoutParams);  
				}
			} else if (action.equals(CLOSE_PREVIEW_ACTION)) {
				if (mSurfaceCreated) {
					mlayoutParams.width = 1;
		    		mlayoutParams.height = 1;
		    		windowManager.updateViewLayout(previewLayout, mlayoutParams);  
				}
			}
		}	
		
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}
	
	@Override
	public void onCreate() {
		// Start foreground service to avoid unexpected kill
        Notification notification = new Notification.Builder(this)
            .setContentTitle("Background Video Recorder")
            .setContentText("")
            .setSmallIcon(R.drawable.ic_launcher)
            .build();
        startForeground(NOTIFICATION_ID, notification);

        // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        
        mMr = new MediaRecorder();
        mMr.setOnInfoListener(this);
        mMr.setOnInfoListener(this);
        
        LayoutInflater inflater = LayoutInflater.from(getApplication());  
        previewLayout = (FrameLayout) inflater.inflate(R.layout.camera_preview, null);
        previewLayout.setBackgroundColor(getResources().getColor(R.color.black));
	    
        //previewSurface = new CameraPreview(this, null);
        
        mlayoutParams = new WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        mlayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowManager.addView(previewLayout, mlayoutParams);
        
        previewSurface = (CameraPreview)previewLayout.findViewById(R.id.preview_surface);
        
        mHolder = previewSurface.getHolder();
        mHolder.addCallback(this);
        
        previewSurface.setOnClickListener(this);
   	}
	
    // Stop recording and remove SurfaceView
    @Override
    public void onDestroy() {
    	stopRecord();
    	releaseMediaRecorder();
    	releaseCamera();
        windowManager.removeView(previewLayout);
        
        sendRefreshStatus();
    }
    
 // Method called right after Surface created (initializing and starting MediaRecorder)
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    	mSurfaceCreated = true;
    	startRecord();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }
	 
	@Override
    public IBinder onBind(Intent intent) {
        return null;
    }
	
	private boolean prepareCamera() {
		try {
			mCam = Camera.open();
		} catch (Exception e) {
			Log.e(TAG, "Open camera: " + e.toString());
			mCam = null;
			return false;
		}
		
		Camera.Parameters parameters = mCam.getParameters();
		parameters.setPreviewSize(VIDEO_WIDTH, VIDEO_HEIGHT);
		if (parameters.isAutoExposureLockSupported())
			parameters.setAutoExposureLock(true);
		mCam.setParameters(parameters);
		
		return true;
	}
	
	private boolean prepareVideoRecorder() {
        mCam.unlock();
        mMr.setCamera(mCam);
        
        mMr.setPreviewDisplay(mHolder.getSurface());
        
        mMr.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMr.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        
        CamcorderProfile profile = CamcorderProfile.get(0, CamcorderProfile.QUALITY_HIGH);
        profile.videoCodec = VideoEncoder.H264;
        profile.videoFrameHeight = VIDEO_HEIGHT;
        profile.videoFrameWidth = VIDEO_WIDTH;
        profile.fileFormat = OutputFormat.MPEG_4;
        //profile.videoFrameRate = 30;
        profile.videoBitRate = 2000000;
        mMr.setProfile(profile);
        
        mCurrentFileName = getOutputMediaFileName();
        mMr.setOutputFile(mCurrentFileName);
        
        mMr.setMaxDuration(REC_DURATION);
        
        try {
            mMr.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        
        return true;
    }
	
	/** Create a File for saving an image or video */
    private static String getOutputMediaFileName() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), 
        								MainActivity.MEDIA_DIR);
        
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", 
        										Locale.CHINA)
        										.format(new Date());
        
        return  mediaStorageDir.getPath() + File.separator +
							"VD-"+ timeStamp + ".mp4" + ".tmp";
    }
    
    private void startRecord() {
    	if (!ExternalStorageHelper.externalStorageAvailable())
    		return;
    	
    	boolean ret = freeStorageSpace();
    	sendRefreshStatus();
    	if (!ret)
    		stopSelf();
    	
    	if (prepareCamera() && prepareVideoRecorder())
    		mMr.start();
    }
    
    private void stopRecord() {
    	mMr.stop();
		mMr.reset();
		releaseCamera();
		
		saveFile(mSaveFileFlag);
    }
    
    private void releaseCamera() {
    	if (mCam != null) {
    		mCam.lock();      // lock camera for later use
        	mCam.release();
        	mCam = null;
    	}
    }
    
    private void releaseMediaRecorder() {
        if (mMr != null) {
        	mMr.release();    // release the recorder object
            mMr = null;
        }
    }
    
    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
    	if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
    		mMr.stop();
    		mMr.reset();
    		
    		saveFile(mSaveFileFlag);
    	
    		boolean ret = freeStorageSpace();
        	sendRefreshStatus();
        	if (!ret)
        		stopSelf();
     		
       		if (prepareVideoRecorder()) 
    			mMr.start();
    	}
    }
    
    @Override
    public void	onError(MediaRecorder mr, int what, int extra) {
    	Log.e(TAG, "error occurs while recording, error type: " 
    				+ what + " extra code: " + extra);
    	
     	releaseMediaRecorder();
    	releaseCamera();
    	windowManager.removeView(previewSurface);
          
        saveFile(mSaveFileFlag);
    	
    	stopSelf();
    }
    
    private void deleteOldFiles() {
    	ArrayList<RecordFileList.FileInfo> recFileList = 
				RecordFileList.getFilelist(MainActivity.MEDIA_DIR, ".mp4");
		
		if (recFileList.size() >= FILE_COUNT) {
			int deleteNum = recFileList.size() - (FILE_COUNT - 1);
			for (int i = 0; i < deleteNum; i++) {
				RecordFileList.FileInfo fileInfo = recFileList.get(i);
	    		File file =new File(fileInfo.path);
	    		file.delete();
			}	
		}
    }
    
    private void sendRefreshStatus() {
    	Intent intent = new Intent();  
        intent.setAction("android.intent.action.REFRESH");  
        sendBroadcast(intent);  
    }
    
    private void saveFile(boolean save) {
    	File file = new File(mCurrentFileName);
    	
    	String fileName = mCurrentFileName.substring(0, mCurrentFileName.lastIndexOf('.'));

    	if (save)
    		fileName = Environment.getExternalStorageDirectory().toString() + 
    					File.separator + 
    					MainActivity.SAVED_DIR + 
    					File.separator + 
    					fileName.substring(fileName.lastIndexOf(File.separator));
    	    	
		file.renameTo(new File(fileName));
    }
    
    @Override
    public void	onClick(View v) {
    		mlayoutParams.width = 1;
    		mlayoutParams.height = 1;
    		windowManager.updateViewLayout(previewLayout, mlayoutParams);  
    }
    
	private boolean checkStorageSpace(int percent) {
		long totalSize = ExternalStorageHelper.getTotalExternalStorageSize();
		long avaSize = ExternalStorageHelper.getAvailableExternalStorageSize();
	   
		return avaSize <= totalSize*percent/100 ? false : true;
	}
   
	private void deleteFilesToFreeSpace() {
		ArrayList<RecordFileList.FileInfo> recFileList = 
					RecordFileList.getFilelist(MainActivity.MEDIA_DIR, ".mp4");
		
		Toast.makeText(getApplication(), 
						" Õ∑≈¥Ê¥¢ø’º‰", 
						Toast.LENGTH_SHORT).show();
		
		for (int i = 0; i < recFileList.size(); i++) {
			RecordFileList.FileInfo fileInfo = recFileList.get(i);
			File file =new File(fileInfo.path);
			file.delete();
		   
			if (checkStorageSpace(20))
				break;
		}
	}
   
	private boolean freeStorageSpace() {
		if (!checkStorageSpace(10)) {
			deleteFilesToFreeSpace();
			if (!checkStorageSpace(10)) 
				return false;
		} else
			deleteOldFiles();
	   
		return true;
	}
}
