package com.mortensickel.radiac;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.Button;
import android.widget.*;
import java.util.List;
import java.util.*;
import java.text.*;
import android.text.TextWatcher;
import android.text.Editable;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.*;
import org.json.*;
public class MainActivity extends Activity 

// todo timeout - done
// todo gps
// todo upload data - done as get
// todo upload with post
// todo save data
// todo settings 
// todo reset ui without uploading data - done
// todo unique unlock code
// todo remote kill
// todo activity for sample registration
// todo select UTM or lat/lon
// todo select unit

{
    @Override
	private String uuid;
	private Calendar startTime,stopTime;
	private SimpleDateFormat sdtHhmmss = new SimpleDateFormat("HH:mm:ss");	
	private final List<Integer> mandatory =Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo);
	private final List<Integer> allItems= Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo,R.id.etComment,
	R.id.cbReference,R.id.cbOtherMeasure,R.id.cbRainDuring,R.id.cbRainBefore);
    private final Integer RESULT_SETTINGS=1;
	private String unlockkey="";
	private final Context context=this;
	private Integer timeout=20;
	private final ShowTimeRunner myTimerThread = new ShowTimeRunner();	
	private DataUploader uploader;
	private String uploadUrl="http://aws.sickel.net/radiac";
	private String errorfile="errors.log";
	private String logfile="logfile.log";
	
	
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
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
	//	uploader = new DataUploader(uploadUrl,errorfile,context);
    }
	
	private void checkLock() throws LockedAppException {
		// Check against some hash of uuid and device unique number
		if (!unlockkey.equals("123")) throw new LockedAppException(getResources().getString(R.string.lockedAppErr));
	}

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		unlockkey=sharedPrefs.getString("pref_unlockkey", "");
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
		// todo save failed uploads
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
		// todo gps -.fake for now...
		EditText crd=(EditText)findViewById(R.id.etLongitude);
		crd.setText("59.435665");
		crd=(EditText)findViewById(R.id.etLatitude);
		crd.setText("15.33243");
	}
	
	private void debug(String t){
		Toast.makeText(getApplicationContext(),t,Toast.LENGTH_SHORT).show();
	}
	
	
	
	public void checkFilled(){
		Boolean ready=true;
		for(Integer i:mandatory){
			EditText et=(EditText)findViewById(i);
			ready=ready && !(et.getText().toString().isEmpty());
		}
		CheckBox ref = (CheckBox)findViewById(R.id.cbReference);
		CheckBox oth = (CheckBox)findViewById(R.id.cbOtherMeasure);
		ready=ready && (ref.isChecked() || oth.isChecked());
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
			if(clss.equals("android.widget.EditText")){
				EditText et=(EditText)vi;
				value=et.getText().toString();
			}
			if(clss.equals("android.widget.CheckBox")){
				CheckBox cb=(CheckBox)vi;
				if(cb.isChecked()){
					value="True";
				}else{
					value="False";
				}
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
							bt=(Button)findViewById(R.id.btConfirm);
							bt.setEnabled(false);

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
