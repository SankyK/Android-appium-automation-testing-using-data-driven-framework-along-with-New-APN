package Activity;

import java.awt.Button;
import java.awt.Cursor;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.text.View;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class ConfigFinalActivity extends Activity implements OnClickListener {
    private static final String TAG = "ConfigActivity";
    TelephonyManager tm;
    AlertDialog mErrorAlert = null;
    private Notification mNotification = null;
    private Button mXButton = null;
    private Button mAssistUpdateButton = null;
    private Button mAssistInstrButton = null;
    private Button mReadAgainButton = null;
    private int mInstructionNumber = 0;
    public static ArrayList<String> NameArr = new ArrayList<String>();
    public static ArrayList<String> ValueArr = new ArrayList<String>();
    public static ArrayList<String> nameArr = new ArrayList<String>();
    public static ArrayList<String> ApnArr = new ArrayList<String>();
    public static ArrayList<String> mmscArr = new ArrayList<String>();
    public static ArrayList<String> mmsportArr = new ArrayList<String>();
    public static ArrayList<String> mmsproxyArr = new ArrayList<String>();
    public static ArrayList<String> portArr = new ArrayList<String>();
    public static ArrayList<String> proxyArr = new ArrayList<String>();
    public static int count;
    public static int TotalSteps = 8;
    int i, g = 0, result = 0;

    public static ContentValues Values = new ContentValues();
    XmlParserHandlerFinal handler;

    public static final Uri APN_TABLE_URI = Uri
            .parse("content://telephony/carriers");
    public static final String Base_URL = "https://wapgate.example.com/REST/phoneSettings";
    public static InputStream stream = null;
    UpdateActivity update;
    public static String status, queryResult = "";

    /** Called when the activity is first created. */
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int version = android.os.Build.VERSION.SDK_INT;
        tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        update = new UpdateActivity();
        handler = new XmlParserHandlerFinal();
        handler.setContext(this.getBaseContext());
        getArrayLists();
        /*
         * boolean deleted = deleteFile("settings.xml");if(deleted){
         * Log.v("settings.xml","deleted"); }else
         * Log.v("settings.xml","failed to delete the file");
         */
        if (ApnArr.isEmpty() || mmscArr.isEmpty()) {

            tryagain();
        } else if (version < VERSION_CODES.ICE_CREAM_SANDWICH) {

            // Update APN table
            try {
                result = updateTable();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }// Settings updated with this atomic call
            catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (result != -1) {
                status = "success";

            } else {
                status = "failure";
            }

            if (status.equals("success")) {
                completeUpdate();
            } else if (status.equals("failure")) {
                 tryagain();
                // showAlert(getString(R.string.unchanged_dialog));
            }

        } else {// ICS and later versions

            // Reduce number of steps to 6
            TotalSteps = 6;
            setContentView(R.layout.assist_instructions);
            String assistUpdate = getString(R.string.instructions_1);
            CharSequence styledText = Html.fromHtml(assistUpdate);
            TextView assistText = (TextView) findViewById(R.id.apn_app_text_cta2);
            assistText.setText(styledText);
            mAssistUpdateButton = (Button) findViewById(R.id.assist_update_btn);
            mAssistUpdateButton.setOnClickListener(this);

        }
    }

    private void getArrayLists() {
        nameArr = update.getnameArr();
        ApnArr = update.getApnArr();
        mmscArr = update.getMMSCArr();
        mmsproxyArr = update.getMmscProxyArr();
        mmsportArr = update.getMmsPortArr();
        proxyArr = update.getProxyArr();
        portArr = update.getPortArr();
        count = update.getCount();  
        queryResult = update.getResult();
    }

    public void onClick(View v) {
        if (v == mAssistUpdateButton) {

            // Update button for ICS and up is selected
            // Get the TextView in the Assist Update UI

            TextView tv = (TextView) findViewById(R.id.apn_app_text_cta2);
            String text = "";
            CharSequence styledText = text;
            switch (mInstructionNumber) {

            case 0:
                // Retrieve the instruction string resource corresponding the
                // 2nd set of instructions
                text = String.format(getString(R.string.apn_app_text_instr),
                        TotalSteps);
                styledText = Html.fromHtml(text);
                // Update the TextView with the correct set of instructions
                tv.setText(styledText);
                // Increment instruction number so the correct instructions
                // string resource can be retrieve the next time the update
                // button is pressed
                mInstructionNumber++;
                break;
            case 1:
                text = getString(R.string.apn_app_text_instr2);
                styledText = Html.fromHtml(text);
                tv.setText(styledText);
                // Increment instruction number so the correct instructions
                // string resource can be retrieve the next time the update
                // button is pressed
                mInstructionNumber++;
                break;
            case 2:
                // Final set of instructions-Change to the corresponding layout

                setContentView(R.layout.assist_instructions);
                String assistUpdateInstr = String.format(
                        getString(R.string.apn_app_text_instr3), TotalSteps);
                styledText = Html.fromHtml(assistUpdateInstr);
                TextView assistInstrText = (TextView) findViewById(R.id.updated_text);
                assistInstrText.setText(styledText);
                mAssistInstrButton = (Button) findViewById(R.id.assist_instr_btn);

                mAssistInstrButton.setOnClickListener(this);

            }
        } else if (v == mAssistInstrButton) {
            // "LET'S DO THIS" Button in final instructions screen for ICS and
            // up is selected
            Values = getValues();
            startActivity(new Intent(Settings.ACTION_APN_SETTINGS));
            try {
                showNotification();
            } catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            finish();
        } else if (v == mAssistInstrButton) {
            startActivity(new Intent(Settings.ACTION_APN_SETTINGS));
            try {
                showNotification();
            } catch (SAXException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (ParserConfigurationException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            try {
                showNotification();
            } catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            finish();
        }
    }

    /*
     * Insert a new APN entry into the system APN table Require an apn name, and
     * the apn address. More can be added. Return an id (_id) that is
     * automatically generated for the new apn entry.
     */
    public int InsertAPN() throws SecurityException {

        int id = -1;
        for (i = 0; i < count; i++) {
            ContentValues values2 = new ContentValues();
            // values2 = values1;
            values2 = getValues();
            ContentResolver resolver = getContentResolver();
            Cursor c = null;
            try {
                Uri newRow = resolver.insert(APN_TABLE_URI, values2);
                // System.out.println("values in insertAPN" + values1);
                if (newRow != null) {
                    c = resolver.query(newRow, null, null, null, null);
                    Log.d(TAG, "Newly added APN:");
                    // TF Settings have been inserted
                    // Obtain the apn id
                    int idindex = c.getColumnIndex("_id");
                    c.moveToFirst();
                    id = c.getShort(idindex);

                    Log.d(TAG, "New ID: " + id
                            + ": Inserting new APN succeeded!");
                }
            } catch (SQLException e) {
                Log.d(TAG, e.getMessage());
            }
            if (c != null)
                c.close();
        }
        return id;

    }

    public ContentValues getValues() {
        ContentValues values = new ContentValues();

        values.put("name", nameArr.get(i));
        values.put("apn", ApnArr.get(i));
        values.put("mmsc", mmscArr.get(i));
        //values.put("mmsproxy", mmsproxyArr.get(i));
        //values.put("mmsport", mmsportArr.get(i));
        // values.put("proxy", proxyArr.get(i));
         values.put("port", portArr.get(i));
        values.put("mcc", (getString(R.string.mcc)));
        if ((tm.getSimOperator()).equals(getString(R.string.numeric_tmo))) {
            values.put("numeric", getString(R.string.numeric_tmo));
            values.put("mnc", (getString(R.string.mnc_tmo)));
        } else if ((tm.getSimOperator())
                .equals(getString(R.string.numeric_att))) {
            values.put("numeric", getString(R.string.numeric_att));
            values.put("mnc", (getString(R.string.mnc_att)));
        }
        return values;
    }
    /*
     * Delete APN data where the indicated field has the values Entire table is
     * deleted if both field and value are null
     */
    private void DeleteAPNs(String field, String[] values)
            throws SecurityException {
        int c = 0;
        c = getContentResolver().delete(APN_TABLE_URI, null, null);
        if (c != 0) {
            String s = "APNs Deleted:\n";
            Log.d(TAG, s);

        }

    }

    /*
     * Return all column names stored in the string array
     */
    private String getAllColumnNames(String[] columnNames) {
        String s = "Column Names:\n";
        for (String t : columnNames) {
            s += t + ":\t";
        }
        return s + "\n";
    }

    /*
     * Copy all data associated with the 1st record Cursor c. Return a
     * ContentValues that contains all record data.
     */
    private ContentValues copyRecordFields(Cursor c) {
        if (c == null)
            return null;
        int row_cnt = c.getCount();
        Log.d(TAG, "Total # of records: " + row_cnt);
        ContentValues values = new ContentValues();//
        if (c.moveToFirst()) {
            String[] columnNames = c.getColumnNames();
            Log.d(TAG, getAllColumnNames(columnNames));
            String row = "";
            for (String columnIndex : columnNames) {
                int i = c.getColumnIndex(columnIndex);
                row += c.getString(i) + ":\t";
                // if (i>0)//Avoid copying the id field
                // id to be auto-generated upon record insertion
                values.put(columnIndex, c.getString(i));
            }
            row += "\n";
            Log.d(TAG, row);
            Log.d(TAG, "End Of Records");
        }
        return values;
    }

    // showAlert displays the text contained in message as an alert
    public void showAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ConfigFinalActivity.this.finish();
            }
        });
        mErrorAlert = builder.create();
        mErrorAlert.show();
    }

    // showErrorAlert displays an alert with layout and a title
    private void showErrorAlert(int layoutRes, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = ConfigFinalActivity.this.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle(title)
        .setView(inflater.inflate(layoutRes, null))
        .setPositiveButton(getString(R.string.assisted_button),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                startActivity(new Intent(
                        Settings.ACTION_APN_SETTINGS));
                try {
                    showNotification();
                } catch (SAXException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        mErrorAlert = builder.create();
        mErrorAlert.show();
    }

    // showNotification starts the process of sending notifications to the bar
    // to assist the user in updating the data settings on ICS and later
    // versions of Android
    @SuppressWarnings("deprecation")    
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void showNotification() throws SAXException, ParserConfigurationException {

        String field = getString(R.string.config_name_label);

        String value = Values.get("name").toString();
        int mId = 1;
        String title = "1 of " + UpdateActivity.TotalSteps + " (Update "
                + field + ":)";
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notifications_icon)
                .setContentTitle(title).setContentText(value);
        Intent resultIntent = new Intent(this,
                NotificationActivityForMultiProf.class);
        resultIntent.putExtra(field, value);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotification = mBuilder.getNotification();
        mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(mId, mNotification);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
        if (mNotification != null) {
            outState.putString("NOTIFICATIONB", mNotification.toString());
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mErrorAlert != null)
            mErrorAlert.dismiss();
    }

    private int updateTable() throws IOException, SAXException,
    ParserConfigurationException {
        int insertResult = -1;
        // returned value if table is not properly updated

        try {

            ContentValues values = new ContentValues();
            // Query the carrier table for the current data settings
            Cursor c = getContentResolver().query(APN_TABLE_URI, null,
                    "current=?", new String[] { "1" }, null);
            values = copyRecordFields(c);
            // Copy the settings into values

            // Replace T-Mo/ATT Data settings if there is no SIM or
            // NET10/T-Mo/ATT SIM is
            // present
            if (tm.getSimState() == TelephonyManager.SIM_STATE_ABSENT
                    || (tm.getSimOperator())
                    .equals(getString(R.string.numeric_tmo))) {

                // delete all APNs before adding new APNs
                DeleteAPNs("numeric=?",
                        new String[] { getString(R.string.numeric_tmo) });
                // Insert Data Settings into Carrier table

                insertResult = InsertAPN();

            } else if (tm.getSimState() == TelephonyManager.SIM_STATE_ABSENT
                    || (tm.getSimOperator())
                    .equals(getString(R.string.numeric_att))) {
                // Delete all APNs before adding new APNs
                DeleteAPNs("numeric=?",
                        new String[] { getString(R.string.numeric_att) });
                // Insert Data Settings into Carrier table

                insertResult = InsertAPN();

            } else
                // non NET10/ non T-Mo SIM/non ATT SIM
                showAlert(getString(R.string.insert_sm_dialog));
        } catch (SecurityException e) {
            showErrorAlert(R.layout.assisted_setting,
                    getString(R.string.assited_title));
            Log.d(TAG, e.getMessage());
        }
        return insertResult;
    }

    private void completeUpdate() {
        // Displaying final layout after pre-ICS automatic settings update
        setContentView(R.layout.completion);
        TextView mCompleted = (TextView) findViewById(R.id.done_text);
        String mDoneText = String.format(getString(R.string.done_text));
        CharSequence styledText = Html.fromHtml(mDoneText);

        mCompleted.setText(styledText);
        mXButton = (Button) findViewById(R.id.x_button);
        mXButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

     public void tryagain() {
     // Displaying final layout after failure of pre-ICS automatic settings
     // update
     setContentView(R.layout.tryagain);
     String tryAgainText = "";
     CharSequence styledTryAgainText;

     tryAgainText = String.format(getString(R.string.tryagain_text1),
     TotalSteps);
     styledTryAgainText = Html.fromHtml(tryAgainText);
     TextView tryAgain1 = (TextView) findViewById(R.id.tryagain_text1);
     tryAgain1.setText(styledTryAgainText);

     tryAgainText = String.format(getString(R.string.tryagain_text2),
     TotalSteps);
     styledTryAgainText = Html.fromHtml(tryAgainText);
     TextView tryAgain2 = (TextView) findViewById(R.id.tryagain_text2);
     tryAgain2.setText(styledTryAgainText);

     tryAgainText = String.format(getString(R.string.tryagain_text3),
     TotalSteps);
     styledTryAgainText = Html.fromHtml(tryAgainText);
     TextView tryAgain3 = (TextView) findViewById(R.id.tryagain_text3);
     tryAgain3.setText(styledTryAgainText);

     }

    // This function return a cursor to the table holding the
    // the APN configurations (Carrier table)
    public Cursor getConfigTableCursor() {
        return getContentResolver()
                .query(APN_TABLE_URI, null, null, null, null);
    }

}