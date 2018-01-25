package com.wonhigh.opus.belleopus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.wonhigh.opus.opuslib.OpusTrackInfo;

import java.util.List;
import java.util.Map;

/**
 * 描述： TODO
 * 作者： gong.xl
 * 邮箱： gong.xl@belle.com.cn
 * 创建时间： 2018/1/25 17:28
 * 修改时间： 2018/1/25 17:28
 * 修改备注：
 */

public class ListViewAdapter extends BaseAdapter {

    Context mContext;

    List<Map<String, Object>> mList;


    public ListViewAdapter(Context mContext, List<Map<String, Object>> mList) {
        this.mContext = mContext;
        this.mList = mList;
    }

    @Override
    public int getCount() {
        return mList == null ? 0 : mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_layout,parent,false);
            holder = new ViewHolder();
            holder.textView = convertView.findViewById(R.id.name);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.textView.setText((String) mList.get(position).get(OpusTrackInfo.TITLE_TITLE));

        return convertView;
    }

    class ViewHolder{
        TextView textView;
    }
}
