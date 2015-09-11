package com.android.settings.fingerprint;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Switch;
import com.android.settings.R;

import java.util.ArrayList;

public class FingerPrintAssociatedAppActivity extends Activity 
    implements CompoundButton.OnCheckedChangeListener, OnItemClickListener, OnClickListener{

    private Switch mSwitch;
    private ListView fingprintSelectList;
    private TextView fingerprintTitle;
    private TextView fingerprintTipMsg;
    private Button fingerprintAddNew;
    private static ArrayList<String> mFingerPrintNameArray; 
    private static ArrayList<String> mAssociateAppPackageArray;
    private static ArrayList<String> mAssociateAppClassArray;
    private static ArrayList<String> mAssociateStatusArray; 
    private FPlistViewAdapter mAdapter;
    private PackageManager pManager;
    private AlertDialog mRenameDlg; //wujiacheng add for bug71650 20150727
    private String FINGERPRINT_NAME_TMP; //wujiacheng add for bug71650 20150727

    public static final int REQUEST_CODE_APP = 1;
    public static final int REQUEST_CODE_NEW_FINGERPRINT = 2;
    public static final String FINGERPRINT_NAME = "fingerprintName";
    public static final String ASSOCIATED_ACTIVITY_NAME = "associatedActivityName";
    public static final String ASSOCIATED_PACKAGE_NAME = "associatedPackageName";
    public static final String ASSOCIATED_APP_LABEL = "associatedAppLabel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingprint_associate_app);
        setTitle(R.string.fingerprint_start_app);

        mFingerPrintNameArray =  new ArrayList<String>();
        mAssociateAppPackageArray = new ArrayList<String>();
        mAssociateAppClassArray = new ArrayList<String>();
        mAssociateStatusArray = new ArrayList<String>();

        mSwitch = (Switch) findViewById(R.id.hct_switchWidget);
        fingprintSelectList = (ListView) findViewById(R.id.fingprint_select_list);
        fingerprintTitle =  (TextView)findViewById(R.id.fingprint_title);
        fingerprintTipMsg = (TextView)findViewById(R.id.fingprint_tip_msg);
        fingerprintAddNew = (Button)findViewById(R.id.add_fingprint);

        pManager = this.getPackageManager();
        
        fingerprintAddNew.setOnClickListener(this);
        mSwitch.setOnCheckedChangeListener(this);
        mAdapter = new FPlistViewAdapter();
        
        fingprintSelectList.setOnItemClickListener(this);
        fingprintSelectList.setAdapter(mAdapter);

        int state = Settings.System.getInt(getContentResolver(), "associatedSwitchState", 0);
        boolean switchState = state == 1 ? true:false;
        mSwitch.setChecked(switchState);

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
	 //add by wangkai 20150806 for bugzilla[73763]
        mSwitch.setChecked(Settings.System.getInt(getContentResolver(), "associatedSwitchState",0) == 1);
        initSelectFingPrintList(mSwitch.isChecked());
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

    public void onCheckedChanged(CompoundButton paramCompoundButton, boolean isChecked) {
        Log.d("VIM", "[onCheckedChanged] " + "isChecked=" + isChecked);
        mSwitch.setChecked(isChecked);
        initSelectFingPrintList(isChecked);
        int state = isChecked ? 1:0;
        Settings.System.putInt(getContentResolver(), "associatedSwitchState", state);
    }

    private void initSelectFingPrintList(boolean switchState){
        Log.d("VIM", "[initSelectFingPrintList] " + "isChecked=" + switchState);
        mFingerPrintNameArray.clear();
        mAssociateAppPackageArray.clear();
        mAssociateAppClassArray.clear();
        mAssociateStatusArray.clear();

        if(switchState) {
            updateSelectFingerPrintList();
        }

        if(switchState) {
            fingerprintTipMsg.setVisibility(View.VISIBLE);
            if(!mFingerPrintNameArray.isEmpty()) {
                fingerprintTitle.setVisibility(View.VISIBLE);
                fingerprintTipMsg.setText(R.string.fingerprint_associated_app_tip);
                fingerprintAddNew.setVisibility(View.GONE);
            } else{
                fingerprintTitle.setVisibility(View.GONE);
                fingerprintTipMsg.setText(R.string.no_fingerprint_msg);
                fingerprintAddNew.setVisibility(View.VISIBLE);
            }
        }else {
            fingerprintTitle.setVisibility(View.GONE);
            fingerprintTipMsg.setVisibility(View.GONE);
            fingerprintAddNew.setVisibility(View.GONE);
        }

        mAdapter.notifyDataSetChanged();
    }

    private void updateSelectFingerPrintList() {
        Cursor cursor = getContentResolver().query(
        FingerPrintProvider.FINGER_PRINT_URI,
        FingerPrintDatabaseHelper.FINGER_PRINT_PROJECTION, null, null, null);

        if (cursor == null) {
            Log.d("VIM", "[updateSelectFingerPrintList] cursor is null! just return");
            cursor.close();
            return;
        }

        Log.d("VIM", "[updateSelectFingerPrintList] cursor counts " + cursor.getCount());
        if (cursor.getCount() == 0) {
            Log.d("VIM", "[initFingerPrintList] cursor has no any data! just return");
            cursor.close();
            return;
        }

        try {
            while (cursor.moveToNext()) {
                String fpName = cursor.getString(cursor.getColumnIndex(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_NAME));
                String pkgName = cursor.getString(cursor.getColumnIndex(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_PACKAGE));
                String clsName = cursor.getString(cursor.getColumnIndex(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_ACTIVITY));
                int state = cursor.getInt(cursor.getColumnIndex(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_STATUS));

                mFingerPrintNameArray.add(fpName); 
                mAssociateAppPackageArray.add(pkgName);
                mAssociateAppClassArray.add(clsName);
                mAssociateStatusArray.add(String.valueOf(state));
            }
        } catch (Exception ex) {
            Log.e("VIM", "[initFingerPrintList] exception! " + ex);
        } finally {
            cursor.close();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        // TODO Auto-generated method stub
        TextView fingprintNameTV = (TextView)arg1.findViewById(R.id.fingerprintName);
        String fingprintName = fingprintNameTV.getText().toString();
        Log.d("VIM", "[onItemClick] fingprintName:" + fingprintName);

        Intent intent = new Intent(FingerPrintAssociatedAppActivity.this, FingerPrintShowAllAppActivity.class);
        intent.putExtra(FINGERPRINT_NAME, fingprintName);
        
        startActivity(intent);
    }

    @Override
    public void onClick(View arg0) {
        // TODO Auto-generated method stub
        addNewFingerprint();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FingerPrintSettings.ENROLL_RESULT) {
            if (data != null) {
                int index = data.getIntExtra(FingerPrintSettings.ENROLL_RESULT_INDEX, -1);
                String fpName = data.getStringExtra(FingerPrintSettings.ENROLL_RESULT_NAME);
                showFPReNameDialog(FingerPrintSettings.KEY_FINGERPRINT_PRE + index, fpName); //wujiacheng add for bug71650 20150727 
            }
        }
    }

    /*wujiacheng add for bug71650 20150727 start*/
    private void showFPReNameDialog(final String key, String fpName) {
        FINGERPRINT_NAME_TMP = fpName; 

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final EditText et = new EditText(this);
        et.setText(fpName);
        et.setSelection(et.length());

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String name = et.getText().toString();

                if (TextUtils.isEmpty(name)) {
                    showErrorInfo(R.string.fingerprint_name_empty_title, R.string.fingerprint_name_empty_msg);
                    mRenameDlg.dismiss();
                    mRenameDlg = null;
                    return;
                }

                if (!FINGERPRINT_NAME_TMP.equals(name) && isExistName(name)) { 
                    showErrorInfo(R.string.fingerprint_name_exist_title, R.string.fingerprint_name_exist_msg);
                    mRenameDlg.dismiss();
                    mRenameDlg = null;
                    return;
                }

                renameByKey(key, name);

                initSelectFingPrintList(mSwitch.isChecked());

                mRenameDlg.dismiss();
                mRenameDlg = null;
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mRenameDlg.dismiss();
                mRenameDlg = null;
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                mRenameDlg = null;
            }
        });

        builder.setView(et);
        mRenameDlg = builder.create();
        Window window = mRenameDlg.getWindow();  
        window.setGravity(Gravity.BOTTOM);
        mRenameDlg.show();
    }

    private boolean isExistName(String name) {
        boolean result = false;

        String trimName = name.trim();
        if (mFingerPrintNameArray.contains(trimName)) {
            result = true;
        }

        return result;
    }

    private void renameByIndex(int index, String fpName) {
        if (index == -1) {
            return;
        }

        ContentValues values =  new ContentValues();
        values.put(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_NAME, fpName);
        values.put(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_RENAME, FingerPrintDatabaseHelper.FP_RENAME);

        FingerPrintUtils.updateFingerPrintByIndex(this, index, values);
        Toast.makeText(this, R.string.fingerprint_rename_finish, Toast.LENGTH_SHORT).show();
    }

    private int getIndexByKey(String key) {
        int index = -1;

        String s = String.valueOf(key.charAt(key.length() - 1));
        index = Integer.parseInt(s);

        return index;
    }

    private void renameByKey(String key, String fpName) {
        int index = getIndexByKey(key);

        renameByIndex(index, fpName);
    }
    private void showErrorInfo(int titleId, int msgId) {
        new AlertDialog.Builder(this)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setTitle(titleId)
            .setMessage(msgId)
            .setPositiveButton(this.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // do nothing
                }
            })
            .create()
            .show();
    }
    /*wujiacheng add for bug71650 20150727 end*/

    private void addNewFingerprint() {
        int fingerCount = FingerPrintUtils.getFingerCount(this);
        if(fingerCount >= FingerPrintSettings.MAX_FINGERPRINT_COUNT) {
            Toast.makeText(this, R.string.fingerprint_reach_limit, Toast.LENGTH_SHORT).show();
             return;
        }

        Intent intent = new Intent();
        intent.setClass(this, com.android.settings.Settings.FingerPrintEnrollActivity.class);
        this.startActivityForResult(intent, FingerPrintSettings.ENROLL_RESULT);
    }
  
    public class FPlistViewAdapter extends BaseAdapter {
        private final LayoutInflater mLayoutInflater;

        public FPlistViewAdapter() {
            // TODO Auto-generated constructor stub
            mLayoutInflater = (LayoutInflater) FingerPrintAssociatedAppActivity.this.getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
        }
        
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            if(mFingerPrintNameArray.isEmpty()){
                return 0;
            }
            else{
                return mFingerPrintNameArray.size();
            }
        }

        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            if(mFingerPrintNameArray.isEmpty()) {
                return null;
            }
            else {
                return mFingerPrintNameArray.get(arg0);
            }
        }

        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            if(mFingerPrintNameArray.isEmpty()){
                return 0;
            }
            else{
                return arg0;
            }
        }

        @Override
        public View getView(int arg0, View arg1, ViewGroup arg2) {
            // TODO Auto-generated method stub
            if(mFingerPrintNameArray.isEmpty()) {
                return null;
            }
            else {
                final View view = mLayoutInflater.inflate(R.layout.listview_fingerprint_item_view, arg2, false);

                TextView fingprintName = (TextView)view.findViewById(R.id.fingerprintName);
                TextView packageName = (TextView)view.findViewById(R.id.packageName);
                String fpName = mFingerPrintNameArray.get(arg0);
                fingprintName.setText(fpName);

                boolean associatedState = FingerPrintUtils.getAssociateApplicationStatus(FingerPrintAssociatedAppActivity.this, fpName);
                if(associatedState && (mAssociateAppPackageArray.get(arg0) != null)) {
                    packageName.setVisibility(View.VISIBLE);

                    String appLabel = FingerPrintUtils.getAppLabel(pManager,  mAssociateAppPackageArray.get(arg0),  mAssociateAppClassArray.get(arg0));
                    packageName.setText(appLabel);
                }else {
                    packageName.setVisibility(View.GONE);
                }

                return view;
            }
        }
        
    }

}
