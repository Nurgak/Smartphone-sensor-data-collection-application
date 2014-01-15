package com.nurgak.trebla.services;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

public class UsbCommunicationManager implements Runnable
{
	static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

	UsbManager usbManager;
	UsbDevice usbDevice = null;
	UsbInterface usbCdcInterface = null;
	UsbInterface usbHidInterface = null;
	UsbEndpoint usbCdcRead = null;
	UsbEndpoint usbCdcWrite = null;
	UsbDeviceConnection usbCdcConnection;

	Thread readThread = null;
	volatile boolean readThreadRunning = true;

	PendingIntent permissionIntent;

	Context context;

	byte[] readBytes = new byte[256];

	public UsbCommunicationManager(Context context)
	{
		this.context = context;
		usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

		// ask permission from user to use the usb device
		permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		context.registerReceiver(usbReceiver, filter);
	}

	public void connect()
	{
		// check if there's a connected usb device
		if(usbManager.getDeviceList().isEmpty())
		{
			Log.d("trebla", "No connected devices");
			return;
		}

		// get the first (only) connected device
		usbDevice = usbManager.getDeviceList().values().iterator().next();

		// user must approve of connection if not in the /res/usb_device_filter.xml file 
		usbManager.requestPermission(usbDevice, permissionIntent);
	}

	public void stop()
	{
		usbDevice = null;
		usbCdcInterface = null;
		usbHidInterface = null;
		usbCdcRead = null;
		usbCdcWrite = null;

		context.unregisterReceiver(usbReceiver);
	}

	public String write(String data)
	{
		if(usbDevice == null)
		{
			return "no usb device selected";
		}

		int sentBytes = 0;
		if(!data.equals(""))
		{
			synchronized(this)
			{
				// send data to usb device
				byte[] bytes = data.getBytes();
				sentBytes = usbCdcConnection.bulkTransfer(usbCdcWrite, bytes, bytes.length, 1000);
			}
		}

		return Integer.toString(sentBytes);
	}

	public String read(StringBuilder dest)
	{
		if(usbCdcRead == null)
		{
			return "not connected to a device";
		}

		String state = "";

		if(readThread != null && readThread.isAlive())
		{
			readThreadRunning = false;
			state = "stopping usb listening thread";
		}
		else
		{
			readThreadRunning = true;
			readThread = new Thread(this);
			readThread.start();
			state = "starting usb listening thread";
		}

		return state;

		//		if(usbDevice == null)
		//		{
		//			return "no usb device selected";
		//		}
		//
		//		// reinitialize read value byte array
		//		//Arrays.fill(readBytes, (byte) 0);
		//
		//		// wait for some data from the mcu
		//		int recvBytes = usbCdcConnection.bulkTransfer(usbCdcRead, readBytes, readBytes.length, 3000);
		//
		//		if(recvBytes > 0)
		//		{
		//			for(int i = 0; i < recvBytes; ++i)
		//			{
		//				dest.append((char) readBytes[i]);
		//			}
		//
		//			Log.d("trebla", "Got some data: " + dest.toString());
		//		}
		//		else
		//		{
		//			Log.d("trebla", "Did not get any data: " + recvBytes);
		//		}
		//
		//		return Integer.toString(recvBytes);
	}

	public String listUsbDevices()
	{
		HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

		if(deviceList.size() == 0)
		{
			return "no usb devices found";
		}

		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		String returnValue = "";
		UsbInterface usbInterface;

		while(deviceIterator.hasNext())
		{
			UsbDevice device = deviceIterator.next();
			returnValue += "Name: " + device.getDeviceName();
			returnValue += "\nID: " + device.getDeviceId();
			returnValue += "\nProtocol: " + device.getDeviceProtocol();
			returnValue += "\nClass: " + device.getDeviceClass();
			returnValue += "\nSubclass: " + device.getDeviceSubclass();
			returnValue += "\nProduct ID: " + device.getProductId();
			returnValue += "\nVendor ID: " + device.getVendorId();
			returnValue += "\nInterface count: " + device.getInterfaceCount();

			for(int i = 0; i < device.getInterfaceCount(); i++)
			{
				usbInterface = device.getInterface(i);
				returnValue += "\n  Interface " + i;
				returnValue += "\n\tInterface ID: " + usbInterface.getId();
				returnValue += "\n\tClass: " + usbInterface.getInterfaceClass();
				returnValue += "\n\tProtocol: " + usbInterface.getInterfaceProtocol();
				returnValue += "\n\tSubclass: " + usbInterface.getInterfaceSubclass();
				returnValue += "\n\tEndpoint count: " + usbInterface.getEndpointCount();

				for(int j = 0; j < usbInterface.getEndpointCount(); j++)
				{
					returnValue += "\n\t  Endpoint " + j;
					returnValue += "\n\t\tAddress: " + usbInterface.getEndpoint(j).getAddress();
					returnValue += "\n\t\tAttributes: " + usbInterface.getEndpoint(j).getAttributes();
					returnValue += "\n\t\tDirection: " + usbInterface.getEndpoint(j).getDirection();
					returnValue += "\n\t\tNumber: " + usbInterface.getEndpoint(j).getEndpointNumber();
					returnValue += "\n\t\tInterval: " + usbInterface.getEndpoint(j).getInterval();
					returnValue += "\n\t\tType: " + usbInterface.getEndpoint(j).getType();
					returnValue += "\n\t\tMax packet size: " + usbInterface.getEndpoint(j).getMaxPacketSize();
				}
			}
		}

		return returnValue;
	}

	private void setupConnection()
	{
		// find the right interface
		for(int i = 0; i < usbDevice.getInterfaceCount(); i++)
		{
			// communications device class (CDC) type device
			if(usbDevice.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA)
			{
				usbCdcInterface = usbDevice.getInterface(i);

				// find the endpoints
				for(int j = 0; j < usbCdcInterface.getEndpointCount(); j++)
				{
					if(usbCdcInterface.getEndpoint(j).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK)
					{
						if(usbCdcInterface.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_OUT)
						{
							// from host to device
							usbCdcWrite = usbCdcInterface.getEndpoint(j);
						}

						if(usbCdcInterface.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_IN)
						{
							// from device to host
							usbCdcRead = usbCdcInterface.getEndpoint(j);
						}
					}
				}
			}
		}
	}

	private final BroadcastReceiver usbReceiver = new BroadcastReceiver()
	{
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if(ACTION_USB_PERMISSION.equals(action))
			{
				// broadcast is like an interrupt and works asynchronously with the class, it must be synced just in case
				synchronized(this)
				{
					if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
					{
						// fetch all the endpoints
						setupConnection();

						// open and claim interface
						usbCdcConnection = usbManager.openDevice(usbDevice);
						usbCdcConnection.claimInterface(usbCdcInterface, true);

						// set dtr to true (ready to accept data)
						usbCdcConnection.controlTransfer(0x21, 0x22, 0x1, 0, null, 0, 0);

						// set flow control to 8N1 at 9600 baud
						/* int baudRate = 9600; byte stopBitsByte = 1; byte
						 * parityBitesByte = 0; byte dataBits = 8; byte[] msg =
						 * { (byte) (baudRate & 0xff), (byte) ((baudRate >> 8) &
						 * 0xff), (byte) ((baudRate >> 16) & 0xff), (byte)
						 * ((baudRate >> 24) & 0xff), stopBitsByte,
						 * parityBitesByte, (byte) dataBits }; */

						//Log.d("trebla", "Flow: " + connection.controlTransfer(0x21, 0x20, 0, 0, new byte[] {(byte) 0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08}, 7, 0));

						//connection.controlTransfer(0x21, 0x20, 0, 0, msg, msg.length, 5000);
					}
					else
					{
						Log.d("trebla", "Permission denied for USB device");
					}
				}
			}
			else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
			{
				if(usbDevice != null)
				{
					usbCdcConnection.releaseInterface(usbCdcInterface);
					usbCdcConnection.close();
					usbCdcConnection = null;
					usbDevice = null;
					Log.d("trebla", "USB connection closed");
				}
			}
		}
	};

	@Override
	public void run()
	{
		Log.d("trebla", "Started the usb linstener");
		ByteBuffer buffer = ByteBuffer.allocate(255);
		UsbRequest request = new UsbRequest();
		request.initialize(usbCdcConnection, usbCdcRead);
		
		String dataByte, data = "";
		int packetState = 0;
		
		while(readThreadRunning)
		{
			// queue a request on the interrupt endpoint
			request.queue(buffer, buffer.capacity());
			// wait for status event
			if(usbCdcConnection.requestWait() == request)
			{
				// there is no way to know how many bytes are coming, so simply forward the non-null values
				
				for(int i = 0; i < buffer.capacity() && buffer.get(i) != 0 ; i++)
				{
					// transform ascii (0-255) to its character equivalent and append
					dataByte = Character.toString((char) buffer.get(i));
					if(packetState == 0 && dataByte.equals("["))
					{
						// start
						packetState = 1;
						data += dataByte;
					}
					else if(packetState == 1 && !dataByte.equals("]"))
					{
						// in-between
						data += dataByte;
					}
					else if(packetState == 1 && dataByte.equals("]"))
					{
						// end
						packetState = 2;
						data += dataByte;
						break;
					}
				}
				
				if(packetState == 2)
				{
					// send data to client
					Intent intent = new Intent();
					intent.setAction("trebla");
					intent.putExtra("data", data);
					context.sendBroadcast(intent);
					
					// reset packet
					packetState = 0;
					data = "";
				}
			}
			else
			{
				Log.e("trebla", "Was not able to read from USB device, ending listening thread");
				readThreadRunning = false;
				break;
			}
		}
	}
}
