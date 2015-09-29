package net.dlogic.android.nfc.nfctagemudataexchange;

import android.app.Activity;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import java.io.IOException;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static NfcAdapter mAdapter;
    private static PendingIntent mPendingIntent;
    private static IntentFilter[] mFilters;
    private static String[][] mTechLists;
    private EditText inputText;
    private TextView info;

    public NfcA tag;
    public boolean tag_connected=false;
    public byte[] previous_received_data;
    public int message_to_send=1;

    public int global_counter=0;

    public DispParam display;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        info = (TextView) findViewById(R.id.info);
        inputText = (EditText) findViewById(R.id.editCustomMessage);
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

        CountDownTimer remainingTimeCounter = new CountDownTimer(300000000, 10) {

            public void onTick(long millisUntilFinished) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (tag_connected){
                            try {
                                ReadDataEmuCard(display, (byte) 0, (byte) 50, tag);
                            } catch (IOException e){
                                tag_connected=false;
                        //        byte[] new_line = {(byte)'\r', (byte)'\n'};
                            //    preview(e.getLocalizedMessage());
                            //    display.display_data = concatenateByteArrays(display.display_data, new_line);
                                String empty=" ";
                             //   display.display_data = concatenateByteArrays(display.display_data, e.toString().getBytes());
                                display.display_data = empty.toString().getBytes();
                                preview(display.display_data);
                            }
                        }
                    }
                });
            }

            public void onFinish() {
                this.start();
            }
        }.start();

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
    //    disp_data.display_data = concatenateByteArrays(disp_data.display_data, "[".toString().getBytes());
    //    disp_data.display_data = concatenateByteArrays(disp_data.display_data, new Integer(received_data.length).toString().getBytes());
    //    disp_data.display_data = concatenateByteArrays(disp_data.display_data, "] : ".toString().getBytes());

        if (received_data.length > 1) {
        //    disp_data.display_data = concatenateByteArrays(disp_data.display_data, received_data);

            boolean not_equal_array=false;
            if (previous_received_data.length <= received_data.length) {
                for (int f = 0; f < previous_received_data.length; f++) {
                    Integer j = (int) received_data[f];
                    Integer k = (int) previous_received_data[f];
                    if (j!=k){
                        not_equal_array=true;
                        break;
                    }
                }
            }
            if (not_equal_array) {

                disp_data.display_data = received_data;
         //       disp_data.display_data = concatenateByteArrays(disp_data.display_data, "\r\n".toString().getBytes());
          //      disp_data.display_data = concatenateByteArrays(disp_data.display_data, previous_received_data);
                previous_received_data=received_data;
                Integer i = (int) received_data[0];
                if (i == 0) {
                    String iamandroid="";
                    if (message_to_send==1){
                        iamandroid="Hey PC where are you?";
                    }
                    if (message_to_send==2){
                        iamandroid="I am Android phone";
                    }
                    if (message_to_send==3){
                        iamandroid=inputText.getText().toString();
                    }
                    byte[] tmp=iamandroid.toString().getBytes();


            //        tmp = {(byte) 'I', (byte) ' ', (byte) 'a', (byte) 'm', (byte) ' ', (byte) 'a', (byte) 'n', (byte) 'd', (byte) 'r', (byte) 'o', (byte) 'i', (byte) 'd',
            //                (byte) ' ', (byte) 'p', (byte) 'h', (byte) 'o', (byte) 'n', (byte) 'e'};
                    previous_received_data = tmp;
                    WriteDataEmuCard(disp_data, (byte) 0, tmp, tag);
                }
                preview(display.display_data);
            }
        } else {
            Integer i = (int)received_data[0];
            if (i==-31){
             //   disp_data.display_data = new byte[]{(byte) 'O', (byte) 'K'};
             //    preview(display.display_data);
            } else {
                if (i==-32){
                    disp_data.display_data = new byte[]{(byte) 'W', (byte) 'r', (byte) 'i', (byte) 't', (byte) 'e', (byte) ' ',(byte) 'f', (byte) 'a', (byte) 'i', (byte) 'l'};
                    preview(display.display_data);
                } else {
                    disp_data.display_data = concatenateByteArrays(disp_data.display_data, i.toString().getBytes());
                }
            }
        }
    //    disp_data.display_data = concatenateByteArrays(disp_data.display_data, "\r\n".toString().getBytes());
    }

    public void WriteDataEmuCard(DispParam disp_data, byte address ,byte[] writeStream, NfcA tag) throws IOException {
        byte byWrCmd = (byte)0xEA;
        byte[] wr_cmd = {byWrCmd, address, (byte) writeStream.length};
        wr_cmd = concatenateByteArrays(wr_cmd, writeStream);
        TransceiveEmuCard(disp_data, wr_cmd, tag);
    }

    public void ReadDataEmuCard(DispParam disp_data, byte address ,byte readLength, NfcA tag) throws IOException {
        byte byRdCmd = (byte)0xEB;
        byte[] rd_cmd = {byRdCmd, address, readLength};
        TransceiveEmuCard(disp_data, rd_cmd, tag);
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
                tag = NfcA.get(tagFromIntent);
;
                display = new DispParam();
                preview(display.display_data);

                byte[] new_line = {(byte)'\r', (byte)'\n'};
/*
                byte byWrCmd = (byte)0xEA;
                byte byRdCmd = (byte)0xEB;
                byte byResponseOk = (byte)0xE1;
                byte byResponseFail = (byte)0xE0;
                byte[] wr_cmd = {byWrCmd, 0, 18};
                byte[] wr_cmdA = {byWrCmd, 0, 60};
                byte[] wr_cmd2 = {byWrCmd, 16, 20};
                byte[] rd_cmd = {byRdCmd, 0, 60};
*/
                int blocks;

                /*
                for (byte i = 0; i < 60; i++) {
                    byte[] tmp = {(byte)(i + 64)};
                    wr_cmd = concatenateByteArrays(wr_cmd, tmp);
                }
                */

            //    byte [] tmp = {(byte)'I',(byte)' ', (byte)'a', (byte)'m', (byte)' ', (byte)'a', (byte)'n', (byte)'d', (byte)'r', (byte)'o', (byte)'i', (byte)'d',
            //            (byte)' ', (byte)'p', (byte)'h', (byte)'o', (byte)'n', (byte)'e'};

                byte [] tmp = {(byte)'0'};

                previous_received_data = tmp;
               /*    wr_cmd = concatenateByteArrays(wr_cmd, tmp);



                for (byte i = 0; i < 20; i++) {
                    byte[] tmp1 = {(byte)64};
                    wr_cmd2 = concatenateByteArrays(wr_cmd2, tmp1);
                }*/

                try {

                    tag.connect();

                    tag_connected=true;

           //         WriteDataEmuCard(display,(byte)0, tmp, tag);

           //         ReadDataEmuCard(display, (byte)0, (byte)33, tag);
                   /* TransceiveEmuCard(display, wr_cmd, tag);
                    /*
                    TransceiveEmuCard(display, wr_cmdA, tag);
                    TransceiveEmuCard(display, rd_cmd, tag);

                    TransceiveEmuCard(display, wr_cmd, tag);
                    TransceiveEmuCard(display, rd_cmd, tag);

                    TransceiveEmuCard(display, wr_cmd2, tag);
                    TransceiveEmuCard(display, rd_cmd, tag);
*/

              //      preview(display.display_data);

                } catch (IOException e) {
               //     preview(e.getLocalizedMessage());
                //    display.display_data = concatenateByteArrays(display.display_data, new_line);
               //     display.display_data = concatenateByteArrays(display.display_data, e.toString().getBytes());
               //     preview(display.display_data);
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

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.rbMessage1:
                if (checked)
                    message_to_send=1;
                    break;
            case R.id.radioButton2:
                if (checked)
                    message_to_send=2;
                    break;
            case R.id.radioButton3:
                if (checked)
                    message_to_send=3;
                    break;
        }
    }

}
