package com.nurgak.trebla.services;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.nurgak.trebla.BoundService;

public class BluetoothClientService extends BoundService implements Runnable
{
	static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	BluetoothAdapter bluetoothAdapter = null;
	BluetoothSocket btSocket = null;
	OutputStream outStream = null;
	BluetoothDevice device = null;
	
	// this should not be hardcoded
	static String address = "14:10:9F:E8:06:99";

	@Override
	public void onCreate()
	{
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
		{
			Log.d("trebla", "Bluetooth not enabled");
			return;
		}

		device = bluetoothAdapter.getRemoteDevice(address);

		try
		{
			btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// try to cancel bluetooth discovery just in case it's running
		bluetoothAdapter.cancelDiscovery();
		
		new Thread(this).start();
	}

	@Override
	public void run()
	{
		while(true)
		{
			// blocking connection here, put in another thread
			Log.d("trebla", "Waiting for a client to connect");
			try
			{
				btSocket.connect();
			}
			catch(IOException e)
			{
				try
				{
					btSocket.close();
				}
				catch(IOException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			try
			{
				outStream = btSocket.getOutputStream();
			}
			catch(IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// parse command here

			String message = "Hello message from client to server.";
			byte[] msgBuffer = message.getBytes();
			try
			{
				outStream.write(msgBuffer);
			}
			catch(IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
