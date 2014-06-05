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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
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
				
		mContainer = (LinearLayout)findViewById(R.id.container);
		mContainer.setOnSystemUiVisibilityChangeListener(this);

		mMenuItemAdapter = new MenuItemAdapter(this);
		mMenuItemList = (ListView)findViewById(R.id.menu_item_list);
		mMenuItemList.setAdapter(mMenuItemAdapter);
		mMenuItemList.setOnItemClickListener(this);
		
		mActionBar = getActionBar();
		mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, 0);
      
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        
        String path = mStorageDir + "/" + StoragePath.CYCLE_DIR;
        mRecordFileFragment = GridFileListFragment.newInstance(path);
        
        path = mStorageDir + "/" + StoragePath.SAVE_DIR;
        mSavedFileFragment = GridFileListFragment.newInstance(path);
        
        ft.add(R.id.fragment_container, mRecordFileFragment, RECORD_FILE_TAB);
		ft.commit();
		mActionBar.setTitle(R.string.action_bar_title_1);
		setActionBarFileCount(mStorageDir + "/" + StoragePath.CYCLE_DIR);
				
        receiver = new StatusReceiver();  
        IntentFilter filter = new IntentFilter();  
        filter.addAction(RecordService.REFRESH_ACTION);
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
		case 0:
			{
				mActionBar.setTitle(R.string.action_bar_title_1);
				setActionBarFileCount(mStorageDir + "/" + StoragePath.CYCLE_DIR);
			
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, mRecordFileFragment, RECORD_FILE_TAB);
				ft.commit();
			}	
			break;
		
		case 1:
			{
				mActionBar.setTitle(R.string.action_bar_title_2);
				setActionBarFileCount(mStorageDir + "/" + StoragePath.SAVE_DIR);
			
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, mSavedFileFragment, SAVED_FILE_TAB);
				ft.commit();
			}	
			break;
		
		case 3:
			break;
			
		case 4:
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
        		
        		Switch saveSwitch = (Switch)findViewById(R.id.save_switch);
        		saveFile = intent.getExtras().getBoolean("save_status");
        		saveSwitch.setChecked(saveFile);
        	} 
        }  
    }  
    
    private class DeleteFilesTask extends AsyncTask<String, Void, Boolean> {
       	@Override
		protected Boolean doInBackground(String ... path) {
       		File dir = new File(path[0]);
       		File[] files = dir.listFiles();
       		if (files != null) {
       			for (File f : files) {
       				if (f.isFile())
       					f.delete();
       			}
       		}
       		
       		return true;
       	}
    	
       	@Override
		protected void onPostExecute(Boolean result) {
       		pd.dismiss();
       		
       		new Handler().postDelayed(new Runnable() {   
				public void run() {   
					if (mSavedFileFragment.isVisible()) {
	        			mSavedFileFragment.refreshFileList();
	        			setActionBarFileCount(mStorageDir + "/" + StoragePath.SAVE_DIR);
					}	
				}   
			}, 500);   
       	}
    }
    
    public void doPositiveClick() {
		pd= ProgressDialog.show(this, 
								getString(R.string.dialog_title_2), 
								getString(R.string.dialog_msg_2));

		new DeleteFilesTask().execute(mStorageDir + "/" + StoragePath.SAVE_DIR);
    }
    
    public void doNegativeClick() {

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
	
	public static class BootBroadcastReceiver extends BroadcastReceiver {
		private static final String action_boot="android.intent.action.BOOT_COMPLETED"; 
		 
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(action_boot)) { 
				Intent i = new Intent(context, RecordService.class);
				context.startService(i);
			}
		}
	}
}
