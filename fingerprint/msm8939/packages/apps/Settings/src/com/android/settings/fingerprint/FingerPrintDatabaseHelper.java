package com.android.settings.fingerprint;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FingerPrintDatabaseHelper extends SQLiteOpenHelper {
    static final String DATABASE_NAME = "finger_print.db";
    static final int DATABASE_VERSION = 1;

    public static final int FP_RENAME_NOT_YET = 0;
    public static final int FP_RENAME = 1;

    public static final String TABLE_FINGER_PRINT = "table_finger_print";
    public static final String TABLE_FINGER_PRINT_ID = "_id";
    public static final String TABLE_FINGER_PRINT_INDEX = "fp_index";
    public static final String TABLE_FINGER_PRINT_NAME = "fp_name";
    public static final String TABLE_FINGER_PRINT_RENAME = "fp_rename";
    public static final String TABLE_FINGER_PRINT_STATUS = "fp_associated_status";
    //public static final String TABLE_FINGER_PRINT_LABEL = "fp_associated_application_label";
    public static final String TABLE_FINGER_PRINT_PACKAGE = "fp_associated_package_name";
    public static final String TABLE_FINGER_PRINT_ACTIVITY = "fp_associated_activity_name";

    public static final String[] FINGER_PRINT_PROJECTION = new String[] {
            TABLE_FINGER_PRINT_ID,
            TABLE_FINGER_PRINT_INDEX, 
            TABLE_FINGER_PRINT_NAME,
            TABLE_FINGER_PRINT_RENAME,
            TABLE_FINGER_PRINT_STATUS,
            //TABLE_FINGER_PRINT_LABEL,
            TABLE_FINGER_PRINT_PACKAGE,
            TABLE_FINGER_PRINT_ACTIVITY
    };

    private static FingerPrintDatabaseHelper sInstance;
    private Context mContext;

    private FingerPrintDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    /* package */static synchronized FingerPrintDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FingerPrintDatabaseHelper(context);
        }

        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL("CREATE TABLE " + TABLE_FINGER_PRINT + " ("
                + TABLE_FINGER_PRINT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TABLE_FINGER_PRINT_INDEX + " INTEGER,"
                + TABLE_FINGER_PRINT_NAME + " TEXT,"
                + TABLE_FINGER_PRINT_RENAME + " INTEGER DEFAULT 0,"
                + TABLE_FINGER_PRINT_STATUS + " INTEGER DEFAULT 0,"
                //+ TABLE_FINGER_PRINT_LABEL + " TEXT,"
                + TABLE_FINGER_PRINT_PACKAGE + " TEXT,"
                + TABLE_FINGER_PRINT_ACTIVITY + " TEXT"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub

    }
}
