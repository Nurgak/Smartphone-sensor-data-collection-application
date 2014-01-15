package com.nurgak.trebla;

import android.util.Log;

import com.nurgak.trebla.services.BatteryBroadcastReceiver;
import com.nurgak.trebla.services.Command;
import com.nurgak.trebla.services.InternalSensorListener;
import com.nurgak.trebla.services.UsbCommunicationManager;

public class TreblaService extends BoundService
{
	// command parser
	Command cmd = null;

	// internal sensors
	InternalSensorListener sensorListener = null;

	// battery
	BatteryBroadcastReceiver batteryReceiver = null;

	// usb
	UsbCommunicationManager usb = null;

	// bluetooth
	// TODO

	// camera
	// TODO

	// gps
	// TODO

	String returnValue = "";

	public String processMessage(String msg)
	{
		// parse the command
		cmd = new Command(msg);

		returnValue = "";

		if(cmd.command.equals("sensor"))
		{
			if(cmd.argument.equals("start"))
			{
				// create and start the internal sensor listener
				sensorListener = new InternalSensorListener(this);
				sensorListener.start();
				returnValue += "internal sensor listener started";
			}
			else if(cmd.argument.equals("stop") && sensorListener != null)
			{
				// stop and remove all references to the sensor listener
				sensorListener.stop();
				sensorListener = null;
				returnValue += "internal sensor listener stopped";
			}
			else if(!cmd.argument.equals("") && sensorListener != null)
			{
				returnValue += sensorListener.readSensorValues(cmd.argument);
			}
			else if(cmd.argument.equals("") && sensorListener != null)
			{
				returnValue += "no sensor type specified";
			}
			else if(sensorListener == null)
			{
				returnValue += "internal sensor listener not started";
			}
		}
		else if(cmd.command.equals("battery"))
		{
			if(cmd.argument.equals("start") && batteryReceiver == null)
			{
				// start battery state listener
				batteryReceiver = new BatteryBroadcastReceiver(this);
				batteryReceiver.start();
				returnValue += "battery listener started";
			}
			else if(cmd.argument.equals("stop") && batteryReceiver != null)
			{
				// stop listening to battery state
				batteryReceiver.stop();
				batteryReceiver = null;
				returnValue += "battery listener stopped";
			}
			else if(cmd.argument.equals("state") && batteryReceiver != null)
			{
				returnValue += batteryReceiver.getBatteryState();
			}
			else if(!cmd.argument.equals("") && batteryReceiver != null)
			{
				returnValue += "no argument specified";
			}
			else if(batteryReceiver == null)
			{
				returnValue += "battery listener not started";
			}
		}
		else if(cmd.command.equals("usb"))
		{
			if(usb == null)
			{
				usb = new UsbCommunicationManager(this);
			}

			if(cmd.checkFlag("l") || cmd.checkFlag("list"))
			{
				returnValue += usb.listUsbDevices();
			}
			else if(cmd.checkFlag("r") || cmd.checkFlag("read"))
			{
				StringBuilder data = new StringBuilder();
				usb.read(data);
				returnValue += data.toString();
			}
			else if((cmd.checkFlag("w") || cmd.checkFlag("write")) && !cmd.argument.equals(""))
			{
				returnValue += "Transferred bytes: " + usb.write(cmd.argument);
			}
			else if(cmd.argument.equals("connect"))
			{
				usb.connect();
				returnValue += "Trying to establish connection";
			}
		}
		else if(cmd.command.equals("picture") || cmd.command.equals("image") || cmd.command.equals("photo"))
		{
			returnValue += "not implemented yet";
		}
		else if(cmd.command.equals("gps"))
		{
			returnValue += "not implemented yet";
		}
		else
		{
			returnValue += "unrecognised command";
		}
		return returnValue;
	}

	@Override
	public void onDestroy()
	{
		Log.d("trebla", "Stopping all bounded services");

		// make sure all services are shut down
		if(usb != null)
		{
			usb.stop();
			usb = null;
		}
		
		if(sensorListener != null)
		{
			sensorListener.stop();
			sensorListener = null;
		}

		if(batteryReceiver != null)
		{
			batteryReceiver.stop();
			batteryReceiver = null;
		}
		super.onDestroy();
	}
}
