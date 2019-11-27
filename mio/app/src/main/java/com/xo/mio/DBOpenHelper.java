package com.xo.mio;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBOpenHelper extends SQLiteOpenHelper {
    final String TAG = "DBOpenHelper";

    final String CREATE_PERSONINFO_TAB =
            "create table tb_personinfo (_id integer primary key autoincrement, " +
                    "name text, sex text, number integer)";

    public DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PERSONINFO_TAB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "version upate" + oldVersion + " -->>" + newVersion);
    }
}
