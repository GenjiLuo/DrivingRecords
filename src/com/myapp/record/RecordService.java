package com.myapp.record;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OutputFormat;
import android.media.MediaRecorder.VideoEncoder;
import android.os.Handler;
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
	public static final String CHECK_STATUS_ACTION = "com.myapp.action.CHECK_STATUS";
	public static final String SAVE_FILE_ACTION = "com.myapp.action.SAVE_FILE";
	public static final String UNSAVE_FILE_ACTION = "com.myapp.action.UNSAVE_FILE";
	public static final String PREVIEW_ACTION = "com.myapp.action.PREVIEW";
	public static final String CLOSE_PREVIEW_ACTION = "com.myapp.action.CLOSE_PREVIEW";
	public static final String REFRESH_ACTION = "android.intent.action.REFRESH";
	public static final String STATUS_ACTION = "android.intent.action.STATUS";
	
	private static final int NOTIFICATION_ID = 1;
	
    private static final int REC_DURATION = 3*60*1000;
    
    private static final long GIGABYTES = 1024*1024*1024;
    private static final long RES_SIZE = 5*GIGABYTES;
    
    private static final int RESTART_TIME = 1000*60;
    private static final int FREEZE_CHECK_TIME = REC_DURATION + 30*1000;
    
    private MediaRecorder mMr = null;
    private Camera mCam = null;
    private WindowManager windowManager;
    private CameraPreview previewSurface;
    private FrameLayout previewLayout;
    private Size mPreviewSize;
    private SurfaceHolder mHolder;
    private LayoutParams mlayoutParams;
    private boolean mSaveFileFlag = false, mSurfaceCreated = false;
    private String mTempFileName;
    private MediaDirectory mCycleDir = new MediaDirectory(), 
    						mSaveDir = new MediaDirectory();   
    private String mStorage;
    private boolean mNoSdCard;
    private Handler mRestartTimer = new Handler(),
    				mFreezeCheckTimer = new Handler(); 
    private RestartRecord mRestart = new RestartRecord();
    private FreezeCheck mFreezeCheck = new FreezeCheck();
    private int mPrevRecCounter = 0, mRecCounter = 0;
    private boolean mNeedRestart = false;
    
    
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
			} else if (action.equals(CHECK_STATUS_ACTION)) {
				Intent i = new Intent();  
		        i.setAction(STATUS_ACTION);
		        i.putExtra("status", mSaveFileFlag ? 1 : 0);
		        sendBroadcast(i);
		        
		        sendRefreshStatus();
			}
		}	
		
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}
	
	@Override
	public void onCreate() {
		Log.i(TAG, "RecordService start");
		
		// Start foreground service to avoid unexpected kill
        Notification notification = new Notification.Builder(this)
            .setContentTitle("Background Video Recorder")
            .setContentText("")
            .setSmallIcon(R.drawable.ic_launcher)
            .build();
        startForeground(NOTIFICATION_ID, notification);
        
		mNoSdCard = !checkStorage();	
		if (mNoSdCard) {
			Log.i(TAG, "No valid storage");
			mRestartTimer.postDelayed(mRestart, RESTART_TIME);
		} else {
			mFreezeCheckTimer.postDelayed(mFreezeCheck, FREEZE_CHECK_TIME);
		}
	        
		// Create new SurfaceView, set its size to 1x1, 
        // move it to the top left corner and set this service as a callback
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        
        mMr = new MediaRecorder();
        mMr.setOnInfoListener(this);
        mMr.setOnInfoListener(this);
        
        LayoutInflater inflater = LayoutInflater.from(getApplication());  
        previewLayout = (FrameLayout) inflater.inflate(R.layout.camera_preview, null);
        //previewLayout.setBackgroundColor(getResources().getColor(R.color.black));
        previewLayout.setOnClickListener(this);
        
        mlayoutParams = new WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        mlayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowManager.addView(previewLayout, mlayoutParams);
        
        previewSurface = (CameraPreview)previewLayout.findViewById(R.id.preview_surface);
        previewSurface.setBackgroundColor(getResources().getColor(R.color.solid_blue));
        
        mHolder = previewSurface.getHolder();
        mHolder.addCallback(this);
   	}
	
    // Stop recording and remove SurfaceView
    @Override
    public void onDestroy() {
    	Log.i(TAG, "RecordService stopped");
    	
    	if (!mNoSdCard) {
    		stopRecord();
    		releaseMediaRecorder();
    		releaseCamera();
    		sendRefreshStatus();
    	}   
    	
    	windowManager.removeView(previewLayout);
    	
    	mRestartTimer.removeCallbacks(mRestart);
    	mFreezeCheckTimer.removeCallbacks(mFreezeCheck);
    	
    	if (mNeedRestart) {
    		Log.i(TAG, "Restart service");
    		
    		Intent intent = new Intent(getApplication(), RecordService.class);
            startService(intent);
    	}
    }
    
    @Override
    public void	onClick(View v) {
    	mlayoutParams.width = 1;
    	mlayoutParams.height = 1;
    	windowManager.updateViewLayout(previewLayout, mlayoutParams);  
    }
    
    // Method called right after Surface created (initializing and starting MediaRecorder)
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    	mSurfaceCreated = true;
    	
    	if (!mNoSdCard)
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
	
	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
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
		
		parameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
		
		if (parameters.isZoomSupported()) {
            parameters.setZoom(0);
        }
		
		//parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
		
		if (parameters.isVideoStabilizationSupported())
			parameters.setVideoStabilization(true);
		
		//if (parameters.isAutoExposureLockSupported())
		//	parameters.setAutoExposureLock(true);
		
		List<Camera.Size> size = parameters.getSupportedPreviewSizes();
		mPreviewSize = getOptimalPreviewSize(size, 1280, 720);
		parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
		previewSurface.setPreviewRatio(mPreviewSize.width, mPreviewSize.height);
		
		List<int[]> frameRates = parameters.getSupportedPreviewFpsRange();
        if (frameRates != null && frameRates.size() > 0) {
        	int[] fpsRange = frameRates.get(frameRates.size() - 1);
        	parameters.setPreviewFpsRange(fpsRange[Parameters.PREVIEW_FPS_MIN_INDEX],
                    						fpsRange[Parameters.PREVIEW_FPS_MAX_INDEX]);
        }
        
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
        profile.videoFrameHeight = mPreviewSize.height;
        profile.videoFrameWidth = mPreviewSize.width;
        profile.fileFormat = OutputFormat.MPEG_4;
        //profile.videoBitRate = 3000000;
        mMr.setProfile(profile);
        
        mTempFileName = getOutputMediaFileName();
        mMr.setOutputFile(mTempFileName);
        
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
    private String getOutputMediaFileName() {
        File mediaStorageDir = new File(mStorage, StoragePath.TEMP_DIR);
        
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", 
        										Locale.CHINA)
        										.format(new Date());
        
        return  mediaStorageDir.getPath() + File.separator +
							"VD-"+ timeStamp + ".mp4" + ".tmp";
    }
    
    private void startRecord() {
       	boolean ret = freeStorageSpace(getCurrentDirectory(mSaveFileFlag));
    	sendRefreshStatus();
    	if (!ret)
    		stopSelf();
    	
    	previewSurface.setBackgroundColor(0x00000000);
    	
    	if (prepareCamera() && prepareVideoRecorder())
    		mMr.start();
    }
    
    private void stopRecord() {
    	previewSurface.setBackgroundColor(getResources().
    										getColor(R.color.solid_blue));
    	
    	mMr.stop();
		mMr.reset();
		
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
    	
    		boolean ret = freeStorageSpace(getCurrentDirectory(mSaveFileFlag));
        	sendRefreshStatus();
        	if (!ret)
        		stopSelf();
     		
       		if (prepareVideoRecorder()) 
    			mMr.start();
       		
       		mRecCounter++;
    	}
    }
    
    @Override
    public void	onError(MediaRecorder mr, int what, int extra) {
    	Log.e(TAG, "error occurs while recording, error type: " 
    				+ what + " extra code: " + extra);
    	
     	releaseMediaRecorder();
    	releaseCamera();
    	windowManager.removeView(previewSurface);
          
       	stopSelf();
    }
    
    private void sendRefreshStatus() {
    	Intent intent = new Intent();  
        intent.setAction(REFRESH_ACTION); 
        intent.putExtra("cycle_total", mCycleDir.totalSize);
        intent.putExtra("cycle_avail", mCycleDir.available);
        intent.putExtra("save_total", mSaveDir.totalSize);
        intent.putExtra("save_avail", mSaveDir.available);
        sendBroadcast(intent);  
    }
    
    private void saveFile(boolean save) {
    	if (!ExternalStorage.checkAvailable(mStorage))
    		return;
    	
    	File file = new File(mTempFileName);
    	
    	String fileName = mTempFileName.substring(0, mTempFileName.lastIndexOf('.'));

    	String path = save ? mSaveDir.directory.getPath() : 
    					mCycleDir.directory.getPath();
    	
    	fileName = path + File.separator + 
    				fileName.substring(fileName.lastIndexOf(File.separator));
    	    	
		file.renameTo(new File(fileName));
    }
    
    private boolean checkFreeSize(long size, long totalSize, int percent) {
		return size > totalSize*(100-percent)/100 ? false : true;
	}
   
	private boolean deleteFilesToFreeSpace(MediaDirectory dir, long desireFreeSize) {
		long freeSize = 0;
		
		ArrayList<RecordFileList.FileInfo> recFileList = 
					RecordFileList.getFilelist(dir.directory.getPath(), ".mp4");
		if (recFileList == null)
			return false;
		
		Toast.makeText(getApplication(), 
						"ÊÍ·Å´æ´¢¿Õ¼ä", 
						Toast.LENGTH_SHORT).show();
		
		for (int i = recFileList.size()-1; i >= 0; i--) {
			RecordFileList.FileInfo fileInfo = recFileList.get(i);
			File file = new File(fileInfo.path);
			freeSize += file.length();
			file.delete();
		   
			if (freeSize >= desireFreeSize)
				return true;
		}
		
		return false;
	}
   
	private boolean freeStorageSpace(MediaDirectory dir) {
		long usedSize = getFileSize(dir.directory);
		boolean ret = true;
		
		if (!checkFreeSize(usedSize, dir.totalSize, 10)) {
			if (!deleteFilesToFreeSpace(dir, usedSize - dir.totalSize*85/100))
				ret = false;
		}	
		
		dir.available = dir.totalSize - usedSize;			   
		return ret;
	}
	
	private long getFileSize(File f) {
		long size = 0;
		
		if (f.isDirectory()) {
			File flist[] = f.listFiles();
			for (File file : flist) {
				size +=  getFileSize(file);
			}
		} else 
			size = f.length();
	
		return size;
	}
	
	private MediaDirectory getCurrentDirectory(boolean flag) {
		return mSaveFileFlag ? mSaveDir : mCycleDir; 
	}
	
	private void deleteTempFiles() {
		File tempDir = new File(mStorage, StoragePath.TEMP_DIR);
		
		File[] files = tempDir.listFiles();
    	if (files != null) {
    		for (int i = 0; i < files.length; i++) 
    			files[i].delete();
    	}	
	}
	
	private boolean checkStorage() {
		String storage;
		
		storage = ExternalStorage.getStorageDirectory();
		if (storage == null)
			return false;
		
		long totalStorageSize = ExternalStorage.
								getTotalStorageSize(storage);
		if (totalStorageSize > RES_SIZE)
			totalStorageSize -= RES_SIZE;
		else 
			return false;
		
		mStorage = storage;
		
		String path = mStorage + "/" + StoragePath.CYCLE_DIR;
		mCycleDir.directory = new File(path);
		mCycleDir.totalSize = totalStorageSize/2;
		mCycleDir.available = mCycleDir.totalSize - getFileSize(mCycleDir.directory);
		if (!mCycleDir.directory.exists())
			mCycleDir.directory.mkdirs();
		
		path = mStorage + "/" + StoragePath.SAVE_DIR;
		mSaveDir.directory = new File(path);
		mSaveDir.totalSize = totalStorageSize/2;
		mSaveDir.available = mSaveDir.totalSize - getFileSize(mSaveDir.directory);
		if (!mSaveDir.directory.exists())
			mSaveDir.directory.mkdirs();
        
       	File tempDir = new File(mStorage, StoragePath.TEMP_DIR);
        if (!tempDir.exists()) 
        	tempDir.mkdirs();
        else 
        	deleteTempFiles();
        
        return true;
	}
	
	private class MediaDirectory {
		File directory;
		long totalSize;
		long available;
	}
	
	private class RestartRecord implements Runnable {
		@Override  
	    public void run() {  
			Log.i(TAG, "Recheck storage");
			
			mNoSdCard = !checkStorage();
			if (mNoSdCard)
				mRestartTimer.postDelayed(this, RESTART_TIME);
			else {
				Log.i(TAG, "Restart record");
				mFreezeCheckTimer.postDelayed(mFreezeCheck, FREEZE_CHECK_TIME);
				startRecord();
			}	
		}
	}
	
	private class FreezeCheck implements Runnable {
		@Override  
	    public void run() {  
			Log.i(TAG, "FreezeCheck");
			
			if (mPrevRecCounter == mRecCounter) {
				Log.i(TAG, "Detected freeze");
				stopSelf();
				mNeedRestart = true;
				return;
			}	
			
			mPrevRecCounter = mRecCounter;
			
			mFreezeCheckTimer.postDelayed(this, FREEZE_CHECK_TIME);
		}
	}
}
