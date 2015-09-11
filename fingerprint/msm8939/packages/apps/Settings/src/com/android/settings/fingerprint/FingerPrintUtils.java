package com.android.settings.fingerprint;

import android.content.ContentValues;
import android.content.Context;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.content.ContentResolver;
import android.app.admin.DevicePolicyManager;

import com.android.internal.widget.LockPatternUtils;

public class FingerPrintUtils {
    
    public static int getFingerCount(Context context) {
        Cursor cursor = context.getContentResolver().query(
                FingerPrintProvider.FINGER_PRINT_URI,
                FingerPrintDatabaseHelper.FINGER_PRINT_PROJECTION, null, null, null);

        if (cursor == null) {
            return 0;
        }

        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public static Uri insertFingerPrint(Context context, ContentValues values) {
        return context.getContentResolver().insert(
                FingerPrintProvider.FINGER_PRINT_URI, values);
    }
    
    public static void deleteFingerPrintByIndex(Context context, int index) {
        String selection = FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_INDEX + "=?";
        String[] selectionArgs = new String[] {
                String.valueOf(index)
        };

        context.getContentResolver().delete(FingerPrintProvider.FINGER_PRINT_URI, selection, selectionArgs);
    }

    public static void updateFingerPrintByIndex(Context context, int index, ContentValues values) {
        String selection = FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_INDEX + "=?";
        String[] selectionArgs = new String[] {
                String.valueOf(index)
        };

        context.getContentResolver().update(
                FingerPrintProvider.FINGER_PRINT_URI, values, selection, selectionArgs);
    }

    public static void updateFingerPrintByName(Context context, String name, ContentValues values) {
        String selection = FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_NAME + "=?";
        String[] selectionArgs = new String[] {
                name
        };

        context.getContentResolver().update(
                FingerPrintProvider.FINGER_PRINT_URI, values, selection, selectionArgs);
    }

    public static int getIntDataByKey(ContentResolver cr, String key, int defaultValue) {
        return Settings.System.getInt(cr, key, defaultValue);
    }

    public static void putIntDataByKey(ContentResolver cr, String key, int Value) {
        Settings.System.putInt(cr, key, Value);
    }

    public static boolean isPasswordQualityNone(LockPatternUtils lockPatternUtils) {
        return (lockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
    }

    public static boolean getAssociateApplicationStatus(Context context, String fpName) {
	  boolean associatedStatus = false;
        String selection = FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_NAME + "=?";
        String[] selectionArgs = new String[] {
                fpName
        };
		
        Cursor cursor = context.getContentResolver().query(
                FingerPrintProvider.FINGER_PRINT_URI,
                FingerPrintDatabaseHelper.FINGER_PRINT_PROJECTION, selection, selectionArgs, null);

        if (cursor == null) {
	      cursor.close();
            return associatedStatus;
        }

        if (cursor.getCount() == 0) {
            cursor.close();
            return associatedStatus;
        }

        try {
            while (cursor.moveToNext()) {
		   int state = cursor.getInt(cursor.getColumnIndex(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_STATUS));
		   associatedStatus = state == 1? true : false;
            }
        } catch (Exception ex) {
            Log.e("VIM", "[getAssociateApplicationStatus] exception! " + ex);
        } finally {
            cursor.close();
        }

	  return associatedStatus;
    }

    public static  String getAppLabel(PackageManager pM, String pkgName, String clsName) {
     	  String label;
     	  ComponentName comName = new ComponentName(pkgName, clsName);
     	  try {
      	      ActivityInfo af = pM.getActivityInfo(comName, 0);
     		label = af.loadLabel(pM).toString();
      	  } catch (NameNotFoundException e) {
      	       // TODO Auto-generated catch block
      	       e.printStackTrace();
     		 label = pkgName;
     		 Log.e("VIM","getAppLabel NameNotFoundException");
      	  }
     	  return label.toString();
    }
	
}
