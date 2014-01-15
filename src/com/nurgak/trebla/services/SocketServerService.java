package com.nurgak.trebla.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import org.apache.http.conn.util.InetAddressUtils;

import com.nurgak.trebla.BoundService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class SocketServerService extends BoundService implements Runnable
{
	ServerSocket serverSocket = null;
	Socket clientSocket = null;
	PrintWriter output = null;

	static InputStreamReader inputStreamReader = null;
	static BufferedReader bufferedReader = null;
	static String msg = null;

	SocketServerReceiver socketServerReceiver = null;

	int serverPort = 8888;

	public SocketServerService()
	{
		// start the socket server and listen to incoming client requests
		try
		{
			serverSocket = new ServerSocket(serverPort);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		Log.d("trebla", "IP: " + getLocalIpAddress() + ":" + serverPort);

		// check if the socket is open to start the listening thread
		new Thread(this).start();
	}

	private void socketServerStop()
	{
		output.close();
		output = null;

		try
		{
			bufferedReader.close();
			bufferedReader = null;
			inputStreamReader.close();
			inputStreamReader = null;
			clientSocket.close();
			clientSocket = null;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void run()
	{
		socketServerReceiver = new SocketServerReceiver();
		
		// start the broadcast receiver
		IntentFilter filter = new IntentFilter();
		filter.addAction("trebla");
		
		// TODO this throws a null pointer exception sometimes for some reason, maybe the receiver is not ready, try to loop while its null
		while(socketServerReceiver == null);
		registerReceiver(socketServerReceiver, filter);

		while(true)
		{
			Log.d("trebla", "Waiting for client");
			try
			{
				// accept the client connection (blocking)
				clientSocket = serverSocket.accept();

				inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
				bufferedReader = new BufferedReader(inputStreamReader);
				output = new PrintWriter(clientSocket.getOutputStream(), true);
			}
			catch(IOException e)
			{
				e.printStackTrace();
				break;
			}

			bindTreblaService();

			// get the client message
			while(true)
			{
				Log.d("trebla", "Listening");
				// read from client (blocking)
				try
				{
					msg = bufferedReader.readLine();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}

				// if not disconnecting on msg == null it loops indefinitely for some reason
				if(msg == null || msg.length() == 0 || msg.equals("close") || msg.equals("exit") || msg.equals("quit"))
				{
					Log.d("trebla", "Disconnecting");
					break;
				}

				Log.d("trebla", "Input: '" + msg + "'");

				// actually use the bounded trebla service
				if(treblaService == null)
				{
					// user might have connected and disconnected too fast and the instance might be null
					break;
				}

				output.append(treblaService.processMessage(msg));

				output.append("\n");
				output.flush();
			}

			unBindTreblaService();
			socketServerStop();
		}
	}

	@Override
	public void onDestroy()
	{
		// when something goes wrong inform the client
		if(output != null)
		{
			output.write("closing");
			output.flush();
			socketServerStop();
		}
		super.onDestroy();
	}

	// this function gets the local IPv4 address to show the user
	private String getLocalIpAddress()
	{
		try
		{
			for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
			{
				NetworkInterface intf = en.nextElement();
				for(Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
				{
					InetAddress inetAddress = enumIpAddr.nextElement();
					if(!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(inetAddress.getHostAddress()))
					{
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		}
		catch(SocketException e)
		{
			e.printStackTrace();
		}
		return "No IP Available";
	}

	public class SocketServerReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			// if a client is connected forward the data to it
			if(output != null)
			{
				output.append(intent.getStringExtra("data") + "\n");
				output.flush();
			}
		}
	}
}
