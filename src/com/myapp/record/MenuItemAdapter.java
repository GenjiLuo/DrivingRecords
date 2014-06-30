package com.myapp.record;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;


public class MenuItemAdapter extends BaseAdapter {
	private static final int ITEM_TYPE_NORMAL = 0;
	private static final int ITEM_TYPE_SWITCH = 1;
	private static final int ITEM_TYPE_SEPARATOR = 2;
	private static final int ITEM_TYPE_BOTTOM = 3;
		
	private LayoutInflater mInflater;
	private Context ctx;
	private MenuItemContent mMenuItemContent[]; 
	
	public MenuItemAdapter(Context context) {
		ctx = context;
		mInflater = LayoutInflater.from(context);
		loadMenuItemContent();
	}
	
	private void loadMenuItemContent() {
		Resources res = ctx.getResources(); 
		mMenuItemContent = MenuItemContent.values();
				
		for (MenuItemContent c : mMenuItemContent) {
			switch (c) {
			case MENU_ITEM_CTRL_RECORD:
				c.itemTitle = res.getString(R.string.menu_item_stop_record);
				c.itemIcon = BitmapFactory.decodeResource(res, R.drawable.ic_stop);
				c.itemType = ITEM_TYPE_NORMAL;
				c.hightLight = false;
				c.enable = true;
				break;
			case MENU_ITEM_PREVIEW:
				c.itemTitle = res.getString(R.string.menu_item_preview);
				c.itemIcon = BitmapFactory.decodeResource(res, R.drawable.ic_camera);
				c.itemType = ITEM_TYPE_NORMAL;
				c.hightLight = false;
				c.enable = true;
				break;
			case MENU_ITEM_TEMP_SAVE:
				c.itemTitle = res.getString(R.string.menu_item_save_record);
				c.itemIcon = BitmapFactory.decodeResource(res, R.drawable.ic_labels);
				c.itemType = ITEM_TYPE_SWITCH;
				c.enable = true;
				break;
			case MENU_ITEM_SEPARATOR_1:
				c.itemType = ITEM_TYPE_SEPARATOR;
				c.enable = false;
				break;
			case MENU_ITEM_LIST_CYCLE_RECORD:
				c.itemTitle = res.getString(R.string.menu_item_list_cycle);
				c.itemIcon = BitmapFactory.decodeResource(res, R.drawable.ic_storage);
				c.itemType = ITEM_TYPE_NORMAL;
				c.hightLight = true;
				c.enable = true;
				break;
			case MENU_ITEM_LIST_TEMP_SAVE_RECORD:
				c.itemTitle = res.getString(R.string.menu_item_list_save);
				c.itemIcon = BitmapFactory.decodeResource(res, R.drawable.ic_storage);
				c.itemType = ITEM_TYPE_NORMAL;
				c.hightLight = false;
				c.enable = true;
				break;
			case MENU_ITEM_LIST_ARCHIVE_RECORD:
				c.itemTitle = res.getString(R.string.menu_item_list_archive);
				c.itemIcon = BitmapFactory.decodeResource(res, R.drawable.ic_storage);
				c.itemType = ITEM_TYPE_NORMAL;
				c.hightLight = false;
				c.enable = true;
				break;
			case MENU_ITEM_SEPARATOR_2:
				c.itemType = ITEM_TYPE_SEPARATOR;
				c.enable = false;
				break;
			case MENU_ITEM_OPTION:
				c.itemTitle = res.getString(R.string.menu_item_settings);
				c.itemIcon = BitmapFactory.decodeResource(res, R.drawable.ic_settings);
				c.itemType = ITEM_TYPE_NORMAL;
				c.hightLight = false;
				c.enable = true;
				break;
			case MENU_ITEM_BOTTOM:
				c.itemType = ITEM_TYPE_BOTTOM;
				c.enable = false;
				break;
			}
		}
	}
	
	@Override
	public int getViewTypeCount() {
	    return 4;
	}

	@Override
	public int getItemViewType(int position) {
		return mMenuItemContent[position].itemType;
	}
	
	@Override
	public int getCount() {
		return mMenuItemContent.length;
	}

	@Override
	public Object getItem(int position) {
		return mMenuItemContent[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
    public boolean isEnabled(int position) {
		return mMenuItemContent[position].enable;
    }
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if (mMenuItemContent[position].itemType == ITEM_TYPE_SEPARATOR) {
			if (convertView == null)
				convertView = mInflater.inflate(R.layout.menu_item_separator, null);
		} else if (mMenuItemContent[position].itemType == ITEM_TYPE_BOTTOM) {
			if (convertView == null)
				convertView = mInflater.inflate(R.layout.menu_item_bottom, null);
		} else {
			if (position == MenuItemContent.MENU_ITEM_TEMP_SAVE.ordinal()) {
				if (convertView == null) {
					convertView = mInflater.inflate(R.layout.menu_item_save_layout, null);
					
					holder = new ViewHolder();
					holder.saveSwitch = (Switch)convertView.findViewById(R.id.save_switch);
					holder.saveSwitch.setOnClickListener((MainActivity)ctx);
					holder.text = (TextView)convertView.findViewById(R.id.menu_item_text);
					holder.image = (ImageView)convertView.findViewById(R.id.menu_item_image);
					
					convertView.setTag(holder);
				} else
					holder = (ViewHolder) convertView.getTag();
						
				if (mMenuItemContent[position].enable) {
					holder.saveSwitch.setEnabled(true);
				} else {
					holder.saveSwitch.setChecked(false);
					holder.saveSwitch.setEnabled(false);
				}
			} else	{
				if (convertView == null) {
					convertView = mInflater.inflate(R.layout.menu_item_layout, null);
					
					holder = new ViewHolder();
					holder.text = (TextView)convertView.findViewById(R.id.menu_item_text);
					holder.image = (ImageView)convertView.findViewById(R.id.menu_item_image);
					holder.hightlight = (FrameLayout)convertView.findViewById(R.id.hight_light_layout);
					
					convertView.setTag(holder);
				} else
					holder = (ViewHolder) convertView.getTag();
				
				if (mMenuItemContent[position].enable && mMenuItemContent[position].hightLight) 
					holder.hightlight.setVisibility(View.VISIBLE);
				else 
					holder.hightlight.setVisibility(View.INVISIBLE);
			}	
			
			holder.text.setText(mMenuItemContent[position].itemTitle);
			
			if (mMenuItemContent[position].enable) 
				holder.text.setTextColor(ctx.getResources().getColor(R.color.black));
			else
				holder.text.setTextColor(ctx.getResources().getColor(R.color.gray));
			
			holder.image.setImageBitmap(mMenuItemContent[position].itemIcon);
		}	
		
		return convertView;
	}
	
	public void setItemTitle(int pos, String title) {
		if (pos >= mMenuItemContent.length)
			return;
		
		if (mMenuItemContent[pos].itemType == ITEM_TYPE_SEPARATOR ||
				mMenuItemContent[pos].itemType == ITEM_TYPE_BOTTOM)
			return;
		
		mMenuItemContent[pos].itemTitle = title;
		notifyDataSetChanged();
	}
	
	public void setItemIcon(int pos, Bitmap icon) {
		if (pos >= mMenuItemContent.length)
			return;
		
		if (mMenuItemContent[pos].itemType == ITEM_TYPE_SEPARATOR ||
				mMenuItemContent[pos].itemType == ITEM_TYPE_BOTTOM)
			return;
		
		mMenuItemContent[pos].itemIcon = icon;
		notifyDataSetChanged();
	}
	
	public void enableItem(int pos, boolean enable) {
		if (pos >= mMenuItemContent.length)
			return;
		
		if (mMenuItemContent[pos].itemType == ITEM_TYPE_SEPARATOR ||
				mMenuItemContent[pos].itemType == ITEM_TYPE_BOTTOM)
			return;
		
		mMenuItemContent[pos].enable = enable;
		
		notifyDataSetChanged();
	}
	
	public void setItemHightlight(int pos, boolean hightlight) {
		if (pos >= mMenuItemContent.length)
			return;
		
		mMenuItemContent[pos].hightLight = hightlight;
		notifyDataSetChanged();
	}
	
	public enum MenuItemContent {
		MENU_ITEM_CTRL_RECORD,
		MENU_ITEM_PREVIEW,
		MENU_ITEM_TEMP_SAVE,
		MENU_ITEM_SEPARATOR_1,
		MENU_ITEM_LIST_CYCLE_RECORD,
		MENU_ITEM_LIST_TEMP_SAVE_RECORD,
		MENU_ITEM_LIST_ARCHIVE_RECORD,
		MENU_ITEM_SEPARATOR_2,
		MENU_ITEM_OPTION,
		MENU_ITEM_BOTTOM;
		
		public String itemTitle;
		public Bitmap itemIcon;
		public int itemType;
		public boolean hightLight;
		public boolean enable;
	}
	
	private class ViewHolder {
		TextView text;
		ImageView image;
		FrameLayout hightlight;
		Switch saveSwitch;
	}
}
