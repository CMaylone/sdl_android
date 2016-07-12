package com.smartdevicelink.transport;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class SdlRouterStatusProvider {

	private static final String TAG = "SdlRouterStateProvider";
		
	private Context context = null;
	private boolean isBound = false;
	ConnectedStatusCallback cb = null;
	Messenger routerServiceMessenger = null;
	private ComponentName routerService = null;

	final Messenger clientMessenger = new Messenger(new ClientHandler());
	
	private ServiceConnection routerConnection= new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d(TAG, "Bound to service " + className.toString());
			routerServiceMessenger = new Messenger(service);
			isBound = true;
			//So we just established our connection
			//Register with router service
			Message msg = Message.obtain();
			msg.what = TransportConstants.ROUTER_STATUS_CONNECTED_STATE_REQUEST;
			msg.replyTo = clientMessenger;
			try {
				routerServiceMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
				if(cb!=null){
					cb.onConnectionStatusUpdate(false, context);
				}
			}			
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d(TAG, "UN-Bound from service " + className.getClassName());
			routerServiceMessenger = null;
			isBound = false;
		}
	};

	public SdlRouterStatusProvider(Context context, ComponentName service, ConnectedStatusCallback callback){
		if(context == null || service == null || callback == null){
			throw new IllegalStateException("Supplied params are not correct. Context == null? "+ (context==null) + " ComponentName == null? " + (service == null) + " ConnectedStatusListener == null? " + callback);
		}
		this.context = context;
		this.routerService = service;
		this.cb = callback;

	}
	
	public void checkIsConnected(){
		if(!bindToService()){
			//We are unable to bind to service
			cb.onConnectionStatusUpdate(false, context);
		}
	}
	
	public void cancel(){
		if(isBound){
			unBindFromService();
		}
	}
	
	private boolean bindToService(){
		if(isBound){
			return true;
		}
		Intent bindingIntent = new Intent();
		bindingIntent.setClassName(this.routerService.getPackageName(), this.routerService.getClassName());//This sets an explicit intent
		//Quickly make sure it's just up and running
		context.startService(bindingIntent);
		bindingIntent.setAction( TransportConstants.BIND_REQUEST_TYPE_STATUS);
		return context.bindService(bindingIntent, routerConnection, Context.BIND_AUTO_CREATE);
	}
	
	private void unBindFromService(){
		try{
			if(context!=null && routerConnection!=null){
				context.unbindService(routerConnection);
			}else{
				Log.w(TAG, "Unable to unbind from router service, context was null");
			}
			
		}catch(IllegalArgumentException e){
			//This is ok
		}
	}
	
	@SuppressLint("HandlerLeak")
	class ClientHandler extends Handler {
    	@Override
        public void handleMessage(Message msg) {
    		  switch (msg.what) {
    		  case TransportConstants.ROUTER_STATUS_CONNECTED_STATE_RESPONSE:
    			  if(cb!=null){
    				  cb.onConnectionStatusUpdate(msg.arg1 == 1, context);
    			  }
    			  unBindFromService();
    			  routerServiceMessenger =null;
    			  break;
    		  default:
    			  break;
    		  }
    	}
	};
	
	public interface ConnectedStatusCallback{
		public void onConnectionStatusUpdate(boolean connected, Context context);
	}
	
}