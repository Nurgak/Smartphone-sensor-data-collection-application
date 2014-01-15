package com.nurgak.trebla.services;

import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPicture extends Service implements PictureCallback, Camera.PreviewCallback
{
	SurfaceView dummySurfaceView;
	SurfaceHolder dummySurfaceHolder;
	Camera camera = null;

	// image bitmap data
	Bitmap bmp = null;

	byte[] pictureData = null;

	Context context;

	public CameraPicture(Context context)
	{
		//this.context = context.getApplicationContext();

		camera = Camera.open();

		Camera.Parameters parameters = camera.getParameters();
		parameters.setPictureFormat(ImageFormat.JPEG);

		camera.setParameters(parameters);
	}

	public byte[] getPictureData()
	{
		return pictureData;
	}

	public void takePicture()
	{
		// android needs to show a preview, so direct the preview to a dummy surface view
		dummySurfaceView = new SurfaceView(this);
		//dummySurfaceView = (SurfaceView) this.findViewById(R.id.cameraSurfaveView);
		//SurfaceTexture dummySurfaceTexture = new SurfaceTexture(1);
		dummySurfaceHolder = dummySurfaceView.getHolder();
		try
		{
			//camera.setPreviewDisplay(null);
			camera.setPreviewDisplay(dummySurfaceView.getHolder());
			//camera.setPreviewTexture(dummySurfaceTexture);
			camera.setPreviewCallback(this);
			camera.startPreview();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		Log.d("trebla", "Taking a picture");

		// take a picture and call an event once the data is available
		camera.takePicture(null, null, this);

		Log.d("trebla", "Delaying");

		camera.release();
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera)
	{
		// decode the data obtained by the camera into a bitmap
		pictureData = data;
		bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera)
	{
		pictureData = data;
		Log.d("trebla", "Got a preview frame");
	}

	@Override
	public IBinder onBind(Intent arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
