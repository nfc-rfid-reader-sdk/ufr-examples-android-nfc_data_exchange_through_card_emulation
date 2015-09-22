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

    private TextView info;

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

    public class DispParam {
        public byte[] display_data;

        DispParam() {
            display_data = new byte[]{};
        }
    }

    public void TransceiveEmuCard(DispParam disp_data, byte[] writeStream, NfcA tag) throws IOException {

        byte[] received_data;

        received_data = tag.transceive(writeStream);
        disp_data.display_data = concatenateByteArrays(disp_data.display_data, "[".toString().getBytes());
        disp_data.display_data = concatenateByteArrays(disp_data.display_data, new Integer(received_data.length).toString().getBytes());
        disp_data.display_data = concatenateByteArrays(disp_data.display_data, "] : ".toString().getBytes());

        if (received_data.length > 1) {
            disp_data.display_data = concatenateByteArrays(disp_data.display_data, received_data);
        } else {
            Integer i = (int)received_data[0];
            disp_data.display_data = concatenateByteArrays(disp_data.display_data, i.toString().getBytes());
        }
        disp_data.display_data = concatenateByteArrays(disp_data.display_data, "\r\n".toString().getBytes());
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
;
                DispParam display = new DispParam();
                preview(display.display_data);

                byte[] new_line = {(byte)'\r', (byte)'\n'};

                byte byWrCmd = (byte)0xEA;
                byte byRdCmd = (byte)0xEB;
                byte byResponseOk = (byte)0xE1;
                byte byResponseFail = (byte)0xE0;
                byte[] wr_cmd = {byWrCmd, 0, 60};
                byte[] wr_cmdA = {byWrCmd, 0, 60};
                byte[] wr_cmd2 = {byWrCmd, 16, 20};
                byte[] rd_cmd = {byRdCmd, 0, 60};

                int blocks;

                for (byte i = 0; i < 60; i++) {
                    byte[] tmp = {(byte)(i + 64)};
                    wr_cmd = concatenateByteArrays(wr_cmd, tmp);
                }

                for (byte i = 0; i < 60; i++) {
                    byte[] tmp = {(byte)60};
                    wr_cmdA = concatenateByteArrays(wr_cmdA, tmp);
                }

                for (byte i = 0; i < 20; i++) {
                    byte[] tmp = {(byte)64};
                    wr_cmd2 = concatenateByteArrays(wr_cmd2, tmp);
                }

                try {

                    tag.connect();

                    TransceiveEmuCard(display, wr_cmdA, tag);
                    TransceiveEmuCard(display, rd_cmd, tag);

                    TransceiveEmuCard(display, wr_cmd, tag);
                    TransceiveEmuCard(display, rd_cmd, tag);

                    TransceiveEmuCard(display, wr_cmd2, tag);
                    TransceiveEmuCard(display, rd_cmd, tag);

                    preview(display.display_data);

                } catch (IOException e) {
                    preview(e.getLocalizedMessage());
                    display.display_data = concatenateByteArrays(display.display_data, new_line);
                    display.display_data = concatenateByteArrays(display.display_data, e.toString().getBytes());
                    preview(display.display_data);
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
