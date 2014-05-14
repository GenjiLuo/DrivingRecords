package com.myapp.record;

import java.io.File;
import java.util.ArrayList;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;


public class GridFileListFragment extends Fragment 
					implements AdapterView.OnItemClickListener {
	private GridView mGrid = null;
	private GridFileListAdapter mAdapter = null;
	private String mFileDir;
			
	public static GridFileListFragment newInstance(String fileDir) {
		GridFileListFragment f = new GridFileListFragment();
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
		
		mAdapter = new GridFileListAdapter(getActivity(), mGrid);
		ArrayList<RecordFileList.FileInfo> list = RecordFileList.getFilelist(mFileDir, ".mp4");
		if (list != null)
			mAdapter.addAll(list);
		
		mGrid.setAdapter(mAdapter);
		
		TextView emptyView = (TextView)getActivity().findViewById(R.id.empty_view);
		emptyView.setText("√ª”– ”∆µ");
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
		mAdapter.clear();
		
		ArrayList<RecordFileList.FileInfo> fileList = 
				RecordFileList.getFilelist(mFileDir, ".mp4");
		if (fileList == null)
			return;
				
		mAdapter.addAll(fileList);
	}
}
