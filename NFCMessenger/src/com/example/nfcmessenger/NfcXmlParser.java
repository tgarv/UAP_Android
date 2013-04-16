package com.example.nfcmessenger;

import java.io.IOException;
import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.graphics.Color;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

// Thanks to http://theopentutorials.com/tutorials/android/xml/android-simple-xmlpullparser-tutorial/
public class NfcXmlParser {
	private Activity activity;
	public NfcXmlParser(Activity activity){
		this.activity = activity;
	}
	
	
	public LinearLayout processXml(String payload) {
    	XmlPullParserFactory factory;
    	String text = "";
    	String tagname = "";
    	LinearLayout baseLayout = new LinearLayout(activity);
    	baseLayout.setOrientation(LinearLayout.VERTICAL);
    	SimpleLayoutElement currentElement = new SimpleLayoutElement();
    	
		try {
			factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
	        XmlPullParser xpp = factory.newPullParser();
	        xpp.setInput(new StringReader(payload));
	        
	        int eventType = xpp.getEventType();
	        System.out.println("attribute count: " + xpp.getAttributeCount());
	        
	        while (eventType != XmlPullParser.END_DOCUMENT) {
	        	tagname = xpp.getName();
	        	if(tagname == null)
	        		tagname = "";
	        	switch (eventType){
	        	
		        	case XmlPullParser.START_DOCUMENT :
//		                System.out.println("Start document");
		        	case XmlPullParser.START_TAG :
		        		if(tagname.equalsIgnoreCase("textview")){
		        			System.out.println("Found a textview!");
		        			currentElement = new SimpleTextView();	// TODO should check if the current element is being used and throw some kind of formatting error
		        		} else if (tagname.equalsIgnoreCase("edittext")){
		        			System.out.println("Found an edittext!");
		        			currentElement = new SimpleEditText();
		        		}
		        		break;
		        		
		        	case XmlPullParser.TEXT :
		        		text = xpp.getText();
		        		break;
		        		
		        	case  XmlPullParser.END_TAG :
		        		if(tagname.equalsIgnoreCase("textview")){
//		        			System.out.println("TextView complete! " + textview.toString());
		        			if (currentElement instanceof SimpleTextView){
		        				TextView tv = ((SimpleTextView)currentElement).toFullTextView();
		        				baseLayout.addView(tv);
		        			} else {
		        				System.out.println("Not able to cast type " + currentElement.getClass().toString());
		        			}
		        			currentElement = new SimpleLayoutElement();
		        		} else if (tagname.equalsIgnoreCase("edittext")){
//		        			System.out.println("EditText complete! " + editText.toString());
		        			if (currentElement instanceof SimpleEditText){
		        				EditText edit = ((SimpleEditText)currentElement).toFullEditText();
		        				baseLayout.addView(edit);
		        			} else {
		        				System.out.println("Not able to cast type " + currentElement.getClass().toString());
		        			}
		        			currentElement = new SimpleLayoutElement();
		        		} else if (tagname.equalsIgnoreCase("text")){
		        			currentElement.setText(text);
		        		} else if (tagname.equalsIgnoreCase("id")){
		        			currentElement.setId(Integer.parseInt(text));
		        		} else if (tagname.equalsIgnoreCase("textcolor")){
		        			currentElement.setColor(text);
		        		} else if (tagname.equalsIgnoreCase("textsize")){
		        			currentElement.setTextSize(Float.parseFloat(text));
		        		}
		        		break;
		        	
	        	}
	            
	            try {
					eventType = xpp.next();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return baseLayout;
    }
	
	class SimpleLayoutElement {
		protected String text;
    	protected int id;
    	protected String color;
    	protected float textSize;
    	protected String tag;
    	
    	public SimpleLayoutElement(){
    		this.text = "";
    		this.id = -1;
    		this.color = "#000000";
    		this.textSize = 12;
    	}
    	
    	public String getTag() {
			return this.tag;
		}
    	
    	public void setText(String t){
    		this.text = t;
    	}
    	
    	public void setId(int i){
    		this.id = i;
    	}
    	
    	public void setColor(String color) {
			this.color = color;
		}
    	
    	public void setTextSize(float textSize){
    		this.textSize = textSize;
    	}
	}
	
	class SimpleTextView extends SimpleLayoutElement {
    	public SimpleTextView(){
    		super();
    		this.tag = "textview";
    	}

		@Override
    	public String toString(){
    		return "TextView: text=" + text + ", id=" + id + ", textsize=" + textSize;
    	}
    	
    	public TextView toFullTextView(){
    		TextView tv = new TextView(activity.getBaseContext());
    		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    		tv.setText(this.text);
    		tv.setId(this.id);
    		tv.setTextColor(Color.parseColor(this.color));
    		tv.setTextSize(this.textSize);
    		tv.setLayoutParams(params);
    		return tv;
    	}
    }
	
	class SimpleEditText extends SimpleLayoutElement {    	
    	public SimpleEditText(){
    		super();
    		this.tag = "edittext";
    	}
    	
    	@Override
    	public String toString(){
    		return "EditText: text=" + text + ", id=" + id;
    	}
    	
    	public EditText toFullEditText(){
    		EditText edit = new EditText(activity.getBaseContext());
    		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    		edit.setHint(this.text);
    		edit.setId(this.id);
    		edit.setTextColor(Color.parseColor(this.color));
    		edit.setTextSize(this.textSize);
    		edit.setCursorVisible(true);
    		edit.setLayoutParams(params);
    		return edit;
    	}
    }
}
