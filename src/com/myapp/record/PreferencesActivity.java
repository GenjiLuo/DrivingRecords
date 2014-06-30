package com.myapp.record;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.WindowManager;


public class PreferencesActivity extends Activity {
	//private static final String SWITCH_PREF_KEY = "switch_pref";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
		
		getActionBar().setTitle(R.string.pref_title);
		
		// Display the fragment as the main content.
		getFragmentManager().beginTransaction().replace(android.R.id.content,
				 	new PrefsFragment()).commit();
	}

	public static class PrefsFragment extends PreferenceFragment 
										implements OnSharedPreferenceChangeListener {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.preferences);
		}
		
		@Override
		public void onResume() {
			super.onResume();
			
			getPreferenceScreen().getSharedPreferences()
            			.registerOnSharedPreferenceChangeListener(this);
		}
		
		@Override
		public void onPause() {
			super.onPause();
			
			getPreferenceScreen().getSharedPreferences()
            			.unregisterOnSharedPreferenceChangeListener(this);
		}
		
		@Override 
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			//if (SWITCH_PREF_KEY.equals(key)) {
			//	if (isRecordServiceRunning()) {
			//		Intent intent = new Intent(getActivity(), RecordService.class);
			//		getActivity().stopService(intent);
			//		getActivity().startService(intent);
			//	}
			//}	
		}
		
		//private boolean isRecordServiceRunning() {  
	    //	ActivityManager manager = (ActivityManager)((Context)getActivity()).getSystemService(Context.ACTIVITY_SERVICE);
	    	
	    //	for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {  
	    //    	if (RecordService.class.getName().equals(service.service.getClassName()))   
	    //            return true;  
	    //    }  
	    	
	    //    return false;  
	    //} 
	}
}
