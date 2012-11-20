package pr.sna.snaprkit;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import pr.sna.snaprkit.Global;
import pr.sna.snaprkit.utils.AlertUtils;
import pr.sna.snaprkit.utils.CameraUtils;
import pr.sna.snaprkit.utils.GeoUtils;
import pr.sna.snaprkit.utils.TransitionDialog;
import pr.sna.snaprkit.utils.UrlUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;

import pr.sna.snaprkit.utils.GeoManager;

public class PictureAcquisitionManager
{
	// Constants for getActivityResult
	public static final int	TAKE_PICTURE = 0;
	public static final int GPS_SETTINGS_SCREEN = 1;
	public static final int SELECT_PICTURE = 2;
	
	// Constants for image sources
	public static final int PICTURE_SOURCE_CAMERA = 0;
	public static final int PICTURE_SOURCE_GALLERY = 1;
	public static final int PICTURE_SOURCE_STATIC = 2;
	
	// Members
	private int    mImageSource;
	private String mImagePath;
	private Uri mImageUri;
	private Location mLocation;
	private boolean mNetworkProviderEnabled;
	private boolean mGpsProviderEnabled;
	private boolean mWifiProviderEnabled;
	private Fragment mFragment;
	private TransitionDialog mTransitionDialog = null;
	private PictureAcquisitionListener mPictureAcquisitionListener;
	private boolean mIsActive;
	private boolean mIsGeolocationActive;
	private GeolocationBroadcastReceiver mGeolocationCallbackReceiver;
	
	private long mTakePhotoTimestamp = -1;
	
	public PictureAcquisitionManager(Fragment fragment)
	{
		mFragment = fragment;
	}
	
    @Override
	protected void finalize() throws Throwable
	{
    	// Unregister the geolocation broadcast receiver
		unregisterGeolocationBroadcastReceiver();
    	
		// Call base implementation
		super.finalize();
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
		
		switch (requestCode)
		{
		    case TAKE_PICTURE:
		    case SELECT_PICTURE:
		    {		    	
		    	// Log result
		    	if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Returned from picture taking with result code " + resultCode + " and source " + mImageSource);
		    	
		    	// Check result
		        if (resultCode == Activity.RESULT_OK)
		        {
		        	if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Picture taken...");
		        	// Continue
		        	onPictureAcquired(data);
		        }
		        else
		        {
		        	if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Picture NOT taken... going back to the previous URL");
		            terminatePictureAcquisition(null);
		        }
		        
		        break;
		    }
		    
		    case GPS_SETTINGS_SCREEN:
		    {
		    	if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Settings closed...");
		    	
		    	onLocationProviderChangesComplete();
		    	
		    	break;
		    }
	    }
		
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
		
	}
	
	public void onLocationProviderChangesComplete()
	{    	
    	// Get location provider states
    	boolean currentNetworkProviderEnabled = GeoUtils.isNetworkProviderEnabled(SnaprKitApplication.getInstance());
    	boolean currentWifiProviderEnabled = GeoUtils.isWifiProviderEnabled(SnaprKitApplication.getInstance());
    	boolean currentGpsProviderEnabled = GeoUtils.isGpsProviderEnabled(SnaprKitApplication.getInstance());
    	
    	// Restart geolocation service if the user changed the location provider settings
    	if ((currentNetworkProviderEnabled != mNetworkProviderEnabled) || 
    			(currentWifiProviderEnabled != mWifiProviderEnabled) || 
    			(currentGpsProviderEnabled != mGpsProviderEnabled))
    	{
        	// Show the location pending dialog
        	showLocationPendingDialog();
    		
    		// Re-attach the broadcast receiver
    		registerGeolocationBroadcastReceiver();
    		
    		// Start Geolocation service again
    		initGeolocation(Global.GEOLOCATION_CALLER_1);
    		
    		// Also broadcast right away
    		broadcastGeolocation();
    	}
    	else
    	{
    		attemptTagging(mLocation);
    	}
	}
	
	public void terminatePictureAcquisition(long timeStamp)
	{
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + " with no imagePath!");
		
		if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod());
		terminatePictureAcquisition(null);
		
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
	}
	
	private void terminatePictureAcquisition(String imagePath)
	{
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + " with imagePath " + imagePath);
		// Close the camera and return filename to calling activity
		mIsActive = false;
        
		// Send message for the geolocation service to finish
		Intent intent = new Intent(SnaprKitApplication.getInstance(), GeolocationService.class);
		SnaprKitApplication.getInstance().stopService(intent);
		
        // Call the picture taken function 
		mPictureAcquisitionListener.onPictureAcquired(SnaprKitApplication.getInstance(), imagePath, mImageSource, (mLocation != null)?mLocation.getLatitude():0, (mLocation != null)?mLocation.getLongitude():0, mTakePhotoTimestamp);
		
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
	}
    
    public boolean hasGeolocationExif(String fileName)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
		try
		{
			// Get the EXIF interface
			ExifInterface exif = new ExifInterface(fileName);
			
			// Get the tags
			String latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
			String latitudeRef =  exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
			String longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
			String longitudeRef =  exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
			
			// Return
			return ((latitude != null) && (longitude != null) && (latitudeRef != null) && (longitudeRef != null));
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Encountered error " + e.toString());
		}
		
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
		
    	return false;
    }
    
    public boolean isActive()
    {
		return mIsActive;
	}
    
    public void setIsActive(boolean isActive)
    {
    	mIsActive = isActive;
    }
    
    public boolean isGeolocationActive()
    {
		return mIsGeolocationActive;
	}
    
    public void setIsGeolocationActive(boolean isGeolocationActive)
    {
    	mIsGeolocationActive = isGeolocationActive;
    }
    
    public int getImageSource()
    {
    	return mImageSource;
    }
    
    public void setImageSource(int imageSource)
    {
    	mImageSource = imageSource;
    }
    
    public String getImagePath()
    {
    	return mImagePath;
    }
    
    public void setImagePath(String imagePath)
    {
    	mImagePath = imagePath;
    }
    
    public Uri getImageUri()
    {
    	return mImageUri;
    }
    
    public void setImageUri(Uri imageUri)
    {
    	mImageUri = imageUri;
    }
    
    public Location getLocation()
    {
    	return mLocation;
    }
    
    public void setLocation(Location location)
    {
    	mLocation = location;
    }
    
	public boolean getNetworkProviderEnabled()
    {
    	return mNetworkProviderEnabled;
    }
    
    public void setNetworkProviderEnabled(boolean networkProviderEnabled)
    {
    	mNetworkProviderEnabled = networkProviderEnabled;
    }
    
    public boolean getGpsProviderEnabled()
    {
    	return mGpsProviderEnabled;
    }
    
    public void setGpsProviderEnabled(boolean gpsProviderEnabled)
    {
    	mGpsProviderEnabled = gpsProviderEnabled;
    }
    
    public boolean getWifiProviderEnabled()
    {
    	return mWifiProviderEnabled;
    }
    
    public void setWifiProviderEnabled(boolean wifiProviderEnabled)
    {
    	mWifiProviderEnabled = wifiProviderEnabled;
    }
    
    public void setPictureAcquisitionListener(PictureAcquisitionListener pictureAcquisitionListener)
    {
    	mPictureAcquisitionListener = pictureAcquisitionListener;
    }
    
    public long getPhotoTimestamp() {
    	return mTakePhotoTimestamp;
    }
    
    public void setPhotoTimestamp(long photoTimestamp) {
    	mTakePhotoTimestamp = photoTimestamp; 
    }
	
    public void onPictureAcquired(Intent data)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	if (data != null) mImageUri = data.getData();
    	boolean hasUri = mImageUri != null;
    	boolean isContentUri = hasUri ? mImageUri.getScheme().equalsIgnoreCase("content") : null;
    	onPictureAcquired(hasUri ? isContentUri ? UrlUtils.getRealPathFromURI(SnaprKitApplication.getInstance(), mImageUri) : UrlUtils.imageUri2Path(mImageUri) : null);
    }
    
    public void onPictureAcquired(String imagePath)
    {    	
    	mImagePath = imagePath;
		
		// Check again
		if (mImagePath == null)
		{
			if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Picture NOT acquired...");
			AlertUtils.showAlert(mFragment.getActivity(), "Failed to retrieve picture contents!", "Error");
            terminatePictureAcquisition(null);
		}
		else
		{	
	    	// Log
	    	if (Global.LOG_MODE) Global.log(Global.TAG, " Got picture filename " + mImagePath);
	    	
	    	ExifData exifData = CameraUtils.getExifData(mImagePath);
	    	Location exifLocation = exifData.getLocation();
			if(exifLocation != null)
			{
				if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Camera provided geolocation data...");
				mLocation = exifLocation;
				terminatePictureAcquisition(mImagePath);
			}
			else
			{
				if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Camera did NOT provide geolocation data...");
				
				// Put up location pending dialog
				showLocationPendingDialog();
				
		    	// Initiate retrieval of geolocation
				registerGeolocationBroadcastReceiver();
				
				// Send the geolocation broadcast
				broadcastGeolocation();
			}
		}
    }
    
    public void onPictureAcquired2(Location location)
    {
    	// STATUS: When we get to this point we either have a good location
    	// or we have reached timeout -- in all cases the service has sent
    	// out the broadcast and it has stopped itself
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Data summary");
    	if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": -> latitude " + ((location!=null)?location.getLatitude():0));
    	if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": -> longitude " + ((location!=null)?location.getLongitude():0));
    	if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": -> accuracy " + ((location!=null)?location.getAccuracy():0));
    	if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": -> imagePath " + mImagePath);
    	
    	// Unregister the broadcast receiver
		unregisterGeolocationBroadcastReceiver();
		
		// Remove location pending
		cancelLocationPendingDialog();
    	
		// Check location quality
    	if (GeoManager.isLocationAcceptable(location))
    	{
    		if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Location is acceptable...");
    		CameraUtils.geotagPicture(mImagePath, location);
    		terminatePictureAcquisition(mImagePath);
    	}
    	else
    	{
    		if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Location is NOT acceptable...");
    		
    		// Check if the location providers are on
    		mNetworkProviderEnabled = GeoUtils.isNetworkProviderEnabled(SnaprKitApplication.getInstance());
    		mGpsProviderEnabled = GeoUtils.isGpsProviderEnabled(SnaprKitApplication.getInstance());
    		mWifiProviderEnabled = GeoUtils.isWifiProviderEnabled(SnaprKitApplication.getInstance());
    		
    		String disabledProviders = CameraUtils.getDisabledProvidersString(mNetworkProviderEnabled, mWifiProviderEnabled, mGpsProviderEnabled);
    		
    		if (disabledProviders != null && disabledProviders.length() > 0)
    		{
    			if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": The " + disabledProviders  +" providers are disabled...");
    			
    			// Build message
    			String title = "Could not retrieve an accurate location";
    			String message;
    			if (disabledProviders.contains("and"))
    			{
    				message = "The " + disabledProviders  +" providers are disabled. Would you like to enable them and try to get the location again?";
    			}
    			else
    			{
    				message = "The " + disabledProviders  +" provider is disabled. Would you like to enable it and try to get the location again?";
    			}
    			
    			// Show the location providers disabled dialog
    			showProviderDisabledAlert(title, message);
    		}
    		else
    		{
    			if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": All location providers enabled, location subpar...");
    			
    			// We finished geolocation and the location was subpar
    			attemptTagging(location);
    		}
    	}
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    public void attemptTagging2(Location location)
    {
    	// Unregister the broadcast receiver
		unregisterGeolocationBroadcastReceiver();
		
		// Remove location pending
		cancelLocationPendingDialog();
		
		// Call the attempt tagging function
		attemptTagging(location);
    }
    
    private void attemptTagging(Location location)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    
    	// Check to see if we have location
    	if (location != null)
		{
    		if(GeoManager.isLocationAcceptable(location))
    		{
				if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Have high quality location, so we'll gladly tag");
    		}
    		else
    		{
    			if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Have low quality location, so we will reluctantly tag");
    		}
    		
    		// Tag
			CameraUtils.geotagPicture(mImagePath, location);
			terminatePictureAcquisition(mImagePath);
		}
		else if (location == null)
		{
			if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Have absolutely no location, so no tagging");
			terminatePictureAcquisition(mImagePath);
		}
		
		cancelLocationPendingDialog();
    }
    
    public class GeolocationBroadcastReceiver extends BroadcastReceiver
	{		
		@Override
		public void onReceive(Context context, Intent intent)
		{
			// Log
			if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + " with intent action " + intent.getAction().toString());
			
			// Check message type
			if (intent.getAction().equals(Global.INTENT_BROADCAST_LOCATION))
			{
				// Set flag to false
				mIsGeolocationActive = false;

				// Get data from the intent
				int broadcast = intent.getIntExtra(Global.PARAM_ACTION, -1);
				
				// Log
				if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received boadcast with id " + broadcast);
				
				// See the type of broadcast
				if (broadcast == Global.BROADCAST_GEOLOCATION_DETAILS)
				{
					// Extract details
					int type = intent.getIntExtra(Global.PARAM_TYPE, -1);
					int caller = intent.getIntExtra(Global.PARAM_GEOLOCATION_CALLER, -1);
					double latitude = intent.getDoubleExtra(Global.PARAM_LATITUDE, 0);
					double longitude = intent.getDoubleExtra(Global.PARAM_LONGITUDE, 0);
					long   dateLong = intent.getLongExtra(Global.PARAM_DATE, 0);
					float  accuracy = intent.getFloatExtra(Global.PARAM_ACCURACY, 0);
					
					// Create location
					Location location = null;
					if (latitude != 0 && longitude != 0)
					{
						location = new Location("unknown");
						location.setLatitude(latitude);
						location.setLongitude(longitude);
						location.setTime(dateLong);
						location.setAccuracy(accuracy);
					}
					
					// Set the location members
					mIsGeolocationActive = false;
					mLocation = location;
					
					// Log
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received broadcast for geolocation details");
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ":      caller " + caller);
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ":      type " + type);
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ":      latitude " + latitude);
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ":      longitude " + longitude);
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ":      accuracy " + accuracy);
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ":      date " + new Date(dateLong));
					
					if (mFragment.getActivity() == null) return;
					
					// Proceed based on caller
					if (caller == Global.GEOLOCATION_CALLER_0)
					{
						// Go to onPictureTaken2
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Continuing with onPictureTaken2");
						onPictureAcquired2(location);
					}
					else if (caller == Global.GEOLOCATION_CALLER_1)
					{
						// Resume with attemptTagging2
						attemptTagging2(location);
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
    
    public boolean unregisterGeolocationBroadcastReceiver()
    {
		// Unregister broadcast receiver
    	if (mGeolocationCallbackReceiver != null && mFragment != null)
    	{
    		SnaprKitApplication.getInstance().unregisterReceiver(mGeolocationCallbackReceiver);
    		mGeolocationCallbackReceiver = null;
    		cancelLocationPendingDialog();
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    }
    
    public boolean registerGeolocationBroadcastReceiver()
    {
    	// Check internals
    	if (mFragment == null || mGeolocationCallbackReceiver != null) return false;
    	
    	// Create new receiver
    	mGeolocationCallbackReceiver = new GeolocationBroadcastReceiver();
    	
		// Register broadcast receiver
		IntentFilter filter = new IntentFilter(Global.INTENT_BROADCAST_LOCATION);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		SnaprKitApplication.getInstance().registerReceiver(mGeolocationCallbackReceiver, filter);
    	
    	// Return
    	return true;
    }
    
    public void initGeolocation(int caller)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	// Reset members
    	mLocation = null;
    	mIsGeolocationActive = true;
    	
    	// Start geolocation in background and retrieve location in multiple ways
    	// Send the information to the upload service via intent
		Intent intent = new Intent(SnaprKitApplication.getInstance(), GeolocationService.class);
        intent.putExtra(Global.PARAM_ACTION, Global.ACTION_GEOLOCATION_START);
        intent.putExtra(Global.PARAM_GEOLOCATION_CALLER, caller);
        SnaprKitApplication.getInstance().startService(intent);
    	
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    public void broadcastGeolocation()
    {
    	// Send message to the service to broadcast location
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Retrieving location results");
		Intent intent = new Intent(SnaprKitApplication.getInstance(), GeolocationService.class);
        intent.putExtra(Global.PARAM_ACTION, Global.ACTION_GEOLOCATION_BROADCAST);
        SnaprKitApplication.getInstance().startService(intent);
    }
    
    public void initCamera()
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	// Prepare file
    	File photo = new File(pr.sna.snaprkit.utils.FileUtils.getDCIMCameraDirectory(), CameraUtils.getPictureName());
    	
    	// Workaround for bug where the OK button in the camera does nothing
    	// Make sure that the photo file path exists!
    	try
    	{
            if(photo.exists() == false)
            {
            	photo.getParentFile().mkdirs();
            	photo.createNewFile();
            }
        }
    	catch (IOException e)
        {
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() +  " Could not create file " + photo.getAbsolutePath());
        	e.printStackTrace();
        }
    	
    	// Request image from the camera
    	Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
        mImageUri = Uri.fromFile(photo);
        mFragment.startActivityForResult(Intent.createChooser(intent, "Select camera"), TAKE_PICTURE);
        // store time stamp of request
        mTakePhotoTimestamp = System.currentTimeMillis();
        
        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    public void initGallery()
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	// Select an image from the gallery
    	Intent intent = new Intent();
        intent.setType("image/*"); // cannot use jpeg or jpg here -- no results
        intent.setAction(Intent.ACTION_GET_CONTENT);
        mFragment.startActivityForResult(Intent.createChooser(intent,"Select Picture"), SELECT_PICTURE);
    	
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
	    
    public void acquirePicture(int source, PictureAcquisitionListener listener) {
    	acquirePicture(source, listener, null);
    }
    
    public void acquirePicture(int source, PictureAcquisitionListener listener, String imagePath)
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    
    	// Indicate that the module is being used
    	mIsActive = true;
    	
    	// Set the source
    	mImageSource = source;
    	
    	// Set the internal listener
    	mPictureAcquisitionListener = listener;
    	
    	// Initialize location finder
    	initGeolocation(Global.GEOLOCATION_CALLER_0);
    	
    	// Determine what to do based on source
    	if (source == PICTURE_SOURCE_CAMERA)
    	{
	    	// Initialize the camera
	    	initCamera();
    	}
    	else if (source == PICTURE_SOURCE_GALLERY)
    	{
    		// Initialize the gallery
	    	initGallery();
    	}
    	else if (source == PICTURE_SOURCE_STATIC)
    	{
    		// Initialize the static picture
    		onPictureAcquired(imagePath);
    	}
        
        // Log
        if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    }

    private void showProviderDisabledAlert(String title, String message)
    {
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	AlertDialog.Builder builder = new AlertDialog.Builder(mFragment.getActivity());  
    	builder.setMessage(message)  
    		.setCancelable(true)
    		.setTitle(title)
    		.setPositiveButton("Yes", 
    			new DialogInterface.OnClickListener()
    			{  
    				public void onClick(DialogInterface dialog, int id)
    				{
    					if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": User asked to enable location providers...");
    					
    					// Enable Wifi
    					if (!mWifiProviderEnabled)
    					{
    						GeoUtils.enableWifiProvider(SnaprKitApplication.getInstance(), true);
    					}
    					
    					// Check if we have to show the dialog
    					if (!mNetworkProviderEnabled || !mGpsProviderEnabled)
    					{
    						// Show dialog and go to onLocationProviderChangesComplete when it closes
    						showGpsOptions();
    					}
    					else if (!mWifiProviderEnabled)
    					{
    						// Just go to onLocationProviderChangesComplete
    						if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Location provider changes complete");
    						onLocationProviderChangesComplete();
    					}
    				}  
    			})
    		.setNegativeButton("No",  
    			new DialogInterface.OnClickListener(){  
	    			public void onClick(DialogInterface dialog, int id)
	    			{
	    				if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": User refused to enable location providers...");
	    				PictureAcquisitionManager.this.attemptTagging(mLocation);
	    			}  
    		});  
		AlertDialog alert = builder.create();  
		alert.show();
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    private void showGpsOptions()
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
		if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod());
        Intent gpsOptionsIntent = new Intent(  
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);  
        mFragment.startActivityForResult(gpsOptionsIntent, GPS_SETTINGS_SCREEN);
        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    public void showLocationPendingDialog()
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	if (mTransitionDialog != null)
    	{
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> Error: Already have a mPleaseWaitDialog" + Global.getCurrentMethod());
    		return;
    	}
    	
    	mTransitionDialog = new TransitionDialog(mFragment.getActivity());
    	mTransitionDialog.showTransitionDialog("Determining your location...", "Please wait");
	    
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    public void cancelLocationPendingDialog()
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	if (mTransitionDialog != null)
    	{
    		mTransitionDialog.cancelTransitionDialog();
    		mTransitionDialog = null;
    	}
    	
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    public interface PictureAcquisitionListener
    {
    	public void onPictureAcquired(Context context, String fileName, int dataSource, double latitude, double longitude, long timeStamp);
    }
}