package pr.sna.snaprkit.utils;

import pr.sna.snaprkit.Global;
import android.content.Context;
import android.content.SharedPreferences;

public class CredentialsUtils
{    
    // Load shared preferences into class members
    public static void loadCredentials(Context context, String[] credentials)
    {
    	// Get the shared preferences object
        SharedPreferences prefs = context.getSharedPreferences(Global.SNAPR_PREFERENCES, Context.MODE_PRIVATE);
        
        // Load preferences
        String userName = prefs.getString(Global.SNAPR_PREFERENCES_USERNAME, "");
        String accessToken = prefs.getString(Global.SNAPR_PREFERENCES_ACCESS_TOKEN, "");
        credentials[0] = userName;
        credentials[1] = accessToken;
    }
    
    public static void saveCredentials(Context context, String userName, String accessToken)
    {
    	// Save shared preferences
		SharedPreferences prefs = context.getSharedPreferences(Global.SNAPR_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString(Global.SNAPR_PREFERENCES_USERNAME, userName);
		prefEditor.putString(Global.SNAPR_PREFERENCES_ACCESS_TOKEN, accessToken);
		prefEditor.commit();
    }
    
    public static void clearCredentials(Context context)
    {
    	// Clear the credentials
    	SharedPreferences prefs = context.getSharedPreferences(Global.SNAPR_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString(Global.SNAPR_PREFERENCES_USERNAME, "");
		prefEditor.putString(Global.SNAPR_PREFERENCES_ACCESS_TOKEN, "");
		prefEditor.commit();
    }
    
    public static boolean haveCredentials(String userName, String accessToken)
    {
    	return (userName!= null && userName.length() > 0 && accessToken != null && accessToken.length() > 0);
    }
}
