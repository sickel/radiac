package com.mortensickel.radiac;
import android.os.AsyncTask;
import java.util.HashMap;
import java.util.Map;
import android.widget.Toast;
import android.content.Context;
import android.content.Context.*;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.json.*;

public class DataUploader extends AsyncTask<HashMap<String,String>, Void,Integer>{

	private Exception exception;
	private Integer status;
	private String urlString;
	private String errorfile;
	private Context context;
	
	
	
	public DataUploader(String url, String errfile,Context ctx){
		this.context=ctx;
		this.errorfile=errfile;
		this.urlString=url;
	}
	
	
	
	protected Integer doInBackground(HashMap<String,String>... paramsets){
		HashMap<String,String> paramset = new HashMap<String, String>();
		paramset=paramsets[0];
		String params=paramset.get("parameters");
		if(params == null) {
			params="";
			for (Map.Entry<String, String> entry : paramset.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				if (!(params.equals(""))) params = params + "&";
				params = params + key + "=" + value;
			}
		}
		try{
			// TODO: Rewrite to use post / json
			// params=URLEncoder.encode(params,"UTF-8");
			URL url = new URL(urlString+"?"+params);

			URLConnection conn = url.openConnection();
			conn.setDoOutput(true); 
			JSONObject json=new JSONObject(paramset);  
			java.io.OutputStreamWriter wr = new java.io.OutputStreamWriter(conn.getOutputStream());
			wr.write(json.toString());
			wr.flush();
			BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputLine;
			while((inputLine = in.readLine())!=null) {
				// todo remote kill comes here
				// reading the servers answer. Ignoring what we get. May check for correct return later
				// Toast.makeText(getApplicationContext(), "result " + inputLine, Toast.LENGTH_LONG).show();
			}
			in.close();
		}catch(Exception e){
			this.exception=e;
			status=0;

			FileOutputStream outputStream;

			try {
				outputStream = context.openFileOutput(errorfile, context.MODE_APPEND);
				outputStream.write(("Error "+e.getMessage()+"\n").getBytes());
				outputStream.write((params+"\n").getBytes());
				outputStream.close();
			} catch (Exception fe) {
				fe.printStackTrace();
			}
		//	Toast.makeText(context," upload error", Toast.LENGTH_SHORT).show();
			
			return null;
		}
		status=10;
		return status;
	}




	protected void onPostExecute(Long res){

		Toast.makeText(context,"tried  upload + debug", Toast.LENGTH_SHORT).show();
		if(status==0){
			// use catlog
			Toast.makeText(context," upload error", Toast.LENGTH_SHORT).show();

		}
	}

}
