package com.android.settings.fingerprint;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class FingerPrintProvider extends ContentProvider {
    private static final String TAG = "FingerPrintProvider";
    private static final String AUTHORITY = "com.android.settings.finger_print";
    public static final String ORDER_BY_ID_ASC = FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_ID
            + " ASC";
    public static final String ORDER_BY_ID_DESC = FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_ID
            + " DESC";
    public static final String ORDER_BY_INDEX_ASC = FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_INDEX
            + " ASC";
    public static final String ORDER_BY_INDEX_DESC = FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_INDEX
            + " DESC";
    public static final Uri FINGER_PRINT_URI = Uri.withAppendedPath(
            Uri.parse("content://" + AUTHORITY), FingerPrintDatabaseHelper.TABLE_FINGER_PRINT);

    private SQLiteOpenHelper mOpenHelper;
    private static final UriMatcher S_URI_MATCHER = new UriMatcher(
            UriMatcher.NO_MATCH);

    public static final int FINGER_PRINT_ALL = 0;
    public static final int FINGER_PRINT_ID = 1;

    static {
        S_URI_MATCHER.addURI(AUTHORITY, FingerPrintDatabaseHelper.TABLE_FINGER_PRINT, FINGER_PRINT_ALL);
        S_URI_MATCHER.addURI(AUTHORITY, FingerPrintDatabaseHelper.TABLE_FINGER_PRINT + "/#",
                FINGER_PRINT_ID);
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        mOpenHelper = FingerPrintDatabaseHelper.getInstance(this.getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        // TODO Auto-generated method stub
        logd("[query] uri : " + uri);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT);
        int match = S_URI_MATCHER.match(uri);

        switch (match) {
        case FINGER_PRINT_ALL:
        case FINGER_PRINT_ID:
            sortOrder = TextUtils.isEmpty(sortOrder) ? ORDER_BY_INDEX_ASC
                    : sortOrder;
            break;

        default:
            throw new IllegalArgumentException("Unknow uri : " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null,
                null, sortOrder);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        int match = S_URI_MATCHER.match(uri);

        String table = FingerPrintDatabaseHelper.TABLE_FINGER_PRINT;
        switch (match) {
        case FINGER_PRINT_ALL:
            table = FingerPrintDatabaseHelper.TABLE_FINGER_PRINT;
            break;

        default:
            throw new IllegalArgumentException("Unknow uri : " + uri);
        }

        if (values == null) {
            throw new IllegalArgumentException("values can not be null!");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(table, null, values);
        if (rowId > 0) {
            Uri insertedUri = ContentUris.withAppendedId(uri, rowId);
            this.getContext().getContentResolver().notifyChange(uri, null);
            logd("[insert] data inserted : " + insertedUri);
            return insertedUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = S_URI_MATCHER.match(uri);
        int count = 0;

        switch (match) {
        case FINGER_PRINT_ALL:
            count = db.delete(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT, selection,
                    selectionArgs);
            break;

        case FINGER_PRINT_ID:
            String dataId = uri.getPathSegments().get(0);
            count = db.delete(
                    FingerPrintDatabaseHelper.TABLE_FINGER_PRINT,
                    FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_ID
                            + "="
                            + dataId
                            + (TextUtils.isEmpty(selection) ? "" : (" AND("
                                    + selection + ")")), selectionArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknow uri : " + uri);
        }

        this.getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        // TODO Auto-generated method stub
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = S_URI_MATCHER.match(uri);
        int count = 0;

        switch (match) {
        case FINGER_PRINT_ALL:
            count = db.update(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT, values, selection,
                    selectionArgs);
            break;

        case FINGER_PRINT_ID:
            String dataId = uri.getPathSegments().get(1);
            count = db.update(
                    FingerPrintDatabaseHelper.TABLE_FINGER_PRINT,
                    values,
                    FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_ID
                            + "="
                            + dataId
                            + (TextUtils.isEmpty(selection) ? "" : (" AND("
                                    + selection + ")")), selectionArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknow uri : " + uri);
        }

        this.getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private static void logd(String strs) {
        Log.d(TAG, strs);
    }
}
