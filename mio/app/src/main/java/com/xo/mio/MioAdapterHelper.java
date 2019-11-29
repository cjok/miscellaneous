package com.xo.mio;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.LinkedList;

public class MioAdapterHelper extends BaseAdapter {

    private LinkedList<DataType> mData;
    private Context mContext;

    public MioAdapterHelper(LinkedList<DataType> mData, Context mContext) {
        this.mData = mData;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void reFresh(LinkedList<DataType> mData) {
        this.mData = mData;
    }

    public void addItem(DataType dataType) {
        if (dataType != null) {
            mData.add(dataType);
        }
        //notifyDataSetChanged();
    }


    public void updateItem(DataType dataType, int position) {
        if (dataType != null) {
            mData.remove(position);
            mData.add(position, dataType);
        }
        notifyDataSetChanged();
    }

    public void removeItem(DataType dataType) {
        if (dataType != null) {
            mData.remove(dataType);
        }
        notifyDataSetChanged();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item, null, false);

       // if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.list_item, null);
       // }

        TextView et_id = convertView.findViewById(R.id.item_id);
        TextView et_category = convertView.findViewById(R.id.item_category);
        TextView et_data = convertView.findViewById(R.id.item_data);
        TextView et_note = convertView.findViewById(R.id.item_note);
        TextView et_date = convertView.findViewById(R.id.item_date);



        ///////////////////////////////////////
        for (DataType data : mData) {
            Log.e("getViewEEEE",  data.toString());
        }
        ///////////////////////////////////////

        et_id.setText(""+mData.get(position).getId());
        et_category.setText(mData.get(position).getCategory());
        et_data.setText(""+mData.get(position).getData());
        et_note.setText(mData.get(position).getNote());
        et_date.setText(mData.get(position).getDate());

        return convertView;
    }
}
