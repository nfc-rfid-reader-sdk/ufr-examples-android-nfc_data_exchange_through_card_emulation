package net.dlogic.android.nfc.nfctagemudataexchange;

import android.app.Activity;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.view.Menu;
import java.io.IOException;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static NfcAdapter mAdapter;
    private static PendingIntent mPendingIntent;
    private static IntentFilter[] mFilters;
    private static String[][] mTechLists;

    private EditText et1, et2, et3, et4, et5, et6;
    private EditText et11, et12, et13, et14, et15, et16;

    private TextView info, errors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        info = (TextView) findViewById(R.id.info);

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mAdapter == null) {
            info.setText("No NFC adapter");
        } else {
            mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

            try {
                ndef.addDataType("*/*");
            } catch (MalformedMimeTypeException e) {
                throw new RuntimeException("fail", e);
            }

            mFilters = new IntentFilter[] { ndef, };

            mTechLists = new String[][] { new String[] { NfcA.class.getName() } };

            Intent intent = getIntent();

            resolveIntent(intent);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null == mAdapter) {
            return;
        }
        mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i("Foreground dispatch", "Discovered tag with intent: " + intent);
        resolveIntent(intent);
    }

    /**
     * Resolves intent after discovered mifare tag
     * @param intent
     */
    public void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (null != mAdapter) {
            Boolean card_exists = NfcAdapter.ACTION_TECH_DISCOVERED.equals(action);

            if (card_exists) {

                Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                NfcA tag = NfcA.get(tagFromIntent);
                byte[] data = {};
                byte[] data_block = {};
                byte[] block_num = {0x30, 0x30};
                byte[] dataSizeLeft = {(byte)'\r', (byte)'\n', (byte)'['};
                byte[] dataSizeRight = {(byte)']', (byte)':', (byte)' '};
                byte[] tag_cmd = {0x30, 0};

                int blocks;

                try {

                    tag.connect();

                    for (blocks = 0; blocks < 16; blocks += 4) {
                        tag_cmd[1] = (byte)blocks;

                        data_block = tag.transceive(tag_cmd);
                        data = concatenateByteArrays(data, dataSizeLeft);
                        if (data_block.length < 10) {
                            block_num[1] = (byte)(0x30 + data_block.length);
                        } else {
                            block_num[0] = (byte)(0x30 + data_block.length / 10);
                            block_num[1] = (byte)(0x30 + data_block.length % 10);
                        }
                        data = concatenateByteArrays(data, block_num);
                        data = concatenateByteArrays(data, dataSizeRight);
                        data = concatenateByteArrays(data, data_block);
                    }

                    preview(data);

                } catch (IOException e) {
                    preview(e.getLocalizedMessage());
                }
            }
        }
    }

     /**
     * Concatenates arrays of bytes
     * @param a
     * @param b
     * @return byte[] Concatenated bytes
     */
    byte[] concatenateByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    /**
     * Sets info text from array of bytes
     * @param data
     */
    public void preview(byte[] data) {
        String text = new String(data);

        info.setText(text);
    }

    /**
     * Sets info text from text string
     * @param text
     */
    public void preview(String text) {

        info.setText(text);
    }

}
