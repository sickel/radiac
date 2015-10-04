package com.mortensickel.radiac;

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
	private final static String BUFFERSTATUS = "BUFFERSTATUS";
	
	
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
		TextView ft=(TextView)findViewById(R.id.acbar_freetext);
		ft.setText("");	
		ft=(TextView)findViewById(R.id.acbar_status);
		ft.setText("");
		restoreStatus();
	}

	protected void restoreStatus(){
		try{
			File status = new File(context.getFilesDir(), BUFFERSTATUS);
			if(status.exists()){
				FileInputStream statusstr = new FileInputStream(status);
				String jsonStr = null;	
				FileChannel fc = statusstr.getChannel();
				MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
				jsonStr = Charset.defaultCharset().decode(bb).toString();
			//	debug(jsonStr);	
				JSONObject jstatus = new JSONObject(jsonStr);
				Iterator<String> iter = jstatus.keys();
				while (iter.hasNext()) {
					String key = iter.next();
					try {
						String value = (String)jstatus.get(key);
						int rid=this.getResources().getIdentifier(key, "id",context.getPackageName());
						Object ft=findViewById(rid);
			
						if (ft!=null){ 
						  if( ft.getClass().equals(EditText.class)) {
							((EditText)ft).setText(value);
					   	   }
						  if(ft.getClass().equals(CheckBox.class)){
							  ((CheckBox)ft).setChecked(value.equals("True"));
						  }
					  	if(ft.getClass().equals(Spinner.class)){
							Spinner sp=(Spinner)ft;
							sp.setSelection(((ArrayAdapter<String>)sp.getAdapter()).getPosition(value));
						}
							
				   	
}
					} catch (JSONException e) {
						Toast.makeText(context,R.string.jsonerror,Toast.LENGTH_LONG).show();
						// Something went wrong!
					}
				    	
				}
				debug("restored");
				status.delete();
				
			//	debug((String)jstatus.get("etLocname"));
			}
			
		}
		catch(Exception e){
			
		}
	}
	
	
	protected void saveStatus(){
		HashMap params=new HashMap<String,String>();
		params=collectParams();
		JSONObject json=new JSONObject(params);

		File status = new File(context.getFilesDir(), BUFFERSTATUS);
		try {
			FileOutputStream out = new FileOutputStream(status);
			out.write(json.toString().getBytes());
			out.close();
				
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	
	@Override
	protected void onStop(){
		saveStatus();
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
		if (!unlockkey.equals("123Radiac")) throw new LockedAppException(getResources().getString(R.string.lockedAppErr));
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
			case R.id.menu_readgps:
				readgps();
				break;
			case R.id.menu_unlock:
			
				break;
		    case R.id.menu_samplereg:
				openSamplereg();
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
			case R.id.menu_dosereg:
				startActivity(new Intent(this, doseregistration.class));
				break;

        }

        return true;
    }
	public void openSamplereg(){
		try{
	 		startActivity(new Intent(this, samplereg.class));
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	public void saveObs(){
		/* Reading log of observations and reupload those that are not uploaded
		 */
		ArrayList<String> linelist = new ArrayList<>();
		try{
			InputStream is=openFileInput(errorfile);
			BufferedReader rdr =new BufferedReader(new InputStreamReader(is));
			String myLine;

			while ((myLine=rdr.readLine())!=null) 
				if(myLine.length()>4)
					if (!(myLine.substring(0,5).equals("Error")))
					linelist.add(myLine);	
			File dir=getFilesDir();
			File from = new File(dir,errorfile);
			File to = new File(dir,errorfile+".bak");
			if(from.exists()) if(!(from.renameTo(to))) debug("Could not rename");
		}catch(Exception e){
			Toast.makeText(getApplicationContext(),getResources().getString(R.string.noDataFound),Toast.LENGTH_SHORT).show();
		}
		Toast.makeText(getApplicationContext(),linelist.size()+" lines read - uploading",Toast.LENGTH_SHORT).show();
		for(String line :linelist){
			try{
				if (!(line.substring(0,5).equals("Error"))){
					// URL url = new URL(line);
                    HashMap<String, String> paramset = new HashMap<String, String>();
                    paramset.put("parameters",line);
					new DataUploader(uploadUrl,errorfile,context).execute(paramset);
					
                 //   new PostObservation().execute(paramset);
				}
			} catch (Exception e) {
				Toast.makeText(getApplicationContext(),"error "+e,Toast.LENGTH_LONG).show();
			}
		}
        Integer lnum=0;
        try {
            lnum=linenumbers(new File(getFilesDir(), errorfile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        showStatus(lnum.toString());
	}
	
	

    public void showStatus(String status){
        if (status.equals("0")) status = ""; else
            status = status + " " + getResources().getString(R.string.setsNotUploaded);
        TextView tvstatus =(TextView)findViewById(R.id.acbar_status);
        tvstatus.setText(status);
    }
	
	Integer linenumbers(File file) throws IOException
    {


        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        int lineCount = 0;
        while ((line = br.readLine()) != null)
            if (!(line.substring(0, 5).equals("Error"))) lineCount++;
        return (lineCount);
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
		findViewById(R.id.btStartMeasure).setEnabled(false);
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
	
		
/*	public void enablefields(boolean enable_all){
		List<Integer> toEnable=Arrays.asList(R.id.btStopMeasure,R.id.etAdmname,R.id.etComment,R.id.etLocname,R.id.etSnowcover
											 ,R.id.cbReference,R.id.cbOtherMeasure,R.id.cbRainBefore,R.id.cbRainDuring);
		for (Integer i : toEnable){
			View et=findViewById(i);
			et.setEnabled(true);
		}
	}*/
	
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
		Integer lnum=0;
        try {
            lnum=linenumbers(new File(getFilesDir(), errorfile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        showStatus(lnum.toString());
	}
	
	public void confirm(View v){
		myTimerThread.resetTime();
		View u =findViewById(R.id.btUndo);
		if(u.isEnabled()){
			resetUi();
		}else{
			enableFields(false);
			HashMap params=new HashMap<String,String>();
			params=collectParams();
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

 public HashMap collectParams(){
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
//	 
	 return(params);
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
