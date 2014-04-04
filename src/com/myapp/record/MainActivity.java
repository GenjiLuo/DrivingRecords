package com.myapp.record;

import java.io.File;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;


public class MainActivity extends Activity implements ActionBar.TabListener {
	public static final String MEDIA_DIR = "DrivingRecords";
	public static final String SAVED_DIR = MEDIA_DIR + "/saved";
	
	private static final String RECORD_FILE_TAB = "record";
	private static final String SAVED_FILE_TAB = "saved";
	
	private FileListFragment mRecordFileFragment, mSavedFileFragment;
	private StatusReceiver receiver;  
	private boolean isRecording = false;
	private boolean saveFile = false;
	private ProgressDialog pd;
	private TextView mStatusView;
		
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		File dir = new File(Environment.getExternalStorageDirectory(), MEDIA_DIR);
        if (!dir.exists()) 
            dir.mkdirs();
        
        dir = new File(Environment.getExternalStorageDirectory(), SAVED_DIR);
        if (!dir.exists()) 
            dir.mkdirs();
 		
        mStatusView = (TextView)findViewById(R.id.status_view);
        
		mRecordFileFragment = FileListFragment.newInstance(MEDIA_DIR);
		mSavedFileFragment = FileListFragment.newInstance(SAVED_DIR);
		
		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, 0);
              
		bar.addTab(bar.newTab().setText(getString(R.string.file_tab)).
					setTabListener(this).setContentDescription(RECORD_FILE_TAB));
        bar.addTab(bar.newTab().setText(getString(R.string.saved_tab)).
        			setTabListener(this).setContentDescription(SAVED_FILE_TAB));
        
        isRecording = isRecordServiceRunning(this);
			    
	    receiver = new StatusReceiver();  
        IntentFilter filter = new IntentFilter();  
        filter.addAction("android.intent.action.REFRESH");  
        registerReceiver(receiver, filter);  
        
        DisplayStorageSize();
        timer.postDelayed(timerRunnable, 1000);
 	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.actions, menu);
		
		return true;
	}
	
	@Override  
    public boolean onPrepareOptionsMenu(Menu menu) {  
		if (isRecording) {
			menu.findItem(R.id.action_rec).
				setIcon(android.R.drawable.ic_media_pause);
			
			menu.findItem(R.id.action_preview).setVisible(true);
		} else {
			menu.findItem(R.id.action_rec).
				setIcon(android.R.drawable.ic_media_play);
			
			menu.findItem(R.id.action_preview).setVisible(false);
		}	
		
		if (saveFile)
			menu.findItem(R.id.action_save).
				setIcon(R.drawable.ic_check_on);
		else
			menu.findItem(R.id.action_save).
				setIcon(R.drawable.ic_check_off);
		
		return super.onPrepareOptionsMenu(menu);  
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		String action;
		
        switch (item.getItemId()) {
        case R.id.action_rec:
        	if (!isRecording) {
        		isRecording = true;
        		
        		intent = new Intent(MainActivity.this, RecordService.class);
        		action = saveFile ? 
							RecordService.SAVE_FILE_ACTION : 
							RecordService.UNSAVE_FILE_ACTION; 
        		intent.setAction(action);
        		
           		startService(intent);
        	} else {
        		isRecording = false;
        		intent = new Intent(MainActivity.this, RecordService.class);
        		stopService(intent);
        	}
        	
        	getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
        	break;
        	
        case R.id.action_preview:
        	intent = new Intent(MainActivity.this, RecordService.class);
    		action = RecordService.PREVIEW_ACTION; 
    		intent.setAction(action);
    		startService(intent);
        	break;
        	
        case R.id.action_delete:
        	DialogFragment newFragment = 
        			AlertDialogFragment.newInstance(R.string.alert_dialog_title);
            newFragment.show(getFragmentManager(), "dialog");
        	break; 
        
        case R.id.action_save:
        	saveFile = !saveFile;
        	
        	if (isRecording) {
        		intent = new Intent(MainActivity.this, RecordService.class);
        		action = saveFile ? 
        					RecordService.SAVE_FILE_ACTION : 
        					RecordService.UNSAVE_FILE_ACTION; 
        		intent.setAction(action);
        		startService(intent);
        	}
        	
        	getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
           	break;
        
        /*case R.id.action_close:
        	finish();
        	break; */
        	
        default: ;	
        }
        
        return true;
    }
	 
	@Override
    protected void onResume() {                                                                                                                                                                                                                                                      
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        if (isRecording) {
        	Intent intent = new Intent(MainActivity.this, RecordService.class);
    		String action = RecordService.CLOSE_PREVIEW_ACTION; 
    		intent.setAction(action);
    		startService(intent);
        }
    }
     
    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    	
    }
    
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
    	if(RECORD_FILE_TAB.equals(tab.getContentDescription())) 
    		ft.add(R.id.list_file_layout, mRecordFileFragment, RECORD_FILE_TAB);
    	else if (SAVED_FILE_TAB.equals(tab.getContentDescription())) 
    		ft.add(R.id.list_file_layout, mSavedFileFragment, SAVED_FILE_TAB);
    }
    
    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    	if(RECORD_FILE_TAB.equals(tab.getContentDescription())) 
    		ft.remove(mRecordFileFragment);
    	else if (SAVED_FILE_TAB.equals(tab.getContentDescription())) 
    		ft.remove(mSavedFileFragment);
    }
   
    public boolean isRecordServiceRunning(Context context) {  
    	ActivityManager manager = (ActivityManager)context.
    							getSystemService(Context.ACTIVITY_SERVICE);
    	
    	for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {  
        	if (RecordService.class.getName().equals(service.service.getClassName()))   
                return true;  
        }  
    	
        return false;  
    } 
    
    private class StatusReceiver extends BroadcastReceiver {  
        @Override  
        public void onReceive(Context context, Intent intent) {  
        	ActionBar.Tab tab = getActionBar().getSelectedTab();
        	
        	if (RECORD_FILE_TAB.equals(tab.getContentDescription())) 
        		mRecordFileFragment.refreshFileList();
        	else if (SAVED_FILE_TAB.equals(tab.getContentDescription())) 	
        	   	mSavedFileFragment.refreshFileList();
        	
        	isRecording = isRecordServiceRunning(getApplication());
        	getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
        }  
    }  
    
    private void deleteSavedFiles() {
    	new Thread() {
    		public void run() {
    			try {
    				ArrayList<RecordFileList.FileInfo> fileList = 
    							RecordFileList.getFilelist(SAVED_DIR, ".mp4");
		
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
		long totalSize = ExternalStorageHelper.getTotalExternalStorageSize();
    	long avaSize = ExternalStorageHelper.getAvailableExternalStorageSize();
    	
    	mStatusView.setText(" 存储空间容量: " + totalSize/1024/1024 + "MB " +
    						"    " + 
    						"可用存储空间: " + avaSize/1024/1024 + "MB");
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
