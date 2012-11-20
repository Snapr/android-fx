package pr.sna.snaprkit.utils;

import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import pr.sna.snaprkit.Global;

public class GeoUtils
{
    /**
     * Determines if the network provider is enabled
     * @return Returns a boolean indicating whether the network provider is enabled 
     */
    public static boolean isNetworkProviderEnabled(Context context)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
    	return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    
    /**
     * Determines if the GPS provider is enabled
     * @return
     */
    public static boolean isGpsProviderEnabled(Context context)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE); 
    	return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    
    /**
     * Determines if the WiFi provider is enabled
     * @return
     */
    public static boolean isWifiProviderEnabled(Context context)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	return wifiManager.isWifiEnabled();
    }
    
    public static boolean enableWifiProvider(Context context, boolean isEnabled)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	try
    	{
    		WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    		wifiManager.setWifiEnabled(isEnabled);
    		return true;
    	}
    	catch (Exception e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod() + ": Error " + e.toString());
    	}
    	
    	return false;
    }
}