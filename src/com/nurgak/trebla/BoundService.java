package com.nurgak.trebla;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

/**
 * This class may be bounded to an application, activity or another service. Its
 * purpose is the serve as a generic boundable service to other services.
 * <p>
 * Some methods are useful only for the communication classes such as binding a
 * trebla service as a trebla service (which also extends this class in order to
 * be bound to a communication class) will never bind another trebla service
 * itself.
 */
public abstract class BoundService extends Service
{
	// instance of the local binder to pass to the client
	private final IBinder localIBinder = new LocalBinder();

	// service and connection to the bounded class
	protected TreblaService treblaService = null;
	private ServiceConnection treblaServiceConnection = null;

	@Override
	public IBinder onBind(Intent intent)
	{
		// must return an IBinder for this service to be bound to the main activity
		return localIBinder;
	}

	// this binder will return the enclosing BinderService instance.
	public class LocalBinder extends Binder
	{
		// return enclosing BinderService instance
		public BoundService getBoundService()
		{
			// required in order to bind the service to the activity/application/service
			return BoundService.this;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// this will keep this service alive no matter what, unless explicitly stopped by user/asked by application
		return START_STICKY;
	}

	public void bindTreblaService()
	{
		// bind a TreblaService instance to this service
		Intent intent = new Intent(this, TreblaService.class);
		treblaServiceConnection = new ServiceConnection()
		{
			@Override
			public void onServiceConnected(ComponentName name, IBinder binder)
			{
				// bounded service instance which can be used to call its methods directly
				treblaService = (TreblaService) ((BoundService.LocalBinder) binder).getBoundService();
			}

			@Override
			public void onServiceDisconnected(ComponentName name)
			{
				// TODO Auto-generated method stub
			}
		};
		bindService(intent, treblaServiceConnection, Context.BIND_AUTO_CREATE);
	}

	// this method is only used by the communication classes in order to unbind from their trebla service
	public void unBindTreblaService()
	{
		if(treblaServiceConnection != null)
		{
			// by unbinding all the trebla service clients (this is the only one) it will get stop the service
			unbindService(treblaServiceConnection);
			treblaServiceConnection = null;
			treblaService = null;
		}
	}

	@Override
	public void onDestroy()
	{
		// just in case unbind from a bounded trebla service if there is any
		unBindTreblaService();
		super.onDestroy();
	}
}
