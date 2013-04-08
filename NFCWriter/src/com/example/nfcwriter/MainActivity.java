package com.example.nfcwriter;

import java.io.IOException;
import java.nio.charset.Charset;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.EditText;
import android.widget.Toast;

// Much of this code is borrowed from http://tapintonfc.blogspot.com/2012/07/the-above-footage-from-our-nfc-workshop.html
public class MainActivity extends Activity {
    private NfcAdapter mNfcAdapter;  
    private IntentFilter[] mWriteTagFilters;  
    private PendingIntent mNfcPendingIntent;    
    private Context context;  

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        context = getApplicationContext();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,  
                  getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP  
                  | Intent.FLAG_ACTIVITY_CLEAR_TOP), 0);
        
        // Intent filters for writing to a tag
        IntentFilter discovery=new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mWriteTagFilters = new IntentFilter[] { discovery };
        
        ((EditText) findViewById(R.id.editText1)).setText("<foo>text='hello, world!' id='1'</foo>");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		if (mNfcAdapter != null) {
			if (mNfcAdapter.isEnabled()) {
				// Enable foreground dispatch, so that this app will receive any NFC tag notifications before
				// being passed of to Android's tag dispatch system.
				mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
			} else {
				Toast.makeText(context, "Sorry, No NFC Adapter found.", Toast.LENGTH_SHORT).show();
			}
		}
	}
    
    @Override  
    protected void onPause() {  
         super.onPause();
         // Turn off foreground dispatch when the app is paused, so that other apps will be able to read NFC tags.
         if(mNfcAdapter != null) mNfcAdapter.disableForegroundDispatch(this);  
    }
    
	@Override
	protected void onNewIntent(Intent intent) {
		Log.i("", "New intent");
		super.onNewIntent(intent);
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
			// validate that this tag can be written
			Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			if (supportedTechs(detectedTag.getTechList())) {
				// check if tag is writable (to the extent that we can
				if (writableTag(detectedTag)) {
					// writeTag here
					String text = ((EditText) findViewById(R.id.editText1)).getText().toString();
					WriteResponse wr = writeTag(getNdefMessage(text, true), detectedTag);
					String message = (wr.getStatus() == 1 ? "Success: " : "Failed: ") + wr.getMessage();
					Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(context, "This tag is not writable", Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(context, "This tag type is not supported", Toast.LENGTH_SHORT).show();
			}
		}
	}
    
	/**
	 * Given and message and an NFC tag, tries to write the message in NDEF format to the tag. Does not write
	 * if the message is too large for the tag or if the tag doesn't support NDEF.
	 * @param message the NdefMessage to write to the tag
	 * @param tag the NFC tag to write the message to
	 * @return WriteResponse with the response text and status information after the write has finished or failed.
	 */
	public WriteResponse writeTag(NdefMessage message, Tag tag) {
		int messageLength = message.toByteArray().length;
		String responseText = "";
		
		try {
			Ndef ndef = Ndef.get(tag);	// Get the NDEF object from the tag
			if (ndef != null) {
				ndef.connect();
				if (!ndef.isWritable()) {	// If the tag is not writable, return a failure response
					return new WriteResponse(0, "Tag is read-only");
				}
				
				Log.i("NFC", "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + messageLength + " bytes");
				if (ndef.getMaxSize() < messageLength) { // Check if the tag can fit the whole message
					responseText = "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + messageLength + " bytes.";
					return new WriteResponse(0, responseText);	// Return a failure response message
				}
				ndef.writeNdefMessage(message);
				responseText = "Wrote message to pre-formatted tag.";
				return new WriteResponse(1, responseText);
			} else {	// The tag is not formatted for NDEF yet
				NdefFormatable format = NdefFormatable.get(tag);
				if (format != null) {
					try {
						format.connect();
						format.format(message);
						responseText = "Formatted tag and wrote message";
						return new WriteResponse(1, responseText);
					} catch (IOException e) {
						responseText = "Failed to format tag.";
						return new WriteResponse(0, responseText);
					}
				} else {
					responseText = "Tag doesn't support NDEF.";
					return new WriteResponse(0, responseText);
				}
			}
		} catch (Exception e) {
			responseText = "Failed to write tag";
			return new WriteResponse(0, responseText);
		}
	}

	/**
	 * Response returned by writeTag, indicates the status (success/failure) of the write (1 for success, 0
	 * for failure) and contains a response message from the write.
	 * @author tgarv
	 *
	 */
	private class WriteResponse {
		int status;
		String message;

		WriteResponse(int Status, String Message) {
			this.status = Status;
			this.message = Message;
		}

		public int getStatus() {
			return status;
		}

		public String getMessage() {
			return message;
		}
	}

	/**
	 * Checks to see if all of the required technologies are included in techs
	 * @param techs The technology list to check if all of the required technologies are supported
	 * @return true if all the required technologies are supported, false otherwises
	 */
	public static boolean supportedTechs(String[] techs) {
		for (String tech : techs){
			Log.i("NFC", tech);
		}
		boolean mifareClassic = false;
		boolean nfcA = false;
		boolean ndef = false;
		for (String tech : techs) {
			if (tech.equals("android.nfc.tech.MifareClassic")) {
				mifareClassic = true;
			} else if (tech.equals("android.nfc.tech.NfcA")) {
				nfcA = true;
			} else if (tech.equals("android.nfc.tech.Ndef")
					|| tech.equals("android.nfc.tech.NdefFormatable")) {
				ndef = true;
			}
		}
		return mifareClassic && nfcA && ndef;	// Returns true if all of the required technologies are supported.
	}

	/**
	 * Determines if an NFC tag is writable
	 * @param tag the NFC tag to check for write ability.
	 * @return true if the tag is writable, false otherwise
	 */
	private boolean writableTag(Tag tag) {
		try {
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();
				if (!ndef.isWritable()) {
					Toast.makeText(context, "Tag is read-only.", Toast.LENGTH_SHORT).show();
					ndef.close();
					return false;
				}
				ndef.close();
				return true;
			}
		} catch (Exception e) {
			Toast.makeText(context, "Failed to read tag", Toast.LENGTH_SHORT).show();
		}
		return false;
	}

	/**
	 * Constructs an NDEF message from the given text
	 * @param text the text that the NDEF message should contain
	 * @param addAAR whether we want to attach an Android Application Record to this NDEF message
	 * @return the constructed NdefMessage
	 */
	private NdefMessage getNdefMessage(String text, boolean addAAR) {
		byte[] textBytes = text.getBytes(Charset.forName("US-ASCII"));
		byte[] payload = new byte[textBytes.length + 1]; // add 1 for the URI
														// Prefix
		payload[0] = 0x00; // Set the message prefix to None (can be HTTP (0x01), etc.)
		System.arraycopy(textBytes, 0, payload, 1, textBytes.length); // appends the text to payload

		NdefRecord rtdUriRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
		if (addAAR) {
			// note: returns AAR for different app (nfcreadtag)
			return new NdefMessage(
					new NdefRecord[] {
							rtdUriRecord,
							NdefRecord
									.createApplicationRecord("com.example.nfcmessenger") });
		} else {
			return new NdefMessage(new NdefRecord[] { rtdUriRecord });
		}
	}
}
