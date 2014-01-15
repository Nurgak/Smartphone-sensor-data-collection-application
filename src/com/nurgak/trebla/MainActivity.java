package com.nurgak.trebla;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity
{
	ScrollView logScroller;
	TextView logTextView;
	Button buttonUpdateLog, buttonStopApplication;
	
	String line;
	String separator;
	Process mProcess;
	BufferedReader reader;
	StringBuilder builder = new StringBuilder();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// set activity layout
		setContentView(R.layout.activity_main);

		logScroller = (ScrollView) findViewById(R.id.logScroller);

		logTextView = (TextView) findViewById(R.id.logTextView);

		buttonUpdateLog = (Button) findViewById(R.id.buttonUpdateLog);
		buttonUpdateLog.setOnClickListener(updateLog);

		buttonStopApplication = (Button) findViewById(R.id.buttonStopApplication);
		buttonStopApplication.setOnClickListener(stopApplication);
		
		separator = System.getProperty("line.separator");
	}

	View.OnClickListener updateLog = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			try
			{
				mProcess = Runtime.getRuntime().exec("logcat -d -t 30 trebla:v *:s");
				reader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
				builder.delete(0, builder.length());
				
				while((line = reader.readLine()) != null)
				{
					builder.append(line);
					builder.append(separator);
				}
				logTextView.setText(builder.toString());
				logScroller.fullScroll(View.FOCUS_DOWN);
			}
			catch(IOException e)
			{
			}
		}
	};

	View.OnClickListener stopApplication = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			finish();
			System.exit(0);
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
