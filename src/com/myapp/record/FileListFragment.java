package com.myapp.record;

import java.io.File;
import java.util.ArrayList;

import android.app.ListFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class FileListFragment extends ListFragment {
	private ArrayAdapter<String> mAdapter = null;
	private String mFileDir;
			
	public static FileListFragment newInstance(String fileDir) {
		FileListFragment f = new FileListFragment();
        Bundle args = new Bundle();
        args.putString("dir", fileDir);
        f.setArguments(args);

        return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mFileDir = getArguments() != null ? 
					getArguments().getString("dir") : 
					null;
					
		mAdapter = new ArrayAdapter<String>(getActivity(),
 						android.R.layout.simple_list_item_1);	
		
		setListAdapter(mAdapter);
	}
	
 	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		refreshFileList();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		ArrayList<RecordFileList.FileInfo> recFileList = 
				RecordFileList.getFilelist(mFileDir, ".mp4");
		RecordFileList.FileInfo fi = recFileList.get(position);
		
		Intent intent = new Intent(Intent.ACTION_VIEW);
		File file = new File(Environment.getExternalStorageDirectory() + 
								"/" + mFileDir + "/" + fi.name);
		intent.setDataAndType(Uri.fromFile(file), "video/*");
		startActivity(intent);
	}
     
	public void refreshFileList() {
		ArrayList<RecordFileList.FileInfo> fileList = 
				RecordFileList.getFilelist(mFileDir, ".mp4");
		
		mAdapter.clear();
		for (int i = 0; i < fileList.size(); i++) 
			mAdapter.add(fileList.get(i).title);
	}
}
