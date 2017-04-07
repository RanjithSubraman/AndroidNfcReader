package com.goovis.androidnfcreader;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.goovis.androidnfcreader.controller.NfcReader;

import static com.goovis.androidnfcreader.util.Util.bin2hex;

public class NfcReaderActivity extends AppCompatActivity {
    private String TAG = NfcReaderActivity.class.getSimpleName();
    private static final String MIME_TEXT_PLAIN = "text/plain";

    private boolean isOnNewIntent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_reader);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        isOnNewIntent = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isOnNewIntent) {
            setupForegroundDispatch(this);
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            stopForegroundDispatch(this);
        }

        super.onPause();
    }

    /**
     * @param activity
     * The corresponding {@link Activity} requesting the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity) {
        final Intent intent = new Intent(activity, activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity,
                0, intent, 0);

        IntentFilter[] filters = new IntentFilter[3];
        String[][] techList = new String[][] {
                { "android.nfc.tech.Ndef" },
                { "android.nfc.tech.IsoDep" },
                { "android.nfc.tech.NfcA" },
                { "android.nfc.tech.NfcB" },
                { "android.nfc.tech.NfcF" },
                { "android.nfc.tech.NfcV" },
                { "android.nfc.tech.NdefFormatable" },
                { "android.nfc.tech.MifareClassic" },
                { "android.nfc.tech.MifareUltralight" },
                { "android.nfc.tech.NfcBarcode" }
        };

        /*
         * Notice that this is the same filter as in our manifest if we want to
         * create a new activity without calling onNewIntent().
         */
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);

        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        }
        catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        /*
         * Notice that this is the same filter as in our manifest if we want to
         * create a new activity without calling onNewIntent().
         */
        filters[1] = new IntentFilter();
        filters[1].addAction(NfcAdapter.ACTION_TECH_DISCOVERED);

        filters[2] = new IntentFilter();
        filters[2].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);

        adapter.enableForegroundDispatch(activity, pendingIntent, filters,
                techList);
        Log.d("Profiler",
                "YastaNfcActivity | NfcAdapter.enableForegroundDispatch called");
    }

    /**
     * @param activity
     * The corresponding requesting to stop the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity) {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
        adapter.disableForegroundDispatch(activity);
        Log.d("Profiler",
                "YastaNfcActivity | NfcAdapter.disableForegroundDispatch called");
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        Log.d("Profiler", "YastaNfcActivity | action = " + action);

        boolean checkNdef = false;

        printTagInfo(intent);

        if (action != null) {
            /*
            * Right now profiler supports only NDEF technology, So please enable ndef"
            */
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
                checkNdef = true;
            } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                String[] techList = tag.getTechList();
                String searchedTech = Ndef.class.getName();

                if (techList != null) {
                    for (String tech : techList) {
                        if (searchedTech.equals(tech)) {
                            checkNdef = true;

                            break;
                        }
                    }
                }
            } else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
                checkNdef = true;
            }

            if (checkNdef) {
                NdefAsyncTask ndefAsyncTask = new NdefAsyncTask();
                ndefAsyncTask.execute(intent);
            }
        } else {
            Log.d(TAG, "Intent is null, So please adjust the card on correct position");
        }
    }

    private void printTagInfo(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (tag != null) {
            printTagUid(tag);
            printTagTechList(tag);
            printTagType(tag);
        }
    }

    private void printTagUid(Tag tag) {
        byte[] uid = tag.getId();
        Log.d(TAG, "UID" + bin2hex(uid));
    }

    private void printTagTechList(Tag tag) {
        String prefix = "android.nfc.tech.";
        String[] techList = tag.getTechList();

        if (techList != null) {
            String techListConcat = "";
            techListConcat += techList[0].substring(prefix.length());

            for (int i = 1; i < techList.length; i++) {
                techListConcat += ("," + techList[i].substring(prefix.length()));
            }

            Log.d(TAG, "Technologies" + techListConcat);
        }
    }

    private void printTagType(Tag tag) {
        String type = "Unknown";
        String[] techList = tag.getTechList();

        if (techList != null) {
            for (String tech : techList) {
                if (tech.equals(MifareClassic.class.getName())) {
                    MifareClassic mifareClassicTag = MifareClassic.get(tag);
                    // Type Info of MifareClassic
                    switch (mifareClassicTag.getType()) {
                        case MifareClassic.TYPE_CLASSIC:
                            type = "Classic";
                            break;
                        case MifareClassic.TYPE_PLUS:
                            type = "Plus";
                            break;
                        case MifareClassic.TYPE_PRO:
                            type = "Pro";
                            break;
                    }

                    Log.d(TAG, "Type :" + "Mifare " + type);
                }
                else if (tech.equals(MifareUltralight.class.getName())) {
                    MifareUltralight mifareUlTag = MifareUltralight.get(tag);

                    // Type Info of MifareUltralight
                    switch (mifareUlTag.getType()) {
                        case MifareUltralight.TYPE_ULTRALIGHT:
                            type = "Ultralight";
                            break;
                        case MifareUltralight.TYPE_ULTRALIGHT_C:
                            type = "Ultralight C";
                            break;
                    }

                    Log.d(TAG, "Type :" + "Mifare " + type);
                }
            }
        }
    }

    private class NdefAsyncTask extends AsyncTask<Intent, Void, String> {
        NfcReader nfcReader = new NfcReader();

        @Override
        protected String doInBackground(Intent... params) {
            Log.d("Profiler",
                    "YastaNfcActivity | doInBackground(Intent... params)");

            String result = "doInBackground() method is executed";
            Intent intent = params[0];

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            Ndef ndef = Ndef.get(tag);

            if (ndef != null) {
                nfcReader.getNdefTagLog(ndef);
                nfcReader.readTag(ndef, false);
            }
            else {
                Log.e(TAG, "NDEF Technology is not supported in this tag");
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }

}
