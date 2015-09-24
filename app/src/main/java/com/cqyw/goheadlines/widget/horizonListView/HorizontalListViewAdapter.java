package com.cqyw.goheadlines.widget.horizonListView;

/**
 * Created by Kairong on 2015/6/3.
 * mail:wangkrhust@gmail.com
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.cqyw.goheadlines.R;

public class HorizontalListViewAdapter extends BaseAdapter {
    private int[] iconIds;
    private String[] title;
    private Context mContext;
    private LayoutInflater mInflater;
    Bitmap iconBitmap = null;
    Bitmap faceBitmap = null;
    private int selectIndex = -1;

    public HorizontalListViewAdapter(Context context, int[] iconIds, String[] title){
        this.mContext = context;
        this.iconIds = iconIds;
        this.title = title;
        mInflater=(LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);//LayoutInflater.from(mContext);
    }
    @Override
    public int getCount() {
        return iconIds.length;
    }
    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if(convertView==null){
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.horizontal_list_item, null);
            holder.mImage=(ImageView)convertView.findViewById(R.id.img_list_item);
            holder.mTitle=(TextView)convertView.findViewById(R.id.txt_list_item);
            convertView.setTag(holder);
        }else{
            holder=(ViewHolder)convertView.getTag();
        }
        if(position == selectIndex){
            convertView.setSelected(true);
        }else{
            convertView.setSelected(false);
        }
        holder.mImage.setImageResource(iconIds[position]);
        holder.mTitle.setText(title[position]);

        return convertView;
    }

    private static class ViewHolder {
        private ImageView mImage;
        private TextView  mTitle;
    }
//    private Bitmap getPropThumnail(int id){
//        Drawable d = mContext.getResources().getDrawable(id);
//        Bitmap b = BitmapUtil.drawableToBitmap(d);
//        int w = mContext.getResources().getDimensionPixelOffset(R.dimen.thumnail_default_width);
//        int h = mContext.getResources().getDimensionPixelSize(R.dimen.thumnail_default_height);
//
//        return ThumbnailUtils.extractThumbnail(b, w, h, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
//    }
//    public void setSelectIndex(int i){
//        selectIndex = i;
//    }
}
