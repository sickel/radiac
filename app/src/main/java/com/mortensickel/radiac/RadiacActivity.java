package com.mortensickel.radiac;
import android.app.*;
import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.preference.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.mortensickel.radiac.LocationService.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.nio.channels.FileChannel;
import java.text.*;
import java.util.*;
import org.json.*;


public class RadiacActivity extends Activity
{
	String unlockkey="";
	String user;
	private Calendar startTime,stopTime;
	private final Context context=this;
	
	
	private SimpleDateFormat sdtHhmmss = new SimpleDateFormat("HH:mm:ss");	
	public List<Integer> mandatory =Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo);
	public List<Integer> allItems= Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo,R.id.etComment,
														R.id.cbReference,R.id.cbOtherMeasure,R.id.cbRainDuring,R.id.cbRainBefore,R.id.spUnit);
	public Button startbutton;
	
	
	
	public void onMeasureStart(View v){
		try{
		try{
			checkLock();
		}catch(LockedAppException e){
			Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
			return;
		}
		// findViewById(R.id.btStartRegisterSample).setEnabled(false);
		startbutton.setEnabled(false);
		enableFields(true);
		findViewById(R.id.btStopMeasure).setEnabled(true);
		findViewById(R.id.etMeasValue).setEnabled(false);
		EditText st=(EditText)findViewById(R.id.etTimeFrom);
		if(st.getText().toString().trim().length() == 0){
			//	if this is used to unlock, starttime should not be reset
			startTime=Calendar.getInstance();			
			st.setText(sdtHhmmss.format(startTime.getTime()));
		}
		//	startGPS();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	
	
	private void enableFields(Boolean ena){	
		for(Integer i:allItems){
			View vi=findViewById(i);
			vi.setEnabled(ena);
		}
		View undo = findViewById(R.id.btUndo);
		undo.setEnabled(!(ena));
	}
	
	
	private void checkLock() throws LockedAppException {
		// Check against some hash of uuid and device unique number
		if (!unlockkey.equals("123Radiac")) throw new LockedAppException(getResources().getString(R.string.lockedAppErr));
	}
	
	public class LockedAppException extends Exception {
		public LockedAppException(String msg) {
			super(msg);
		}
	}
	
	@Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		unlockkey=sharedPrefs.getString("pref_unlockkey", "");
		try{
			checkLock();
		}catch(LockedAppException e){
			Toast.makeText(context,R.string.lockedAppErr,Toast.LENGTH_LONG).show();
		}
		user=sharedPrefs.getString("pref_user_name","");
		if (user.length()>0){

		}
	}



	
	
	
}
