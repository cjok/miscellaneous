package com.xo.mio;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBOpenHelper extends SQLiteOpenHelper {
    final String TAG = "DBOpenHelper";
    public final String TABLE_IN = "tb_in";
    public final String TABLE_OUT = "tb_out";

    final String CREATE_IN_TAB =
            "create table " + TABLE_IN + "(id integer primary key autoincrement, " +
                    "category text, data integer, note text, date text)";

    final String CREATE_OUT_TAB =
            "create table " + TABLE_IN + "(id integer primary key autoincrement, " +
                    "category text, data integer, note text, date text)";

    public DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_IN_TAB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "version upate" + oldVersion + " -->>" + newVersion);
    }
}
