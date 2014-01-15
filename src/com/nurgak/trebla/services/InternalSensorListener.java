package com.nurgak.trebla.services;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class InternalSensorListener implements SensorEventListener
{
	float acc[] = new float[3];
	float gyro[] = new float[3];
	float temp[] = new float[1];
	float pressure[] = new float[1];
	float gravity[] = new float[1];
	float light[] = new float[1];
	float mfield[] = new float[1];
	float rh[] = new float[1];
	float rotation[] = new float[1];
	float proximity[] = new float[1];
	
	SensorManager sensorManager = null;
	
	Context context;
	
	public InternalSensorListener(Context context)
	{
		// save context of the calling trebla service
		this.context = context;
	}
	
	public void start()
	{
		// internal sensor listener setup
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		
		// listen to all sensors
		List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
		for(Sensor sensor : sensors)
		{
			sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		Log.d("trebla", "Internal sensor listener started");
	}
	
	public void stop()
	{
		// unregister the listener for all sensors
		sensorManager.unregisterListener(this);
		sensorManager = null;
		Log.d("trebla", "Internal sensor listener stopped");
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		switch(event.sensor.getType())
		{
		case Sensor.TYPE_ACCELEROMETER:
			acc = event.values;
			break;
		case Sensor.TYPE_GYROSCOPE:
			gyro = event.values;
			break;
		case Sensor.TYPE_AMBIENT_TEMPERATURE:
			temp = event.values;
			break;
		case Sensor.TYPE_PRESSURE:
			pressure = event.values;
			break;
		case Sensor.TYPE_GRAVITY:
			gravity = event.values;
			break;
		case Sensor.TYPE_LIGHT:
			light = event.values;
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			mfield = event.values;
			break;
		case Sensor.TYPE_RELATIVE_HUMIDITY:
			rh = event.values;
			break;
		case Sensor.TYPE_ROTATION_VECTOR:
			rotation = event.values;
			break;
		case Sensor.TYPE_PROXIMITY:
			proximity = event.values;
			break;
		}
	}
	
	public String readSensorValues(String sensorType)
	{
		if(sensorType == null || sensorType.equals(""))
		{
			return null;
		}
		
		if(sensorType.equals("acc"))
		{
			return java.util.Arrays.toString(acc);
		}
		else if(sensorType.equals("gyro"))
		{
			return java.util.Arrays.toString(gyro);
		}
		else if(sensorType.equals("temp"))
		{
			return java.util.Arrays.toString(temp);
		}
		else if(sensorType.equals("pressure"))
		{
			return java.util.Arrays.toString(pressure);
		}
		else if(sensorType.equals("light"))
		{
			return java.util.Arrays.toString(light);
		}
		else if(sensorType.equals("mfield"))
		{
			return java.util.Arrays.toString(mfield);
		}
		else if(sensorType.equals("rh"))
		{
			return java.util.Arrays.toString(rh);
		}
		else if(sensorType.equals("rotation"))
		{
			return java.util.Arrays.toString(rotation);
		}
		else if(sensorType.equals("proximity"))
		{
			return java.util.Arrays.toString(proximity);
		}
		return null;
	}
}
