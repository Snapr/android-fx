package pr.sna.snaprkit;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import pr.sna.snaprkit.utils.UrlUtils;

public class UploadInfo
{
	// ------------------------------------------------------------------------
	// Constants
	// ------------------------------------------------------------------------
	
	// access_token not saved to JSON for security but needs to be restored as well 
	private static final String UPLOAD_URL = "upload_url";
	private static final String ID = "id";
	private static final String FILENAME = "fileName";
	private static final String DESCRIPTION = "description";
	private static final String LATITUDE = "latitude";
	private static final String LONGITUDE = "longitude";
	private static final String LOCATION_NAME = "location_name";
	private static final String PICTURE_DATE_TIME = "date_time";
	private static final String PRIVACY = "privacy";
	private static final String TWEET = "tweet";
	private static final String FACEBOOK_FEED = "facebook_feed";
	private static final String FACEBOOK_ALBUM = "facebook_album";
	private static final String FACEBOOK_ALBUM_NAME = "facebook_album_name";
	private static final String TUMBLR = "tumblr";
	private static final String FOURSQUARE_CHECKIN = "foursquare_checkin";
	private static final String FOURSQUARE_VENUE = "foursquare_venue";
	private static final String APP_GROUP = "app_group";
	private static final String PUBLIC_GROUP = "public_group";
	private static final String UPLOAD_PARAMS = "upload_params";
	
	// ------------------------------------------------------------------------
	// Members
	// ------------------------------------------------------------------------
	
	// Upload data
	private String mAccessToken;
	private String mUploadUrl;
	private String mId;
	private String mFileName;
	private String mDescription;
	private Double mLatitude;
	private Double mLongitude;
	private String mLocationName;
	private Date   mDateTime;
	private String mPrivacy;
	private boolean mTweet;
	private boolean mFacebookFeed;
	private boolean mFacebookAlbum;
	private String mFacebookAlbumName;
	private boolean mTumblr;
	private boolean mFoursquareCheckin;
	private String mFoursquareVenue;
	private String mAppGroup;
	private String mPublicGroup;
	private String mUploadParams;
	
	// Add new members here and modify toJSON() function
	// Only required for upload description data, not internals
	

	// ------------------------------------------------------------------------
	// JSON Translation
	// ------------------------------------------------------------------------
    
    // Creates a JSON object containing all class properties
    public JSONObject toJSON()
    {
    	// Declare
    	JSONObject json = new JSONObject();
    	
    	// Save the entire object state to JSON
    	try
    	{
    		// Add each download description field
    		// json.put(this.ACCESS_TOKEN, mAccessToken); // Access token not saved for security
    		json.put(UPLOAD_URL, (mUploadUrl != null)?mUploadUrl:"");
    		json.put(ID, (mId != null)?mId:"");
    		json.put(FILENAME, (mFileName != null)?mFileName:"");
    		json.put(DESCRIPTION, (mDescription != null)?mDescription:"");
    		json.put(LATITUDE, getLatitudeString());
    		json.put(LONGITUDE, getLongitudeString());
    		json.put(LOCATION_NAME, (mLocationName != null)?mLocationName:"");
    		json.put(PICTURE_DATE_TIME, (this.getPictureDateTime() != null)?this.getPictureDateTime().getTime():null);
    		json.put(PRIVACY, (mPrivacy != null)?mPrivacy:"");
    		json.put(TWEET, mTweet);
    		json.put(FACEBOOK_FEED, mFacebookFeed);
    		json.put(FACEBOOK_ALBUM, mFacebookAlbum);
    		json.put(FACEBOOK_ALBUM_NAME, (mFacebookAlbumName != null)?mFacebookAlbumName:"");
    		json.put(TUMBLR, mTumblr);
    		json.put(FOURSQUARE_CHECKIN, mFoursquareCheckin);
    		json.put(FOURSQUARE_VENUE, mFoursquareVenue);
    		json.put(APP_GROUP, mAppGroup);
    		json.put(PUBLIC_GROUP, mPublicGroup);
    		json.put(UPLOAD_PARAMS, mUploadParams);
    	}
    	catch(JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to save object state to JSON!" + e.toString());
    	}
    	
    	// Return
    	return json;
    }
    
    public static UploadInfo fromJSON(JSONObject json)
    {
    	// Declare
    	UploadInfo uploadInfo = new UploadInfo();
    	
    	// Read each JSON property and save it into the object
    	// Upload URL
    	try
    	{
    		uploadInfo.mUploadUrl = json.getString(UPLOAD_URL);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore upload URL");
    	}
    	
    	// Id
    	try
    	{
    		uploadInfo.mId = json.getString(ID);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore ID");
    	}
    	
    	// Filename
    	try
    	{
    		uploadInfo.mFileName = json.getString(FILENAME);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore filename");
    	}
    	
    	// Description
    	try
    	{
    		uploadInfo.mDescription = json.getString(DESCRIPTION);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore description");
    	}
    	
    	// Latitude
    	try
    	{
    		uploadInfo.mLatitude = json.getDouble(LATITUDE);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore latitude");
    	}
    	
    	// Longitude
    	try
    	{
    		uploadInfo.mLongitude = json.getDouble(LONGITUDE);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore longitude");
    	}

    	// Location name
    	try
    	{
    		uploadInfo.mLocationName = json.getString(LOCATION_NAME);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore location name");
    	}

    	// Picture date time
    	try
    	{
    		uploadInfo.mDateTime = new Date(json.getLong(PICTURE_DATE_TIME));
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore picture date time");
    	}

    	// Privacy
    	try
    	{
    		uploadInfo.mPrivacy = json.getString(PRIVACY);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore privacy");
    	}

    	// Tweet
    	try
    	{
    		uploadInfo.mTweet = json.getBoolean(TWEET);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore tweet flag");
    	}

    	// Facebook Feed
    	try
    	{
    		uploadInfo.mFacebookFeed = json.getBoolean(FACEBOOK_FEED);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore facebook feed flag");
    	}

    	// Facebook Album
    	try
    	{
    		uploadInfo.mFacebookAlbum = json.getBoolean(FACEBOOK_ALBUM);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore facebook album flag");
    	}

    	// Facebook Album Name
    	try
    	{
    		uploadInfo.mFacebookAlbumName = json.getString(FACEBOOK_ALBUM_NAME);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore facebook album name");
    	}
    	
    	// Tumblr
    	try
    	{
    		uploadInfo.mTumblr = json.getBoolean(TUMBLR);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore tumblr flag");
    	}

    	// Foursquare Checkin
    	try
    	{
    		uploadInfo.mFoursquareCheckin = json.getBoolean(FOURSQUARE_CHECKIN);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore foursquare checkin flag");
    	}
    	
    	// Foursquare Venue
    	try
    	{
    		uploadInfo.mFoursquareVenue = json.getString(FOURSQUARE_VENUE);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore foursquare venue");
    	}
    	
    	// App Group
    	try
    	{
    		uploadInfo.mAppGroup = json.getString(APP_GROUP);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore app group");
    	}
    	
    	// Public Group
    	try
    	{
    		uploadInfo.mPublicGroup = json.getString(PUBLIC_GROUP);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore public group");
    	}
    	
    	// Upload Params
    	try
    	{
    		uploadInfo.mUploadParams = json.getString(UPLOAD_PARAMS);
    	}
    	catch (JSONException e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to restore upload params");
    	}
    	
    	// Return
    	return uploadInfo;
    }
    
    public JSONObject getUploadProgressJSON(String uploadStatus, int percent)
    {
    	// Declare
		JSONObject jsonUpload = new JSONObject();
		JSONObject jsonLocation = new JSONObject();
		JSONObject jsonShared = new JSONObject();
		
		try
		{
			// Fill location object
			jsonLocation.put(Global.PARAM_LATITUDE, mLatitude);
			jsonLocation.put(Global.PARAM_LONGITUDE, mLongitude);
			jsonLocation.put(Global.PARAM_LOCATION, (mFoursquareVenue!=null)?mFoursquareVenue:"");
			
			// Fill shared object
			jsonShared.put(Global.PARAM_TWEETED, mTweet);
			jsonShared.put(Global.PARAM_FACEBOOK_NEWSFEED, mFacebookFeed);
			jsonShared.put(Global.PARAM_FOURSQUARE_CHECKIN, mFoursquareCheckin);
			jsonShared.put(Global.PARAM_TUMBLR, mTumblr);
			jsonShared.put(Global.PARAM_FOURSQUARE_VENUE, mFoursquareVenue);
			
			// Fill upload object
			jsonUpload.put(Global.PARAM_ID, (mId != null)?mId:"");
			jsonUpload.put(Global.PARAM_THUMBNAIL, (mFileName != null)?mFileName:"");
			jsonUpload.put(Global.PARAM_UPLOAD_STATUS, uploadStatus);
			jsonUpload.put(Global.PARAM_PERCENT_COMPLETE, percent);
			jsonUpload.put(Global.PARAM_STATUS, (mPrivacy!=null)?mPrivacy:"");
			jsonUpload.put(Global.PARAM_DESCRIPTION, (mDescription != null)?mDescription:"");
			jsonUpload.put(Global.PARAM_LOCATION, jsonLocation);
			jsonUpload.put(Global.PARAM_DATE, getPictureDateTimeString());
			jsonUpload.put(Global.PARAM_SHARED, jsonShared);
		}
		catch (JSONException e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed with JSON error " + e);
		}
		
		return jsonUpload;
    }
    
	// ------------------------------------------------------------------------
	// Constructors and Destructor
	// ------------------------------------------------------------------------
    
    public UploadInfo()
    {
    }
	
	// ------------------------------------------------------------------------
	// Getters / Setters
	// ------------------------------------------------------------------------
    
    public String getId()
    {
		return mId;
	}

	public void setId(String uploadId)
	{
		this.mId = uploadId;
	}

	public String getDescription()
	{
		return mDescription;
	}

	public void setDescription(String description)
	{
		this.mDescription = description;
	}

	public String getPrivacy()
	{
		return mPrivacy;
	}

	public void setPrivacy(String privacy)
	{
		this.mPrivacy = privacy;
	}

	public boolean getTweet()
	{
		return mTweet;
	}

	public void setTweet(boolean tweet)
	{
		this.mTweet = tweet;
	}

	public String getAccessToken()
	{
		return mAccessToken;
	}

	public void setAccessToken(String accessToken)
	{
		this.mAccessToken = accessToken;
	}
	
	public boolean getFacebookFeed()
	{
		return mFacebookFeed;
	}

	public void setFacebookFeed(boolean facebookFeed)
	{
		this.mFacebookFeed = facebookFeed;
	}
	
	public boolean getFacebookAlbum()
	{
		return mFacebookAlbum;
	}

	public void setFacebookAlbum(boolean facebookAlbum)
	{
		this.mFacebookAlbum = facebookAlbum;
	}

	public String getFacebookAlbumName()
	{
		return mFacebookAlbumName;
	}

	public void setFacebookAlbumName(String facebookAlbumName)
	{
		this.mFacebookAlbumName = facebookAlbumName;
	}
	
	public boolean getTumblr()
	{
		return mTumblr;
	}

	public void setTumblr(boolean tumblr) {
		this.mTumblr = tumblr;
	}

	public boolean getFoursquareCheckin()
	{
		return mFoursquareCheckin;
	}

	public void setFoursquareCheckin(boolean foursquareCheckin)
	{
		this.mFoursquareCheckin = foursquareCheckin;
	}
		
	public String getUploadUrl() {
		return mUploadUrl;
	}
	
	public void setUploadUrl(String uploadUrl)
	{
		mUploadUrl = uploadUrl;
	}

	public String getFoursquareVenue()
	{
		return mFoursquareVenue;
	}

	public void setFoursquareVenue(String foursquareVenue)
	{
		mFoursquareVenue = foursquareVenue;
	}
	
	public String getAppGroup()
	{
		return mAppGroup;
	}

	public void setAppGroup(String appGroup)
	{
		mAppGroup = appGroup;
	}
	
	public String getPublicGroup()
	{
		return mPublicGroup;
	}

	public void setPublicGroup(String publicGroup)
	{
		mAppGroup = publicGroup;
	}

	public String getUploadParams()
	{
		return mUploadParams;
	}

	public void setUploadParams(String uploadParams)
	{
		mUploadParams = uploadParams;
	}
	
	public String getFileName()
	{
		return mFileName;
	}
	
	public String getFileNameUrl()
	{
		return UrlUtils.fileNameToUrl(mFileName);
	}
	
	public void setFileName(String fullFilePath)
	{
		// Check to see that the filename does not have a file:// prefix
		if (fullFilePath != null)
		{
			// Check startin sequence
			if (fullFilePath.startsWith("file://"))
			{
				// Save a substring
				mFileName = UrlUtils.urlToFileName(fullFilePath);
			}
			else
			{
				// Save the full path
				mFileName = fullFilePath;
			}
		}
	}
	
	public Double getLatitude()
	{
		return mLatitude;
	}

	public String getLatitudeString()
	{
		return ((mLatitude != null)?mLatitude.toString():"");
	}
	
	public void setLatitude(Double latitude) {
		this.mLatitude = latitude;
	}

	public void setLatitude(String latitude)
	{
		try
		{
			this.mLatitude = Double.parseDouble(latitude);
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Error parsing double string " + e.toString());
		}
	}
	
	public Double getLongitude()
	{
		return mLongitude;
	}

	public String getLongitudeString()
	{
		return ((mLongitude != null)?mLongitude.toString():"");
	}
	
	public void setLongitude(Double longitude)
	{
		this.mLongitude = longitude;
	}
	
	public void setLongitude(String longitude)
	{
		try
		{
			this.mLongitude = Double.parseDouble(longitude);
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Error parsing double string " + e.toString());
		}
	}

	public String getLocationName()
	{
		return mLocationName;
	}

	public void setLocationName(String locationName)
	{
		this.mLocationName = locationName;
	}
	
	public Date getPictureDateTime()
	{
		return mDateTime;
	}
	
	public String getPictureDateTimeString()
	{
		String dateString = "";
		SimpleDateFormat sdf = new SimpleDateFormat(Global.DATE_FORMAT_API);
		if (mDateTime!=null)
		{
			dateString = sdf.format(mDateTime);
		}
		return (dateString);
	}

	public void setPictureDateTime(Date date)
	{
		this.mDateTime = date;
	}
	
	public void setPictureDateTime(String date)
	{
		// Declare
		SimpleDateFormat sdf = null;
		
		// Try to parse using regular format
		try
		{
			sdf = new SimpleDateFormat(Global.DATE_FORMAT_API);
			this.mDateTime = sdf.parse(date);
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + ": Error parsing date string " + e.toString());
		}
		
		// Try to parse using HTC Sensation format
		if (this.mDateTime == null)
		{
			try
			{
				sdf = new SimpleDateFormat(Global.DATE_FORMAT_API_HTC_SENSATION);
				this.mDateTime = sdf.parse(date);
			}
			catch (Exception e)
			{
				if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + ": Error parsing date string " + e.toString());
			}
		}
	}
}