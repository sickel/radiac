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
public class MainActivity extends Activity 

// todo timeout
// todo gps
// todo save data
// todo settings
// todo.reset ui

{
    @Override
	
	private Calendar startTime,stopTime;
	private SimpleDateFormat sdtHhmmss = new SimpleDateFormat("HH:mm:ss");	
	private final List<Integer> mandatory =Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo);
	private final List<Integer> allItems= Arrays.asList(R.id.etAdmname,R.id.etLatitude,R.id.etLocname,R.id.etLongitude,R.id.etMeasValue,R.id.etSnowcover,R.id.etTimeFrom,R.id.etTimeTo,R.id.etComment,
	R.id.cbReference,R.id.cbOtherMeasure,R.id.cbRainDuring,R.id.cbRainBefore);
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		for(Integer i: mandatory){
			EditText et=(EditText)findViewById(i);
			et.addTextChangedListener(new TextWatcher(){
					public void afterTextChanged(Editable s) {
						checkFilled();	
					}
					public void beforeTextChanged(CharSequence s, int start, int count, int after){}
					public void onTextChanged(CharSequence s, int start, int before, int count){}
				}); 
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
	}
	
	private void debug(String t){
		Toast.makeText(getApplicationContext(),t,Toast.LENGTH_SHORT).show();
	}
	
	
	
	public void checkFilled(){
		Boolean ready=true;
	//	debug("in");
		for(Integer i:mandatory){
			EditText et=(EditText)findViewById(i);
			ready=ready && !(et.getText().toString().isEmpty());
		}
		CheckBox ref = (CheckBox)findViewById(R.id.cbReference);
		CheckBox oth = (CheckBox)findViewById(R.id.cbOtherMeasure);
		ready=ready && (ref.isChecked() || oth.isChecked());
//		debug("out");
		View save=findViewById(R.id.btConfirm);
		save.setEnabled(ready);
	}
	
	public void confirm(View v){
		View u =findViewById(R.id.btUndo);
		if(u.isEnabled()){
			u.setEnabled(false);
			for(Integer i:allItems){
				View vi=findViewById(i);
				String clss =	vi.getClass().getName();
			//	debug(clss);
				if(clss.equals("android.widget.EditText")){
					EditText et=(EditText)vi;
					et.setText("");
				}
				if(clss.equals("android.widget.CheckBox")){
					CheckBox cb=(CheckBox)vi;
					cb.setChecked(false);
				}
			}		
		}else{
			// todo store data
			enableFields(false);
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
