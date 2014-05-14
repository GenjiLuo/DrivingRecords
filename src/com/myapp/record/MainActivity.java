package com.myapp.record;

import java.io.File;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
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
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;

import com.myapp.record.RecordFileList.FileInfo;


public class MainActivity extends Activity implements 
							View.OnClickListener, 
							View.OnSystemUiVisibilityChangeListener, 
							AdapterView.OnItemClickListener {
	private static final String RECORD_FILE_TAB = "record";
	private static final String SAVED_FILE_TAB = "saved";
	
	private GridFileListFragment mRecordFileFragment = null, 
								mSavedFileFragment = null;
	private StatusReceiver receiver;  
	private boolean saveFile = false;
	private boolean noSecondaryStorage = false;
	private ProgressDialog pd;
	private String mStorageDir;
	private LinearLayout mContainer;
	private ListView mMenuItemList;
	private MenuItemAdapter mMenuItemAdapter;
	private ActionBar mActionBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
		
		setContentView(R.layout.activity_main);
		
		mStorageDir = ExternalStorage.getStorageDirectory();
		if (mStorageDir == null)
			noSecondaryStorage = true;
			
		FrameLayout fl = (FrameLayout)findViewById(R.id.menu_layout);
		if (noSecondaryStorage)
			fl.setVisibility(View.GONE);
		
		mContainer = (LinearLayout)findViewById(R.id.container);
		mContainer.setOnSystemUiVisibilityChangeListener(this);

		mMenuItemAdapter = new MenuItemAdapter(this);
		mMenuItemList = (ListView)findViewById(R.id.menu_item_list);
		mMenuItemList.setAdapter(mMenuItemAdapter);
		mMenuItemList.setOnItemClickListener(this);
		
		mActionBar = getActionBar();
		mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, 0);
      
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        
        String path = noSecondaryStorage ? null : 
										mStorageDir + "/" +
										StoragePath.CYCLE_DIR;
        mRecordFileFragment = GridFileListFragment.newInstance(path);
        
        path = noSecondaryStorage ? null : 
									mStorageDir + "/" +
									StoragePath.SAVE_DIR;
        mSavedFileFragment = GridFileListFragment.newInstance(path);
        
        ft.add(R.id.fragment_container, mRecordFileFragment, RECORD_FILE_TAB);
		ft.commit();
		mActionBar.setTitle(R.string.action_bar_title_1);
		setActionBarFileCount(mStorageDir + "/" + StoragePath.CYCLE_DIR);
				
        receiver = new StatusReceiver();  
        IntentFilter filter = new IntentFilter();  
        filter.addAction(RecordService.REFRESH_ACTION);
        filter.addAction(RecordService.STATUS_ACTION);
        registerReceiver(receiver, filter);  
        
        BitmapCache.initBitmapCache(this);
   	}

	@Override
    protected void onSaveInstanceState(Bundle outState) {
      
    }
	
	@Override 
	public void onSystemUiVisibilityChange(int visibility) {
		if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) 
			closePreview();
	}
	
	@Override 
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch (position) {
		case 1:
			{
				mActionBar.setTitle(R.string.action_bar_title_1);
				setActionBarFileCount(mStorageDir + "/" + StoragePath.CYCLE_DIR);
			
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, mRecordFileFragment, RECORD_FILE_TAB);
				ft.commit();
			}	
			break;
		
		case 2:
			{
				mActionBar.setTitle(R.string.action_bar_title_2);
				setActionBarFileCount(mStorageDir + "/" + StoragePath.SAVE_DIR);
			
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, mSavedFileFragment, SAVED_FILE_TAB);
				ft.commit();
			}	
			break;
		
		case 4:
			break;
			
		case 5:
			DialogFragment newFragment = 
			AlertDialogFragment.newInstance(R.string.alert_dialog_title);
			newFragment.show(getFragmentManager(), "dialog");
			break;
			
		case 6:
			mContainer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			
			Intent intent = new Intent(MainActivity.this, RecordService.class);
    		String action = RecordService.PREVIEW_ACTION; 
    		intent.setAction(action);
    		startService(intent);
			break;
			
		default: break;	
		}
	}
	
	@Override
    public void	onClick(View v) {
		saveFile = !saveFile;
        	
		Intent intent = new Intent(MainActivity.this, RecordService.class);
		String action = saveFile ? 
        				RecordService.SAVE_FILE_ACTION : 
        				RecordService.UNSAVE_FILE_ACTION; 
		intent.setAction(action);
		startService(intent);
	}	
	 
	@Override
    protected void onResume() {                                                                                                                                                                                                                                                      
        super.onResume();
        
        Intent intent = new Intent(MainActivity.this, RecordService.class);
        intent.setAction(RecordService.CHECK_STATUS_ACTION);
        startService(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	unregisterReceiver(receiver);  
    }
 
    private int getFileCount(String path) {
    	ArrayList<FileInfo> fileInfo = RecordFileList.getFilelist(path, ".mp4");
    	if (fileInfo != null)
    		return fileInfo.size();
    	
    	return 0;
    }
    
    private void setActionBarFileCount(String path) {
    	int count = getFileCount(path);
		mActionBar.setSubtitle(String.valueOf(count) + "¸öÎÄ¼þ");
    }
    
    private void closePreview() {
    	Intent intent = new Intent(MainActivity.this, RecordService.class);
        String action = RecordService.CLOSE_PREVIEW_ACTION; 
        intent.setAction(action);
        startService(intent);
    }
    
    private class StatusReceiver extends BroadcastReceiver {  
        @Override  
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	
        	if (action.equals(RecordService.REFRESH_ACTION)) {
        		if (mRecordFileFragment.isVisible()) {
        			mRecordFileFragment.refreshFileList();
        			setActionBarFileCount(mStorageDir + "/" + StoragePath.CYCLE_DIR);
        		}	
        		
        		if (mSavedFileFragment.isVisible()) {
        			mSavedFileFragment.refreshFileList();
        			setActionBarFileCount(mStorageDir + "/" + StoragePath.SAVE_DIR);
        		}	
        	} else if (action.equals(RecordService.STATUS_ACTION)) {
        		int s = intent.getExtras().getInt("status");
        		if (s != 0) {
           			saveFile = true;
           			Switch saveSwitch = (Switch)findViewById(R.id.save_switch);
           			saveSwitch.setChecked(saveFile);
        		}	
        	}
        }  
    }  
    
    private void deleteSavedFiles() {
    	new Thread() {
    		public void run() {
    			try {
    				ArrayList<RecordFileList.FileInfo> fileList = 
    							RecordFileList.getFilelist(mStorageDir + 
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
				if (mSavedFileFragment.isVisible()) 
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
}
