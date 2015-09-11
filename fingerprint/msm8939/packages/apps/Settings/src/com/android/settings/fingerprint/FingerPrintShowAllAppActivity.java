package com.android.settings.fingerprint;


import android.app.Activity;
import android.app.ActionBar;
import android.content.Context;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.ButtonBarHandler;
import com.android.settings.R;

import org.apache.http.entity.SerializableEntity;

import java.util.List;

public class FingerPrintShowAllAppActivity extends Activity implements 
    CompoundButton.OnCheckedChangeListener,OnItemClickListener{

    private String fingerPrintName;
    private String mPackageName;

    private Switch mSwitch;
    private ListView fingprintSelectAppList;
    private FingerprintAllAppAdapter mAdapter;
    private PackageManager pManager;
    private static List<ResolveInfo> apps;
    public static final String ASSOCIATED_STATUS = "associatedStatus";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingprint_show_all_app);

        Intent intent = getIntent();
        fingerPrintName = intent.getStringExtra(FingerPrintAssociatedAppActivity.FINGERPRINT_NAME);
        Log.d("VIM","[FingerPrintShowAllAppActivity] onCreate fingerPrintName:" + fingerPrintName);
        setTitle(fingerPrintName); 
        mSwitch = (Switch) findViewById(R.id.startApp_switchWidget);
        mSwitch.setOnCheckedChangeListener(this);

        fingprintSelectAppList = (ListView) findViewById(R.id.fingprint_all_app_list);

        pManager = this.getPackageManager();
        mAdapter = new FingerprintAllAppAdapter();

        fingprintSelectAppList.setOnItemClickListener(this);

        ActionBar actionbar = getActionBar();
        if(actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true); 
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        /*
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        apps = pManager.queryIntentActivities(mainIntent, 0);
        apps = filterAllApplication(apps);
        */
        fingprintSelectAppList.setAdapter(mAdapter);

        boolean state = getAssociatedSwitchStatus();
        mSwitch.setChecked(state);
    }

    private void updateAppList() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        apps = pManager.queryIntentActivities(mainIntent, 0);
        apps = filterAllApplication(apps);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        // TODO Auto-generated method stub
        CheckedTextView appLabelcheck = (CheckedTextView) arg1.findViewById(R.id.app_label);

        appLabelcheck.setChecked(true);
        String activityNames = apps.get(arg2).activityInfo.name;
        String packageNames = apps.get(arg2).activityInfo.packageName;
        String appLabel = apps.get(arg2).loadLabel(pManager).toString();

        savePackageInfoToDababase(fingerPrintName, appLabel, packageNames, activityNames);  

        finish();
    }

    public void onCheckedChanged(CompoundButton paramCompoundButton, boolean isChecked) {
        Log.d("VIM", "[FingerPrintShowAllAppActivity onCheckedChanged] " + "isChecked=" + isChecked);
        mSwitch.setChecked(isChecked);
        setAssociatedSwitchStatus(isChecked);
	 //add by wangkai 20150806 for bugzilla[73763] start
	 int state = isChecked ? 1:0;
	 Settings.System.putInt(getContentResolver(), "associatedSwitchState", state);
	 //end by wangkai
        if(isChecked) {
            updateAppList();
        }else{
            apps = null;
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void savePackageInfoToDababase(String fingerprint, String appLabel, 
                String packageName, String ActivityName) {

        ContentValues values =  new ContentValues();
        //values.put(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_LABEL, appLabel);
        values.put(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_PACKAGE, packageName);
        values.put(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_ACTIVITY, ActivityName);
        FingerPrintUtils.updateFingerPrintByName(FingerPrintShowAllAppActivity.this, fingerprint, values);
    }

    private boolean getAssociatedSwitchStatus(){
        boolean associatedState = FingerPrintUtils.getAssociateApplicationStatus(FingerPrintShowAllAppActivity.this, fingerPrintName);
        return associatedState;
    }

    private void setAssociatedSwitchStatus(boolean state){
        ContentValues values =  new ContentValues();
        values.put(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_STATUS, state? 1 : 0);
        FingerPrintUtils.updateFingerPrintByName(FingerPrintShowAllAppActivity.this, fingerPrintName, values);
    }

    private List<ResolveInfo> filterAllApplication(List<ResolveInfo> applist) {
        Cursor cursor = getContentResolver().query(
        FingerPrintProvider.FINGER_PRINT_URI,
        FingerPrintDatabaseHelper.FINGER_PRINT_PROJECTION, null, null, null);

        if (cursor == null) {
            Log.d("VIM", "[filterAllApplication] cursor is null! just return");
            cursor.close();
            return applist;
        }

        Log.d("VIM", "[filterAllApplication] cursor counts " + cursor.getCount());
        if (cursor.getCount() == 0) {
            Log.d("VIM", "[filterAllApplication] cursor has no any data! just return");
            cursor.close();
            return applist;
        }

        try {
            while (cursor.moveToNext()) {
                String fpName = cursor.getString(cursor.getColumnIndex(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_NAME));
                String packageName = cursor.getString(cursor.getColumnIndex(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_PACKAGE));
                if(!fpName.equals(fingerPrintName)) {
                    for(int i=0; i<applist.size(); i++) {
                        if(packageName.equals(applist.get(i).activityInfo.packageName)) {
                            applist.remove(i);
                        }
                    }
                }else {
                     mPackageName = packageName;
                }
            }
        } catch (Exception ex) {
            Log.e("VIM", "[filterAllApplication] exception! " + ex);
        } finally {
            cursor.close();
        }
        return applist;
    }

    public class FingerprintAllAppAdapter extends BaseAdapter{
        
        private final LayoutInflater mLayoutInflater;
        public FingerprintAllAppAdapter() {
            // TODO Auto-generated constructor stub
            mLayoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            if(apps == null) {
                return 0;
            }
            return apps.size();
        }

        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            CharSequence appLabel = null;
            if(apps == null || apps.isEmpty()){
                return null;
            }
            
            try {
                appLabel = apps.get(arg0).loadLabel(pManager);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                appLabel = apps.get(arg0).resolvePackageName;
                Log.i("VIM","getItem packageName:" + appLabel);
            }
            
            return appLabel;
        }

        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return arg0;
        }

        @Override
        public View getView(int arg0, View arg1, ViewGroup arg2) {
            // TODO Auto-generated method stub
            if(apps == null || apps.isEmpty()){
                return null;
            }
            
            View view = mLayoutInflater.inflate(R.layout.listview_app_item_view, arg2, false);
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            CheckedTextView appLabel = (CheckedTextView) view.findViewById(R.id.app_label);

            icon.setImageDrawable(apps.get(arg0).loadIcon(pManager));
            appLabel.setText(apps.get(arg0).loadLabel(pManager));
            appLabel.setChecked(false);
            if (mPackageName != null && mPackageName.equals(apps.get(arg0).activityInfo.packageName)) {
                appLabel.setChecked(true);
            }
            
            return view;
        }
    }
}
