package pr.sna.snaprkit;

import pr.sna.snaprkit.utils.GeoManager;
import pr.sna.snaprkit.utils.GeoManager.GeoListener;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;

public class GeolocationService extends Service
{	
	// ------------------------------------------------------------------------
	// Members
	// ------------------------------------------------------------------------
	GeoManager mGeoManager;
	boolean mBroadcastResult;
	int mGeolocationResult;
	Location mLocation;
	int mCaller;
	
	private GeoListener mGeoListener = new GeoListener()
    {
		@Override
		public void onLocation(Location location, int caller)
		{
			// Log
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
			
			// Display the location in log    	
	        Double latitude = location.getLatitude();
	        Double longitude = location.getLongitude();
	        Float  accuracy = location.getAccuracy();
	        if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Got location: Provider: " + location.getProvider() + " Accuracy: " + accuracy.toString() + " Latitude: " + latitude.toString() + " Longitude" + longitude.toString());
	        
	        // Log
	        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
		}

		@Override
		public void onTerminate(Location location, int caller)
		{
			// Declare
			int geolocationResult = Global.GEOLOCATION_RESULT_TERMINATE;
			
			// We don't need to do anything on termination
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
			
			// Log location
			if (location != null)
			{
				Double latitude = location.getLatitude();
				Double longitude = location.getLongitude();
				Float accuracy = location.getAccuracy();
				if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Terminated with location " + latitude.toString() + ", " + longitude.toString() + " and accuracy " + accuracy.toString());
			}
			else
			{
				if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Terminated with location null");
			}
			
			// Set globals
			mLocation = location;
			mGeolocationResult = geolocationResult;
			
			// Determine how to process location
			if (mBroadcastResult == true)
			{
				// Send broadcast
				broadcastLocation(location, geolocationResult, mCaller);
				
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, "onTerminate sent geolocation broadcast to the broadcast receiver");
				
				// Stop the service itself
				GeolocationService.this.stopSelf();
			}
			
			// Log
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
		}

		@Override
		public void onTimeOut(Location location, int caller)
		{
			// Declare
			int geolocationResult = Global.GEOLOCATION_RESULT_TIMEOUT; 
			
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
			
			// Check location
			if (location != null)
			{
				// Log location
				Double latitude = location.getLatitude();
				Double longitude = location.getLongitude();
				Float accuracy = location.getAccuracy();
				if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Timed out with location " + latitude.toString() + ", " + longitude.toString() + " and accuracy " + accuracy.toString());
			}
			else
			{
				if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Timed out with location null");
			}
			
			// Set globals
			mLocation = location;
			mGeolocationResult = geolocationResult;
			
			// Determine how to process location
			if (mBroadcastResult == true)
			{
				// Send broadcast
				broadcastLocation(location, geolocationResult, mCaller);
				
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, "onTimeOut sent geolocation broadcast to the broadcast receiver");
				
				// Stop the service itself
				GeolocationService.this.stopSelf();
			}
			
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
		}
    };
	
	
	@Override
	public void onCreate()
	{
		// Log
		if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
		
		super.onCreate();
		
		// Log
		if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
	}

	@Override
	public void onDestroy()
	{
		// Log
		if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
		
		// Terminate
		mGeoManager.terminate();
		
		// Perform other cleanup
		super.onDestroy();
		
		// Log
		if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
	}
	
	private void broadcastLocation(Location location, int resultType, int caller)
	{
		if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
		
		// Send an intent to broadcast to the main app
		Intent intent = new Intent();
		intent.setAction(Global.INTENT_BROADCAST_LOCATION);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtra(Global.PARAM_ACTION, Global.BROADCAST_GEOLOCATION_DETAILS);
		intent.putExtra(Global.PARAM_GEOLOCATION_CALLER, caller);
		intent.putExtra(Global.PARAM_TYPE, resultType);
		intent.putExtra(Global.PARAM_LONGITUDE, (location != null)?location.getLongitude():0);
		intent.putExtra(Global.PARAM_LATITUDE, (location != null)?location.getLatitude():0);
		intent.putExtra(Global.PARAM_ACCURACY, (location != null)?location.getAccuracy():0);
		sendBroadcast(intent);
		
		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + "Sent broadcast with location info" );
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// Declare
		int result;
		
		// Log
		if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
		
		// Call the base implementation
		result = super.onStartCommand(intent, flags, startId);
		
		// Check parameters
		if (intent == null)
			return result;
		
		// Check the intent message type
		int action = intent.getIntExtra(Global.PARAM_ACTION, -1);
		if (action == Global.ACTION_GEOLOCATION_START)
		{
			// Set flags
			mGeolocationResult = Global.GEOLOCATION_RESULT_NONE;
			mBroadcastResult = false;
			mCaller = intent.getIntExtra(Global.PARAM_GEOLOCATION_CALLER, -1);
			
			// Start geolocation
			mGeoManager = new GeoManager(GeolocationService.this);
			mGeoManager.getLocation(mGeoListener, Global.GEOLOCATION_CAMERA);
			if (Global.LOG_MODE) Global.log(Global.TAG, " Set geomanager to  " + mGeoManager);
		}
		else if (action == Global.ACTION_GEOLOCATION_BROADCAST)
		{
			// Check the result
			if (mGeolocationResult != Global.GEOLOCATION_RESULT_NONE)
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, "onStartCommand sent geolocation broadcast to the broadcast receiver");
				
				// Broadcast the result
				broadcastLocation(mLocation, mGeolocationResult, mCaller);
				
				// Stop the service itself
				GeolocationService.this.stopSelf();
			}
			else
			{
				// Set flag to enable delayed broadcast
				mBroadcastResult = true;
			}
		}
		else if (action == -1)
		{
			// Do nothing
		}
		
		// Log
		if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
		
		// Start sticky
		return result;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}