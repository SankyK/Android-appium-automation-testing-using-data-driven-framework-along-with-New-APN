package com.android.settings;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import android.telephony.TelephonyManager;
import java.util.ArrayList;
public class ApnSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "ApnSettings";
    public static final String EXTRA_POSITION = "position";
    public static final String RESTORE_CARRIERS_URI =
        "content://telephony/carriers/restore";
    public static final String PREFERRED_APN_URI =
        "content://telephony/carriers/preferapn";
    public static final String APN_ID = "apn_id";
    public static final String SUB_ID = "sub_id";
    public static final String MVNO_TYPE = "mvno_type";
    public static final String MVNO_MATCH_DATA = "mvno_match_data";
    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;
    private static final int MVNO_TYPE_INDEX = 4;
    private static final int MVNO_MATCH_DATA_INDEX = 5;
    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;
    private static final int EVENT_RESTORE_DEFAULTAPN_START = 1;
    private static final int EVENT_RESTORE_DEFAULTAPN_COMPLETE = 2;
    private static final int DIALOG_RESTORE_DEFAULTAPN = 1001;
    private static final Uri DEFAULTAPN_URI = Uri.parse(RESTORE_CARRIERS_URI);
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);
    private static boolean mRestoreDefaultApnMode;
    private RestoreApnUiHandler mRestoreApnUiHandler;
    private RestoreApnProcessHandler mRestoreApnProcessHandler;
    private HandlerThread mRestoreDefaultApnThread;
    private SubscriptionInfo mSubscriptionInfo;
    private UiccController mUiccController;
    private String mMvnoType;
    private String mMvnoMatchData;
    private UserManager mUm;
    private String mSelectedKey;
    private IntentFilter mMobileStateFilter;
    private boolean mUnavailable;
    private boolean mHideImsApn;
    private boolean mAllowAddingApns;
    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                PhoneConstants.DataState state = getMobileDataState(intent);
                switch (state) {
                case CONNECTED:
                    if (!mRestoreDefaultApnMode) {
                        fillList();
                    } else {
                        showDialog(DIALOG_RESTORE_DEFAULTAPN);
                    }
                    break;
                }
            }
        }
    };
    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APN;
    }
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Activity activity = getActivity();
        final int subId = activity.getIntent().getIntExtra(SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mUm = (UserManager) getSystemService(Context.USER_SERVICE);
        mMobileStateFilter = new IntentFilter(
                TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        if (!mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            setHasOptionsMenu(true);
        }
        mSubscriptionInfo = SubscriptionManager.from(activity).getActiveSubscriptionInfo(subId);
        mUiccController = UiccController.getInstance();
        CarrierConfigManager configManager = (CarrierConfigManager)
                getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = configManager.getConfig();
        mHideImsApn = b.getBoolean(CarrierConfigManager.KEY_HIDE_IMS_APN_BOOL);
        mAllowAddingApns = b.getBoolean(CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TextView empty = (TextView) getView().findViewById(android.R.id.empty);
        if (empty != null) {
            empty.setText(R.string.apn_settings_not_available);
            getListView().setEmptyView(empty);
        }
        if (mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)
                || UserHandle.myUserId()!= UserHandle.USER_OWNER) {
            mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
            return;
        }
        addPreferencesFromResource(R.xml.apn_settings);
        getListView().setItemsCanFocus(true);
    }
    @Override
    public void onResume() {
        super.onResume();
        if (mUnavailable) {
            return;
        }
        getActivity().registerReceiver(mMobileStateReceiver, mMobileStateFilter);
        if (!mRestoreDefaultApnMode) {
            fillList();
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        if (mUnavailable) {
            return;
        }
        getActivity().unregisterReceiver(mMobileStateReceiver);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mRestoreDefaultApnThread != null) {
            mRestoreDefaultApnThread.quit();
        }
    }
    private void fillList() {
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final String mccmnc = mSubscriptionInfo == null ? ""
            : tm.getSimOperator(mSubscriptionInfo.getSubscriptionId());
        Log.d(TAG, "mccmnc = " + mccmnc);
        StringBuilder where = new StringBuilder("numeric=\"" + mccmnc +
                "\" AND NOT (type='ia' AND (apn=\"\" OR apn IS NULL)) AND user_visible!=0");
        if (mHideImsApn) {
            where.append(" AND NOT (type='ims')");
        }
        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[] {
                "_id", "name", "apn", "type", "mvno_type", "mvno_match_data"}, where.toString(),
                null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            IccRecords r = null;
            if (mUiccController != null && mSubscriptionInfo != null) {
                r = mUiccController.getIccRecords(SubscriptionManager.getPhoneId(
                        mSubscriptionInfo.getSubscriptionId()), UiccController.APP_FAM_3GPP);
            }
            PreferenceGroup apnList = (PreferenceGroup) findPreference("apn_list");
            apnList.removeAll();
            ArrayList<ApnPreference> mnoApnList = new ArrayList<ApnPreference>();
            ArrayList<ApnPreference> mvnoApnList = new ArrayList<ApnPreference>();
            ArrayList<ApnPreference> mnoMmsApnList = new ArrayList<ApnPreference>();
            ArrayList<ApnPreference> mvnoMmsApnList = new ArrayList<ApnPreference>();
            mSelectedKey = getSelectedApnKey();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(NAME_INDEX);
                String apn = cursor.getString(APN_INDEX);
                String key = cursor.getString(ID_INDEX);
                String type = cursor.getString(TYPES_INDEX);
                String mvnoType = cursor.getString(MVNO_TYPE_INDEX);
                String mvnoMatchData = cursor.getString(MVNO_MATCH_DATA_INDEX);
                ApnPreference pref = new ApnPreference(getActivity());
                pref.setKey(key);
                pref.setTitle(name);
                pref.setSummary(apn);
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);
                boolean selectable = ((type == null) || !type.equals("mms"));
                pref.setSelectable(selectable);
                if (selectable) {
                    if ((mSelectedKey != null) && mSelectedKey.equals(key)) {
                        pref.setChecked();
                    }
                    addApnToList(pref, mnoApnList, mvnoApnList, r, mvnoType, mvnoMatchData);
                } else {
                    addApnToList(pref, mnoMmsApnList, mvnoMmsApnList, r, mvnoType, mvnoMatchData);
                }
                cursor.moveToNext();
            }
            cursor.close();
            if (!mvnoApnList.isEmpty()) {
                mnoApnList = mvnoApnList;
                mnoMmsApnList = mvnoMmsApnList;
                // Also save the mvno info
            }
            for (Preference preference : mnoApnList) {
                apnList.addPreference(preference);
            }
            for (Preference preference : mnoMmsApnList) {
                apnList.addPreference(preference);
            }
        }
    }
    private void addApnToList(ApnPreference pref, ArrayList<ApnPreference> mnoList,
                              ArrayList<ApnPreference> mvnoList, IccRecords r, String mvnoType,
                              String mvnoMatchData) {
        if (r != null && !TextUtils.isEmpty(mvnoType) && !TextUtils.isEmpty(mvnoMatchData)) {
            if (ApnSetting.mvnoMatches(r, mvnoType, mvnoMatchData)) {
                mvnoList.add(pref);
                // Since adding to mvno list, save mvno info
                mMvnoType = mvnoType;
                mMvnoMatchData = mvnoMatchData;
            }
        } else {
            mnoList.add(pref);
        }
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mUnavailable) {
            if (mAllowAddingApns) {
                menu.add(0, MENU_NEW, 0,
                        getResources().getString(R.string.menu_new))
                        .setIcon(android.R.drawable.ic_menu_add)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            menu.add(0, MENU_RESTORE, 0,
                    getResources().getString(R.string.menu_restore))
                    .setIcon(android.R.drawable.ic_menu_upload);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEW:
            addNewApn();
            return true;
        case MENU_RESTORE:
            restoreDefaultApn();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void addNewApn() {
        Intent intent = new Intent(Intent.ACTION_INSERT, Telephony.Carriers.CONTENT_URI);
        int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        intent.putExtra(SUB_ID, subId);
        if (!TextUtils.isEmpty(mMvnoType) && !TextUtils.isEmpty(mMvnoMatchData)) {
            intent.putExtra(MVNO_TYPE, mMvnoType);
            intent.putExtra(MVNO_MATCH_DATA, mMvnoMatchData);
        }
        startActivity(intent);
    }
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int pos = Integer.parseInt(preference.getKey());
        Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
        startActivity(new Intent(Intent.ACTION_EDIT, url));
        return true;
    }
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
        }
        return true;
    }
    private void setSelectedApnKey(String key) {
        mSelectedKey = key;
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(APN_ID, mSelectedKey);
        resolver.update(PREFERAPN_URI, values, null, null);
    }
    private String getSelectedApnKey() {
        String key = null;
        Cursor cursor = getContentResolver().query(PREFERAPN_URI, new String[] {"_id"},
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();
        return key;
    }
    private boolean restoreDefaultApn() {
        showDialog(DIALOG_RESTORE_DEFAULTAPN);
        mRestoreDefaultApnMode = true;
        if (mRestoreApnUiHandler == null) {
            mRestoreApnUiHandler = new RestoreApnUiHandler();
        }
        if (mRestoreApnProcessHandler == null ||
            mRestoreDefaultApnThread == null) {
            mRestoreDefaultApnThread = new HandlerThread(
                    "Restore default APN Handler: Process Thread");
            mRestoreDefaultApnThread.start();
            mRestoreApnProcessHandler = new RestoreApnProcessHandler(
                    mRestoreDefaultApnThread.getLooper(), mRestoreApnUiHandler);
        }
        mRestoreApnProcessHandler
                .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_START);
        return true;
    }
    private class RestoreApnUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_COMPLETE:
                    Activity activity = getActivity();
                    if (activity == null) {
                        mRestoreDefaultApnMode = false;
                        return;
                    }
                    fillList();
                    getPreferenceScreen().setEnabled(true);
                    mRestoreDefaultApnMode = false;
                    removeDialog(DIALOG_RESTORE_DEFAULTAPN);
                    Toast.makeText(
                        activity,
                        getResources().getString(
                                R.string.restore_default_apn_completed),
                        Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;
        public RestoreApnProcessHandler(Looper looper, Handler restoreApnUiHandler) {
            super(looper);
            this.mRestoreApnUiHandler = restoreApnUiHandler;
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_START:
                    ContentResolver resolver = getContentResolver();
                    resolver.delete(DEFAULTAPN_URI, null, null);
                    mRestoreApnUiHandler
                        .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_COMPLETE);
                    break;
            }
        }
    }
    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getResources().getString(R.string.restore_default_apn));
            dialog.setCancelable(false);
            return dialog;
        }
        return null;
    }
}