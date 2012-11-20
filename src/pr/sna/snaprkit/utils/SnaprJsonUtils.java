package pr.sna.snaprkit.utils;

import org.json.JSONObject;

import pr.sna.snaprkit.Global;

public class SnaprJsonUtils
{
		/**
	 * @param json The JSON response from the server
	 * @param section The section in the JSON response or null
	 *        to return the result for the entire operation
	 * @return Boolean value representing operation result
	 */
	public static boolean getOperationResult(JSONObject json, String section)
	{
		boolean result = false;
		JSONObject jsonResponse;
		JSONObject jsonObject;

		try
		{
			if (section == null || section.length() == 0)
			{
				jsonObject = json; 
			}
			else
			{
				jsonResponse = json.getJSONObject("response");
				jsonObject = jsonResponse.getJSONObject(section);
			}
			
			result = jsonObject.getBoolean("success");
		}
		catch(Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to get operation result for " + ((section != null)?section:"operation") + " due to error " + e.toString());
		}
		
		return result;
	}
	
	public static int getOperationErrorCode(JSONObject json, String section)
	{
		JSONObject jsonResponse = null;
		JSONObject jsonObject = null;
		
		try
		{
			if (section == null || section.length() == 0)
			{
				jsonObject = json; 
			}
			else
			{
				jsonResponse = json.getJSONObject("response");
				jsonObject = jsonResponse.getJSONObject(section);
			}
			
			JSONObject error =  jsonObject.getJSONObject("error");
			
			return error.getInt("code");
		}
		catch(Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to get error code due to error " + e.toString());
		}
		
		return -1;
	}

	public static String getOperationErrorMessage(JSONObject json, String section)
	{
		JSONObject jsonResponse = null;
		JSONObject jsonObject = null;
		
		try
		{
			if (section == null || section.length() == 0)
			{
				jsonObject = json; 
			}
			else
			{
				jsonResponse = json.getJSONObject("response");
				jsonObject = jsonResponse.getJSONObject(section);
			}
			
			JSONObject error =  jsonObject.getJSONObject("error");
			
			return error.getString("message");
		}
		catch(Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to get error message due to error " + e.toString());
		}
		
		return "Unknown";
	}
}