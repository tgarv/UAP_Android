package com.example.nfcmessenger;

import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {
	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;
	private IntentFilter[] mFilters;
    private String[][] mTechLists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        
        NdefMessage mMessage = new NdefMessage(NdefRecord.createUri("http://www.android.com"));
        mAdapter.setNdefPushMessage(mMessage, this);
        
        Log.i("NFC", mAdapter.toString());
        
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        mFilters = new IntentFilter[] {
                ndef,
        };

        // Setup a tech list for all NfcF tags
        mTechLists = new String[][] { new String[] { NfcF.class.getName() } };
        
        // Now, check if the application was launched by an Android Application Record (AAR), which means that it
        // was launched by a tag and we should call onNewIntent to force the app to behave like it does when it's
        // already open when a tag is read.
        Bundle extras = getIntent().getExtras();
        if (extras != null){
        	if (extras.containsKey("android.nfc.extra.TAG")) {
        		// This means the app was launched by an AAR, so we should process the intent like we would when resuming.
        		onNewIntent(getIntent());
        	}
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null){
        	mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i("Foreground dispatch", "Discovered tag with intent: " + intent);
        Log.i("NFC", intent.getExtras().keySet().toString());
        for(String k : intent.getExtras().keySet()){
        	Log.i("NFC", intent.getExtras().get(k).toString());
        }
        
        Parcelable[] p = intent.getExtras().getParcelableArray("android.nfc.extra.NDEF_MESSAGES");
        if(p != null){
        	for(Parcelable parcel : p){
        		NdefMessage ndef = (NdefMessage) parcel;
        		Log.i("NFC", "NDEF message: " + ndef.toString());
        		String payload = "";
        		try {
					payload = new String(ndef.getRecords()[0].getPayload(), 0, ndef.getRecords()[0].getPayload().length, "ASCII");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		Log.i("NFC", "Message payload: " + payload);
        		((TextView)findViewById(R.id.textView1)).setText(payload);
        	}
        }
        else
        	Log.i("NFC", "There are no NDEF messages");
//        Log.i("NFC", "Extras: " + intent.getExtras());
//        mText.setText("Discovered tag " + ++mCount + " with intent: " + intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAdapter != null) mAdapter.disableForegroundDispatch(this);
    }
    
}
