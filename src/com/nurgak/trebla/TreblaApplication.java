package com.nurgak.trebla;

import com.nurgak.trebla.services.SocketServerService;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class TreblaApplication extends Application
{
	BoundService commService;
	ServiceConnection serviceConnection;

	@Override
	public void onCreate()
	{
		// bind the connection service to the application
		// here the service is the socket connection service by default, when other options become available this shall be changed
		Intent intent = new Intent(this, SocketServerService.class);
		//Intent intent = new Intent(this, BluetoothClientService.class);
		serviceConnection = new ServiceConnection()
		{
			@Override
			public void onServiceConnected(ComponentName name, IBinder binder)
			{
				// bounded service instance which can be used to call its methods directly
				commService = ((BoundService.LocalBinder) binder).getBoundService();
			}

			@Override
			public void onServiceDisconnected(ComponentName name)
			{
				// TODO Auto-generated method stub
			}
		};
		
		if(bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE))
		{
			// communication service is bound and up
		}

		super.onCreate();
	}

	@Override
	public void onTerminate()
	{
		// when the activity is stopped the communication service must be unbound
		unbindService(serviceConnection);

		super.onTerminate();
	}
}
