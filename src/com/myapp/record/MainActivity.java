package com.myapp.record;

import java.io.File;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
	private static final String ARCHIVE_FILE_TAB = "archive";
	
	private GridFileListFragment mRecordFileFragment = null, 
								mSavedFileFragment = null,
								mArchiveFileFragment = null;
	private StatusReceiver receiver;  
	private boolean saveFile = false;
	private LinearLayout mContainer;
	private ListView mMenuItemList;
	private MenuItemAdapter mMenuItemAdapter;
	private ActionBar mActionBar;
	private boolean isRecording;
	
	public String mStorageDir;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
					
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
        mRecordFileFragment = GridFileListFragment.newInstance(path, R.menu.cycle_list_menu);
        
        path = mStorageDir + "/" + StoragePath.SAVE_DIR;
        mSavedFileFragment = GridFileListFragment.newInstance(path, R.menu.saved_list_menu);
        
        path = mStorageDir + "/" + StoragePath.ARCHIVE_DIR;
        File dir = new File(path);
        if (!dir.exists())
        	dir.mkdirs();
        
        mArchiveFileFragment = GridFileListFragment.newInstance(path, R.menu.archive_list_menu);
        
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

	private boolean isRecordServiceRunning() {  
    	ActivityManager manager = (ActivityManager)((Context)this).getSystemService(Context.ACTIVITY_SERVICE);
    	
    	for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {  
        	if (RecordService.class.getName().equals(service.service.getClassName()))   
                return true;  
        }  
    	
        return false;  
    } 
	
	private void setItemState(boolean state) {
		String title = state ? 
				getResources().getString(R.string.menu_item_stop_record) : 
				getResources().getString(R.string.menu_item_start_record);
		Bitmap icon =  state ? 
				BitmapFactory.decodeResource(getResources(), R.drawable.ic_stop) :
				BitmapFactory.decodeResource(getResources(), R.drawable.ic_record);	
				
		mMenuItemAdapter.setItemTitle(MenuItemAdapter.MenuItemContent.MENU_ITEM_CTRL_RECORD.ordinal() , title);
		mMenuItemAdapter.setItemIcon(MenuItemAdapter.MenuItemContent.MENU_ITEM_CTRL_RECORD.ordinal() , icon);
		
		mMenuItemAdapter.enableItem(MenuItemAdapter.MenuItemContent.MENU_ITEM_TEMP_SAVE.ordinal(), state);
		mMenuItemAdapter.enableItem(MenuItemAdapter.MenuItemContent.MENU_ITEM_PREVIEW.ordinal(), state);
	}
	
	@Override
    protected void onSaveInstanceState(Bundle outState) { 
      
    }
	
	@Override 
	public void onSystemUiVisibilityChange(int visibility) {
		if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0 && isRecording) {
			closePreview();
			//checkStatus();
		}	
	}
	
	@Override 
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (MenuItemAdapter.MenuItemContent.MENU_ITEM_CTRL_RECORD.ordinal() == position) {
			isRecording = !isRecording;
			
			Intent intent = new Intent(MainActivity.this, RecordService.class);
			if (isRecording) 
				startService(intent);
			else 
				stopService(intent);
			
			setItemState(isRecording);
		} else if (MenuItemAdapter.MenuItemContent.MENU_ITEM_PREVIEW.ordinal() == position) {
			mContainer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			
			Intent intent = new Intent(MainActivity.this, RecordService.class);
    		String action = RecordService.PREVIEW_ACTION; 
    		intent.setAction(action);
    		startService(intent);
		} else if (MenuItemAdapter.MenuItemContent.MENU_ITEM_TEMP_SAVE.ordinal() == position) {
			
		} else if (MenuItemAdapter.MenuItemContent.MENU_ITEM_LIST_CYCLE_RECORD.ordinal() == position) {
			mMenuItemAdapter.setItemHightlight(MenuItemAdapter.MenuItemContent.MENU_ITEM_LIST_CYCLE_RECORD.ordinal(), true);
			mMenuItemAdapter.setItemHightlight(MenuItemAdapter.MenuItemContent.MENU_ITEM_LIST_TEMP_SAVE_RECORD.ordinal(), false);
			mMenuItemAdapter.setItemHightlight(MenuItemAdapter.MenuItemContent.MENU_ITEM_LIST_ARCHIVE_RECORD.ordinal(), false);
			
			mActionBar.setTitle(R.string.action_bar_title_1);
			setActionBarFileCount(mStorageDir + "/" + StoragePath.CYCLE_DIR);
		
			mSavedFileFragment.closeActionMode();
			mArchiveFileFragment.closeActionMode();
			
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.fragment_container, mRecordFileFragment, RECORD_FILE_TAB);
			ft.commit();
		} else if (MenuItemAdapter.MenuItemContent.MENU_ITEM_LIST_TEMP_SAVE_RECORD.ordinal() == position) {
			mMenuItemAdapter.setItemHightlight(MenuItemAdapter.MenuItemContent.MENU_ITEM_LIST_CYCLE_RECORD.ordinal(), false);
			mMenuItemAdapter.setItemHightlight(MenuItemAdapter.MenuItemContent.MENU_ITEM_LIST_TEMP_SAVE_RECORD.ordinal(), true);
			mMenuItemAdapter.setItemHightlight(MenuItemAdapter.MenuItemContent.MENU_ITEM_LIST_ARCHIVE_RECORD.ordinal(), false);
			
			mActionBar.setTitle(R.string.action_bar_title_2);
			setActionBarFileCount(mStorageDir + "/" + StoragePath.SAVE_DIR);
		
			mRecordFileFragment.closeActionMode();
			mArchiveFileFragment.closeActionMode();
			
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.fragment_container, mSavedFileFragment, SAVED_FILE_TAB);
			ft.commit();
		} else if (MenuItemAdapter.MenuItemContent.MENU_ITEM_LIST_ARCHIVE_RECORD.ordinal() == position) {
			mMenuItemAdapter.setItemHightlight(MenuItemAdapter.MenuItemContent.MENU_ITEM_LIST_CYCLE_RECORD.ordinal(), false);
			mMenuItemAdapter.setItemHightlight(MenuItemAdapter.MenuItemContent.MENU_ITEM_LIST_TEMP_SAVE_RECORD.ordinal(), false);
			mMenuItemAdapter.setItemHightlight(MenuItemAdapter.MenuItemContent.MENU_ITEM_LIST_ARCHIVE_RECORD.ordinal(), true);
			
			mActionBar.setTitle(R.string.action_bar_title_3);
			setActionBarFileCount(mStorageDir + "/" + StoragePath.ARCHIVE_DIR);
			
			mRecordFileFragment.closeActionMode();
			mSavedFileFragment.closeActionMode();
			
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.fragment_container, mArchiveFileFragment, ARCHIVE_FILE_TAB);
			ft.commit();
			
		} else if (MenuItemAdapter.MenuItemContent.MENU_ITEM_OPTION.ordinal() == position) {
			Intent intent = new Intent(this, PreferencesActivity.class);
			startActivity(intent);
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
        
        isRecording = isRecordServiceRunning();
        setItemState(isRecording);
        
        if (isRecording) 
        	checkStatus();
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
    
    public void setActionBarFileCount(String path) {
    	int count = getFileCount(path);
		mActionBar.setSubtitle(String.valueOf(count) + "¸öÎÄ¼þ");
    }
    
    private void closePreview() {
    	Intent intent = new Intent(MainActivity.this, RecordService.class);
        String action = RecordService.CLOSE_PREVIEW_ACTION; 
        intent.setAction(action);
        startService(intent);
    }
    
    private void checkStatus() {
    	Intent intent = new Intent(MainActivity.this, RecordService.class);
    	intent.setAction(RecordService.CHECK_STATUS_ACTION);
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
        		if (saveSwitch != null) {
        			if (saveSwitch.isEnabled()) {
        				saveFile = intent.getExtras().getBoolean("save_status");
        				saveSwitch.setChecked(saveFile);
        			}	
        		}	
        	} 
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
