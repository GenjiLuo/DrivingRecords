package com.myapp.record;

import java.io.File;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;


public class MainActivity extends Activity implements 
							ActionBar.TabListener,
							View.OnClickListener {
	private static final String RECORD_FILE_TAB = "record";
	private static final String SAVED_FILE_TAB = "saved";
	
	private FileListFragment mRecordFileFragment = null, 
								mSavedFileFragment = null;
	private StatusReceiver receiver;  
	private boolean saveFile = false;
	private boolean noSecondaryStorage = false;
	private ProgressDialog pd;
	private TextView mStorageSizeText, mDirectorySizeText;
	private ExternalStorage.StorageDirectory mStorageDir;
	private Button mPreviewButton, mDeleteButton;
	private ToggleButton mSaveButton;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
		
		setContentView(R.layout.activity_main);
		
		mStorageDir = ExternalStorage.getStorageDirectory();
		if (mStorageDir.type == ExternalStorage.PRIMARY_STORAGE)
			noSecondaryStorage = true;
		
		mStorageSizeText = (TextView)findViewById(R.id.storage_size_text);
		mDirectorySizeText = (TextView)findViewById(R.id.dir_size_text);
		
		mPreviewButton = (Button)findViewById(R.id.preview_btn);
		mPreviewButton.setOnClickListener(this);
				
		mSaveButton = (ToggleButton)findViewById(R.id.save_btn);
		mSaveButton.setOnClickListener(this);
				
		mDeleteButton = (Button)findViewById(R.id.delete_btn);
		mDeleteButton.setOnClickListener(this);
		
		LinearLayout ll = (LinearLayout)findViewById(R.id.button_layout);
		if (noSecondaryStorage)
			ll.setVisibility(View.GONE);

		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, 0);
              
		bar.addTab(bar.newTab().setText(getString(R.string.file_tab)).
					setTabListener(this).setContentDescription(RECORD_FILE_TAB));
        bar.addTab(bar.newTab().setText(getString(R.string.saved_tab)).
        			setTabListener(this).setContentDescription(SAVED_FILE_TAB));
        
        if (savedInstanceState != null) 
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        
        Fragment f = getFragmentManager().findFragmentByTag(RECORD_FILE_TAB);
        if (f != null && !f.isDetached()) {
            FragmentTransaction ft =getFragmentManager().beginTransaction();
            ft.detach(f);
            ft.commit();
        }
        
        f = getFragmentManager().findFragmentByTag(SAVED_FILE_TAB);
        if (f != null && !f.isDetached()) {
            FragmentTransaction ft =getFragmentManager().beginTransaction();
            ft.detach(f);
            ft.commit();
        }
             
        receiver = new StatusReceiver();  
        IntentFilter filter = new IntentFilter();  
        filter.addAction(RecordService.REFRESH_ACTION);
        filter.addAction(RecordService.STATUS_ACTION);
        registerReceiver(receiver, filter);  
        
        DisplayStorageSize();
        timer.postDelayed(timerRunnable, 1000);
   	}

	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }
	
	@Override
    public void	onClick(View v) {
		if (v == mPreviewButton) {
			Intent intent = new Intent(MainActivity.this, RecordService.class);
    		String action = RecordService.PREVIEW_ACTION; 
    		intent.setAction(action);
    		startService(intent);
		} else if (v == mSaveButton) {
			saveFile = !saveFile;
        	
			Intent intent = new Intent(MainActivity.this, RecordService.class);
			String action = saveFile ? 
        					RecordService.SAVE_FILE_ACTION : 
        					RecordService.UNSAVE_FILE_ACTION; 
			intent.setAction(action);
			startService(intent);
		} else if (v == mDeleteButton) {
			DialogFragment newFragment = 
        			AlertDialogFragment.newInstance(R.string.alert_dialog_title);
            newFragment.show(getFragmentManager(), "dialog");
		}	
	}
	 
	@Override
    protected void onResume() {                                                                                                                                                                                                                                                      
        super.onResume();
        
        if (!noSecondaryStorage) {
        	Intent intent = new Intent(MainActivity.this, RecordService.class);
        	intent.setAction(RecordService.CHECK_STATUS_ACTION);
        	startService(intent);
        }	
        
        if (mRecordFileFragment != null)
        	mRecordFileFragment.refreshFileList();
        
        if (mSavedFileFragment != null)
        	mSavedFileFragment.refreshFileList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        if (!noSecondaryStorage) {
        	Intent intent = new Intent(MainActivity.this, RecordService.class);
        	String action = RecordService.CLOSE_PREVIEW_ACTION; 
        	intent.setAction(action);
        	startService(intent);
        }	
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	unregisterReceiver(receiver);  
    }
    
    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    	
    }
    
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
    	if(RECORD_FILE_TAB.equals(tab.getContentDescription())) {
    		if (mRecordFileFragment == null) {
    			String path = noSecondaryStorage ? null : 
    								mStorageDir.directory.getPath() + "/" +
    								StoragePath.CYCLE_DIR;
    			
    			mRecordFileFragment = FileListFragment.newInstance(path);
    			ft.add(R.id.list_file_layout, mRecordFileFragment, RECORD_FILE_TAB);
    		} else {
    			ft.attach(mRecordFileFragment);
    		}	
    	} else if (SAVED_FILE_TAB.equals(tab.getContentDescription())) {
    		if (mSavedFileFragment == null) {
    			String path = noSecondaryStorage ? null : 
									mStorageDir.directory.getPath() + "/" +
									StoragePath.SAVE_DIR;
    			
    			mSavedFileFragment = FileListFragment.newInstance(path);
    			ft.add(R.id.list_file_layout, mSavedFileFragment, SAVED_FILE_TAB);
    		} else {
    			ft.attach(mSavedFileFragment);
    		}	
    	}	
    }
    
    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    	if(RECORD_FILE_TAB.equals(tab.getContentDescription())) {
    		if (mRecordFileFragment != null)
    			ft.detach(mRecordFileFragment);
    	} else if (SAVED_FILE_TAB.equals(tab.getContentDescription())) { 
    		if (mSavedFileFragment != null)
    			ft.detach(mSavedFileFragment);
    	}	
    }
 
/*    
    private boolean isRecordServiceRunning() {  
    	ActivityManager manager = (ActivityManager)this.
    							getSystemService(Context.ACTIVITY_SERVICE);
    	
    	for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {  
        	if (RecordService.class.getName().equals(service.service.getClassName()))   
                return true;  
        }  
    	
        return false;  
    } 
 */   
    
    private class StatusReceiver extends BroadcastReceiver {  
        @Override  
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	
        	if (action.equals(RecordService.REFRESH_ACTION)) {
        		ActionBar.Tab tab = getActionBar().getSelectedTab();
        	
        		if (RECORD_FILE_TAB.equals(tab.getContentDescription())) 
        			mRecordFileFragment.refreshFileList();
        		else if (SAVED_FILE_TAB.equals(tab.getContentDescription())) 	
        			mSavedFileFragment.refreshFileList();
        		
        		long cycleTotal = intent.getExtras().getLong("cycle_total");
        		long cycleAvail = intent.getExtras().getLong("cycle_avail");
        		long resTotal = intent.getExtras().getLong("save_total");
        		long resAvail = intent.getExtras().getLong("save_avail");
        		
        		DisplayDirectorySize(cycleTotal, cycleAvail, resTotal, resAvail);
        	} else if (action.equals(RecordService.STATUS_ACTION)) {
        		int s = intent.getExtras().getInt("status");
        		if (s != 0) {
        			mSaveButton.setChecked(true);
        			saveFile = true;
        		}	
        	}
        }  
    }  
    
    private void deleteSavedFiles() {
    	new Thread() {
    		public void run() {
    			try {
    				ArrayList<RecordFileList.FileInfo> fileList = 
    							RecordFileList.getFilelist(mStorageDir.directory.getPath() + 
    														"/" +
    														StoragePath.SAVE_DIR, 
															".mp4");
		
    				for (RecordFileList.FileInfo fileInfo : fileList) {
    					File file = new File(fileInfo.path);
    					file.delete();
    				}
	   
    				Message msg = new Message();
    				msg.what = 0;
    				msg.obj = pd;
    				handler.sendMessage(msg);
    			} catch (Exception e) {
	   
    			}
    		}
    	}.start();
    }
    
    private static Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			switch (msg.what) {
			case 0:
				ProgressDialog progDialog = (ProgressDialog)msg.obj;
				progDialog.dismiss();
				break;
							
			default: ;	
			}
		}
	};
	
	public void doPositiveClick() {
        // Do stuff here.
		pd= ProgressDialog.show(this, 
								getString(R.string.dialog_title_2), 
								getString(R.string.dialog_msg_2));

		deleteSavedFiles();

		new Handler().postDelayed(new Runnable() {   
			public void run() {   
				ActionBar.Tab tab = getActionBar().getSelectedTab();
				if (SAVED_FILE_TAB.equals(tab.getContentDescription())) 
					mSavedFileFragment.refreshFileList();	
			}   
		}, 500);   
    }
    
    public void doNegativeClick() {
        // Do stuff here.
    }
    
	public static class AlertDialogFragment extends DialogFragment {
		public static AlertDialogFragment newInstance(int title) {
			AlertDialogFragment frag = new AlertDialogFragment();
			Bundle args = new Bundle();
			args.putInt("title", title);
			frag.setArguments(args);
			return frag;
		}
	        
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			int title = getArguments().getInt("title");
	            
			return new AlertDialog.Builder(getActivity())
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setTitle(title)
							.setPositiveButton(R.string.alert_dialog_ok,
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
											((MainActivity)getActivity()).doPositiveClick();
										}
									})
							.setNegativeButton(R.string.alert_dialog_cancel,
									new DialogInterface.OnClickListener() {
	                            		public void onClick(DialogInterface dialog, int whichButton) {
	                            			((MainActivity)getActivity()).doNegativeClick();
	                            		}
	                        		})
	                        .create();
		}
	}
	
	private void DisplayStorageSize() {
		String info;
		
		if (noSecondaryStorage) {
			info = "Ã»ÓÐ´æ´¢¿¨";
		} else {
			long totalSize = ExternalStorage.
								getTotalStorageSize(mStorageDir.directory.
													getPath());
			long avaSize = ExternalStorage.
    							getAvailableStorageSize(mStorageDir.directory.
    													getPath());
    	
			info = "SD  Total: " + totalSize/1024/1024 + "MB" +
					"   " + 
					"Available: " + avaSize/1024/1024 + "MB";
		}
    	
		mStorageSizeText.setText(info);
	}
	
	private void DisplayDirectorySize(long cycleTotal, long cycleAvail, long saveTotal, long saveAvail) {
		String info;
		
		info = "Cycle Total: " + cycleTotal/1024/1024 + "MB" +
				"  " + "Cycle Available: " + cycleAvail/1024/1024 + "MB" +
				"    " + "Res Total: " + saveTotal/1024/1024 + "MB" +
				"  " + "Res Available: " + saveAvail/1024/1024 + "MB";
		
		mDirectorySizeText.setText(info);
	}
	
	Handler timer = new Handler();  
	
	Runnable timerRunnable = new Runnable() {  
	    @Override  
	    public void run() {  
	    	
	    	DisplayStorageSize();
	    	timer.postDelayed(this, 2000);  
	    }  
	};  
}
