package com.mortensickel.radiac;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.preference.*;

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
// import com.jhlabs.*;

// done timeout 
// done gps basic lat / lon
// todo gps average
// todo select UTM or lat/lon
// todo project gps to utm
// todo fetch location and kommune from server when coordinatea are known
// done upload data - done as get
// todo upload with post
// done save data
// done settings - started 
// done reset ui without uploading data 
// todo unique unlock code
// todo remote kill
// todo activity for sample registration
// done select unit 
// todo dose registration form
// done check if unit is set
// done upload unit
// done handle.quitting

public class MainActivity extends RadiacActivity

{
   
	
	private Calendar startTime,stopTime;
	private SimpleDateFormat sdtHhmmss = new SimpleDateFormat("HH:mm:ss");	
	//public final List<Integer> mandatory =Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo);
	//private final List<Integer> allItems= Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo,R.id.etComment,
	//R.id.cbReference,R.id.cbOtherMeasure,R.id.cbRainDuring,R.id.cbRainBefore,R.id.spUnit);
    	private String user;
	private String patrol;
	private String unlockkey="";
	private final Context context=this;
	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
        mandatory =Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo);
	    allItems= Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo,R.id.etComment,
								R.id.cbReference,R.id.cbOtherMeasure,R.id.cbRainDuring,R.id.cbRainBefore,R.id.spUnit);
		setContentView(R.layout.main);
		super.onCreate(savedInstanceState);
		
		Thread showtimeThread;
		showtimeThread = new Thread(myTimerThread);
		showtimeThread.start();
		Spinner spUnit =(Spinner)findViewById(R.id.spUnit);
/*		spUnit.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
					// your code here
				}

				@Override
				public void onNothingSelected(AdapterView<?> parentView) {
					// your code here
				}

			});	*/
		
		TextView ft=(TextView)findViewById(R.id.acbar_freetext);
		ft.setText("");	
		ft=(TextView)findViewById(R.id.acbar_status);
		ft.setText("");
		stopButton=R.id.btStopMeasure;
		undoButton=R.id.btUndo;
		startButton=R.id.btStartMeasure;
		startTimeField=R.id.etTimeFrom;
		restoreStatus();
	}

	
	
	
	
	@Override
	protected void onStop(){
		saveStatus();
		if(lServiceBound){		
			stopGPS();
		}
		super.onStop();
	
	}


	
	
	
	
	private void checkLock() throws LockedAppException {
		// Check against some hash of uuid and device unique number
		if (!unlockkey.equals("123Radiac")) throw new LockedAppException(getResources().getString(R.string.lockedAppErr));
	}
	
	@Override
	protected void onStart(){
		super.onStart();
		mandatory =Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo);
	    final List<Integer> allItems= Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo,R.id.etComment,
		R.id.cbReference,R.id.cbOtherMeasure,R.id.cbRainDuring,R.id.cbRainBefore,R.id.spUnit);
		
		startGPS();
	}
	
	
	private void startGPS(){
		Intent intent=new Intent(this, LocationService.class);
		intent.setAction("startListening"   );
		startService(intent);
	   	lServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocalBinder binder = (LocalBinder) service;
              // todo fix. why doesn't this work?
				lService = binder.getService();
                lServiceBound=true;
            }
			

            @Override
            public void onServiceDisconnected(ComponentName name) {
                lServiceBound = false;
            }
        };
        bindService(intent,lServiceConnection,Context.BIND_AUTO_CREATE);
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
	
	


	
	
	
	
	
    
	
	
	public void onMeastypeClicked(View v){
		// makes sure max one is checked at any time
		CheckBox ref = (CheckBox)findViewById(R.id.cbReference);
		CheckBox clkd=(CheckBox)v;
		if(clkd.isChecked()){
			if(ref==clkd){
				ref=(CheckBox)findViewById(R.id.cbOtherMeasure);
			}
			ref.setChecked(false);	
		}
		checkFilled();
	}
	
	public void onMeasureStart(View v){
		super.onMeasureStart(v);
		/*
		EditText st=(EditText)findViewById(R.id.etTimeFrom);
		if(st.getText().toString().trim().length() == 0){
		//	if this is used to unlock, starttime should not be reset
			startTime=Calendar.getInstance();			
			st.setText(sdtHhmmss.format(startTime.getTime()));
		}
	//	startGPS();*/
		findViewById(R.id.btStartMeasure).setEnabled(false);
		findViewById(R.id.etMeasValue).setEnabled(false);
		
		}
	
		
/*	public void enablefields(boolean enable_all){
		List<Integer> toEnable=Arrays.asList(R.id.btStopMeasure,R.id.etAdmname,R.id.etComment,R.id.etLocname,R.id.etSnowcover
											 ,R.id.cbReference,R.id.cbOtherMeasure,R.id.cbRainBefore,R.id.cbRainDuring);
		for (Integer i : toEnable){
			View et=findViewById(i);
			et.setEnabled(true);
		}
	}*/
	
	public void onMeasureStop(View v){
		super.onMeasureStop(v);
		stopTime=Calendar.getInstance();
		EditText st=(EditText)findViewById(R.id.etTimeTo);
		st.setText(sdtHhmmss.format(stopTime.getTime()));
		readgps();
		List<Integer> toEnable=Arrays.asList(R.id.etMeasValue,R.id.etLatitude,R.id.etLongitude,R.id.etTimeFrom,R.id.etTimeTo);
		for (Integer i : toEnable){
			View et=findViewById(i);
			et.setEnabled(true);
		}
	}

	
	
	public void undo(View v){
		enableFields(true);
		// todo send undo
	}	
		
	private void enableFields(Boolean ena){	
		for(Integer i:allItems){
			View vi=findViewById(i);
			vi.setEnabled(ena);
		}
		View undo = findViewById(R.id.btUndo);
		undo.setEnabled(!(ena));
	}

 
	
	
	

	
	


	
		
}
