package com.myapp.record;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;


public class MenuItemAdapter extends BaseAdapter {
	private final int mResId[] = {
		R.drawable.ic_repeat,
		R.drawable.ic_collections,
		0,
		R.drawable.ic_save, 
		R.drawable.ic_delete, 
		0,
		R.drawable.ic_camera
	};
	
	private String mMenuItem[];
	private LayoutInflater mInflater;
	private Bitmap[] mIcons;
	private Context ctx;
	 
	public MenuItemAdapter(Context context) {
		ctx = context;
		
		mInflater = LayoutInflater.from(context);
		
		mIcons = new Bitmap[mResId.length];
		
		for (int i = 0; i < mResId.length; i++) 
			mIcons[i] = BitmapFactory.decodeResource(context.getResources(), 
													mResId[i]);
		
		mMenuItem = context.getResources().getStringArray(R.array.menu_item_string);
	}
	
	@Override
	public int getCount() {
		return mMenuItem.length;
	}

	@Override
	public Object getItem(int position) {
		return mMenuItem[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
    public boolean isEnabled(int position) {
        return !mMenuItem[position].startsWith("-");
    }
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (mMenuItem[position].startsWith("-")) 
			convertView = mInflater.inflate(R.layout.menu_item_separator, null);
		else if (mMenuItem[position].startsWith("*"))
			convertView = mInflater.inflate(R.layout.menu_item_bottom, null);	
		else {
			if (mResId[position] == R.drawable.ic_save) {
				convertView = mInflater.inflate(R.layout.menu_item_save_layout, null);
				Switch saveSwitch = (Switch)convertView.findViewById(R.id.save_switch);
				saveSwitch.setOnClickListener((MainActivity)ctx);
			} else	
				convertView = mInflater.inflate(R.layout.menu_item_layout, null);
			
			TextView text = (TextView)convertView.findViewById(R.id.menu_item_text);
			text.setText(mMenuItem[position]);
			
			ImageView image = (ImageView)convertView.findViewById(R.id.menu_item_image);
			image.setImageBitmap(mIcons[position]);
		}	
		
		return convertView;
	}
	
	public static class ViewHolder {
		TextView text;
		ImageView image;
	}
}
