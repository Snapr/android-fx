package pr.sna.snaprkit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import pr.sna.snaprkit.PictureAcquisitionManager.PictureAcquisitionListener;
import pr.sna.snaprkit.dummy.FeatherActivity;
import pr.sna.snaprkit.utils.AlertUtils;
import pr.sna.snaprkit.utils.AssetUtils;
import pr.sna.snaprkit.utils.AssetsCopier;
import pr.sna.snaprkit.utils.AssetsCopier.AssetCopierListener;
import pr.sna.snaprkit.utils.CameraUtils;
import pr.sna.snaprkit.utils.CredentialsUtils;
import pr.sna.snaprkit.utils.FileUtils;
import pr.sna.snaprkit.utils.GeoManager;
import pr.sna.snaprkit.utils.GeoManager.GeoListener;
import pr.sna.snaprkit.utils.NetworkUtils;
import pr.sna.snaprkit.utils.UrlUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SnaprKitFragment extends Fragment
{
	// Constants
	private static final int ACTION_REQUEST_FEATHER = 100;
	
	// Members
	private View mView;
	private boolean mQueueUploadModeWifiOnly = false;
	private boolean mQueueUploadModeOn = true;
	private WebView mWebView;
	private ArrayList<UrlMapping> mActionMappings = new ArrayList<UrlMapping>();
	private String  mUserName;
	private String  mAccessToken;
	private PictureAcquisitionManager mPictureAcquisitionManager;
	private GeoManager mGeoManager; // Used for snapr://get_location
	private ConnectivityBroadcastReceiver mConnectivityReceiver;
	private UploadBroadcastReceiver mServiceCallbackReceiver;
	private boolean mMenuInitDone = false;
	private double mLastPictureLatitude = 0;
	private double mLastPictureLongitude = 0;
	private Date mLastPictureDate = null;
	private String mCurrentUrl = null;
	@SuppressWarnings("unused")
	private boolean mRestoredFromSavedState = false;
	private String mSharedPictureFileName = null;
	private SnaprKitListener mSnaprKitListener = null;
	private PictureAcquisitionListener mPictureAcquisitionListener;

	private String mFilterPackPath;		// the location (under assets) where the filter packs will be loaded from
	private String mStickerPathPath;	// the location (under assets) where the sticker packs will be loaded from
	
	private Intent mPendingIntent;
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
    	// Save UI state changes to the savedInstanceState.
    	// This bundle will be passed to onCreate if the process is
    	// killed and restarted.
		
		// Save the WebViewActivity members
		outState.putBoolean("mMenuInitDone", mMenuInitDone);
		outState.putDouble("mLastPictureLatitude", mLastPictureLatitude);
		outState.putDouble("mLastPictureLongitude", mLastPictureLongitude);
		outState.putLong("mLastPictureDate", (mLastPictureDate == null)?0:mLastPictureDate.getTime());
		outState.putString("mCurrentUrl", mCurrentUrl);
		
		// Save the CameraManager members
		int imageSource = mPictureAcquisitionManager.getImageSource();
		outState.putInt("mImageSource", imageSource);
		
		Uri imageUri = mPictureAcquisitionManager.getImageUri();
		String imagePath = UrlUtils.imageUri2Path(imageUri);
		outState.putString("mImagePath", imagePath);
		
		Location location = mPictureAcquisitionManager.getLocation();
		outState.putDouble("mLatitude", (location!=null)?location.getLatitude():-1);
		outState.putDouble("mLongitude", (location!=null)?location.getLongitude():-1);
		
		outState.putBoolean("mIsActive", mPictureAcquisitionManager.isActive());
		outState.putBoolean("mIsGeolocationActive", mPictureAcquisitionManager.isGeolocationActive());
		
		outState.putBoolean("mNetworkProviderEnabled", mPictureAcquisitionManager.getNetworkProviderEnabled());
		outState.putBoolean("mGpsProviderEnabled", mPictureAcquisitionManager.getGpsProviderEnabled());
		outState.putBoolean("mWifiProviderEnabled", mPictureAcquisitionManager.getWifiProviderEnabled());
		
		outState.putLong("mPhotoTimestamp", mPictureAcquisitionManager.getPhotoTimestamp());
		
		// Save the WebView history
		mWebView.saveState(outState);
	}	

	@Override public void onDestroy() {
		super.onDestroy();
		closeBroadcastReceivers();
	}
	
	@Override
	public void onPause()
	{
		// Call base implementation
		super.onPause();
		
		// Shut down broadcast receivers
		closeBroadcastReceivers();
		
		// Save the queue settings
		saveQueueSettings();
	}

	@Override
	public void onResume()
	{
		// Call base implementation
		super.onResume();
		
    	// Initialize the connectivity and upload broadcast receivers
    	initBroadcastReceivers();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (mView == null)
		{
			// Inflate view
			mView = inflater.inflate(R.layout.snaprkit_main, container, false);
			
	        // Prepare the webview
			initWebView(mView, savedInstanceState);
		}
		else
		{
			ViewGroup parent = (ViewGroup)mView.getParent();
			parent.removeView(mView);
		}
		
		return mView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		
		// Different handling for logging mode vs normal app
		if (Global.LOG_MODE)
		{
			inflater.inflate(R.menu.snaprkit_debug, menu);
		}
		else
		{
			super.onCreateOptionsMenu(menu, inflater);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Different handling for logging mode vs normal app
		if (Global.LOG_MODE)
		{
		    if (item.getItemId() == R.id.clearLogs)
			{
				clearLogs();
			}
			else if (item.getItemId() == R.id.sendCurrentLog)
			{
				sendLog(Global.FILE_LOG.getAbsolutePath());
			}
			else if (item.getItemId() == R.id.sendPreviousLog)
			{
				sendPreviousLog();
			}
		    return true;
		}
		else
		{
			return super.onOptionsItemSelected(item);
		}
	}

	private PictureAcquisitionListener getPictureAcquisitionListener() {
		if (mPictureAcquisitionListener != null) return mPictureAcquisitionListener;
		mPictureAcquisitionListener = new PictureAcquisitionListener()
	    {
			@Override
			public void onPictureAcquired(Context context, String filename, int dataSource, double latitude, double longitude, long timestamp)
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + " with filename " + filename);
				
				// Save the latitude and longitude
				mLastPictureLatitude = latitude;
				mLastPictureLongitude = longitude;
				mLastPictureDate = new Date();
				
				// Display pic options page
				if (filename != null)
				{
					// Process differently based on data source
					if (dataSource == PictureAcquisitionManager.PICTURE_SOURCE_CAMERA)
					{
						// For camera, we already have a copy of the picture, so edit in place
						SnaprImageEditFragmentActivity.Builder builder = new SnaprImageEditFragmentActivity.Builder(new File(filename), new File(filename), true, timestamp);
						builder.setStickerPackPath(mStickerPathPath);
						builder.setFilterPackPath(mFilterPackPath);
						displayPhotoEdit(SnaprKitApplication.getInstance(), builder);
					}
					else
					{
						// For gallery provide output filename so module makes copy of source picture
						File outputFile = new File(FileUtils.getDCIMCameraDirectory() + "/" + CameraUtils.getPictureName());
						SnaprImageEditFragmentActivity.Builder builder = new SnaprImageEditFragmentActivity.Builder(new File(filename), outputFile);
						builder.setStickerPackPath(mStickerPathPath);
						builder.setFilterPackPath(mFilterPackPath);
						displayPhotoEdit(SnaprKitApplication.getInstance(), builder);
					}
				}
				else
				{
					if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Got filename NULL");
					if (mWebView.canGoBack()) mWebView.goBack();
				}
				
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
			}
	    };
	    return mPictureAcquisitionListener;
	}
	
    // GeoListener to support snapr://get_location
    private GeoListener mGeoListener = new GeoListener()
    {
		@Override
		public void onLocation(Location location, int caller)
		{
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + " from caller " + caller);
			
			// Display the location in log    	
	        Double latitude = location.getLatitude();
	        Double longitude = location.getLongitude();
	        Float  accuracy = location.getAccuracy();
	        if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Got location: Provider: " + location.getProvider() + " Accuracy: " + accuracy.toString() + " Latitude: " + latitude.toString() + " Longitude" + longitude.toString());
	        //displayLocation(location);
	        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod() + " from caller " + caller);
		}

		@Override
		public void onTerminate(Location location, int caller)
		{
			// We don't need to do anything on termination
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + " from caller " + caller);
			
			if (caller == Global.GEOLOCATION_GET_LOCATION)
			{
				onFinishedSnaprGetLocation(location);
			}
			else
			{
				onFinishedPhotoShareLocation(location);
			}
			
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod() + " from caller " + caller);
		}

		@Override
		public void onTimeOut(Location location, int caller)
		{
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + " from caller " + caller);
			
			if (caller == Global.GEOLOCATION_GET_LOCATION)
			{
				onFinishedSnaprGetLocation(location);
			}
			else
			{
				onFinishedPhotoShareLocation(location);
			}
			
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod() + " from caller " + caller);
		}
    };

	public class ConnectivityBroadcastReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
			
			// Get network info for item that just completed
			NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			
			// Check result
			if(info != null)
			{
				// Get connectivity state
				boolean connected = (info.getState() == NetworkInfo.State.CONNECTED);
	    		
	    		// Is the network connected?
	    		if (connected)
	    		{
	    			// Is the queue set to Wifi only?
	    			if (mQueueUploadModeWifiOnly)
	    			{
	    				// Check network type
	    				if(info.getType() == ConnectivityManager.TYPE_WIFI)
	    				{
	    					// Update the settings
	    					if (mMenuInitDone)
		    				{
	    						updateQueueSettings(true, mQueueUploadModeWifiOnly);
		    				}
	    				}
	    			}
	    			else
	    			{
	    				// Update settings
	    				if (mMenuInitDone)
	    				{
	    					updateQueueSettings(true, mQueueUploadModeWifiOnly);
	    				}
	    			}
	    		}			
			}
			
			if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
		}
    }
    
	public class UploadBroadcastReceiver extends BroadcastReceiver
	{
		int  numUploadsFailed = 0;
		Date dateLastFailed = null; 
		
		@Override
		public void onReceive(Context context, Intent intent)
		{
			// Log
			if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + " with intent action " + intent.getAction().toString());
			
			if (intent.getAction().equals(Global.INTENT_BROADCAST_UPLOAD))
			{
				// Get data from the intent
				int broadcast = intent.getIntExtra(Global.PARAM_ACTION, -1);
				
				// Log
				if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received boadcast with id " + broadcast);
				
				// See the type of broadcast
				if (broadcast == Global.BROADCAST_UPLOAD_STARTED ||
					broadcast == Global.BROADCAST_UPLOAD_CANCELED ||
					broadcast == Global.BROADCAST_UPLOAD_COMPLETED)
				{
					// Log
					if (broadcast == Global.BROADCAST_UPLOAD_STARTED)
					{
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received broadcast for upload started");	
					}
					else if (broadcast == Global.BROADCAST_UPLOAD_CANCELED)
					{
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received broadcast for upload cancelled");
					}
					else if (broadcast == Global.BROADCAST_UPLOAD_COMPLETED)
					{
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received broadcast for upload completed");
					}
					
					// Get num uploads
					int numUploads = intent.getIntExtra(Global.PARAM_NUM_UPLOADS, -1);
					
					// Log
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Number of uploads is " + numUploads);
					
					// Check number of uploads
					if (numUploads == -1)
					{
						// Log 
						if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Error - Upload intent did not contain the number of uploads");
					}
					else
					{						
						// Call JavaScript
						mWebView.loadUrl("javascript:upload_count(" + numUploads + ");");
						
						// Log
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Called JavaScript with upload count " + numUploads);
					}
					
					// For upload cancelled we need to call the upload_cancelled JS function
					if (broadcast == Global.BROADCAST_UPLOAD_CANCELED)
					{
						String id = intent.getStringExtra(Global.PARAM_ID);
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Sending upload_cancelled('" + id + "')");
						mWebView.loadUrl("javascript:upload_cancelled('" + id + "');");
					}
					
					// For upload completed call the upload_complete function
					// For upload completed, also see if we need to notify of signups
					if (broadcast == Global.BROADCAST_UPLOAD_COMPLETED)
					{
						// Call upload_completed JS
						String id = intent.getStringExtra(Global.PARAM_ID);
						String snaprId = intent.getStringExtra(Global.PARAM_SNAPR_ID);
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Sending upload_completed('" + id + "', '"  + snaprId + "')");
						mWebView.loadUrl("javascript:upload_completed('" + id + "', '"  + snaprId + "')");
						
						// Log
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Determining signups needed");
						
						String signupsNeeded = intent.getStringExtra(Global.PARAM_SIGNUPS_NEEDED);
						String redirectUrl = "";
						if (signupsNeeded != null && signupsNeeded.length() != 0)
						{
							// Log
							if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Need signups for " + signupsNeeded);
							
							// Get the needed signups URL and load it
							String url = getSignupsNeededUrl(snaprId, signupsNeeded, redirectUrl);
							if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Signups URL is " + url);
							mWebView.loadUrl(url);
						}
						else
						{
							// Log
							if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": No signups needed");
						}
					}
				}
				else if (broadcast == Global.BROADCAST_UPLOAD_PROGRESS)
				{
					// Log
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received broadcast for upload progress...");
					
					// Check if we must show progress
					/*
					if (mSendUploadProgress == true)
					{
					*/
						// Extract intent data
						String jsonData = intent.getStringExtra(Global.PARAM_JSON_DATA);
						
						// Create JSON for upload progress
						try
						{
							// Get queue size
							JSONObject json = new JSONObject(jsonData);
							JSONArray uploads = json.getJSONArray("uploads");
							int numUploads = uploads.length();
														
							// Call JavaScript
							if (numUploads > 0)
							{
								if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Outputting progress JSON..." + jsonData);
								mWebView.loadUrl("javascript:upload_progress('" + jsonData + "','json_text');");
							}
						}
						catch(Exception e)
						{
							// Log 
							if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Error - Failed to create upoad progress JSON and send it to JavaScript");
						}
					/*
					}
					else
					{
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Not showing upload progress...");
					}
					*/
				}
				else if (broadcast == Global.BROADCAST_UPLOAD_STATUS)
				{					
					// Log
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received broadcast for upload status");
					
					// Log
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Updating queue based on upload status");

					// Create JSON for upload progress
					try
					{
					
						// Extract intent data
						String jsonData = intent.getStringExtra(Global.PARAM_JSON_DATA);
						
						// Get queue size
						JSONObject json = new JSONObject(jsonData);
						JSONArray uploads = json.getJSONArray("uploads");
						int numUploads = uploads.length();
													
						// Call JavaScript
						if (numUploads > 0)
						{
							// Update the progress
							if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Outputting progress JSON..." + jsonData);
							mWebView.loadUrl("javascript:upload_count(" + numUploads + ");");
							mWebView.loadUrl("javascript:upload_progress('" + jsonData + "','json_text');");
							mWebView.loadUrl("javascript:upload_count(" + numUploads + ");");
						}
					}
					catch (Exception e)
					{
						if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed with error " + e);
					}
				}
				else if (broadcast == Global.BROADCAST_UPLOAD_ERROR)
				{
					// Display alert dialog
					String errorMessage = intent.getStringExtra(Global.PARAM_ERROR_MESSAGE);
					showUploadError(errorMessage);
					
					// Check availability of connection and increment internal
					if (NetworkUtils.isAnyConnected(SnaprKitApplication.getInstance()))
					{
						// Check date of last failure
						Date now = new Date();
						if ((now.getTime() - ((dateLastFailed!=null)?dateLastFailed.getTime():0)) < Global.CONNECTION_FAILURE_TIME_THRESHOLD )
						{
							// Update last failure date and time
							numUploadsFailed = numUploadsFailed + 1;
							dateLastFailed = now;
							
							// Check if we exceeded threshold
							if (numUploadsFailed >= Global.CONNECTION_FAILURE_NUM_THRESHOLD)
							{
								// Set the queue to paused
								updateQueueSettings(false, mQueueUploadModeWifiOnly);
								
								// Reset the data
								numUploadsFailed = 0;
								dateLastFailed = null;
							}
						}
					}
				}
				else
				{
					// Log
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received broadcast for an unknown event");
				}
			}
			
			// Log
			if (Global.LOG_MODE) Global.log( " <- " + Global.getCurrentMethod());
		}
	}
	
	/**
     * Function called from onPictureAcquired or after we return from Aviary 
     */
	private void displayPhotoEdit(Context context, SnaprImageEditFragmentActivity.Builder builder)
	{
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
		Intent intent = SnaprImageEditFragmentActivity.getIntentForStartActivity(context, builder); 
		if (getActivity() != null) startActivityForResult(intent, SnaprImageEditFragmentActivity.EDIT_IMAGE);
		else mPendingIntent = intent;
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
	}
	
	/**
     * Function called from onPictureAcquired or after we return from Aviary 
     */
	private void displayPhotoShareOptions(String fileName, double latitude, double longitude)
	{
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
		
		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": After pic taken queueOn " + mQueueUploadModeOn + " queueWifi " + mQueueUploadModeWifiOnly);
		
		if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Got filename " + fileName);
		String redirectUrl = getSnaprUrl(UrlUtils.getFullLocalUrl(Global.URL_UPLOAD), true);
		String url = getSharePictureUrl(fileName, latitude, longitude, redirectUrl);
		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Redirecting to " + url);
		mWebView.loadUrl(url);
		
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
	}
	
    /**
     * Function called after we receive location for snapr://get_location via 
     * either termination or timeout 
     */
    private void onFinishedSnaprGetLocation(Location location)
    {	
		// Log location
		if (location != null)
		{
			// Log
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			float accuracy = location.getAccuracy();
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Terminated with location " + latitude + ", " + longitude + " and accuracy " + accuracy);
			
			// Communicate with JavaScript
			mWebView.loadUrl("javascript:set_location(" + latitude + ", " + longitude + ")");
		}
		else
		{
			// Log
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Terminated with location null");
			
			// Communicate with JavaScript
			mWebView.loadUrl("javascript:location_error('" + R.string.snaprkit_location_error + "')");
		}
		
		// Close the pending dialog
		mGeoManager.cancelLocationPendingDialog();
    }
    
    private void onFinishedPhotoShareLocation(Location location)
    {
    	// Declare
    	double latitude = 0;
    	double longitude = 0;
    	
    	if (location != null)
		{
			// Log
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			float accuracy = location.getAccuracy();
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Terminated with location " + latitude + ", " + longitude + " and accuracy " + accuracy);
			
			// Get the shared picture data
			SharedPictureInfo info = getSharedPictureInfo();
			String fileName = info.getFileName();
			
			// Get the URL
	        String url = getSharePictureUrl(fileName, latitude, longitude, null);
	        
	        // Go to URL
	        mWebView.loadUrl(url);
		}
		else
		{
			// Log
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Terminated with location null");
		}
    	
    	// Close the pending dialog
		mGeoManager.cancelLocationPendingDialog();
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (requestCode)
		{
		    case PictureAcquisitionManager.TAKE_PICTURE:
		    {
		    	// Process result
		    	mPictureAcquisitionManager.setPictureAcquisitionListener(getPictureAcquisitionListener());
		    	mPictureAcquisitionManager.onActivityResult(requestCode, resultCode, data);
		        
		        break;
		    }
		    
		    case PictureAcquisitionManager.GPS_SETTINGS_SCREEN:
		    {
		    	mPictureAcquisitionManager.setPictureAcquisitionListener(getPictureAcquisitionListener());
		    	mPictureAcquisitionManager.onActivityResult(requestCode, resultCode, data);
		    	
		    	break;
		    }
		    
		    case PictureAcquisitionManager.SELECT_PICTURE:
		    {
		    	// Process result
		    	mPictureAcquisitionManager.setPictureAcquisitionListener(getPictureAcquisitionListener());
		    	mPictureAcquisitionManager.onActivityResult(requestCode, resultCode, data);
		        
		        break;
		    }
		    
		    case WebViewExternalActivity.DISPLAY_EXTERNAL_URL:
		    {		    	
		    	if (resultCode == Activity.RESULT_OK)
		        {
		    		// Extract the info
		        	String url = data.getStringExtra(Global.PARAM_URL);
		        	
		        	// Log
		        	if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Returned from external URL display with redirect URL " + url);
		        	
		        	// Navigate to page
		        	Action action = getActionForUrl(url);
					action.run(url);
		        }
		    	else
		    	{
		    		if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Returned from external URL display with a non-OK error code");
		    	}
		    	break;
		    }
		    // FX module photo edit complete
		    case SnaprImageEditFragmentActivity.EDIT_IMAGE:
		    {
		    	if (resultCode == Activity.RESULT_OK)
		        {
			    	// Get the filename
					String fileName = data.getStringExtra(SnaprImageEditFragmentActivity.EXTRA_FILEPATH);
					
					// Get the analytics
					ArrayList<String> analytics = data.getStringArrayListExtra(SnaprImageEditFragmentActivity.EXTRA_ANALYTICS);
					
					// Send analytics data out through listener
					if (mSnaprKitListener != null && analytics != null)
					{
						for (int i = 0; i< analytics.size(); i++)
						{
							String url = analytics.get(i);
							mSnaprKitListener.onSnaprKitParent(url);
						}
					}
					
					// Display the share options
					displayPhotoShareOptions(fileName, mLastPictureLatitude, mLastPictureLongitude);
		        }
		    	else
		    	{
		    		if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Returned from FX module with a non-OK error code");
		    		mWebView.loadUrl(getStartupUrl());
		    	}
		    	break;
		    }
		    
		    case ACTION_REQUEST_FEATHER:
		    {
		    	// Enable Aviary Feather based on settings
		    	if (Global.FEATURE_AVIARY_SDK)
		    	{
			    	// Get the URI
			    	String fileName = null;
			    	
			    	// Check activity result
			    	if (resultCode == Activity.RESULT_OK)
			    	{
			    		// Get the image uri
				    	Uri imageUri = data.getData();
			    		
				    	// Transform the URI to a filename
				    	fileName = UrlUtils.getRealPathFromURI(SnaprKitFragment.this.getActivity(), imageUri);
				    	
				    	// Re-tag the picture (due to Aviary dropping the latitude and longitude)
				    	Location location = new Location("");
				    	location.setTime((mLastPictureDate!=null)?mLastPictureDate.getTime():0);
				    	location.setLatitude(mLastPictureLatitude);
				    	location.setLongitude(mLastPictureLongitude);
				    	CameraUtils.geotagPicture(fileName, location);
			    	}
			    	
			    	// Display the picture options
			    	if (fileName != null)
			    	{
			    		displayPhotoShareOptions(fileName, mLastPictureLatitude, mLastPictureLongitude);
			    	}
			    	else
			    	{
			    		if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Got filename NULL");
			    	}
		    	}
		    	
		    	break;
		    }
	    }
		
	}
    
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// Handle screen orientation changes
		super.onConfigurationChanged(newConfig);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Create regular suspects
    	super.onCreate(savedInstanceState);
    	
    	// Set the options menu
    	if (Global.LOG_MODE) setHasOptionsMenu(true);
    	
    	// Retain instance
    	setRetainInstance(true);
    	
        // Log
        if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    }
    
    @Override public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	
    	// Init rest of the app
        init(savedInstanceState);
        
        if (mPendingIntent != null) {
        	startActivityForResult(mPendingIntent, SnaprImageEditFragmentActivity.EDIT_IMAGE);
        	mPendingIntent = null;
        }
        
//        Location location = mPictureAcquisitionManager.getLocation();
//        int callerId = mPictureAcquisitionManager.getCallerId();
//        if (location == null || callerId == -1) return;
//        // Proceed based on caller
//		
//        if (callerId == Global.GEOLOCATION_CALLER_0)
//		{
//			// Go to onPictureTaken2
//			if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Continuing with onPictureTaken2");
//			mPictureAcquisitionManager.onPictureAcquired2(mPictureAcquisitionManager.getLocation());
//		}
//		else if (callerId == Global.GEOLOCATION_CALLER_1)
//		{
//			// Resume with attemptTagging2
//			mPictureAcquisitionManager.attemptTagging2(location);
//		}
    }
    
    
    
	private boolean onBackPressed()
	{
		// Get the current webview url
		String currentUrl = mWebView.getUrl();
		String previousUrl = UrlUtils.getPreviousUrl(mWebView);
		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod()+": Got prev url " + previousUrl);
		
		// Specialized handling
		if (currentUrl != null && UrlUtils.normalUrl(currentUrl).startsWith(UrlUtils.getFullLocalUrl(Global.URL_MENU)))
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Specialized back button handling for menu - deferring to OS for menu page");
			//super.onBackPressed();
			return false;
		}
		else if (previousUrl != null && UrlUtils.normalUrl(previousUrl).startsWith(UrlUtils.getFullLocalUrl(Global.URL_MENU)))
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Specialized back button handling for going back to menu");
			String url = getStartupUrl(); // Retrieve the URL with all its initialization params
			mWebView.loadUrl(url);
			return true;
		}
		else if (currentUrl != null & previousUrl != null && UrlUtils.normalUrl(currentUrl).startsWith(UrlUtils.getFullLocalUrl(Global.URL_FEED)) && UrlUtils.normalUrl(previousUrl).startsWith(UrlUtils.getFullLocalUrl(Global.URL_PHOTO_SHARE)))
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Specialized back button handling for post-picture queue - going to camera");
			String url = getSnaprUrl(UrlUtils.getFullLocalUrl(Global.URL_UPLOAD), true);
			mWebView.loadUrl(url);
			cameraAction.run("snapr://camera"); // Call this because it does not trigger
			return true;
		}
		else
		{
	    	// Generic back button handling
			if (mWebView.canGoBack())
			{
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Generic back button handling - going back to the previous URL");
				mWebView.goBack();
			}
			else
			{
				String startupUrl = getStartupUrl();
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Generic back button handling - no history so going to " + startupUrl);
				mWebView.loadUrl(startupUrl);
			}
			
			// Return
			return true;
		}
	}

	// Checks whether the user credentials are loaded
    private Boolean haveCredentials()
    {
    	// Having the username and access token filled in signifies we are logged in
    	if((mUserName != null) && (mUserName.length()>0) && (mAccessToken != null) && (mAccessToken.length()>0))
    	{
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    }
    
    // Load shared preferences into class members
    private void loadQueueSettings()
    {
    	// Get the shared preferences object
        SharedPreferences prefs = getContext().getSharedPreferences(Global.SNAPR_PREFERENCES, Context.MODE_PRIVATE);
        
        // Load queue preferences
        mQueueUploadModeOn = prefs.getBoolean(Global.SNAPR_PREFERENCES_QUEUE_ON, true);
        mQueueUploadModeWifiOnly = prefs.getBoolean(Global.SNAPR_PREFERENCES_QUEUE_WIFI_ONLY, false);
    }
    
    private void saveQueueSettings()
    {
    	// Save queue preferences
		SharedPreferences prefs = getContext().getSharedPreferences(Global.SNAPR_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putBoolean(Global.SNAPR_PREFERENCES_QUEUE_ON, mQueueUploadModeOn);
		prefEditor.putBoolean(Global.SNAPR_PREFERENCES_QUEUE_WIFI_ONLY, mQueueUploadModeWifiOnly);
		prefEditor.commit();
    }
    
    /*
    private void displayLocation(Location location, Integer casenum)
    {
    	// Display the location in toast
    	Double latitude = location.getLatitude();
    	Double longitude = location.getLongitude();
    	
    	// Display
    	Toast.makeText(getView().getContext(), casenum.toString() + " Location: " + latitude.toString() + ", " + longitude.toString(), Toast.LENGTH_LONG).show();
    }
    */
    
    /**
     * Create a SNAPR URL with the required login parameters
     * @param baseUrl Base page URL
     * @param useMenuPageForAjax Flag that indicates whether to 
     * ajaxify URL using the menu page as the base page (otherwise
     * we use the baseUrl page as the base page)
     * @return Returns an URL based on the base URL and containing 
     * the login parameters
     */
    private String getSnaprUrl(String baseUrl, boolean useMenuPageForAjax)
    {
    	// Declare
        String url;
    	Vector<BasicNameValuePair> params;
        
        // Add login parameters
    	params = new Vector<BasicNameValuePair>();
    	params.add(new BasicNameValuePair(Global.PARAM_SNAPR_USER, mUserName));
    	params.add(new BasicNameValuePair(Global.PARAM_ACCESS_TOKEN, mAccessToken));
        
        // Create the URL
        url = UrlUtils.createUrl(baseUrl, params, false);
        
        // Convert to AJAX URL and return
        return UrlUtils.ajaxUrl(url, useMenuPageForAjax);
    }
    
    /**
     * Build the startup URL
     * return Returns the startup URL
     */
    private String getStartupUrl()
    {
        // Declare
        String url;
    	Vector<BasicNameValuePair> params;
        
    	// Add appmode = android every time
        params = new Vector<BasicNameValuePair>();
        params.add(new BasicNameValuePair(Global.PARAM_APPMODE, "android"));
        params.add(new BasicNameValuePair(Global.PARAM_ENVIRONMENT, Global.ENVIRONMENT));
        
        // Customize some parameters based on logged in status
        if(haveCredentials())
		{
        	// We have username and token, so create URL that performs login
        	params.add(new BasicNameValuePair(Global.PARAM_SNAPR_USER, mUserName));
        	params.add(new BasicNameValuePair(Global.PARAM_ACCESS_TOKEN, mAccessToken));
		}
        else
        {
        	// We have no username and password, so create URL that indicates new user
        	params.add(new BasicNameValuePair(Global.PARAM_NEW_USER, "true"));
        }
        
        // Create the URL
        url = UrlUtils.createUrl(UrlUtils.getFullLocalUrl(Global.URL_MENU), params, false);
        
        // Convert to AJAX URL and return
        return UrlUtils.ajaxUrl(url, false);
    }
    
    /**
     * Build the picture sharing URL
     * @return Returns the picture sharing URL
     */
    private String getSharePictureUrl(String imageName, double latitude, double longitude, String redirectUrl)
    {
        // Declare
        String url;
    	Vector<BasicNameValuePair> params;
        
    	// Add appmode = android every time
        params = new Vector<BasicNameValuePair>();
        params.add(new BasicNameValuePair(Global.PARAM_APPMODE, "android"));
        params.add(new BasicNameValuePair(Global.PARAM_ENVIRONMENT, Global.ENVIRONMENT));
        
        // Customize some parameters based on logged in status
        if(haveCredentials())
		{
        	// We have username and token, so create URL that performs login
        	params.add(new BasicNameValuePair(Global.PARAM_SNAPR_USER, mUserName));
        	params.add(new BasicNameValuePair(Global.PARAM_ACCESS_TOKEN, mAccessToken));
		}
        else
        {
        	// We have no username and password, so create URL that indicates new user
        	params.add(new BasicNameValuePair(Global.PARAM_NEW_USER, "true"));
        }
        
        // Pass the image name
        if(imageName.startsWith("file:///") == false)
        {
        	imageName = "file:///" + imageName;
        }
        params.add(new BasicNameValuePair(Global.PARAM_PHOTO_PATH, imageName));
        
        if((latitude != 0) && (longitude != 0))
        {
        	params.add(new BasicNameValuePair(Global.PARAM_LATITUDE, Double.valueOf(latitude).toString()));
        	params.add(new BasicNameValuePair(Global.PARAM_LONGITUDE, Double.valueOf(longitude).toString()));
        }
        
        if ((redirectUrl != null) && (redirectUrl.length() > 0))
        {
        	params.add(new BasicNameValuePair(Global.PARAM_REDIRECT_URL, redirectUrl));
        }
        
        // Create the URL
        String val = UrlUtils.getFullLocalUrl(Global.URL_PHOTO_SHARE);
        url = UrlUtils.createUrl(val, params, false);
        
        // Convert to AJAX URL and return
        return UrlUtils.ajaxUrl(url, true);
    }
    
    /**
     * Build the signups for shared services URL 
     * @return Returns the signups URL
     */
    private String getSignupsNeededUrl(String id, String signupsNeeded, String redirectUrl)
    {
        // Declare
        String url;
    	Vector<BasicNameValuePair> params;
        
    	// Add appmode = android every time
        params = new Vector<BasicNameValuePair>();
        params.add(new BasicNameValuePair(Global.PARAM_APPMODE, "android"));
        params.add(new BasicNameValuePair(Global.PARAM_ENVIRONMENT, Global.ENVIRONMENT));
        
        // Customize some parameters based on logged in status
        if(haveCredentials())
		{
        	// We have username and token, so create URL that performs login
        	params.add(new BasicNameValuePair(Global.PARAM_SNAPR_USER, mUserName));
        	params.add(new BasicNameValuePair(Global.PARAM_ACCESS_TOKEN, mAccessToken));
		}
        else
        {
        	// We have no username and password, so create URL that indicates new user
        	params.add(new BasicNameValuePair(Global.PARAM_NEW_USER, "true"));
        }
        
        // Pass the image id
        params.add(new BasicNameValuePair(Global.PARAM_PHOTO_ID, id));
        
        // Pass the signups needed
        params.add(new BasicNameValuePair(Global.PARAM_TO_LINK, signupsNeeded));
        
        // Pass in a blank shared param
        params.add(new BasicNameValuePair(Global.PARAM_SHARED, ""));
        
        // Pass the redirect url
        if ((redirectUrl != null) && (redirectUrl.length() > 0))
        {
        	params.add(new BasicNameValuePair(Global.PARAM_REDIRECT_URL, redirectUrl));
        }
        
        // Create the URL
        url = UrlUtils.createUrl(UrlUtils.getFullLocalUrl(Global.URL_LINKED_SERVICES), params, true);
        
        // Convert to AJAX URL and return
        return UrlUtils.ajaxUrl(url, true);
    }
    
    // The action performed for snaprkit-parent:// URLs
    private Action snaprKitParentAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{
    		// Send info through listener
    		mSnaprKitListener.onSnaprKitParent(url);
//    		System.out.println("SnaprKit parent action: " + url);
    	}
    };
    
    // The action performed for login
    private Action loginAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{
    		// Login URL - get credentials and redirect to startup URL
			
			// Convert AJAX URL to normal URL - necessary for Uri parsing to work
			String normalUrl = UrlUtils.normalUrl(url);
			
			// Parse the URL parameters
			Uri uri = Uri.parse(normalUrl);
			mUserName = uri.getQueryParameter(Global.PARAM_SNAPR_USER);
			mAccessToken = uri.getQueryParameter(Global.PARAM_ACCESS_TOKEN);
			
			// Check if we have proper credentials now that we loaded info from the URL
			if(haveCredentials())
			{
				// Save user credentials
				CredentialsUtils.saveCredentials(getContext(), mUserName, mAccessToken);
			}
			
			// Get startup URL
			url = getStartupUrl();
			
			// Load the startup URL
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + " Redirect to " + url);
			mWebView.loadUrl(url);
    	}
    };

    // The action performed for logout
    private Action logoutAction = new Action() {
    	@Override
    	public void run(String url)
    	{
    		// Clear the locally stored login credentials
    		CredentialsUtils.clearCredentials(getContext());
    	}
    };
    
    /** 
     * Start the fragment process to use a given static image. 
     * Note: this should be used for development purposes only.
     */
    protected void runStaticAction(String url) {
    	if (Global.LOG_MODE) Global.log("-> runStaticAction()");
    	if (mPictureAcquisitionManager == null || mPictureAcquisitionManager.isActive()) return;
   		mPictureAcquisitionManager.acquirePicture(PictureAcquisitionManager.PICTURE_SOURCE_STATIC, getPictureAcquisitionListener(), url);			
    }
    
    /** 
     * Start the fragment process to take an image from the camera. 
     */
    protected void runCameraAction() {
    	cameraAction.run("snapr://camera");
    }
    
    // The action performed for camera shoot
    private Action cameraAction = new Action() {
    	@Override
    	public void run(String url)
    	{
			if (Global.LOG_MODE) Global.log("-> cameraAction()");
			
			// Start the camera and take picture
			if ((mPictureAcquisitionManager != null) && (mPictureAcquisitionManager.isActive() == false))
			{
				mPictureAcquisitionManager.acquirePicture(PictureAcquisitionManager.PICTURE_SOURCE_CAMERA, getPictureAcquisitionListener());			
			}
    	}
    };

    /** Start the fragment process to select an image from the gallery. */
    protected void runGalleryAction() {
    	photoGalleryAction.run("snapr://photo-library");
    }
    
    // The action performed for photo selection from gallery
    private Action photoGalleryAction = new Action() {
    	@Override
    	public void run(String url)
    	{
			if (Global.LOG_MODE) Global.log("-> photoGalleryAction()");

			// Start the gallery and let user select picture
			if ((mPictureAcquisitionManager != null) && (mPictureAcquisitionManager.isActive() == false))
			{
				mPictureAcquisitionManager.acquirePicture(PictureAcquisitionManager.PICTURE_SOURCE_GALLERY, getPictureAcquisitionListener());			
			}
    	}
    };
    
    // The action performed for snapr://redirect
    private Action redirectAction = new Action() {
    	@Override
    	public void run(String url)
    	{
			if (Global.LOG_MODE) Global.log(Global.TAG, "redirectAction(): Started with URL " + url);
			
			// Remove the first part of the URL
			String redirectUrl = url.replace("snapr://redirect?url=", "");
			
			// Load our URL
			if (Global.LOG_MODE) Global.log("redirectAction(): Redirecting to " + redirectUrl);
			mWebView.loadUrl(redirectUrl);
    	}
    };
    
    // Creates an upload id based on current date time
    private String getUploadId()
    {
    	Random random = new Random();
    	int randomInt = random.nextInt();
    	if (randomInt < 0) randomInt = randomInt * -1;
    	return Integer.valueOf(randomInt).toString();
    }

    // The action performed for snapr://upload
    private Action uploadAction = new Action() {
    	@Override
    	public void run(String url)
    	{			
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Received URL " + url);
			
			// Extract the parameters from the query
			Uri uri = Uri.parse(url);
			String id = uri.getQueryParameter(Global.PARAM_ID);
			String description = uri.getQueryParameter(Global.PARAM_DESCRIPTION);
			String status = uri.getQueryParameter(Global.PARAM_STATUS);
			String tweet = uri.getQueryParameter(Global.PARAM_TWEET);
			String facebookFeed = uri.getQueryParameter(Global.PARAM_FACEBOOK_FEED);
			String facebookAlbum = uri.getQueryParameter(Global.PARAM_FACEBOOK_ALBUM);
			String facebookAlbumName = uri.getQueryParameter(Global.PARAM_FACEBOOK_ALBUM_NAME);
			String tumblr = uri.getQueryParameter(Global.PARAM_TUMBLR);
			String foursquareCheckin = uri.getQueryParameter(Global.PARAM_FOURSQUARE_CHECKIN);
			String photo = uri.getQueryParameter(Global.PARAM_PHOTO);
			String redirectUrl = uri.getQueryParameter(Global.PARAM_REDIRECT_URL);
			String foursquareVenueId = uri.getQueryParameter(Global.PARAM_FOURSQUARE_VENUE);
			String appGroup = uri.getQueryParameter(Global.PARAM_APP_GROUP);
			String publicGroup = uri.getQueryParameter(Global.PARAM_PUBLIC_GROUP);
			String uploadParams = uri.getEncodedQuery();
			
			// Fix filename
			photo = photo.replace(":////", ":///");
			if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Filename is  " + photo);
			
			// Get the picture latitude / longitude from EXIF
			String latitude = "";
			String longitude = "";
			ExifData exifData = CameraUtils.getExifData(UrlUtils.urlToFileName(photo));
			Location geoLocation = exifData.getLocation();
			if (geoLocation != null)
			{
				latitude = "" + geoLocation.getLatitude();
				longitude = "" + geoLocation.getLongitude();
			}
			
			// Get the picture date
			String date;
			String modifyDateTime = exifData.getModifyDateTimeString();
			String originalDateTime = exifData.getOriginalDateTimeString();
			if (originalDateTime != null && originalDateTime.length() != 0)
			{
				date = originalDateTime;
			}
			else if (modifyDateTime != null && modifyDateTime.length() != 0)
			{
				date = modifyDateTime;
				
			}
			else
			{
				date = "";
			}
			
			// Set the location
			String location = "";
			
			// If the redirect URL does not exist, use default
			if (redirectUrl == null || redirectUrl.length() == 0)
			{
				redirectUrl = getSnaprUrl(UrlUtils.getFullLocalUrl(Global.URL_UPLOAD), false);
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found redirect URL " + redirectUrl);
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Original URL was " + url);
			}
			else
			{
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": No redirect URL in original URL ");
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Original URL was " + url);
			}
			
			// Create the id if blank
			if (id == null || id.length() == 0 || id.equals("undefined"))
			{
				id = getUploadId();
			}
			
			// Log the description
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": step Received description as " + description);
			
			// Send the information to the upload service via intent
			Intent uploadIntent = new Intent(getContext(), UploadService.class);
	        uploadIntent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_ADD);
	        uploadIntent.putExtra(Global.PARAM_ACCESS_TOKEN, mAccessToken);
	        uploadIntent.putExtra(Global.PARAM_ID, id);
	        uploadIntent.putExtra(Global.PARAM_PHOTO, photo);
	        uploadIntent.putExtra(Global.PARAM_DESCRIPTION, description);
	        uploadIntent.putExtra(Global.PARAM_LATITUDE, latitude);
	        uploadIntent.putExtra(Global.PARAM_LONGITUDE, longitude);
	        uploadIntent.putExtra(Global.PARAM_LOCATION, location);
	        uploadIntent.putExtra(Global.PARAM_DATE, date);
	        uploadIntent.putExtra(Global.PARAM_PRIVACY, status);
	        uploadIntent.putExtra(Global.PARAM_TWEET, isParamTrue(tweet));
	        uploadIntent.putExtra(Global.PARAM_FACEBOOK_FEED, isParamTrue(facebookFeed));
	        uploadIntent.putExtra(Global.PARAM_FACEBOOK_ALBUM, isParamTrue(facebookAlbum));
	        uploadIntent.putExtra(Global.PARAM_FACEBOOK_ALBUM_NAME, facebookAlbumName);
	        uploadIntent.putExtra(Global.PARAM_TUMBLR, isParamTrue(tumblr));
	        uploadIntent.putExtra(Global.PARAM_FOURSQUARE_CHECKIN, isParamTrue(foursquareCheckin));
	        uploadIntent.putExtra(Global.PARAM_FOURSQUARE_VENUE, foursquareVenueId);
	        uploadIntent.putExtra(Global.PARAM_APP_GROUP, appGroup);
	        uploadIntent.putExtra(Global.PARAM_PUBLIC_GROUP, publicGroup);
	        uploadIntent.putExtra(Global.PARAM_UPLOAD_PARAMS, uploadParams);
	        
	        getContext().startService(uploadIntent);
			
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());			
    	}
    };
    
    private boolean isParamTrue(String paramValue)
    {
    	return (paramValue!=null) && (paramValue.equals("checked") || paramValue.equals("true"));
    }
    
    // The action performed for snapr://upload_progress
    private Action uploadProgressAction = new Action() {
    	@Override
    	public void run(String url)
    	{			
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Received URL " + url);		
		
			if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Requesting queue status update");
			Intent uploadIntent = new Intent(getContext(), UploadService.class);
			uploadIntent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_UPDATE_STATUS);
			getContext().startService(uploadIntent);
			
	        // Logs
	        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };

    // The action performed for snapr://get_location
    private Action getLocationAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Received URL " + url);
    		
    		// Initialize the geo manager
    		mGeoManager = new GeoManager(getContext());
    		
    		// Determine caller
    		int caller;
    		if (url == null)
    		{
    			caller = Global.GEOLOCATION_PHOTO_SHARE;
    		}
    		else
    		{
    			caller = Global.GEOLOCATION_GET_LOCATION;
    		}
    		
    		// Request the location
    		mGeoManager.showLocationPendingDialog();
    		mGeoManager.getLocation(mGeoListener, caller);
    		
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };

    // The action performed for snapr://aviary
    private Action editPhotoAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{
    	    // Enable only if we have Aviary Feather
    		if (Global.FEATURE_AVIARY_SDK)
    	    {
	    		// Log
	    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> editPhotoAction(): Received URL " + url);
	    
	    		// Get the filepath parameter
	    		Uri sourceUri = Uri.parse(url);
	    		String photoPath = sourceUri.getQueryParameter(Global.PARAM_PHOTO_PATH);
	    		
	    		// Create the photo uri
	    		Uri uri = Uri.parse(photoPath);
	    		
	    		// Create the intent needed to start feather
	    		Intent newIntent = new Intent(getContext(), FeatherActivity.class);
	    		
	    		// Set the source image uri
	    		newIntent.setData( uri );
	    		
	    		// Pass the required api key/secret ( http://developers.aviary.com/geteffectskey )
	    		newIntent.putExtra("API_KEY", "d3fd1ff6b");
	    		newIntent.putExtra("API_SECRET", "1896d2192");
	    		
	    		// Pass the uri of the destination image file (optional)
	    		// This will be the same uri you will receive in the onActivityResult
	    		//newIntent.putExtra( "output", Uri.parse( "file://" + mOutputFile.getAbsolutePath() ) );
	    		
	    		// Format of the destination image (optional)
	    		newIntent.putExtra("output-format", Bitmap.CompressFormat.JPEG.name() );
	    		
	    		// Output format quality (optional)
	    		newIntent.putExtra("output-quality", 100 );
	    		
	    		// You can force feather to display only a certain tools
	    		//newIntent.putExtra("tools-list", new String[]{"EFFECTS", "CROP", "BRIGHTNESS", "CONTRAST", "SATURATION", "COLORS", "STICKERS", "TEXT", "DRAWING", "RED_EYE", "WHITEN", "BLEMISH", "ROTATE", "FLIP", "BLUR", "SHARPEN"} );
	    		
	    		// Start feather
	    		startActivityForResult(newIntent, ACTION_REQUEST_FEATHER );
	    		
	    		// Log
	    		if (Global.LOG_MODE) Global.log(Global.TAG, " <- editPhotoAction()");
    	    }
    	}
    };
    
    private void startQueue()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	// Start the queue via intent
		Intent intent = new Intent(getContext(), UploadService.class);
        intent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_START);
        intent.putExtra(Global.PARAM_QUEUE_UPLOAD_MODE_ON, mQueueUploadModeOn);
        intent.putExtra(Global.PARAM_QUEUE_UPLOAD_MODE_WIFI_ONLY, mQueueUploadModeWifiOnly);
        getContext().startService(intent);
        
        // Log
        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    private void stopQueue()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	// Stop the queue via intent
		Intent intent = new Intent(getContext(), UploadService.class);
        intent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_STOP);
        intent.putExtra(Global.PARAM_QUEUE_UPLOAD_MODE_ON, mQueueUploadModeOn);
        intent.putExtra(Global.PARAM_QUEUE_UPLOAD_MODE_WIFI_ONLY, mQueueUploadModeWifiOnly);
        getContext().startService(intent);
        
        // Log
        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    private void sendQueueUploadMode()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Params - wifi only " + mQueueUploadModeWifiOnly + " queueOn " + mQueueUploadModeOn);
    	
    	// Send the information to the upload service via intent
		Intent intent = new Intent(getContext(), UploadService.class);
        intent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_UPLOAD_MODE);
        intent.putExtra(Global.PARAM_QUEUE_UPLOAD_MODE_ON, mQueueUploadModeOn);
        intent.putExtra(Global.PARAM_QUEUE_UPLOAD_MODE_WIFI_ONLY, mQueueUploadModeWifiOnly);
        getContext().startService(intent);
        
        // Log
        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    private void updateQueueSettings(boolean queueUploadModeOn, boolean queueUploadModeWifiOnly)
    {
    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Current On is " + mQueueUploadModeOn + " and wi-fi is " + mQueueUploadModeWifiOnly);
    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": New On is " + queueUploadModeOn + " and wi-fi is " + queueUploadModeWifiOnly);
    	
		boolean oldQueueUploadModeWifiOnly = mQueueUploadModeWifiOnly;
		mQueueUploadModeWifiOnly = queueUploadModeWifiOnly;
		//boolean oldQueueUploadModeOn = queueUploadModeOn;
		mQueueUploadModeOn = queueUploadModeOn;
    	
		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": old wifi on" + oldQueueUploadModeWifiOnly);
		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": new wifi on" + mQueueUploadModeWifiOnly);
		
		// Notify service of upload mode changes
    	sendQueueUploadMode();
    	
    	// Notify UI of upload mode changes
    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Sending " + "javascript:queue_settings('" + getUploadModeString(queueUploadModeWifiOnly) + "', " + (!queueUploadModeOn) + ");");
    	mWebView.loadUrl("javascript:queue_settings('" + getUploadModeString(queueUploadModeWifiOnly) + "', " + (!queueUploadModeOn) + ");");
		
		// User pressed the wifi button to enable wifi only
		if (!oldQueueUploadModeWifiOnly && mQueueUploadModeWifiOnly)
		{
			// If queue is on we may have to switch it off
			if (mQueueUploadModeOn)
			{
				// Check the Wifi state
				boolean wifiConnected = NetworkUtils.isWifiConnected(getContext());
				if (!wifiConnected)
				{
					// Stop queue
					if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Stopping queue due to Wifi setting getting enabled!");
					stopQueue();
				}
			}
		}
		// User pressed the wifi button to disable wifi only
		else if (oldQueueUploadModeWifiOnly && !mQueueUploadModeWifiOnly)
		{
			// If queue is off we may have to switch it on
			if  (mQueueUploadModeOn)
			{
				// Check if we have any other connection connection
				boolean anyConnected = NetworkUtils.isAnyConnected(getContext());
				
				if (anyConnected)
				{
					// Start queue
					if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Starting queue due to Wifi setting getting disabled!");
					startQueue();
				}
			}
		}
    }
    
    // The action performed for snapr://upload?setting=
    private Action queueSettingAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueSettingAction: Received URL " + url);
    		
    		// Get the setting from the URL
    		Uri uri = Uri.parse(url);
    		String settingString = uri.getQueryParameter(Global.PARAM_SETTING);
    		boolean queueUploadModeWifiOnly = getWifiOnlyFromUploadMode(settingString);
    		boolean queueUploadModeOn = getOnFromUploadMode(settingString);
	    	
    		// Update the queue
    		updateQueueSettings(queueUploadModeOn, queueUploadModeWifiOnly);
    		
	    	// Log
	    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    // The action performed for snapr://upload?start
    private Action queueStartAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{	
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueStartAction: Received URL " + url);
    		
    		// Set the queue on/of state
    		//mQueueUploadModeOn = true;
    		updateQueueSettings(true, mQueueUploadModeWifiOnly);
    		
    		// Send the information to the upload service via intent
			startQueue();
	        
	        // Log
	        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    // The action performed for snapr://upload?stop
    private Action queueStopAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueStopAction: Received URL " + url);
    		
    		// Set the queue on/of state
    		//mQueueUploadModeOn = false;
    		updateQueueSettings(false, mQueueUploadModeWifiOnly);
    		
    		// Send the information to the upload service via intent
			stopQueue();
	        
	        // Log
	        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    // The action performed for snapr://upload?clear
    private Action queueClearAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{	
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueClearAction: Received URL " + url);
    		
    		// Send the information to the upload service via intent
			Intent intent = new Intent(getContext(), UploadService.class);
	        intent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_CLEAR);
	        getContext().startService(intent);
	        
	        // Log
	        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };

    // The action performed for snapr://upload?cancel=id
    private Action queueCancelAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{	
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueCancelAction: Received URL " + url);
    		
    		// Get the id from the url
    		Uri uri = Uri.parse(url);
    		String id = uri.getQueryParameter(Global.PARAM_CANCEL);
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueCancelAction: Got cancel for id " + id);
    		
    		// Send the information to the upload service via intent
			Intent intent = new Intent(getContext(), UploadService.class);
	        intent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_REMOVE);
	        intent.putExtra(Global.PARAM_ID, id);
	        if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueCancelAction: Before call to service");
	        getContext().startService(intent);
	        if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueCancelAction: After call to service");
	        
	        // Log
	        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    // The action performed when browsing an external site
    private Action externalBrowseAction = new Action() {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> externalBrowseAction: Received URL " + url);

    		// For snapr://link urls extract the embedded url parameter
    		if (url != null && url.startsWith("snapr://link?url="))
    		{
    			url = url.substring(17);
    		}
    		
    		/*
    		Uri uri = Uri.parse(url);
    		if (uri.getScheme().equals(Global.SCHEME_SNAPR))
			{
    			// Set the url to the embedded parameter
    			url = uri.getQueryParameter(Global.PARAM_URL);
			}
			*/
    		
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Starting external webview with URL " + url);
    		
    		// Start the external browse activity
    		Intent intent  = new Intent(getContext(), WebViewExternalActivity.class);
    		intent.putExtra("url", url);
			startActivityForResult(intent, WebViewExternalActivity.DISPLAY_EXTERNAL_URL);
			
			// Log
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };

    // The default action performed when there is no match in the action table
    private Action defaultAction = new Action() {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> defaultAction: Received URL " + url);
    		
    		// Load the startup URL
			mWebView.loadUrl(url);
			
			// Log
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };

    private void initActionMap()
    {
    	mActionMappings.add(new UrlMapping("snaprkit-parent://.*", snaprKitParentAction));
		mActionMappings.add(new UrlMapping("snapr://login.*", loginAction));
		mActionMappings.add(new UrlMapping("snapr://logout.*", logoutAction));
		mActionMappings.add(new UrlMapping("snapr://get_location.*", getLocationAction));
		mActionMappings.add(new UrlMapping("snapr://upload_progress.*", uploadProgressAction));
		mActionMappings.add(new UrlMapping("snapr://upload\\?setting.*", queueSettingAction));
		mActionMappings.add(new UrlMapping("snapr://upload\\?start.*", queueStartAction));
		mActionMappings.add(new UrlMapping("snapr://upload\\?stop.*", queueStopAction));
		mActionMappings.add(new UrlMapping("snapr://upload\\?clear.*", queueClearAction));
		mActionMappings.add(new UrlMapping("snapr://upload\\?cancel.*", queueCancelAction));
		mActionMappings.add(new UrlMapping("snapr://upload\\?.*", uploadAction));
		mActionMappings.add(new UrlMapping("snapr://redirect.*", redirectAction));
		mActionMappings.add(new UrlMapping("snapr://camera.*", cameraAction));
    	mActionMappings.add(new UrlMapping("snapr://photo-library.*", photoGalleryAction));
    	mActionMappings.add(new UrlMapping("snapr://link.*", externalBrowseAction));
    	if (Global.FEATURE_AVIARY_SDK) mActionMappings.add(new UrlMapping("snapr://aviary.*", editPhotoAction));
		mActionMappings.add(new UrlMapping("snapr://.*", defaultAction));
		mActionMappings.add(new UrlMapping("file://.*", defaultAction));
		mActionMappings.add(new UrlMapping("http://.*", externalBrowseAction));
		mActionMappings.add(new UrlMapping("https://.*", externalBrowseAction));
    }
    
    // Set webview settings and display startup page
	// mWebView global should have been prepopulated
    private void initWebView(View view, Bundle savedInstanceState)
    {
    	// Declare
    	String url = null;
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
        
    	// Get the webview control
        mWebView = (WebView) view.findViewById(R.id.webview);
        
        // Override clicks
        mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, "shouldOverrideUrlLoading() " + url);
				
				// Run appropriate action
				//String urlKey = UrlUtils.getUrlKey(url);
				//if (Global.LOG_MODE) Global.log("Overriding url: " + url + " using url key: " + urlKey);
				//Action action = getActionForUrl(urlKey);
				Action action = getActionForUrl(url);
				action.run(url);
				
				// Set the current Url
				if (url != null)
				{
					String normalUrl = UrlUtils.normalUrl(url);
					if (UrlUtils.isLocalUrl(normalUrl))
					{						
						mCurrentUrl = url;
					}
				}
				
				// Indicate that we handled the request
				return true;
			}

			@Override
			public void onPageFinished(WebView view, String url)
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, "onPageFinished() " + url);
				
				// Get normal URL
				String normalUrl = UrlUtils.normalUrl(url);
				
				// Set the current Url
				if (normalUrl != null && UrlUtils.isLocalUrl(normalUrl))
				{
					mCurrentUrl = url;
				}
				
				// Perform queue setting init after first page load
				// We need the page loaded so the JS is present
				if (normalUrl != null && mMenuInitDone == false)
				{
					// Change flag
					mMenuInitDone = true;
				
					// Set queue settings
					mWebView.loadUrl("javascript:queue_settings('" + getUploadModeString(mQueueUploadModeWifiOnly) + "', " + (!mQueueUploadModeOn) + ");");
			    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Set JavaScript queue settings to Wifi-only " + mQueueUploadModeWifiOnly + " and upload mode on " + (!mQueueUploadModeOn));
				}
				
				// Dismiss the camera transition dialog
				/*
				if (mTransitionDialog != null)
				{
					mTransitionDialog.cancelTransitionDialog();
				}
				*/
				
				// Clear the history every time we hit index.html
				if(normalUrl.contains(UrlUtils.getFullLocalUrl(Global.URL_MENU)))
				{
					// Clear the history
					mWebView.clearHistory();
				}
				
				// Check if we have a listener and call it
				// Exclude snaprkit-parent urls
				if (mSnaprKitListener != null && url != null && !url.startsWith("snaprkit-parent://"))
				{
					mSnaprKitListener.onPageFinished(url);
				}
				
				// Proceed as usual
				super.onPageFinished(view, url);
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, "onPageStarted() " + url);
				
        		// Carry on
				super.onPageStarted(view, url, favicon);
			}

			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl)
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, "onReceiveError() got error " + description);
			}
    	});
        
        // Add support for JavaScript alert dialogs - necessary for unlink dialogs
        mWebView.setWebChromeClient(new WebChromeClient() {

			@Override  
            public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result)  
            {
				if (!SnaprKitFragment.this.getActivity().isFinishing()) // Need check to avoid random crashes when we are in the backgroound
				{
	                new AlertDialog.Builder(getActivity())  
	                    .setTitle(R.string.snaprkit_name)  
	                    .setMessage(message)  
	                    .setPositiveButton(android.R.string.ok,  
	                            new AlertDialog.OnClickListener()  
	                            {  
	                                public void onClick(DialogInterface dialog, int which)  
	                                {  
	                                    result.confirm();  
	                                }  
	                            })  
	                    .setCancelable(false)  
	                    .create()  
	                    .show();  
				}
          
                return true;  
            };
            
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) 
            {
            	if (!getActivity().isFinishing()) // Need check to avoid random crashes when we are in the backgroound
            	{
	                new AlertDialog.Builder(getActivity())
	                    .setTitle(R.string.snaprkit_name)
	                    .setMessage(message)
	                    .setPositiveButton(android.R.string.ok, 
	                            new DialogInterface.OnClickListener() 
	                            {
	                                public void onClick(DialogInterface dialog, int which) 
	                                {
	                                    result.confirm();
	                                }
	                            })
	                    .setNegativeButton(android.R.string.cancel, 
	                            new DialogInterface.OnClickListener() 
	                            {
	                                public void onClick(DialogInterface dialog, int which) 
	                                {
	                                    result.cancel();
	                                }
	                            })
	                .create()
	                .show();
            	}
            
                return true;
            };
            
            @Override
            public void onReceivedTitle(WebView view, String title)
            {
                //setTitle(title);
                super.onReceivedTitle(view, title);
            }
        });
        
        // Enable JavaScript
        mWebView.getSettings().setJavaScriptEnabled(true);
        
        // Enable HTML5 local storage and make it persistent
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDatabasePath("/data/data/" + getActivity().getPackageName() + "/databases/");
        
        // Clear spurious cache data
        mWebView.clearHistory();
        mWebView.clearFormData();
        mWebView.clearCache(true);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setUseWideViewPort(true);
        
        // Accept cookies
        CookieSyncManager.createInstance(getContext()); 
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        
        // Make sure that the webview does not allocate blank space on the side for the scrollbars
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        
        // Restore the webview history
        // Must be done before using the webview, otherwise the restore fails
        if (savedInstanceState != null && savedInstanceState.isEmpty() == false)
        {
        	// Log
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found webview state");
        	
        	// Restore the state
        	mWebView.restoreState(savedInstanceState);
        	
        	// Process current URL
        	if (mCurrentUrl !=null && mCurrentUrl.startsWith(Global.SCHEME_SNAPR))
			{
				// Special exception for SNAPR URLs to prevent external action triggers
				url = null;
				
				// Log the previous URL
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found snapr:// scheme on startup and previous URL:" + UrlUtils.getPreviousUrl(mWebView));
			}
			else
			{
				url = mCurrentUrl;
			}
        	
        	// Load the URL
        	mWebView.loadUrl(url);
        	
        	// Log
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Started via activity restore flow with initial URL " + url);
        }
        else
        {
        	if (mCurrentUrl != null)
        		{
        		//mWebView.loadUrl(mCurrentUrl);
        		mWebView.loadUrl( "javascript:window.location.reload( true )" );
        		}
        }
        
        // Bugfix for http://code.google.com/p/android/issues/detail?id=7189
        // If we want the soft keyboard to show for form fields in the webview
        // we must obtain focus after load. We also need to add an event handler 
        // to obtain focus after every field touch
        mWebView.requestFocus(View.FOCUS_DOWN);
        mWebView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        if (!v.hasFocus()) {
                            v.requestFocus();
                        }
                        break;
                }
                return false;
            }
        });
       
        // Log
        if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    }    
    
    public void setSharedPictureFileName(String sharedPictureFileName)
    {
    	mSharedPictureFileName = sharedPictureFileName;
    }
    
    private SharedPictureInfo getSharedPictureInfo()
    {
    	// Declare
    	SharedPictureInfo info = null;
    	double latitude = 0;
    	double longitude = 0;
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Check that we have a shared picture name
    	if (mSharedPictureFileName != null)
    	{
    	
	    	// Get the EXIF location from the filename
	    	Location exifLocation = getExifLocation(mSharedPictureFileName);
	    	
	    	if (exifLocation != null)
	    	{
	    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found location information within picture");
	    		latitude = exifLocation.getLatitude();
	    		longitude = exifLocation.getLongitude();
	    	}
	    	else
	    	{
	    		// Log
	    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found no location information within picture");
	    	}
	    	
			// Add the info
	    	info  = new SharedPictureInfo();
	    	info.setFileName(mSharedPictureFileName);
	    	info.setLatitude(latitude);
	    	info.setLongitude(longitude);
    	}
        
        // Return
        return info;
    }
    
    private Location getExifLocation(String fileName)
    {
		// Try to get the location from the file
    	ExifData exifData = CameraUtils.getExifData(fileName);
		return exifData.getLocation();
    }

	// Get the wifi setting from the upload mode / setting strin
    private String getUploadModeString(boolean wifiOnly)
    {
    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Got wifi " + wifiOnly);
    	if (wifiOnly == true)
    	{
    		return "Wi-Fi Only"; 
    	}
    	else
		{
    		return  "On";
		}
    }

    /**
     * Gets the on queue setting from the upload mode / setting string
     * @param uploadMode The upload mode or setting string
     */
    private boolean getOnFromUploadMode(String uploadMode)
    {
    	if("On".equals(uploadMode))
    	{
    		return true;
    	}
    	else if ("Wi-Fi Only".equals(uploadMode))
    	{
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    }
    
    /**
     * Gets the queue settings from the upload mode / setting string
     * @param uploadMode The upload mode or setting string
     */
    private boolean getWifiOnlyFromUploadMode(String uploadMode)
    {
    	if("On".equals(uploadMode))
    	{
    		return false;
    	}
    	else if ("Wi-Fi Only".equals(uploadMode))
    	{
    		return true;
    	}
    	else
    	{
    		return true;
    	}
    }
    
    private void initGlobals()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	Global.densityDpi = Global.getScreenDensity(SnaprKitFragment.this.getActivity());
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    private void initQueueSettings()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	try
    	{
	    	// Load the queue settings into variables
	    	loadQueueSettings();
	    	
	    	// Notify JavaScript
	    	// Now done onPageFinished to guarantee that the JS is loaded
	    	
	    	// Notify service of upload mode
	    	sendQueueUploadMode();
	    	
	    	// If the queue is supposed to be on, then turn it on
	    	if (mQueueUploadModeOn)
	    	{
	    		startQueue();
	    	}
	    	else
	    	{
	    		stopQueue();
	    	}
    	}
    	catch (Exception e)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to intialize queue settings due to error " + e.toString());
    	}
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    private void initSavedState(Bundle savedInstanceState)
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	if ((savedInstanceState != null) && (savedInstanceState.isEmpty() == false))
		{
    		// Log
        	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + ": Found saved state");
        	
        	// Set flag
        	mRestoredFromSavedState = true;
    		
			mMenuInitDone = savedInstanceState.getBoolean("mMenuInitDone");
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored mMenuInitDone to " + mMenuInitDone);
			
			mLastPictureLatitude = savedInstanceState.getDouble("mLastPictureLatitude");
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored mLastPictureLatitude to " + mLastPictureLatitude);
			
			mLastPictureLongitude = savedInstanceState.getDouble("mLastPictureLongitude");
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored mLastPictureLongitude to " + mLastPictureLongitude);
			
			long date = savedInstanceState.getLong("mLastPictureDate");
			if (date != 0) mLastPictureDate = new Date(date);
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored mLastPictureDate to " + mLastPictureDate);
			
			mCurrentUrl = savedInstanceState.getString("mCurrentUrl");
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored mCurrentUrl to " + mCurrentUrl);
		}
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    private void initCameraManager(Bundle savedInstanceState)
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());

    	if (mPictureAcquisitionManager == null) mPictureAcquisitionManager = new PictureAcquisitionManager(SnaprKitFragment.this);
    	
    	if ((savedInstanceState != null) && (savedInstanceState.isEmpty() == false))
    	{
    		// Log
        	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + ": Found saved state");
    		
        	long photoTimestamp = savedInstanceState.getLong("mPhotoTimestamp");
        	mPictureAcquisitionManager.setPhotoTimestamp(photoTimestamp);
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored mPhotoTimestamp to " + photoTimestamp);
        	
    		// Restore saved variables
        	int imageSource = savedInstanceState.getInt("mImageSource");
    		mPictureAcquisitionManager.setImageSource(imageSource);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored imageSource to " + imageSource);
        	
    		// Note that we restore both image URI and path -- must be done this way
        	// because depending on reentry point, the code inside the class may not 
        	// set the image path
    		String imagePath = savedInstanceState.getString("mImagePath");
    		Uri imageUri = UrlUtils.imagePath2Uri(imagePath);
    		mPictureAcquisitionManager.setImageUri(imageUri);
    		mPictureAcquisitionManager.setImagePath(imagePath);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored imageUri to " + imageUri);
    		
    		boolean isActive = savedInstanceState.getBoolean("mIsActive");
    		mPictureAcquisitionManager.setIsActive(isActive);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored isActive to " + isActive);
    		
    		boolean isGeolocationActive = savedInstanceState.getBoolean("mIsGeolocationActive");
    		mPictureAcquisitionManager.setIsGeolocationActive(isGeolocationActive);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored isGeolocationActive to " + isGeolocationActive);
    		
    		double latitude = savedInstanceState.getDouble("mLatitude");
    		double longitude = savedInstanceState.getDouble("mLongitude");
    		if (latitude != -1 && longitude != -1)
    		{
    			Location location = new Location ("unknown");
    			location.setLatitude(latitude);
    			location.setLongitude(longitude);
    			mPictureAcquisitionManager.setLocation(location);
    			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored location latitude to " + latitude);
    			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored location longitude to " + longitude);
    		}
    		else
    		{
    			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored location to null");
    		}
    		
    		boolean networkProviderEnabled = savedInstanceState.getBoolean("mNetworkProviderEnabled");
    		mPictureAcquisitionManager.setNetworkProviderEnabled(networkProviderEnabled);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored networkProviderEnabled to " + networkProviderEnabled);
    		
    		boolean gpsProviderEnabled = savedInstanceState.getBoolean("mGpsProviderEnabled");
    		mPictureAcquisitionManager.setGpsProviderEnabled(gpsProviderEnabled);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored gpsProviderEnabled to " + gpsProviderEnabled);
    		
    		boolean wifiProviderEnabled = savedInstanceState.getBoolean("mWifiProviderEnabled");
    		mPictureAcquisitionManager.setWifiProviderEnabled(wifiProviderEnabled);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored wifiProviderEnabled to " + wifiProviderEnabled);
    		
    		// Add back the camera listener, but use current version
    		mPictureAcquisitionManager.setPictureAcquisitionListener(getPictureAcquisitionListener());
    	}
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    private void initBroadcastReceivers()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Register connectivity receiver
    	mConnectivityReceiver = new ConnectivityBroadcastReceiver();
    	getContext().registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    	
    	// Register upload receiver
    	IntentFilter filter = new IntentFilter(Global.INTENT_BROADCAST_UPLOAD);
    	filter.addCategory(Intent.CATEGORY_DEFAULT);
    	mServiceCallbackReceiver = new UploadBroadcastReceiver();
    	getContext().registerReceiver(mServiceCallbackReceiver, filter);

    	// Register camera manager geolocation receiver
    	// but only if we are currently doing geolocation
		if (mPictureAcquisitionManager != null && mPictureAcquisitionManager.isGeolocationActive())
		{
			mPictureAcquisitionManager.registerGeolocationBroadcastReceiver();
		}
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    private void closeBroadcastReceivers()
    {
		// Unregister connectivity receiver
		if (mConnectivityReceiver != null)
		{
			getContext().unregisterReceiver(mConnectivityReceiver);
			mConnectivityReceiver = null;
		}
		
		// Unregister upload receiver
		if (mServiceCallbackReceiver != null)
		{
			getContext().unregisterReceiver(mServiceCallbackReceiver);
			mServiceCallbackReceiver = null;
		}
		
		// Unregister camera manager geolocation receiver
		if (mPictureAcquisitionManager != null)
		{
			mPictureAcquisitionManager.unregisterGeolocationBroadcastReceiver();
		}
    }
    
    private void performLogsMaintenance()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Get the cache directory
    	String logsDirectory = FileUtils.getLogsDirectory();
    	long cacheSize = FileUtils.getDirectorySize(logsDirectory);
    	
    	// Check the cache size
    	if (cacheSize > Global.LOG_MAX_SIZE)
    	{
    		FileUtils.cleanDirectory(logsDirectory);
    	}
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    private void clearLogs()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Get the logs directory
    	String logsDirectory = FileUtils.getLogsDirectory();
    	if (FileUtils.isDirectoryPresent(logsDirectory))
    	{
    		FileUtils.cleanDirectory(logsDirectory);
    	}
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    // Send the log through email
    private void sendLog(String logFileName)
    {
    	Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND); 
        emailIntent.setType("text/plain");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"snaprandroid@gmail.com"}); 
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Android Device Log"); 
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "<Please explain the problem here>"); 
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+ logFileName));
        startActivity(Intent.createChooser(emailIntent, "Send email with log..."));
    }
    
    // Ask the user to select a log and then send it thru email
    private void sendPreviousLog()
    {
    	// Get the list of files for the log directory
    	String logDirectory = FileUtils.getLogsDirectory();
    	File   logDir = new File(logDirectory);
    	String[] files = logDir.list();
    	
    	// Sort the file list in reverse order
    	Arrays.sort(files, new Comparator<String>() {

    	    @Override
    	    public int compare(String o1, String o2) {
    	        return o2.compareTo(o1);
    	    }
    	});
    	
    	// Convert to arrayList
    	ArrayList<String> filesList = new ArrayList<String>(Arrays.asList(files));
    	filesList.remove(0);
    	files = filesList.toArray(new String[filesList.size()]);
    	
    	// Invoke the log file picker
    	showLogFilePicker(files);
    }
    
    private void init(Bundle savedInstanceState)
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Initialize globals
    	initGlobals();
    	
    	// Prune the logs
    	if (Global.LOG_MODE)
    	{
    		performLogsMaintenance();
    	}
    	
    	// Initialize the camera manager
    	initCameraManager(savedInstanceState);
    	
    	// Load credentials from preferences
    	String credentials[] = new String[2];
    	CredentialsUtils.loadCredentials(SnaprKitFragment.this.getActivity(), credentials);
    	mUserName = credentials[0];
    	mAccessToken = credentials[1];
    	
    	// Initialize queue settings
    	initQueueSettings();
    	
    	// Initialize the saved state
    	initSavedState(savedInstanceState);
    	
    	// Set action map
    	initActionMap();
        
        // Log
        if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    }
	
	private void showUploadError(String errorMessage)
	{
		AlertUtils.showAlert(getActivity(), errorMessage, "Upload Error");
	}
	
    /**
     * Show a picker to allow the user to select a file
     */
	private void showLogFilePicker(final String[] files)
	{
		// Do not build dialog if the activity is finishing
		if (SnaprKitFragment.this.getActivity().isFinishing()) return;
		
		// Display alert
		AlertDialog.Builder builder = new AlertDialog.Builder(SnaprKitFragment.this.getActivity());
		builder.setTitle("Select a log:");
		builder.setItems(files, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialogInterface, int itemIndex)
			{
				// Call the log send with the file
				String fileName = Global.DIR_LOGS + "/" + files[itemIndex]; 
				sendLog(fileName);
			}
		});
		builder.create().show();
	}
    
	private interface Action
	{
		public void run(String url);
	}
	
	private class UrlMapping {
		public String url;
		public Action action;

		public UrlMapping(String url, Action action) {
			this.url = url;
			this.action = action;
		}
	}
	
	/**
	 * Goes through the list of mappings from beginning to end until it finds a match. 
	 * The mapping uses regular expressions.
	 */
	private Action getActionForUrl(String url) {
		Action action = defaultAction;
		if (url != null) {
			for (UrlMapping urlMapping : mActionMappings) {
				if (url.matches(urlMapping.url)) {
					if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": URL " + url + " matches " + urlMapping.url);
					action = urlMapping.action;
					break;
				}
			}
		}
		return action;
	}
	
	private Context getContext()
	{
		return SnaprKitApplication.getInstance();
	}
	
	// ------------------------------------------------------------------------
	// Library interface
	// ------------------------------------------------------------------------
	
	// Callback interface
	public interface SnaprKitListener
    {
    	public void onPageFinished(String url);
    	public void onSnaprKitParent(String url);
    }
	
	// Callback setting function
	public void setSnaprKitListener(SnaprKitListener listener)
	{
		mSnaprKitListener = listener;
	}
	
	// Moves to the previous page
	public boolean goBack()
	{
		return onBackPressed();
	}
	
	// Start via the normal flow with default page
	public void startNormalFlow()
	{
		startNormalFlow(null);
	}
	
	
	
	private class PostAssetCopyUrlRedirecter implements AssetCopierListener
	{
		// Members
		String mUrl;
		
		public PostAssetCopyUrlRedirecter(String url)
		{
			mUrl = url;
		}

		@Override
		public void onComplete()
		{
			if (mUrl != null)
			{
				mWebView.loadUrl(mUrl);
			}
		}
	}
	
	// Start via the normal flow with custom page
	public void startNormalFlow(String pageUrl)
	{
		// Initialize URL base
		Global.URL_BASE = Global.getLocalUrlBase(getActivity());
		
		// Set default URL
		if (pageUrl == null)
		{
			pageUrl = getStartupUrl();
		}
		
		// Load the URL
		loadUserProvidedUrl(pageUrl);
	}
	
	private void loadUserProvidedUrl(String pageUrl)
	{
       	// Ensure we have a full URL
    	String fullUrl = UrlUtils.relativeUrlToFullUrl(pageUrl);
		
		// Workaround for bug 17535:
    	// Using HTML files from assets folder does not allow passing parameters or anchor strings 
    	// http://code.google.com/p/android/issues/detail?id=17535
    	// Ensure our assets have been extracted to data partition on Honeycomb and ICS
    	if (android.os.Build.VERSION.SDK_INT >= Global.SDK_HONEYCOMB && !AssetUtils.areSnaprAssetsPresent(SnaprKitFragment.this.getActivity()))
    	{
    		// Copy assets and load
			PostAssetCopyUrlRedirecter postAssetCopyUrlRedirecter = new PostAssetCopyUrlRedirecter(fullUrl);
    		AssetsCopier assetsCopier = new AssetsCopier(getActivity(), postAssetCopyUrlRedirecter); 
    		assetsCopier.execute();
    	}
    	else
    	{
        	// Load it
        	if (fullUrl!=null)
        	{
        		mWebView.loadUrl(fullUrl);
        	}
    	}
	}
	
	// Start via the share flow
	public void startShareFlow(String sharedPictureFileName)
	{
		// Declare
		String url = null;
		
		// Initialize URL base
		Global.URL_BASE = Global.getLocalUrlBase(getActivity());
		
		// Set the shared picture filename
		mSharedPictureFileName = sharedPictureFileName;
		
		// Check if we have an image passed in as data
        SharedPictureInfo info = getSharedPictureInfo();
        if (info != null)
        {
        	// Log
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Started via share picture flow");
        	
        	// Get the info data
        	String fileName = info.getFileName();
        	double latitude = info.getLatitude();
        	double longitude = info.getLongitude();
        	 
			// Check if we succeeded
			if(latitude == 0 && longitude == 0)
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found no location information within picture");
				
				// Get the location from location providers
				// This will load later so we do not need to load url below
				getLocationAction.run(null);
			}
			else
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found location information in picture (" + latitude + "," + longitude + ")");
				
	        	// Get the URL
		        url = getSharePictureUrl(fileName, latitude, longitude, null);	
			}
        }
        
        // Check url
        if (url != null)
        {
        	loadUserProvidedUrl(url);
        }
	}
	
	// Move to the specified page URL
	// Url is provided using base name from HTML build
	public void goToPage(String pageUrl)
	{
		// Initialize URL base
		if (Global.URL_BASE == null) Global.URL_BASE = Global.getLocalUrlBase(getActivity());
		
		loadUserProvidedUrl(pageUrl);
	}
	
	// Set the credentials
	public void setCredentials(String snaprUser, String accessToken)
	{
		// Set username and access token
		mUserName = snaprUser;
		mAccessToken = accessToken;
		
		// Set the shared preferences
		CredentialsUtils.saveCredentials(getContext(), mUserName, mAccessToken);
	}
	
	// Clear the credentials
	public void clearCredentials()
	{
		// Clear class members
		mUserName = null;
		mAccessToken = null;
		
		// Clear shared preferences
		CredentialsUtils.clearCredentials(getContext());
	}
	
    public void setFilterPackPath(String path)
    {
    	mFilterPackPath = path;
    }

    public void setStickerPackPath(String path)
    {
    	mStickerPathPath = path;
    }
    
	
}