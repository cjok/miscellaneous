package com.android.settings.fingerprint;

import android.R.integer;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.widget.Toast;
import android.view.Gravity;
import android.app.AlertDialog;
import android.widget.Button;
import android.content.DialogInterface;
import android.view.View;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ListView;
import android.text.TextUtils;
import android.content.ContentValues;
import android.widget.EditText;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.preference.PreferenceActivity;
import android.app.ActionBar;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.HashMap;

import com.android.internal.widget.LockPatternUtils;

import com.android.settings.ChooseLockPassword;
import com.android.settings.ChooseLockPattern;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.Utils;

import egistec.fingerauth.api.FPAuthListeners;
import egistec.fingerauth.api.FPAuthListeners.EnrollListener;
import egistec.fingerauth.api.FPAuthListeners.EnrollMapProgressListener;
import egistec.fingerauth.api.FPAuthListeners.StatusListener;
import egistec.fingerauth.api.SettingLib;
import com.android.settings.fingerprint.AutoInterrpt.GetTHDCValueListener;

import com.hipad.fingerprint.IFingerPrintUtils;
import com.hipad.fingerprint.FingerPrintListener;

public class FingerPrintSettings extends PreferenceActivity
                implements FingerprintSwitchPreference.SwitchChange {
    private static final String TAG = "FingerPrintSettings";
    private static final String TAG_FP = "VIM";

    private static final String CONFIRM_CREDENTIALS = "confirm_credentials";

    public static final int MAX_FINGERPRINT_COUNT = 5;
    public static final int UNLOCK_FEATURE_CLOSE = 0;
    public static final int UNLOCK_FEATURE_OPEN = 1;
    public static final String FP_PRE = "user";

    public static final String KEY_FINGERPRINT_UNLOCK_KEYGUARD = "fingerprint_unlock_keyguard";
    public static final String KEY_FINGERPRINT_COLD_UNLOCK_KEYGUARD = "fingerprint_cold_unlock_keyguard";
    private static final String KEY_FINGERPRINT_LIST = "fingerprint_list";
    private static final String KEY_FINGERPRINT_FUNCTION = "fingerprint_application";
    private static final String KEY_FINGERPRINT_NEW = "fingerprint_new";
    private static final String KEY_RE_CALIBRATION = "re_calibration";
    public static final String KEY_FINGERPRINT_PRE = "fingerprint_key_";
    private static final String KEY_FINGERPRINT_UNLOCK_START_APP = "fingerprint_unlock_start_app";
    private String FINGERPRINT_NAME_TMP; //wujiacheng add for bug71402 20150724

    private static final int ACTION_INDEX_ASSOCIATED = 0;
    private static final int ACTION_INDEX_RENAME = 1;
    private static final int ACTION_INDEX_DELETE = 2;
    private static final int ACTION_INDEX_CANCEL = 3;

    private static final int CHECK_PWD = 1000;
    private static final int START_TO_SET_PWD = 1001;
    public static final int ENROLL_RESULT = 1002;
    public static final String ENROLL_RESULT_NEED_REFRESH = "enroll_result_need_refresh";
    public static final String ENROLL_RESULT_INDEX = "enroll_result_index";
    public static final String ENROLL_RESULT_NAME = "enroll_result_name";

    private static final String INTERRUPT_KEY = "interrrupt_key";
    private static final int INTERRUPT_SUCCESS = 0;
    private static final int INTERRUPT_FAIL = 1;
  
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;
    private ArrayList<String> mFingerPrintKeyArray;
    private ArrayList<String> mFingerPrintNameArray;
    private HashMap<String, String> mHMFPKeyName;

    private FingerPrintPreference mFingerprintNew;
    private Preference mReCalibration;
    private PreferenceCategory mPreferenceCategoryFunction;
    private PreferenceCategory mPreferenceCategoryList;
    private FingerprintSwitchPreference mUnlockKeyguard;
    private FingerprintSwitchPreference mColdUnlockKeyguard;
    private Preference mUnlockStartAppPreference;
    private boolean mFPServiceConnected;

    private boolean mWaitingForConfirmation = false;
    private boolean mForFirstCheck = true;
    private boolean mDeleteTrigger = false;

    private AlertDialog mFPMgrDlg;
    private AlertDialog mFPRenameDlg;
    private ProgressDialog mProgressDialog;

    private InterruptHandler mInterruptHandler;

    private static final int MSG_SHOW_CALIBRATION_SUCCESS = 0;
    private static final int MSG_SHOW_CALIBRATION_FAILED = 1;
    private static final int MSG_SHOW_CALIBRATION_DLG = 2;
    private static final int MSG_DISMISS_CALIBRATION_DLG = 3;
    private Handler mHandler =  new Handler() {
        public void handleMessage(Message msg) {
            boolean redo = false;

            switch (msg.what) {
            case MSG_SHOW_CALIBRATION_SUCCESS:
                redo = (Boolean)msg.obj;
                Toast.makeText(FingerPrintSettings.this, (redo ? R.string.fingerprint_recalibration_success : R.string.fingerprint_calibration_success), Toast.LENGTH_SHORT).show();
                break;

            case MSG_SHOW_CALIBRATION_FAILED:
                redo = (Boolean)msg.obj;
                Toast.makeText(FingerPrintSettings.this, (redo ? R.string.fingerprint_recalibration_failed : R.string.fingerprint_calibration_failed), Toast.LENGTH_SHORT).show();
                break;

            case MSG_SHOW_CALIBRATION_DLG:
                redo = (Boolean)msg.obj;
                showProgressing(redo);
                break;

            case MSG_DISMISS_CALIBRATION_DLG:
                dismissProgressing();
                break;
            }
        }
    };

    private static final int MSG_INTERRUPT_THREAD_EXIT = 0;
    private static final int MSG_INTERRUPT_THREAD_DO_INTERRUPT = 1;
    final class InterruptHandler extends Handler {
        public InterruptHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            switch (msg.what) {
            case MSG_INTERRUPT_THREAD_EXIT:
                this.getLooper().quit();
                break;

            case MSG_INTERRUPT_THREAD_DO_INTERRUPT:
                boolean redo = (Boolean)msg.obj;
                doInterrupt(redo);
                break;
            }
        }
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.addPreferencesFromResource(R.xml.fingerprint_main);

        this.setTitle(R.string.fingerprint_settings_title);

        initAllPreferences();
        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(this);
        mLockPatternUtils = new LockPatternUtils(this);

        HandlerThread ht = new HandlerThread("interrupt_thread");
        ht.start();
        mInterruptHandler =  new InterruptHandler(ht.getLooper());

        checkPwd();

        logi("[onCreate]");

        ActionBar actionbar = this.getActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true); 
        }

        // forTestInterface();
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

    private void forTestInterface() {
        IBinder binder = ServiceManager.getService(Context.HIPAD_FINGER_PRINT_SERVICE);
        logd("[forTestInterface] binder : " + binder);

        final IFingerPrintUtils utils = IFingerPrintUtils.Stub.asInterface(binder);
        final FingerPrintListener listener = new FingerPrintListener.Stub() {  
            public void onBadImage(int status) {}
            public void onServiceConnected(){
                loge("listener onServiceConnected!");
                if (utils != null) {
                    try {
                        utils.captureRawData();
                    } catch (RemoteException re) {
                        loge("[forTestInterface] re : " + re);
                    }
                }
            }
            public void onServiceDisConnected(){}
            public void onFingerFetch(){}
            public void onFingerImageGetted(){
                loge("listener onFingerImageGetted!");

                if (utils != null) {
                    try {
                        utils.close();
                    } catch (RemoteException re) {
                        loge("[forTestInterface] re : " + re);
                    }
                }
            }
            public void onUserAbort(){}
            public void onStatus(int status){}
            public void onSuccess(){}
            public void onFail(){}
            public void onTimeout(){
                loge("listener onTimeout!");

                /*
                if (utils != null) {
                    try {
                        utils.open();
                    } catch (RemoteException re) {
                        loge("[forTestInterface] re : " + re);
                    }
                }
                */
            }
            public void onCameraAttemptToUse(){}
            public void onCameraRelease(){}
        };

        logd("[forTestInterface] utils : " + utils);
        if (utils != null) {
            try {
                utils.test();
                utils.addListener(listener);
                utils.open();
            } catch (RemoteException re) {
                loge("[forTestInterface] re : " + re);
            }
        }
    }
    
    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        mInterruptHandler.sendEmptyMessage(MSG_INTERRUPT_THREAD_EXIT);
    }

    private int getIndexByKey(String key) {
        int index = -1;

        String s = String.valueOf(key.charAt(key.length() - 1));
        index = Integer.parseInt(s);

        return index;
    }

    private void deleteFPByKey(String key) {
        int index = getIndexByKey(key);
        logd("[deleteFPByKey] index  : " + index);

        if (index == -1) {
            loge("[deleteFPByKey] error when getting index from key : " + key);
            return;
        }

        deleteEnrolledFP(FP_PRE + "_" + index, index);
    }

    private void deleteEnrolledFP(final String fpName, final int index) {
        final SettingLib lib = new SettingLib(this);
        lib.setStatusListener(new FPAuthListeners.StatusListener() {
            public void onBadImage(int status) {
                logd("[deleteEnrolledFP] onBadImage status : " + status);
            }

            public void onFingerFetch() {
                // do nothing
            }

            public void onFingerImageGetted() {
                // do nothing
            }

            public void onServiceConnected() {
                logd("[deleteEnrolledFP] onServiceConnected");
                if (lib.deleteFeature(fpName)) {
                    FingerPrintUtils.deleteFingerPrintByIndex(FingerPrintSettings.this, index);
                    Toast.makeText(FingerPrintSettings.this, R.string.fingerprint_delete_finish, Toast.LENGTH_SHORT).show();

                    mDeleteTrigger = true;
                    refreshUI();
                } else {
                    Toast.makeText(FingerPrintSettings.this, R.string.fingerprint_delete_failed, Toast.LENGTH_SHORT).show();
                }

                lib.abort();
                lib.disconnectDevice();
                lib.cleanListeners();
                lib.unbind();
            }

            public void onServiceDisConnected() {
                logd("[deleteEnrolledFP] onServiceDisConnected");
            }

            public void onStatus(int status) {
                logd("[deleteEnrolledFP] onStatus status : " + (status));
            }

            public void onUserAbort() {
                logd("[deleteEnrolledFP] onUserAbort ");
            }
        });

        lib.bind();
    }

    private void processDeleteFP() {
        if (FingerPrintUtils.getFingerCount(this) == 0) {
            mUnlockKeyguard.setCurrentStatus(false);
        }
    }

    private boolean isColdUnlockEnable() {
        return mLockPatternUtils.isFingerprintColdUnlockEnable();
    }

    private void refreshUI() {
        if (!mWaitingForConfirmation) {
            removeAllPreference();
            mPreferenceCategoryList.addPreference(mReCalibration);
            mPreferenceCategoryList.addPreference(mFingerprintNew);

            updateFingerPrintList();

            logd("[refreshUI] mDeleteTrigger : " + mDeleteTrigger);
            if (!mDeleteTrigger) {
                updateUnlockKeyguard();
            } else {
                processDeleteFP();
                mDeleteTrigger = false;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // check calibration
        if (!isInterruptDone()) {
            Message msg = mHandler.obtainMessage(MSG_SHOW_CALIBRATION_DLG, (Boolean)false);
            msg.sendToTarget();

            msg = mInterruptHandler.obtainMessage(MSG_INTERRUPT_THREAD_DO_INTERRUPT, (Boolean)false);
            msg.sendToTarget();
        }

        refreshUI();
    }

    private void removeCalibration() {
        final SettingLib lib = new SettingLib(this);
        lib.setStatusListener(new FPAuthListeners.StatusListener() {
            public void onBadImage(int status) {
                logd("[removeCalibration] onBadImage status : " + status);
            }

            public void onFingerFetch() {
                // do nothing
            }

            public void onFingerImageGetted() {
                // do nothing
            }

            public void onServiceConnected() {
                logd("[removeCalibration] onServiceConnected");

                Message msg = mHandler.obtainMessage(MSG_SHOW_CALIBRATION_DLG, (Boolean)true);
                msg.sendToTarget();

                boolean success = false;
                if (lib.removeCalibration() == 0) {
                    FingerPrintUtils.putIntDataByKey(FingerPrintSettings.this.getContentResolver(), 
                                        INTERRUPT_KEY, INTERRUPT_FAIL);
                    loge("[removeCalibration] success!");
                    success = true;
                } else {
                    loge("[removeCalibration] failed!");
                }

                lib.abort();
                lib.disconnectDevice();
                lib.cleanListeners();
                lib.unbind();

                if (success) {
                    msg = mInterruptHandler.obtainMessage(MSG_INTERRUPT_THREAD_DO_INTERRUPT, (Boolean)true);
                    msg.sendToTarget();
                } else {
                    mHandler.sendEmptyMessage(MSG_DISMISS_CALIBRATION_DLG);

                    msg = mInterruptHandler.obtainMessage(MSG_SHOW_CALIBRATION_FAILED, (Boolean)true);
                    msg.sendToTarget();
                }
            }

            public void onServiceDisConnected() {
                logd("[removeCalibration] onServiceDisConnected");
                mHandler.sendEmptyMessage(MSG_DISMISS_CALIBRATION_DLG);
            }

            public void onStatus(int status) {
                logd("[removeCalibration] onStatus arg0 = " + (status));
            }

            public void onUserAbort() {
                logd("[removeCalibration] onUserAbort ");
            }
        });

        lib.bind();
    }

    private void showProgressing(boolean redo) {
        if (mProgressDialog != null) {
            logd("[showProgressing] already showing...");
            return;
        }

        int strId = redo ? R.string.fingerprint_re_calibration_msg : R.string.fingerprint_calibration_msg;

        mProgressDialog = ProgressDialog.show(this, 
                getString(R.string.fingerprint_calibration_title), 
                getString(strId),
                true);
    }

    private void dismissProgressing() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private boolean isInterruptDone() {
        int status = FingerPrintUtils.getIntDataByKey(this.getContentResolver(), 
                INTERRUPT_KEY, INTERRUPT_FAIL);

        logd("[isInterruptDone] status : " + status);

        return (status == INTERRUPT_SUCCESS);
    }

    private void doInterrupt(final boolean redo) {
        AutoInterrpt autoInterrpt = new AutoInterrpt(this);
        autoInterrpt.setGetTHDCValueListener(new GetTHDCValueListener() {
            @Override
            public void getTHDCValue(int iDCvalue, int iTHvalue) {
                logd("[doInterrupt] [getTHDCValue] interrupt result : iDCvalue : " + iDCvalue + ", iTHvalue : " + iTHvalue);
            }

            @Override
            public void OnSuccess() {
                loge("[doInterrupt] [OnSuccess] interrupt success!");
                FingerPrintUtils.putIntDataByKey(FingerPrintSettings.this.getContentResolver(), 
                                    INTERRUPT_KEY, INTERRUPT_SUCCESS);

                Message msg = mHandler.obtainMessage(MSG_SHOW_CALIBRATION_SUCCESS, (Boolean)redo);
                msg.sendToTarget();

                mHandler.sendEmptyMessage(MSG_DISMISS_CALIBRATION_DLG);
            }
            
            @Override
            public void OnFail() {
                loge("[doInterrupt] [OnFail] interrupt failed!");
                FingerPrintUtils.putIntDataByKey(FingerPrintSettings.this.getContentResolver(), 
                                    INTERRUPT_KEY, INTERRUPT_FAIL);

                Message msg = mHandler.obtainMessage(MSG_SHOW_CALIBRATION_FAILED, (Boolean)redo);
                msg.sendToTarget();

                mHandler.sendEmptyMessage(MSG_DISMISS_CALIBRATION_DLG);
            }
        });

        autoInterrpt.GetAutoTHDCValue();
    }

    private void checkPwd() {
        if (mWaitingForConfirmation) {
            logd("[checkPwd] checking has already started, just return!");
            return;
        }

        if (!FingerPrintUtils.isPasswordQualityNone(mLockPatternUtils)) {
            logd("[checkPwd] pwd enabled ");
            mWaitingForConfirmation = true;
            mChooseLockSettingsHelper.launchConfirmationActivity(CHECK_PWD, "Fingerprint" ,null);  //wujiacheng modified for bug72703 20150731
        } else {
            logd("[checkPwd] pwd disabled ");
        }
    }

    private boolean isPwdSet() {
        boolean set = false;
        if (FingerPrintUtils.isPasswordQualityNone(mLockPatternUtils)) {
            logd("[isPwdSet] no any pwd yet.");

            final Bundle extras = new Bundle();
            extras.putBoolean(FingerprintEnrollFragment.SET_FOR_FP, true);

            Intent intent = Utils.onBuildStartFragmentIntent(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment", extras,
                    null, R.string.lock_settings_picker_title, null, false);

            this.startActivityForResult(intent, START_TO_SET_PWD);
            mWaitingForConfirmation = true;

            set = false;
        } else {
            // do nothing
            set = true;
        }

        return set;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        logd("[onActivityResult] requestCode : " + requestCode + ", resultCode : " + resultCode);
        if (requestCode == CHECK_PWD) {
            mWaitingForConfirmation = false;
            if (resultCode == Activity.RESULT_CANCELED) {
                logd("[onActivityResult] [CHECK_PWD] pwd check failed. remove the pre-finger print.");
                finish();
            }  else {
                logd("[onActivityResult] [CHECK_PWD] pwd check ok.");
            }
        } else if (requestCode == START_TO_SET_PWD) {
            mWaitingForConfirmation = false;
            if (resultCode == Activity.RESULT_CANCELED) {
                logd("[onActivityResult] [START_TO_SET_PWD] pwd set cancel.");
                mUnlockKeyguard.setCurrentStatus(false);
            }  else {
                logd("[onActivityResult] [START_TO_SET_PWD] pwd set finished.");
                mLockPatternUtils.saveLockFingerprint();
                mColdUnlockKeyguard.setCurrentStatus(true);
            }
        } else if (requestCode == ENROLL_RESULT) {
            logd("[onActivityResult] ENROLL_RESULT data : " + data);

            if (data != null) {
                int index = data.getIntExtra(ENROLL_RESULT_INDEX, -1);
                String fpName = data.getStringExtra(ENROLL_RESULT_NAME);
                showFPRenameDlg(KEY_FINGERPRINT_PRE + index, fpName);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void initAllPreferences() {
        mUnlockKeyguard = (FingerprintSwitchPreference) this.findPreference(KEY_FINGERPRINT_UNLOCK_KEYGUARD);
        mUnlockKeyguard.setSwitchChange(this);

        // mColdUnlockKeyguard = (FingerprintSwitchPreference) this.findPreference(KEY_FINGERPRINT_COLD_UNLOCK_KEYGUARD);
        // mColdUnlockKeyguard.setSwitchChange(this);

        mPreferenceCategoryFunction = (PreferenceCategory) this.findPreference(KEY_FINGERPRINT_FUNCTION);
        mPreferenceCategoryList = (PreferenceCategory) this.findPreference(KEY_FINGERPRINT_LIST);


        mFingerPrintKeyArray =  new ArrayList<String>();
        mFingerPrintNameArray =  new ArrayList<String>();
        mHMFPKeyName = new HashMap<String, String>();
        initNewFingerPrint();
        initColdUnlockFingerPrint();
        initUnlockStartAppPreference();

        initReCalibration();
    }

    private void removeAllPreference() {
        mPreferenceCategoryList.removeAll();
        /*wujiacheng modified for bug74185 201508147 start*/
        if(!mUnlockKeyguard.getCurrentStatus()) {
            mPreferenceCategoryFunction.removePreference(mColdUnlockKeyguard);
            logd("removeAllPreference removePreference mColdUnlockKeyguard");
        }
        /*wujiacheng modified for bug74185 201508147 end*/
        mFingerPrintKeyArray.clear();
        mFingerPrintNameArray.clear();
        mHMFPKeyName.clear();
    }

    private boolean isExistName(String name) {
        boolean result = false;

        String trimName = name.trim();
        if (mFingerPrintNameArray.contains(trimName)) {
            result = true;
        }

        return result;
    }

    private void updateFingerPrintList() {
        Cursor cursor = this.getContentResolver().query(
                FingerPrintProvider.FINGER_PRINT_URI,
                FingerPrintDatabaseHelper.FINGER_PRINT_PROJECTION, null, null, null);

        if (cursor == null) {
            logd("[updateFingerPrintList] cursor is null! just return");
            return;
        }

        logd("[updateFingerPrintList] cursor counts " + cursor.getCount());
        if (cursor.getCount() == 0) {
            logd("[updateFingerPrintList] cursor has no any data! just return");
            cursor.close();
            return;
        }

        int i = 0;

        try {
            while (cursor.moveToNext()) {
                Preference fingerPrintPre = new FingerPrintPreference(this);
                String name = cursor.getString(cursor.getColumnIndex(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_NAME));
                String pkgName = cursor.getString(cursor.getColumnIndex(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_PACKAGE));
                String clsName = cursor.getString(cursor.getColumnIndex(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_ACTIVITY));
                int associatedAppState = cursor.getInt(cursor.getColumnIndex(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_STATUS));
                int associatedState = Settings.System.getInt(this.getContentResolver(), "associatedSwitchState", 0);
                fingerPrintPre.setTitle(name);
                if ((associatedAppState == 1) && (associatedState == 1) && (pkgName != null)) {
                    String appLabel = FingerPrintUtils.getAppLabel(this.getPackageManager(), pkgName, clsName);
                    fingerPrintPre.setSummary(appLabel);
                }
                mFingerPrintNameArray.add(name);
                String key = KEY_FINGERPRINT_PRE + cursor.getString(cursor.getColumnIndex(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_INDEX));
                fingerPrintPre.setKey(key);
                fingerPrintPre.setOrder(i++);
                mFingerPrintKeyArray.add(key);

                mHMFPKeyName.put(key, name);
                mPreferenceCategoryList.addPreference(fingerPrintPre);
            }
        } catch (Exception ex) {
            loge("[updateFingerPrintList] exception! " + ex);
        } finally {
            cursor.close();
        }
    }

    private void updateUnlockKeyguard() {
        boolean open  = mLockPatternUtils.isLockFingerprintEnabled();

        logd("[updateUnlockKeyguard] open : " + open);
        if (open) {
            mPreferenceCategoryFunction.addPreference(mColdUnlockKeyguard);
            updateColdUnlockKeyguard();

            mUnlockKeyguard.setCurrentStatus(true);
        } else {
            mUnlockKeyguard.setCurrentStatus(false);

            mLockPatternUtils.deleteLockFingerprint();
            mLockPatternUtils.disableFingerprintColdUnlock();

            mPreferenceCategoryFunction.removePreference(mColdUnlockKeyguard);
        }
    }

    private void updateColdUnlockKeyguard() {
        boolean open  = isColdUnlockEnable();

        logd("[updateColdUnlockKeyguard] open : " + open);
        if (open) {
            mColdUnlockKeyguard.setCurrentStatus(true);
        } else {
            mColdUnlockKeyguard.setCurrentStatus(false);
        }
    }

    private void initNewFingerPrint() {
        mFingerprintNew = new FingerPrintPreference(this);
        mFingerprintNew.setTitle(this.getString(R.string.fingerprint_title_new));
        mFingerprintNew.setKey(KEY_FINGERPRINT_NEW);
        mFingerprintNew.setIcon(android.R.drawable.ic_menu_add);
        mFingerprintNew.setOrder(20);
    }

    private void initColdUnlockFingerPrint() {
        mColdUnlockKeyguard = new FingerprintSwitchPreference(this, null);
        mColdUnlockKeyguard.setTitle(this.getString(R.string.fingerprint_cold_unlock_keyguard_title));
        mColdUnlockKeyguard.setSummary(this.getString(R.string.fingerprint_cold_unlock_keyguard_summary));
        mColdUnlockKeyguard.setKey(KEY_FINGERPRINT_COLD_UNLOCK_KEYGUARD);
        mColdUnlockKeyguard.setLayoutResource(R.layout.hct_preference_switch_item);
        mColdUnlockKeyguard.setOrder(2);
        mColdUnlockKeyguard.setSwitchChange(this);
    }

    private void initUnlockStartAppPreference() {
        mUnlockStartAppPreference = new Preference(this, null);
        mUnlockStartAppPreference.setTitle(this.getString(R.string.fingerprint_unlock_start_app));
        mUnlockStartAppPreference.setKey(KEY_FINGERPRINT_UNLOCK_START_APP);
        mUnlockStartAppPreference.setLayoutResource(R.layout.finger_print_start_app_preference);
        mUnlockStartAppPreference.setOrder(3);

        mPreferenceCategoryFunction.addPreference(mUnlockStartAppPreference);
    }

    private void initReCalibration() {
        mReCalibration = new Preference(this);
        mReCalibration.setTitle(this.getString(R.string.fingerprint_title_recalibration));
        mReCalibration.setKey(KEY_RE_CALIBRATION);
        mReCalibration.setIcon(android.R.drawable.ic_menu_add);
        mReCalibration.setOrder(30);
    }

    private boolean reachMaxLimit() {
        int fingerCount = FingerPrintUtils.getFingerCount(this);
        return (fingerCount >= MAX_FINGERPRINT_COUNT);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen paramPreferenceScreen, Preference paramPreference) {
        String key = paramPreference.getKey();
        logd("[onPreferenceTreeClick] key : " + key);

        if (paramPreference instanceof FingerprintSwitchPreference) {
            ((FingerprintSwitchPreference)paramPreference).setCurrentStatus(!((FingerprintSwitchPreference)paramPreference).getCurrentStatus());
            if(key.equals("fingerprint_unlock_keyguard")) {
                if(((FingerprintSwitchPreference)paramPreference).getCurrentStatus()) {
                    mColdUnlockKeyguard.setCurrentStatus(true);
                    mLockPatternUtils.enableFingerprintColdUnlock();
                }
            }
            
        } else if (paramPreference == mFingerprintNew) {
            callEnrollFragment();
        } else if (paramPreference == mReCalibration) {
            removeCalibration();
        } else if (mFingerPrintKeyArray.contains(key)) {
            logd("[onPreferenceTreeClick] key in the array!");
            boolean isAssociatedApp = false;
            if (paramPreference.getSummary() != null) {
                isAssociatedApp = true;
            }
            showFPMangeDlg(key, isAssociatedApp);
        }else if(key.equals(KEY_FINGERPRINT_UNLOCK_START_APP)){
            logd("KEY_FINGERPRINT_UNLOCK_START_APP");	
            startApplication();
        }

        return super.onPreferenceTreeClick(paramPreferenceScreen, paramPreference);    
    }

    private void showFPMangeDlg(final String key, boolean isAssociatedApp) {
        if (mFPMgrDlg != null) {
            logd("[showFPMangeDlg] finger print manage dlg has been showed.");
            return;
        }

        logd("[showFPMangeDlg] key : " + key);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String[] actions = this.getResources().getStringArray(R.array.fp_actions);
        if (isAssociatedApp) {
            actions[0] = this.getString(R.string.reset_associated_app);
        }

        View view = LayoutInflater.from(this).inflate(R.layout.fp_manage, null);
        ListView list = (ListView) view.findViewById(R.id.fp_list);
        list.setAdapter(new FPListAdapter(this, actions));
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                switch (position) {
                case ACTION_INDEX_ASSOCIATED:
                    showFPShowAllAppActivity(mHMFPKeyName.get(key));
                    break;
                case ACTION_INDEX_RENAME:
                    showFPRenameDlg(key, mHMFPKeyName.get(key));
                    break;

                case ACTION_INDEX_DELETE:
                    deleteFPByKey(key);
                    break;

                case ACTION_INDEX_CANCEL:
                    // do nothing
                    break;

                default:
                    logw("position error : " + position);
                    break;
                }

                mFPMgrDlg.dismiss();
                mFPMgrDlg = null;
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                mFPMgrDlg = null;
            }
        });

        builder.setView(view);
        mFPMgrDlg = builder.create();
        Window window = mFPMgrDlg.getWindow();  
        window.setGravity(Gravity.BOTTOM);
        mFPMgrDlg.show();
    }

    private void renameByIndex(int index, String fpName) {
        logd("[renameByKey] index  : " + index + ", fpName : " + fpName);

        if (index == -1) {
            loge("[renameByKey] error index!");
            return;
        }

        ContentValues values =  new ContentValues();
        values.put(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_NAME, fpName);
        values.put(FingerPrintDatabaseHelper.TABLE_FINGER_PRINT_RENAME, FingerPrintDatabaseHelper.FP_RENAME);

        FingerPrintUtils.updateFingerPrintByIndex(this, index, values);
        Toast.makeText(this, R.string.fingerprint_rename_finish, Toast.LENGTH_SHORT).show();
    }

    private void renameByKey(String key, String fpName) {
        int index = getIndexByKey(key);
        logd("[renameByKey] index  : " + index);

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

    private void showFPShowAllAppActivity(String FpName) {
        Intent intent = new Intent(this, FingerPrintShowAllAppActivity.class);
        intent.putExtra(FingerPrintAssociatedAppActivity.FINGERPRINT_NAME, FpName);
        startActivity(intent);
    }

    private void showFPRenameDlg(final String key, String fpName) {
        if (mFPRenameDlg != null) {
            logd("[showFPRenameDlg] finger print rename dlg has been showed.");
            return;
        }
        FINGERPRINT_NAME_TMP = fpName; //wujiacheng modified for bug71402 20150724
        logd("[showFPRenameDlg] key : " + key);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final EditText et = new EditText(this);
        et.setText(fpName);
        et.setSelection(et.length());

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String name = et.getText().toString();

                if (TextUtils.isEmpty(name)) {
                    showErrorInfo(R.string.fingerprint_name_empty_title, R.string.fingerprint_name_empty_msg);
                    /*wujiacheng add for bug70407 20150721 start*/
                    mFPRenameDlg.dismiss();
                    mFPRenameDlg = null;
                    /*wujiacheng add for bug70407 20150721 end*/
                    return;
                }

                if (!FINGERPRINT_NAME_TMP.equals(name) && isExistName(name)) { /*wujiacheng modified for bug71402 20150724*/
                    showErrorInfo(R.string.fingerprint_name_exist_title, R.string.fingerprint_name_exist_msg);
                    /*wujiacheng add for bug70407 20150721 start*/
                    mFPRenameDlg.dismiss();
                    mFPRenameDlg = null;
                    /*wujiacheng add for bug70407 20150721 end*/
                    return;
                }

                renameByKey(key, name);

                refreshUI();

                mFPRenameDlg.dismiss();
                mFPRenameDlg = null;
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mFPRenameDlg.dismiss();
                mFPRenameDlg = null;
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                mFPRenameDlg = null;
            }
        });

        builder.setView(et);
        mFPRenameDlg = builder.create();
        Window window = mFPRenameDlg.getWindow();  
        window.setGravity(Gravity.BOTTOM);
        mFPRenameDlg.show();
    }

    @Override
    public void onSwitchChange(String key, boolean state) {
        logd("[onSwitchChange] key : " + key + ", state : " + state);

        if (KEY_FINGERPRINT_UNLOCK_KEYGUARD.equals(key)) {
            mPreferenceCategoryFunction.removePreference(mColdUnlockKeyguard);

            if (state) {
                if (FingerPrintUtils.getFingerCount(this) == 0) {
                    logd("[onSwitchChange] no any data of finger print! Try to enroll one");
                    callEnrollFragment();
                    return;
                }

                if (isPwdSet()) {
                    mLockPatternUtils.saveLockFingerprint();
                }
                
                mPreferenceCategoryFunction.addPreference(mColdUnlockKeyguard);
                // mLockPatternUtils.enableFingerprintColdUnlock();
                //updateColdUnlockKeyguard();
            } else {
                mLockPatternUtils.deleteLockFingerprint();
                mLockPatternUtils.disableFingerprintColdUnlock();
            }
        } else if (KEY_FINGERPRINT_COLD_UNLOCK_KEYGUARD.equals(key)) {
            if (state) {
                mLockPatternUtils.enableFingerprintColdUnlock();
            } else {
                mLockPatternUtils.disableFingerprintColdUnlock();
            }
        }
    }

    @Override
    public void onSwitchClick(View v, String key) {
        logd("[onSwitchChange] key : " + key);
        if (KEY_FINGERPRINT_UNLOCK_KEYGUARD.equals(key)) {
            boolean open  = mLockPatternUtils.isLockFingerprintEnabled();
            if(open) {
                mLockPatternUtils.enableFingerprintColdUnlock();
                updateColdUnlockKeyguard();
            }
        }
    }

    private void callEnrollFragment () {
        if (reachMaxLimit()) {
            Toast.makeText(this, R.string.fingerprint_reach_limit, Toast.LENGTH_SHORT).show();
            return;
        }

        //startFragment(this, "com.android.settings.fingerprint.FingerprintEnrollFragment",
        //        R.string.lock_settings_picker_title, 0, null);

        Intent intent = new Intent();
        intent.setClass(this, com.android.settings.Settings.FingerPrintEnrollActivity.class);
        this.startActivityForResult(intent, ENROLL_RESULT);
    }

    private void startApplication() {
        Intent intent = new Intent();
        intent.setClass(this, FingerPrintAssociatedAppActivity.class);
        this.startActivity(intent);
    }

    private class FPListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private String[] mData;
        
        final class ViewHolder{
            public TextView title;
        }

        public FPListAdapter(Context context, String[] data) {
            mInflater = LayoutInflater.from(context);
            mData =  new String[data.length];
            for (int i=0; i<data.length; i++) {
                mData[i] = data[i];
            }
        }

        @Override
        public int getCount() {
            return mData.length;
        }

        @Override
        public Object getItem(int position) {
            return mData[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1,null);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(android.R.id.text1);
                convertView.setTag(holder);
            } else{
                holder = (ViewHolder) convertView.getTag();
            }

            holder.title.setText(mData[position]);

            if (position == (mData.length - 1)) {
                holder.title.setGravity(Gravity.CENTER);
            } else {
                holder.title.setGravity(Gravity.LEFT | Gravity.CENTER);
            }
            
            return convertView;
        }
    }

    private static void logi(String strs) {
        Log.i(TAG_FP, strs);
    }

    private static void logd(String strs) {
        Log.d(TAG_FP, strs);
    }

    private static void logw(String strs) {
        Log.w(TAG_FP, strs);
    }

    private static void loge(String strs) {
        Log.e(TAG_FP, strs);
    }
}
