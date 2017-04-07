package com.goovis.androidnfcreader.controller;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.Ndef;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ranjithsubramaniam on 4/6/17.
 */

public class NfcReader {
    private String TAG = NfcReader.class.getSimpleName();

    public void getNdefTagLog(Ndef ndef) {
        Log.d("Profiler", "YastaNfcActivity | void getNdefTagLog()");

        String maxSize = String.valueOf(ndef.getMaxSize());
        Log.d(TAG, "Maximum NDEF message size (Bytes)" + maxSize);

        String isWritable = String.valueOf(ndef.isWritable());
        Log.d(TAG, "Tag is writable" + isWritable);

        String canMakeReadOnly = String.valueOf(ndef.canMakeReadOnly());
        Log.d(TAG, "Tag can be made read-only" + canMakeReadOnly);
    }

    public void readTag(Ndef ndef, boolean isReadFromCache) {
        if (isReadFromCache) {
            readFromCache(ndef);
        } else {
            readFromTag(ndef);
        }
    }

    private void readFromCache(Ndef ndef) {
        Log.d("Profiler", "YastaNfcActivity | readFromCache()");

        NdefMessage ndefMessage = ndef.getCachedNdefMessage();

        if (ndefMessage != null) {
            getRecordInfo(ndefMessage);
        } else {
            Log.d(TAG, "Cache is empty");
        }
    }

    private void readFromTag(Ndef ndef) {
        Log.d("Profiler", "YastaNfcActivity | readFromTag()");

        NdefMessage readMessage;
        long startReadTime;
        long endReadTime;

        try {
            ndef.connect();
            startReadTime = SystemClock.uptimeMillis();
            readMessage = ndef.getNdefMessage();
            endReadTime = SystemClock.uptimeMillis();
            Log.d(TAG, "Read operation time" + String.valueOf(endReadTime - startReadTime) + " ms");

            if (readMessage != null) {
                getRecordInfo(readMessage);
            }
            else {
                Log.d(TAG, "Read NdefMessage is null");
            }
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        finally {
            try {
                if (ndef.isConnected()) {
                    ndef.close();
                }
            }
            catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    private void getRecordInfo(NdefMessage ndefMessage) {
        NdefRecord[] records = ndefMessage.getRecords();

        Log.d(TAG, "Number of records" + String.valueOf(records.length));

        // getByteArrayLength() is supporting API 16 onwards
        Log.d(TAG, "Message size" + String.valueOf(ndefMessage.getByteArrayLength()));

        for (NdefRecord ndefRecord : records) {
            Log.d(TAG, "TNF type" + getTnfResult(ndefRecord.getTnf()));
            Log.d(TAG, "Type" + getTypeResult(ndefRecord.getType()));
            Log.d(TAG,"Payload length" + String.valueOf(ndefRecord.getPayload().length));

            if ((ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN)) {
                try {
                    if (Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                        Log.d(TAG,"Payload data" + readText(ndefRecord));
                    }
                    else if (Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_URI)) {
                        Log.d(TAG,"Payload data" + readWellKnownUri(ndefRecord));
                    }
                    else if (Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_SMART_POSTER)) {
                        List<String> recordData = readSmartPoster(ndefRecord);

                        for (String recData : recordData) {
                            Log.d(TAG, "Payload data" + recData);
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG,"Unsupported Encoding" + Log.getStackTraceString(e));
                }
            } else {
                try {
                        /*
                         * This will print other types of data (MIME_MEDIA,
                         * EMPTY, UNKNOWN, UNCHANGED etc). It may print non
                         * text characters as well.
                         */
                    Log.e(TAG,"Payload data" + new String(ndefRecord.getPayload(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Unsupported Encoding" + Log.getStackTraceString(e));
                }
            }
        }
    }

    private String getTnfResult(short tnf) {
        Log.d("Profiler", "YastaNfcActivity | getTnfResult()");

        String sResult;

        switch (tnf) {
            case NdefRecord.TNF_ABSOLUTE_URI:
                sResult = "TNF_ABSOLUTE_URI";

                break;

            case NdefRecord.TNF_EMPTY:
                sResult = "TNF_EMPTY";

                break;

            case NdefRecord.TNF_EXTERNAL_TYPE:
                sResult = "TNF_EXTERNAL_TYPE";

                break;

            case NdefRecord.TNF_MIME_MEDIA:
                sResult = "TNF_MIME_MEDIA";

                break;

            case NdefRecord.TNF_UNCHANGED:
                sResult = "TNF_UNCHANGED";

                break;

            case NdefRecord.TNF_UNKNOWN:
                sResult = "TNF_UNKNOWN";

                break;

            case NdefRecord.TNF_WELL_KNOWN:
                sResult = "TNF_WELL_KNOWN";

                break;

            default:
                sResult = "Undefined TNF value of " + tnf + " returned";

                break;
        }

        return sResult;
    }

    private String getTypeResult(byte[] type) {
        Log.d("Profiler", "YastaNfcActivity | getTypeResult()");

        String sResult;

        if (Arrays.equals(type, NdefRecord.RTD_ALTERNATIVE_CARRIER)) {
            sResult = "RTD_ALTERNATIVE_CARRIER";
        } else if (Arrays.equals(type, NdefRecord.RTD_HANDOVER_CARRIER)) {
            sResult = "RTD_HANDOVER_CARRIER";
        } else if (Arrays.equals(type, NdefRecord.RTD_HANDOVER_REQUEST)) {
            sResult = "RTD_HANDOVER_REQUEST";
        } else if (Arrays.equals(type, NdefRecord.RTD_HANDOVER_SELECT)) {
            sResult = "RTD_HANDOVER_SELECT";
        } else if (Arrays.equals(type, NdefRecord.RTD_SMART_POSTER)) {
            sResult = "RTD_SMART_POSTER";
        } else if (Arrays.equals(type, NdefRecord.RTD_TEXT)) {
            sResult = "RTD_TEXT";
        } else if (Arrays.equals(type, NdefRecord.RTD_URI)) {
            sResult = "RTD_URI";
        } else {
            sResult = new String(type, Charset.forName("UTF-8"));
        }

        return sResult;
    }

    private String readText(NdefRecord record)
            throws UnsupportedEncodingException {
        Log.d("Profiler", "YastaNfcActivity | readText()");

        byte[] payload = record.getPayload();

        // Get the Text Encoding
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

        // Get the Language Code
        int languageCodeLength = payload[0] & 0063;

        // Get the Text
        return new String(payload, languageCodeLength + 1,
                payload.length - languageCodeLength - 1, textEncoding);
    }

    private String readWellKnownUri(NdefRecord record) {
        Log.d("Profiler", "YastaNfcActivity | readWellKnownUri()");

        byte[] payload = record.getPayload();

        String prefix = "";

        if (URI_PREFIX_MAP.length > (int) payload[0]) {
            prefix = URI_PREFIX_MAP[(int) payload[0]];
        }

        return prefix.concat(new String(payload, 1, payload.length - 1,
                Charset.forName("UTF-8")));
    }

    private List<String> readSmartPoster(NdefRecord record) {
        List<String> data = new ArrayList<>();

        try {
            NdefMessage subRecords = new NdefMessage(record.getPayload());

            for (NdefRecord rec : subRecords.getRecords()) {
                if (Arrays.equals(rec.getType(), NdefRecord.RTD_URI)) {
                    data.add(readWellKnownUri(rec));
                } else if (Arrays.equals(rec.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        data.add(readText(rec));
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                }
            }
        } catch (FormatException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return data;
    }

    private static final String[] URI_PREFIX_MAP = new String[] {
            "", // 0x00
            "http://www.", // 0x01
            "https://www.", // 0x02
            "http://", // 0x03
            "https://", // 0x04
            "tel:", // 0x05
            "mailto:", // 0x06
            "ftp://anonymous:anonymous@", // 0x07
            "ftp://ftp.", // 0x08
            "ftps://", // 0x09
            "sftp://", // 0x0A
            "smb://", // 0x0B
            "nfs://", // 0x0C
            "ftp://", // 0x0D
            "dav://", // 0x0E
            "news:", // 0x0F
            "telnet://", // 0x10
            "imap:", // 0x11
            "rtsp://", // 0x12
            "urn:", // 0x13
            "pop:", // 0x14
            "sip:", // 0x15
            "sips:", // 0x16
            "tftp:", // 0x17
            "btspp://", // 0x18
            "btl2cap://", // 0x19
            "btgoep://", // 0x1A
            "tcpobex://", // 0x1B
            "irdaobex://", // 0x1C
            "file://", // 0x1D
            "urn:epc:id:", // 0x1E
            "urn:epc:tag:", // 0x1F
            "urn:epc:pat:", // 0x20
            "urn:epc:raw:", // 0x21
            "urn:epc:", // 0x22
    };

}
