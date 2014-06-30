package com.myapp.record;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;


public class GridFileListFragment extends Fragment 
					implements AdapterView.OnItemClickListener {
	private GridView mGrid = null;
	private GridFileListAdapter mAdapter = null;
	private String mFileDir = null;
	private int menuRes;
	private ActionMode mActionMode = null;
	private boolean inActionMode = false;
	
	public static GridFileListFragment newInstance(String fileDir, int menuResource) {
		GridFileListFragment f = new GridFileListFragment();
        Bundle args = new Bundle();
        args.putString("dir", fileDir);
        args.putInt("menu", menuResource);
        f.setArguments(args);

        return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		if (args != null) {
			mFileDir = args.getString("dir");
			menuRes = args.getInt("menu");
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container,
				false);
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
			
		mGrid = (GridView)getActivity().findViewById(R.id.grid_view);
		mGrid.setOnItemClickListener(this);
		mGrid.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		mGrid.setMultiChoiceModeListener(new ModeCallback());
		
		mAdapter = new GridFileListAdapter(getActivity(), mGrid);
		ArrayList<RecordFileList.FileInfo> list = RecordFileList.getFilelist(mFileDir, ".mp4");
		if (list != null)
			mAdapter.addAll(list);
		
		mGrid.setAdapter(mAdapter);
		
		TextView emptyView = (TextView)getActivity().findViewById(R.id.empty_view);
		emptyView.setText("没有视频");
		emptyView.setVisibility(View.GONE);  
		mGrid.setEmptyView(emptyView);  
		
		if (savedInstanceState != null) 
			mGrid.smoothScrollToPosition(savedInstanceState.getInt("pos", 0));
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	    outState.putInt("pos", mGrid.getFirstVisiblePosition());
	}
	
 	@Override
 	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ArrayList<RecordFileList.FileInfo> recFileList = 
				RecordFileList.getFilelist(mFileDir, ".mp4");
		RecordFileList.FileInfo fi = recFileList.get(position);
		
		Intent intent = new Intent(Intent.ACTION_VIEW);
		File file = new File(mFileDir + "/" + fi.name);
		intent.setDataAndType(Uri.fromFile(file), "video/*");
		startActivity(intent);
	}
 	
 	public void refreshFileList() {
 		if (inActionMode)
 			return;
 		
		mAdapter.clear();
		
		ArrayList<RecordFileList.FileInfo> fileList = 
				RecordFileList.getFilelist(mFileDir, ".mp4");
		if (fileList == null)
			return;
				
		mAdapter.addAll(fileList);
	}
 	
 	public void closeActionMode() {
 		if (mActionMode != null) {
 			mActionMode.finish();
 			mActionMode = null;
 		}
 	}
 	
 	private class ModeCallback implements AbsListView.MultiChoiceModeListener {
 		@Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
 			mActionMode = mode;
 			inActionMode = true;
 			MenuInflater inflater = getActivity().getMenuInflater();
 			inflater.inflate(menuRes, menu);
 			return true;
        }
 		
 		@Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

 		@Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
 			switch (item.getItemId()) {
 			case R.id.archive:
 				String fileList[] = mAdapter.getSelectedFiles();
 				new MoveFilesTask().execute(fileList);
 				break;
 				
 			case R.id.select_all:
 				int count = mAdapter.getCount();
 				
 				for (int i = 0; i < count; i++) {
 					mGrid.setItemChecked(i, true);
 					mAdapter.setSelectState(i, true); 
 				}
 				
 				break;
 				
 			case R.id.discard:
 				AlertDialogFragment dialog = AlertDialogFragment.
 									newInstance(R.string.alert_dialog_title,
 												getTag());
 				dialog.show(getFragmentManager(), "dialog");
 				break;
 				
 			default: ;	
 			}
 			
            return true;
        }

 		@Override
        public void onDestroyActionMode(ActionMode mode) {
 			inActionMode = false;
        	mAdapter.resetSelectState();
        	refreshFileList();
        	((MainActivity)getActivity()).setActionBarFileCount(mFileDir);        	
        }

 		@Override
        public void onItemCheckedStateChanged(ActionMode mode,
                				int position, long id, boolean checked) {
 			mAdapter.setSelectState(position, checked); 
 			setTitle(mode);
        }
 		
 		private void setTitle(ActionMode mode) {
            final int checkedCount = mGrid.getCheckedItemCount();
            String title = "选中了" + checkedCount + "项";
            mode.setTitle(title);
        }
    }
 	
	private class MoveFilesTask extends AsyncTask<String[], Void, Boolean> {
		private ProgressDialog mProgress;
 		private String mDstPath;
 		
 		@Override 
 		protected void onPreExecute () {
 			mProgress = ProgressDialog.show(getActivity(), 
 											getString(R.string.progress_dialog_title_1), 
 											getString(R.string.progress_dialog_msg_1));
 			
 			mDstPath = ((MainActivity)getActivity()).mStorageDir + "/" + StoragePath.ARCHIVE_DIR;
 		}
 		
 		@Override
		protected Boolean doInBackground(String[] ... files) {
 			for (String path : files[0]) {
 				String filename = path.substring(path.lastIndexOf("/"));
 				File f = new File(path);
 				f.renameTo(new File(mDstPath + "/" + filename));
 			}
 			
 			try {
 				Thread.sleep(500);
 			} catch (InterruptedException e) {
 				
 			}
 			
 			return true;
 		}
 		
 		@Override
		protected void onPostExecute(Boolean result) {
 			mProgress.dismiss();
 			closeActionMode();
 		}
	}
 	 
 	public static class AlertDialogFragment extends DialogFragment {
 		public static AlertDialogFragment newInstance(int title, String tag) {
			AlertDialogFragment frag = new AlertDialogFragment();
			Bundle args = new Bundle();
			args.putInt("title", title);
			args.putString("tag", tag);
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
									FragmentManager fm = getActivity().getFragmentManager();
									GridFileListFragment f = (GridFileListFragment)fm.
														findFragmentByTag(getArguments().getString("tag"));
									f.deleteFiles();
								}
							})
						.setNegativeButton(R.string.alert_dialog_cancel,
							new DialogInterface.OnClickListener() {
	                       		public void onClick(DialogInterface dialog, int whichButton) {
	          
	                       		}
	                    	})
	                    .create();
		}
	}
 	
 	private class DeleteFilesTask extends AsyncTask<String[], Void, Boolean> {
 		private ProgressDialog mProgress;
 		
 		@Override 
 		protected void onPreExecute () {
 			mProgress = ProgressDialog.show(getActivity(), 
											getString(R.string.progress_dialog_title_2), 
											getString(R.string.progress_dialog_msg_2));
 		}
 		
       	@Override
		protected Boolean doInBackground(String[] ... files) {
       		for (String path : files[0]) {
       			File f = new File(path);
       			if (f.isFile())
       				f.delete();
       		}
       		
       		try {
 				Thread.sleep(500);
 			} catch (InterruptedException e) {
 				
 			}
       		
       		return true;
       	}
    	
       	@Override
		protected void onPostExecute(Boolean result) {
       		mProgress.dismiss();
       		closeActionMode();
       	}
    }
 	
 	public void deleteFiles() {
 		String[] fileList = mAdapter.getSelectedFiles();
 		new DeleteFilesTask().execute(fileList);
 	}

}
