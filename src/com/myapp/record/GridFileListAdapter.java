package com.myapp.record;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;


public class GridFileListAdapter extends BaseAdapter 
								implements AbsListView.OnScrollListener {
	private ArrayList<RecordFileList.FileInfo> mFileList = 
						new ArrayList<RecordFileList.FileInfo>();
	private ArrayList<String> mSelectedFiles = new ArrayList<String>();
	private LayoutInflater mInflater;
	private GridView mGridView;
	private int mFirstItem, mItemCount;
	private ArrayList<LoadThumbnail> mLoaderList = new ArrayList<LoadThumbnail>();
	private boolean  mIsScroll = false;
	private int mContainerWidth;

	public GridFileListAdapter(Context context, GridView gridView) {
		mGridView = gridView;
		mGridView.setOnScrollListener(this);

		mInflater = LayoutInflater.from(context);

		View v = ((Activity)context).findViewById(R.id.fragment_container);
		ViewGroup.LayoutParams params = v.getLayoutParams();
		mContainerWidth = params.width;
	}

	private void notifyChange() {
		if (mFirstItem == 0) 
			notifyDataSetChanged();
	}
	
	public void addAll(ArrayList<RecordFileList.FileInfo> list) {
		mFileList.addAll(list);
		notifyChange();
	}

	public void add(RecordFileList.FileInfo fileInfo) {
		mFileList.add(0, fileInfo);
		notifyChange();
	}

	public void remove(int position) {
		if (position >= mFileList.size())
			return;

		mFileList.remove(position);
		notifyChange();
	}
	
	public void clear() {
		mFileList.clear();
		notifyChange();
	}

	public void setSelectState(int position, boolean select) {
		RecordFileList.FileInfo fileInfo = mFileList.get(position);
		boolean exist = false;
		int index;
		
		for (index = 0; index < mSelectedFiles.size(); index++) {
			String s = mSelectedFiles.get(index);
			if (s.equals(fileInfo.path)) { 
				exist = true;
				break;
			}	
		}	
		
		if (select) {
			if (exist) return;
			mSelectedFiles.add(fileInfo.path);
		} else {
			if (exist) 
				mSelectedFiles.remove(index);
		}
		
		notifyDataSetChanged();
	}
	
	public void resetSelectState() {
		mSelectedFiles.clear();
		notifyDataSetChanged();
	}
	
	public String[] getSelectedFiles() {
		return (String[])mSelectedFiles.toArray(new String[0]);
	}
	
	@Override
	public int getCount() {
		return mFileList.size();
	}

	@Override
	public Object getItem(int position) {
		return mFileList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	private String createFileInfomation(RecordFileList.FileInfo fileInfo) {
		String prefix;
		
		String name[] = fileInfo.name.split("\\.");
		if (name == null || name.length != 2)
			return null;

		String seg[] = name[0].split("-");
		if (seg == null || seg.length != 3)
			return null;
		if (seg[1].length() != 8 || seg[2].length() != 6)
			return null;
		
		Calendar currentDate = Calendar.getInstance();
		int currentDay = currentDate.get(Calendar.DAY_OF_YEAR);
		
		Calendar fileDate = Calendar.getInstance();
		fileDate.setTimeInMillis(fileInfo.lastModified);
		int fileDay = fileDate.get(Calendar.DAY_OF_YEAR);
		
		int d = currentDay - fileDay;
		if (d == 0) 
			prefix = "今天";
		else if (d == 1)
			prefix = "昨天";
		else if (d <= 6) 
			prefix = d + "天前";
		else {
			if (seg[1].substring(4, 5).equals("0")) 
				prefix = seg[1].substring(5, 6) + "月";
			else
				prefix = seg[1].substring(4, 6) + "月";
			
			prefix += seg[1].substring(6, 8) + "日";
		}	
				
		return prefix + "    " + 
				seg[2].substring(0, 2) + ":" + 
				seg[2].substring(2, 4) + ":" +
				seg[2].substring(4, 6);  
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.grid_item_layout, null);
			int viewWidth = (mContainerWidth-5*3)/4; 
			convertView.setLayoutParams(new GridView.LayoutParams(viewWidth, viewWidth));
            
			holder = new ViewHolder();
			holder.text = (TextView)convertView.findViewById(R.id.text);
			holder.image = (ImageView)convertView.findViewById(R.id.icon);
			holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
			holder.selectMark = (FrameLayout)convertView.findViewById(R.id.select_mark);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		RecordFileList.FileInfo info = mFileList.get(position);
		holder.text.setText(createFileInfomation(info));
		
		boolean selected = false;
		for (String s : mSelectedFiles) {
			if (s.equals(info.path))
				selected = true;
		}
		
		holder.selectMark.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);

		final Bitmap bitmap = BitmapCache.getBitmapFromMemoryCache(info.name);
		if (bitmap != null) {
			holder.image.setVisibility(View.VISIBLE);
			holder.image.setImageBitmap(bitmap);
		} else {
			holder.image.setVisibility(View.INVISIBLE);
			if (mIsScroll == false) 
				createSingleLoaderTask(position);
		}

		return convertView;
	}

	private class ViewHolder {
		TextView text;
		ImageView image;
		FrameLayout selectMark;
	}
	
	private class LoadThumbnail extends AsyncTask<RecordFileList.FileInfo, Void, Bitmap> {
		private int mPosition;
		private RecordFileList.FileInfo mInfo;

		public LoadThumbnail(int pos) {
			mPosition = pos;
		}

		@Override
		protected Bitmap doInBackground(RecordFileList.FileInfo ... fileInfo) {
			mInfo = fileInfo[0];
			
			Bitmap bitmap;
			
			bitmap = BitmapCache.getBitmapFromDiskCache(fileInfo[0].name);
			if (bitmap == null) {
				Bitmap p = ThumbnailUtils.
							createVideoThumbnail(fileInfo[0].path, 
										MediaStore.Video.Thumbnails.MINI_KIND);
				if (p != null)
					bitmap = Bitmap.createScaledBitmap(p, p.getWidth()/2, p.getHeight()/2, false);
			}
			
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result != null) {
				BitmapCache.addBitmapToCache(mInfo.name, result);

				if (mPosition >= mFirstItem && mPosition < mFirstItem + mItemCount) {
					ImageView image = getImageView(mPosition);
					if (image != null) {
						image.setVisibility(View.VISIBLE);
						image.setImageBitmap(result);
					}	
				}
			}
		}
		
		@Override
		protected void onCancelled(Bitmap result)  {
			onPostExecute(result);
		}
	}

	private ImageView getImageView(int position) {
		if (position < mFirstItem || position >= mFirstItem + mItemCount)
			return null;

		View view = mGridView.getChildAt(position - mFirstItem);
		ViewHolder holder = (ViewHolder)view.getTag();
		return holder.image;
	}

	private void createSingleLoaderTask(int position) {
		LoadThumbnail loader = new LoadThumbnail(position);
		RecordFileList.FileInfo info = mFileList.get(position);
		loader.execute(info);
		mLoaderList.add(loader);
	}

	private void createLoaderTask(int firstItem, int itemCount) {
		for (int i = 0; i < itemCount; i++) {
			RecordFileList.FileInfo info = mFileList.get(firstItem+i);

			if (BitmapCache.getBitmapFromMemoryCache(info.name) == null) { 
				LoadThumbnail loader = new LoadThumbnail(firstItem+i);
				loader.execute(info);
				mLoaderList.add(loader);
			}	
		}
	}

	private void deleteLoaderTask() {
		if (mLoaderList.size() > 0) {
			for (int i = 0; i < mLoaderList.size(); i++) {
				LoadThumbnail loader = mLoaderList.get(i);
				loader.cancel(false);
				mLoaderList.remove(i);
			}	
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		//Log.i(TAG, "first: " + firstVisibleItem + " " + "count: " + visibleItemCount
		//		+ " " + "total count: " + totalItemCount);

		mFirstItem = firstVisibleItem;
		mItemCount = visibleItemCount; 
	}

	@Override 
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch (scrollState) {
		case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
			//Log.i(TAG, "GridView fling");
			mIsScroll = true;
			break;

		case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
			//Log.i(TAG, "GridView idle");
			mIsScroll = false;
			createLoaderTask(mFirstItem, mItemCount);
			break;

		case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
			//Log.i(TAG, "GridView touch scroll");
			mIsScroll = true;
			deleteLoaderTask();
			break;
		}
	}
}
