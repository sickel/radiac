package com.mortensickel.radiac;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.Button;
import android.widget.*;
import android.widget.AdapterView;
import java.util.List;
import java.util.*;
import java.text.*;
import android.text.TextWatcher;
import android.text.Editable;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.*;
import org.json.*;
import java.io.FileOutputStream;
import com.mortensickel.radiac.LocationService.LocalBinder;
import android.location.Location;
// done timeout 
// done gps basic lat / lon
// todo gps average
// todo gps utm
// done upload data - done as get
// todo upload with post
// todo save data
// todo settings - started 
// todo rewrite settings to use fragments
// done reset ui without uploading data 
// todo unique unlock code
// todo remote kill
// todo activity for sample registration
// todo select UTM or lat/lon
// todo project gps to utm
// done select unit 
// done check if unit is set
// done upload unit

public class MainActivity extends Activity

{
   
	private String uuid;
	private Calendar startTime,stopTime;
	private SimpleDateFormat sdtHhmmss = new SimpleDateFormat("HH:mm:ss");	
	private final List<Integer> mandatory =Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo);
	private final List<Integer> allItems= Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo,R.id.etComment,
	R.id.cbReference,R.id.cbOtherMeasure,R.id.cbRainDuring,R.id.cbRainBefore,R.id.spUnit);
    private final Integer RESULT_SETTINGS=1;
	private String user;
	private String patrol;
	private String unlockkey="";
	private final Context context=this;
	private Integer timeout=20;
	private final ShowTimeRunner myTimerThread = new ShowTimeRunner();	
	protected ServiceConnection lServiceConnection;
	public boolean lServiceBound=false;
	private String uploadUrl="http://aws.sickel.net/radiac";
	private String errorfile="errors.log";
	private String logfile="logfile.log";
	private LocationService lService; 

	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		ActionBar actionBar = getActionBar();
        assert actionBar != null;
        actionBar.setCustomView(R.layout.actionbar);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);	
		uuid=Installation.id(getApplicationContext());
		for(Integer i: mandatory){
			// checks if registration is finished
			EditText et=(EditText)findViewById(i);
			et.addTextChangedListener(new TextWatcher(){
				public void afterTextChanged(Editable s) {
					checkFilled();	
				}
				public void beforeTextChanged(CharSequence s, int start, int count, int after){}
				public void onTextChanged(CharSequence s, int start, int before, int count){}
			}); 
		}
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
		
	}


	@Override
	protected void onStop(){
		if(lServiceBound){		
			stopGPS();
		}
		super.onStop();
	}


	protected void  stopGPS(){
        // TODO: see if it is possible to turn of GPS immediately
        unbindService(lServiceConnection);
		lServiceBound=false;
	}
	
	
	
	
	private void checkLock() throws LockedAppException {
		// Check against some hash of uuid and device unique number
		if (!unlockkey.equals("123")) throw new LockedAppException(getResources().getString(R.string.lockedAppErr));
	}
	
	@Override
	protected void onStart(){
		super.onStart();
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
	
	


	
	
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO: Rewrite to use fragments
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_settings:
                Intent i = new Intent(this, UserSettingsActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                break;
			case R.id.menu_upload:
				saveObs();	
				break;
			case R.id.menu_resetui:
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
				alertDialogBuilder.setTitle("");
				alertDialogBuilder
					.setMessage(getResources().getString(R.string.resetmeasure))
					.setCancelable(false)
					.setPositiveButton(getResources().getString(R.string.yes),new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							// if this button is clicked, reset user interface to start new measurement
							MainActivity.this.enableFields(false);
							MainActivity.this.resetUi();
						}
					})
					.setNegativeButton(getResources().getString(R.string.no),new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							// do nothing just return
							dialog.cancel();
						}
					});
				AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
				break;


        }

        return true;
    }
	
	private void saveObs(){
		// todo save }failed uploads
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
		
		try{
			checkLock();
		}catch(LockedAppException e){
			Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
			return;
		}
		Button bt=(Button)findViewById(R.id.btStartMeasure);
		bt.setEnabled(false);
		List<Integer> toEnable=Arrays.asList(R.id.btStopMeasure,R.id.etAdmname,R.id.etComment,R.id.etLocname,R.id.etSnowcover
		,R.id.cbReference,R.id.cbOtherMeasure,R.id.cbRainBefore,R.id.cbRainDuring);
		for (Integer i : toEnable){
			View et=findViewById(i);
			et.setEnabled(true);
		}
		startTime=Calendar.getInstance();
		EditText st=(EditText)findViewById(R.id.etTimeFrom);
		st.setText(sdtHhmmss.format(startTime.getTime()));
	//	startGPS();
		}
	
	
	public void onMeasureStop(View v){
		Button bt=(Button)findViewById(R.id.btStopMeasure);
		bt.setEnabled(false);
		List<Integer> toEnable=Arrays.asList(R.id.etMeasValue,R.id.etLatitude,R.id.etLongitude,R.id.etTimeFrom,R.id.etTimeTo);
		for (Integer i : toEnable){
			View et=findViewById(i);
			et.setEnabled(true);
		}
		stopTime=Calendar.getInstance();
		EditText st=(EditText)findViewById(R.id.etTimeTo);
		st.setText(sdtHhmmss.format(stopTime.getTime()));
		readgps();
	}
		
public void readgps(){			
		try{
            if(lServiceBound){
				Location loc=lService.getLocation();	
				EditText crd=(EditText)findViewById(R.id.etLongitude);
			//	debug(loc.toString());
			//	crd.setText("59.435665");
				crd.setText(String.valueOf(loc.getLongitude()));
				crd=(EditText)findViewById(R.id.etLatitude);
				crd.setText(String.valueOf(loc.getLatitude()));		
			//	crd.setText("15.33243");
				stopGPS();
				}
			else{
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.GPSServiceNotAvailable)+"-if",Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.GPSLocationNotAvailable),Toast.LENGTH_LONG).show();
        }
	}
	
	private void debug(String t){
		Toast.makeText(getApplicationContext(),t,Toast.LENGTH_SHORT).show();
	}
	
	private Float numFromEditText(int id) throws NumberFormatException
	{
		EditText et=(EditText)findViewById(id);
		String txt=et.getText().toString().trim();
		if (txt.length() == 0){
			return (float)0;
		}
		return Float.parseFloat(txt);
	}
	
	public void checkFilled(){
		Float north=(float)0;
		Float east=(float)0;		
		try{
			// use values of nort and east to check i latlon or utm
		 	north=numFromEditText(R.id.etLongitude);
			east=numFromEditText(R.id.etLongitude);
		}catch(NumberFormatException e){
			Toast.makeText(context,"ugyldig tall",Toast.LENGTH_LONG).show();
		}
		Boolean ready=true;
		for(Integer i:mandatory){
			EditText et=(EditText)findViewById(i);
			ready=ready && !(et.getText().toString().isEmpty());
		}
		CheckBox ref = (CheckBox)findViewById(R.id.cbReference);
		CheckBox oth = (CheckBox)findViewById(R.id.cbOtherMeasure);
		ready=ready && (ref.isChecked() || oth.isChecked());
		Spinner spUnit =(Spinner)findViewById(R.id.spUnit);
		ready=ready && spUnit.getSelectedItemId() > 0;
		View save=findViewById(R.id.btConfirm);
		save.setEnabled(ready);
		myTimerThread.resetTime();
	}
	
	public void confirm(View v){
		myTimerThread.resetTime();
		View u =findViewById(R.id.btUndo);
		if(u.isEnabled()){
			resetUi();
		}else{
			// todo store data
			enableFields(false);
			HashMap params=new HashMap<String,String>();
		params.put("uuid",uuid);
	//	debug("here");
		// todo time, name, patrulje
		for(Integer i: allItems){
			View vi=findViewById(i);
			String key=getResources().getResourceEntryName(i);
			String value="";
			String clss =vi.getClass().getName();
			switch(clss){
			case "android.widget.EditText":
				EditText et=(EditText)vi;
				value=et.getText().toString();
				break;
			
			case "android.widget.CheckBox":
				CheckBox cb=(CheckBox)vi;
				if(cb.isChecked()){
					value="True";
				}else{
					value="False";
				}
				break;
			
		    case "android.widget.Spinner":
				Spinner sp=(Spinner)vi;
				value=sp.getSelectedItem().toString();
				break;
			default:
				debug(clss);
				break;
			}
			params.put(key,value);
	
			//ongoing
		}
		JSONObject json=new JSONObject(params);
		debug(json.toString());
		//Toast.makeText(context,json.toString(),Toast.LENGTH_LONG);
		//json.putAll(params);
		new DataUploader(uploadUrl,errorfile,context).execute(params);
		}
	}
	
	public void resetUi(){
		View u = findViewById(R.id.btUndo);
		u.setEnabled(false);
		for(Integer i:allItems){
			View vi=findViewById(i);
			String clss =	vi.getClass().getName();
			if(clss.equals("android.widget.EditText")){
				EditText et=(EditText)vi;
				et.setText("");
			}
			if(clss.equals("android.widget.CheckBox")){
				CheckBox cb=(CheckBox)vi;
				cb.setChecked(false);
			}
		}
		View bstart=findViewById(R.id.btStartMeasure);
		bstart.setEnabled(true);
		List<Integer> buttons =Arrays.asList(R.id.btConfirm,R.id.btUndo,R.id.btStopMeasure);
		for (Integer i : buttons){
			View btn=findViewById(i);
			btn.setEnabled(false);
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
	
	
	public class LockedAppException extends Exception {
		public LockedAppException(String msg) {
			super(msg);
		}
	}
	
	
	

	void doWork(final long startTime){
		runOnUiThread(new Runnable(){
			public void run(){
				try{
					Date dt= new Date();
					long sec=dt.getTime();
					sec=(sec-startTime)/1000;

					if(sec>timeout && timeout > 0){
							// undo timeout. to be set in settings
						Button bt=(Button)findViewById(R.id.btUndo);
						bt.setEnabled(false);
						// lock fields - should also be unlockab
					}
				}catch(Exception e){}
			}
		});

	}


	class ShowTimeRunner implements Runnable
	{
		private long startTime=new Date().getTime();
		public void resetTime(){
			this.startTime=new Date().getTime();
		}

		@Override
		public void run()
		{
			while(!Thread.currentThread().isInterrupted()){
				try{
					doWork(startTime);
					Thread.sleep(1000);
				}catch(InterruptedException e){
					Thread.currentThread().interrupt();
				}catch(Exception e){}
			}
		}
	}

		
}
