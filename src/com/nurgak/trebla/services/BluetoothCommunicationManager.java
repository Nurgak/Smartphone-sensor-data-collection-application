package com.nurgak.trebla.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class BluetoothCommunicationManager extends Service
{
	private final static String TAG = "trebla";
	public final static boolean D = true;
	
	BluetoothAdapter bluetoothAdapter;
	
	// Member fields
	private BluetoothThread bluetoothThread;
	private boolean busy, stoppingConnection;
	
	public String listBluetoothDevices()
	{
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		// service needs a context, for this it needs to be started with startService or bindService
		// otherwise these calls will return null pointer errors
		registerReceiver(Receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		registerReceiver(Receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		
		bluetoothAdapter.startDiscovery();
		
		return null;
	}
	
	private final BroadcastReceiver Receiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if(BluetoothDevice.ACTION_FOUND.equals(action))
			{
				// Found a device in range
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// If it's not a paired device add it to the list
				if(device.getBondState() != BluetoothDevice.BOND_BONDED)
				{
					Log.d(TAG, "Device found: " + device.getName());
				}
			}
			else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
			{
				Log.d(TAG, "Finished discoverting devices");
			}
		}
	};

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * 
	 * @param device
	 *            The BluetoothDevice to connect
	 */
	public synchronized void connect(BluetoothDevice device)
	{
		if(D)
			Log.i(TAG, "Connecting to " + device.getName());
		stoppingConnection = false;
		busy = false;

		// Cancel any thread currently running a connection
		if(bluetoothThread != null)
		{
			bluetoothThread.cancel();
			bluetoothThread = null;
		}

		// Start the thread to connect with the given device
		bluetoothThread = new BluetoothThread(device);
		bluetoothThread.start();
	}

	/**
	 * This thread runs during a connection with a remote device. It handles the
	 * initial connection and all incoming and outgoing transmissions.
	 */
	private class BluetoothThread extends Thread
	{
		private final BluetoothSocket socket;
		private InputStream inStream;
		private OutputStream outStream;

		public BluetoothThread(BluetoothDevice device)
		{
			BluetoothSocket tmp = null;
			try
			{
				// General purpose UUID
				tmp = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			socket = tmp;
		}

		public void run()
		{
			// Connect to the socket
			try
			{
				// Blocking function, needs the timeout
				if(D)
					Log.i(TAG, "Connecting to socket");
				socket.connect();
			}
			catch(IOException e)
			{
				// If the user didn't cancel the connection then it has failed (timeout)
				if(!stoppingConnection)
				{
					if(D)
						Log.e(TAG, "Cound not connect to socket");
					e.printStackTrace();
					try
					{
						socket.close();
					}
					catch(IOException e1)
					{
						if(D)
							Log.e(TAG, "Cound not close the socket");
						e1.printStackTrace();
					}
					disconnect();
				}
				return;
			}

			// Get the BluetoothSocket input and output streams
			try
			{
				inStream = socket.getInputStream();
				outStream = socket.getOutputStream();
			}
			catch(IOException e)
			{
				// Failed to get the streams
				disconnect();
				e.printStackTrace();
				return;
			}

			byte[] buffer = new byte[1024];
			byte ch;
			int bytes;
			String input;

			// Keep listening to the InputStream while connected
			while(true)
			{
				try
				{
					// Make a packet, use \n (new line or NL) as packet end
					// println() used in Arduino code adds \r\n to the end of the stream
					bytes = 0;
					while((ch = (byte) inStream.read()) != '\n')
					{
						buffer[bytes++] = ch;
					}
					// Prevent read errors (if you mess enough with it)
					if(bytes > 0)
					{
						// The carriage return (\r) character has to be removed
						input = new String(buffer, "UTF-8").substring(0, bytes - 1);

						if(D)
							Log.v(TAG, "Read: " + input);
					}
					busy = false;

				}
				catch(IOException e)
				{
					// read() will inevitably throw an error, even when just disconnecting
					if(!stoppingConnection)
					{
						if(D)
							Log.e(TAG, "Failed to read");
						e.printStackTrace();
						disconnect();
					}
					break;
				}
			}
		}

		public boolean write(String out)
		{
			if(outStream == null)
			{
				return false;
			}

			if(D)
				Log.v(TAG, "Write: " + out);
			try
			{
				if(out != null)
				{
					outStream.write(out.getBytes());
				}
				else
				{
					// This is a special case for the filler
					outStream.write(0);
				}
				// End packet with a new line
				outStream.write('\n');
				return true;
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			return false;
		}

		public void cancel()
		{
			try
			{
				if(inStream != null)
				{
					inStream.close();
				}
				if(outStream != null)
				{
					outStream.close();
				}
				if(socket != null)
				{
					socket.close();
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method sends data to the Bluetooth device in an unsynchronized
	 * manner, actually it calls the write() method inside the connected thread,
	 * but it also makes sure the device is not busy. If "r" is sent (reset
	 * flag) it will pass all flags and will be sent even if the device is busy.
	 * 
	 * @param out
	 *            String to send to the Bluetooth device
	 * @return Success of failure to write
	 */
	public boolean write(String out)
	{
		// The device hasn't finished processing last command, reset commands ("r") it always get sent
		if(busy && !out.equals(out))
		{
			if(D)
				Log.v(TAG, "Busy");
			return false;
		}
		busy = true;

		// Create temporary object
		BluetoothThread r;
		// Synchronize a copy of the BluetoothThread
		synchronized(this)
		{
			r = bluetoothThread;
		}
		// Perform the write unsynchronized
		return r.write(out);
	}

	/**
	 * Stop all threads
	 */
	public synchronized void disconnect()
	{
		// Do not stop twice
		if(!stoppingConnection)
		{
			stoppingConnection = true;
			if(D)
				Log.i(TAG, "Stop");
			if(bluetoothThread != null)
			{
				bluetoothThread.cancel();
				bluetoothThread = null;
			}
		}
	}

	@Override
	public IBinder onBind(Intent arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
