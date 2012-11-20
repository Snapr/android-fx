package pr.sna.snaprkit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import pr.sna.snaprkit.utils.AssetUtils;
import pr.sna.snaprkit.utils.FileUtils;
import android.app.Activity;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

public class Global
{
	// General settings
	public static final String TAG = "SNAPRLIB";
	
	// Java debugging
    public static final boolean LOG_MODE = false;
    
	// HTML debugging and local URL base
    public static final boolean HTML_DEBUG = false;
	public static final String URL_BASE_SDCARD_HTML = "file://" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/snaprkit_html/";  // must end in slash
	public static final String URL_BASE_ASSETS_HTML = "file:///android_asset/snaprkit_html/"; // must end in slash
	public static String URL_BASE = "";              // generated based on HTML_DEBUG and URL_BASE_* values above, overwritten from start flows in Honeycomb+
	
	// Remote environment and remote URL base
	public static final String ENVIRONMENT = "live-android";              // either dev-android or live-android
	public static final String URL_SERVER_PROD = "https://api.sna.pr/";   // must end in slash
	public static final String URL_SERVER_DEV = "http://dev.sna.pr/api/"; // must end in slash
	public static final String URL_SERVER = getRemoteUrlBase();           // generated based on ENVIRONMENT and URL_SERVER_* values above
	
	// Shared Preferences
	public static final String SNAPR_PREFERENCES = "SnaprPrefs";
	public static final String SNAPR_PREFERENCES_USERNAME = "username";
	public static final String SNAPR_PREFERENCES_ACCESS_TOKEN = "access_token";
	public static final String SNAPR_PREFERENCES_QUEUE_ON = "QueueOn";
	public static final String SNAPR_PREFERENCES_QUEUE_WIFI_ONLY = "QueueWifiOnly";
	public static final String SNAPR_PREFERENCES_MAP_LAST_LATITUDE = "map_last_latitude";
	public static final String SNAPR_PREFERENCES_MAP_LAST_LONGITUDE = "map_last_longitude";
	public static final String SNAPR_PREFERENCES_MAP_LAST_ZOOM_LEVEL = "map_last_zoom_level";
	public static final String SNAPR_PREFERENCES_MAP_FILTER_LAST_INDEX = "map_filter_last_index";
	public static final String SNAPR_PREFERENCES_MAP_FILTER_LAST_DATE = "map_filter_last_date";

	// Features
	public static final boolean FEATURE_AVIARY_SDK = false;
	
	// Android SDK versions
	public static final int     SDK_HONEYCOMB = 11;
	
	// Directories
	public static final String DIR_DCIM = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM";
	public static final String DIR_LOGS = Environment.getExternalStorageDirectory().getAbsolutePath() + "/snapr/logs";
	
	// Cache
	public static final long   CACHE_MAX_SIZE     = 50 * 1024 * 1024 ; // 50 MB
	
	// Logging
	public static final boolean LOG_DISK = false; 
	public static final long   LOG_MAX_SIZE       =  1 * 1024 * 1024;  //  1 MB
	public static final String LOG_NAME_PREFIX = "snapr_";
	public static final File   FILE_LOG = FileUtils.openLogFile();
	
	// Schemes
	public static final String SCHEME_SNAPR = "snapr";
	public static final String SCHEME_FILE = "file";
	
	// Pages
	public static final String URL_MENU = "index.html";
	public static final String URL_UPLOAD = "upload/";
	public static final String URL_LINKED_SERVICES = "connect/";
	public static final String URL_FEED = "feed/";
	public static final String URL_MAP = "map/";
	public static final String URL_PEOPLE = "people/";
	public static final String URL_PHOTO_SHARE = "photo-edit/";
	
	public static final String URL_SNAPR_LOGIN = "snapr://login";
	public static final String URL_SNAPR_LOGOUT = "snapr://logout";
	public static final String URL_SNAPR_GET_LOCATION = "snapr://get_location";
	public static final String URL_SNAPR_UPLOAD = "snapr://upload";
	public static final String URL_SNAPR_UPLOAD_PROGRESS = "snapr://upload_progress";
	
	public static final String URL_UPLOAD_LOCATION = getRemoteUrlBase() + "upload/";
	public static final String URL_SEARCH_LOCATION = getRemoteUrlBase() + "search/";
	public static final String URL_MAPS_GEOCODE = "http://maps.googleapis.com/maps/api/geocode/json";
	
	// Parameters
	public static final String PARAM_USERNAME = "username";
	public static final String PARAM_SNAPR_USER = "snapr_user";
	public static final String PARAM_ACCESS_TOKEN = "access_token";
	public static final String PARAM_APPMODE = "appmode";
	public static final String PARAM_ENVIRONMENT = "environment";
	public static final String PARAM_NEW_USER = "new_user";
	public static final String PARAM_CLIENT_ID = "client_id";
	public static final String PARAM_PHOTO_PATH = "photo_path";
	public static final String PARAM_LATITUDE = "latitude";
	public static final String PARAM_LONGITUDE = "longitude";
	public static final String PARAM_REDIRECT = "redirect";
	public static final String PARAM_REDIRECT_URL = "redirect_url";
	public static final String PARAM_URL = "url";
	public static final String PARAM_ID = "id";
	public static final String PARAM_DESCRIPTION = "description";
	public static final String PARAM_STATUS = "status";
	public static final String PARAM_TWEET = "tweet";
	public static final String PARAM_TWEETED = "tweeted";
	public static final String PARAM_FACEBOOK_FEED = "facebook_feed";
	public static final String PARAM_FACEBOOK_ALBUM = "facebook_album";
	public static final String PARAM_FACEBOOK_ALBUM_NAME = "facebook_album_name";
	public static final String PARAM_FACEBOOK_NEWSFEED = "facebook_newsfeed";
	public static final String PARAM_TUMBLR = "tumblr";
	public static final String PARAM_FOURSQUARE_CHECKIN = "foursquare_checkin";
	public static final String PARAM_FOURSQUARE_VENUE = "foursquare_venue";
	public static final String PARAM_PRIVACY = "privacy";
	public static final String PARAM_DEVICE_TIME = "device_time";
	public static final String PARAM_ACTION = "action";
	public static final String PARAM_NUM_UPLOADS = "num_uploads"; 
	public static final String PARAM_PERCENT = "percent";
	public static final String PARAM_THUMBNAIL = "thumbnail";
	public static final String PARAM_UPLOAD_STATUS = "upload_status";
	public static final String PARAM_PERCENT_COMPLETE ="percent_complete";
	public static final String PARAM_LOCATION = "location";
	public static final String PARAM_DATE = "date";
	public static final String PARAM_VENUE_ID = "venue_id";
	public static final String PARAM_VENUE_NAME = "venue_name";
	public static final String PARAM_VENUE_SOURCE = "venue_source";
	public static final String PARAM_SHARED = "shared";
	public static final String PARAM_UPLOAD = "upload";
	public static final String PARAM_SEND = "send";
	public static final String PARAM_FOURSQUARE_VENUE_ID = "foursquare_venue_id";
	public static final String PARAM_UPLOAD_MODE = "upload_mode";
	public static final String PARAM_SIGNUPS_NEEDED = "needed_signups";
	public static final String PARAM_PHOTO = "photo";
	public static final String PARAM_TO_LINK = "to_link";
	public static final String PARAM_SETTING = "setting";
	public static final String PARAM_CANCEL = "cancel";
	public static final String PARAM_QUEUE_UPLOAD_MODE_WIFI_ONLY = "queue_wifi_only";
	public static final String PARAM_QUEUE_UPLOAD_MODE_ON = "queue_on";
	public static final String PARAM_JSON_DATA = "json_data";
	public static final String PARAM_PHOTO_ID = "photo_id";
	public static final String PARAM_AREA = "area";
	public static final String PARAM_BACK = "back";
	public static final String PARAM_ZOOM_LEVEL = "zoom_level";
	public static final String PARAM_ERROR_MESSAGE = "error_message";
	public static final String PARAM_KEYWORDS = "keywords";
	public static final String PARAM_RESULTS = "results";
	public static final String PARAM_VIEWPORT = "viewport";
	public static final String PARAM_LAT = "lat";
	public static final String PARAM_LNG = "lng";
	public static final String PARAM_NORTHEAST = "northeast";
	public static final String PARAM_SOUTHWEST = "southwest";
	public static final String PARAM_FORMATTED_ADDRESS = "formatted_address";
	public static final String PARAM_GEOMETRY = "geometry";
	public static final String PARAM_SENSOR = "sensor";
	public static final String PARAM_ADDRESS = "address";
	public static final String PARAM_GROUP = "group";
	public static final String PARAM_N = "n";
	public static final String PARAM_SNAPR_ID = "snapr_id";
	public static final String PARAM_TYPE = "type";
	public static final String PARAM_GEOLOCATION_CALLER = "geolocation_caller";
	public static final String PARAM_ACCURACY = "accuracy";
	public static final String PARAM_FIRST_NAME = "first_name";
	public static final String PARAM_EMAIL = "email";
	public static final String PARAM_KEY = "key";
	public static final String PARAM_APP_GROUP = "app_group";
	public static final String PARAM_PUBLIC_GROUP = "public_group";
	public static final String PARAM_UPLOAD_PARAMS = "upload_params";
	
	// Images
	public static final String IMAGE_NAME_PREFIX = "SNAPR_";
	public static final String IMAGE_NAME_DATE_FORMAT = "yyyyMMdd_kkmmss"; // 20110101_230101
	
	// File queue messaging
	public static final int    ACTION_QUEUE_START = 0;
	public static final int    ACTION_QUEUE_STOP = 1;
	public static final int    ACTION_QUEUE_CLEAR = 2;
	public static final int    ACTION_QUEUE_ADD = 3;
	public static final int    ACTION_QUEUE_REMOVE = 4;
	public static final int    ACTION_QUEUE_UPLOAD_MODE = 5;
	public static final int    ACTION_QUEUE_UPDATE_STATUS = 6;
	
	public static final int    ACTION_GEOLOCATION_START = 0;
	public static final int    ACTION_GEOLOCATION_BROADCAST = 1;
	
	// Upload broadcast messaging
	public static final String INTENT_BROADCAST_UPLOAD = "com.snapr.intent.action.UPLOAD_CHANGE";
	public static final int    BROADCAST_UPLOAD_STARTED = 0;
	public static final int    BROADCAST_UPLOAD_PROGRESS = 1;
	public static final int    BROADCAST_UPLOAD_CANCELED = 2;
	//public static final int    BROADCAST_UPLOAD_FAILED = 3;
	public static final int    BROADCAST_UPLOAD_COMPLETED = 4;
	public static final int    BROADCAST_UPLOAD_STATUS = 5;
	public static final int    BROADCAST_UPLOAD_ERROR = 6;
	
	// Upload modes
	public static final int    UPLOAD_MODE_ON = 0;
	public static final int    UPLOAD_MODE_WIFI = 1;
	public static final int    UPLOAD_MODE_STOPPED = 2;
	
	// Location broadcast messaging
	public static final String INTENT_BROADCAST_LOCATION = "com.snapr.intent.action.LOCATION_CHANGE";
	public static final int    BROADCAST_GEOLOCATION_DETAILS = 0;
	
	// Location results
	public static final int    GEOLOCATION_RESULT_NONE = 0;
	public static final int    GEOLOCATION_RESULT_TIMEOUT = 1;
	public static final int    GEOLOCATION_RESULT_TERMINATE = 2;
	
	// Location callers
	public static final int    GEOLOCATION_CALLER_0 = 0;
	public static final int    GEOLOCATION_CALLER_1 = 0;
	
	// Share services
	public static final String SHARE_SERVICE_TWITTER = "twitter";
	public static final String SHARE_SERVICE_FACEBOOK = "facebook";
	public static final String SHARE_SERVICE_TUMBLR = "tumblr";
	public static final String SHARE_SERVICE_FOURSQUARE = "foursquare";
	
	// Share services signup errors
	public static final int    ERROR_TWITTER_SIGNUP_NEEDED = 20;
	public static final int    ERROR_FACEBOOK_SIGNUP_NEEDED = 28;
	public static final int    ERROR_TUMBLR_SIGNUP_NEEDED = 30;
	public static final int    ERROR_FOURSQUARE_SIGNUP_NEEDED = 29;
	
	// Geocoder errors
	public static final int    GEOCODER_NOT_PRESENT = 0;
	public static final int    GEOCODER_ERROR = 1;
	
	// Download mode - HTTP / HTTPS
	public static final boolean DOWNLOAD_PICTURES_VIA_HTTPS = true;
	
	// Formats
	public static final String DATE_FORMAT_API = "yyyy-MM-dd HH:mm:ss Z";
	public static final String DATE_FORMAT_API_HTC_SENSATION = "yyyy/MM/dd HH:mm:ss Z"; // HTC Sensation date/time format bug fix
	
	// Connection failure threshold
	public static final int    CONNECTION_FAILURE_TIME_THRESHOLD = 30000; // 30 seconds
	public static final int    CONNECTION_FAILURE_NUM_THRESHOLD  = 5;     // Fail 5 times and we pause
	
	// Screen density - populated once app loads first activity
	public static int          densityDpi;
	
	// Geolocation callers
	public static final int		GEOLOCATION_GET_LOCATION = 0;
	public static final int		GEOLOCATION_CAMERA = 1;
	public static final int		GEOLOCATION_MAP = 2;
	public static final int		GEOLOCATION_PHOTO_SHARE = 3;
	
	// ---------------------------------------------------------------------------------
	// Global methods
	// ---------------------------------------------------------------------------------
	
	public static String getCurrentMethod()
	{
		if (LOG_MODE)
		{
			String className = Thread.currentThread().getStackTrace()[3]
					.getClassName();
			int lastPeriod = className.lastIndexOf('.');
			if (lastPeriod >= className.length() - 1)
			{
				className = "";
			} else if (lastPeriod >= 0)
			{
				className = className.substring(lastPeriod + 1);
			}
			return className + "."
					+ Thread.currentThread().getStackTrace()[3].getMethodName()
					+ "()";
		} else
		{
			return null;
		}
	}

	public static void log(String message)
	{
		log(TAG, message);
	}

	public static void log(String tag, String message)
	{
		if (Global.LOG_MODE)
		{
			// Add to log buffer
			Log.i(tag, message);

			// Add to log disk file
			appendLog(message);
		}
	}

	@SuppressWarnings("unused")
	private static void appendLog(String text)
	{
		if (!Global.LOG_DISK)
			return;

		try
		{
			// BufferedWriter for performance, true to set append to file flag
			BufferedWriter buf = new BufferedWriter(new FileWriter(FILE_LOG,
					true));
			buf.append(text);
			buf.newLine();
			buf.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static int getScreenDensity(Activity activity)
	{
		// Determine graphic resolution in dpi
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		// Return
		return metrics.densityDpi;
	}

	@SuppressWarnings("unused")
	public static String getLocalUrlBase(Activity activity)
	{
		if (HTML_DEBUG == true)
		{
			return URL_BASE_SDCARD_HTML;
		}
		else
		{
			if (android.os.Build.VERSION.SDK_INT >= Global.SDK_HONEYCOMB)
			{
				// Special workaround for Honeycomb
				return "file://" + AssetUtils.getSnaprAssetsDirectory(activity);
			}
			else
			{
				return URL_BASE_ASSETS_HTML;
			}
		}
	}
	
	public static String getRemoteUrlBase()
	{
		if (ENVIRONMENT.startsWith("live"))
		{
			return URL_SERVER_PROD;
		}
		else
		{
			return URL_SERVER_DEV;
		}
	}
}
