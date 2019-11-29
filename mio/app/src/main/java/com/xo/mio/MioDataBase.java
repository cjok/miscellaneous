package com.xo.mio;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MioDataBase {
    public final static String TABLE_IN = "tb_in";
    public final static String TABLE_OUT = "tb_out";

    private DBOpenHelper dbOpenHelper;
    private SQLiteDatabase db;

    private Context mContext;


    public MioDataBase(DBOpenHelper dbOpenHelper, Context mContext) {
        this.dbOpenHelper = dbOpenHelper;
        this.mContext = mContext;
        db = dbOpenHelper.getWritableDatabase();

    }

    public void Insert(String tableName, DataType dataType) {

        ContentValues values = new ContentValues();
        values.put(DataType.TYPE_ID, (Integer) null);  //auto increase
        values.put(DataType.TYPE_CATEGORY, dataType.getCategory());
        values.put(DataType.TYPE_DATA, dataType.getData());
        values.put(DataType.TYPE_NOTE, dataType.getNote());
        values.put(DataType.TYPE_DATE, dataType.getDate());

        db.insert(tableName, null, values);

        Toast.makeText(mContext, dataType.toString(), Toast.LENGTH_SHORT).show();
    }


    public void DeleteById(String tableName, int id) {
        db.delete(tableName, "id = ?", new String[]{String.valueOf(id)});
        Toast.makeText(mContext, "DeleteById: " + id, Toast.LENGTH_SHORT).show();
    }

    public void UpdateById(String tableName, DataType dataType) {
        ContentValues values = new ContentValues();
        values.put(DataType.TYPE_ID, dataType.getId());
        values.put(DataType.TYPE_CATEGORY, dataType.getCategory());
        values.put(DataType.TYPE_DATA, dataType.getData());
        values.put(DataType.TYPE_NOTE, dataType.getNote());
        values.put(DataType.TYPE_DATE, dataType.getDate());

        db.update(tableName, values, "id = ?",
                new String[]{String.valueOf(dataType.getId())});

        Toast.makeText(mContext, dataType.toString(), Toast.LENGTH_SHORT).show();

    }

    public LinkedList<DataType> QueryAllToLinkedList(String tableName) {
        Cursor cursor = db.query(tableName, null,null, null,
                null, null, null);
        LinkedList<DataType> listdatas = new LinkedList<DataType>();

        if (cursor.moveToFirst()) {
            do {
                DataType data = new DataType();
                data.setId(cursor.getInt(cursor.getColumnIndex(DataType.TYPE_ID)));
                data.setCategory(cursor.getString(cursor.getColumnIndex(DataType.TYPE_CATEGORY)));
                data.setData(cursor.getInt(cursor.getColumnIndex(DataType.TYPE_DATA)));
                data.setNote(cursor.getString(cursor.getColumnIndex(DataType.TYPE_NOTE)));
                data.setDate(cursor.getString(cursor.getColumnIndex(DataType.TYPE_DATE)));

                listdatas.add(data);
            }while (cursor.moveToNext());

            return listdatas;
        }

        return listdatas;
    }


    public List<DataType> QueryAll(String tableName) {
        Cursor cursor = db.query(tableName, null,null, null,
                null, null, null);
        List<DataType> listdatas = new ArrayList<DataType>();

        if (cursor.moveToFirst()) {
            do {
                DataType data = new DataType();
                data.setId(cursor.getInt(cursor.getColumnIndex(DataType.TYPE_ID)));
                data.setCategory(cursor.getString(cursor.getColumnIndex(DataType.TYPE_CATEGORY)));
                data.setData(cursor.getInt(cursor.getColumnIndex(DataType.TYPE_DATA)));
                data.setNote(cursor.getString(cursor.getColumnIndex(DataType.TYPE_NOTE)));
                data.setDate(cursor.getString(cursor.getColumnIndex(DataType.TYPE_DATE)));

                listdatas.add(data);
            }while (cursor.moveToNext());

            return listdatas;
        }

        return null;
    }


    public List<Map<String, Object>> ConvertToListMap(List<DataType> listdatas) {

        List<Map<String, Object>> listItem = new ArrayList<Map<String, Object>>();

        for (DataType data : listdatas) {
            Map<String, Object> item = new HashMap<>();
            item.put(DataType.TYPE_ID, data.getId());
            item.put(DataType.TYPE_CATEGORY, data.getCategory());
            item.put(DataType.TYPE_DATA, data.getData());
            item.put(DataType.TYPE_NOTE, data.getNote());
            item.put(DataType.TYPE_DATE, data.getDate());

            listItem.add(item);
        }

        return listItem;
    }


}
