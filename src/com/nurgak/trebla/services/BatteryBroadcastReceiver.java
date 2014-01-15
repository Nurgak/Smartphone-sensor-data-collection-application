package com.nurgak.trebla.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryBroadcastReceiver extends BroadcastReceiver
{
	float[] batteryState = new float[3];
	
	Context context;
	
	public BatteryBroadcastReceiver(Context context)
	{
		this.context = context;
	}
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		// voltage
		batteryState[0] = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000;
		// temperature
		batteryState[1] = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10;
		// charge in percent
		batteryState[2] = 100*intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)/intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
	}
	
	public void start()
	{
		// start battery state listener
		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		context.registerReceiver(this, filter);
		Log.d("trebla", "Battery receiver started");
	}
	
	public void stop()
	{
		// stop listening to battery state
		context.unregisterReceiver(this);
		Log.d("trebla", "Battery receiver stopped");
	}
	
	public String getBatteryState()
	{
		return java.util.Arrays.toString(batteryState);
	}
}
