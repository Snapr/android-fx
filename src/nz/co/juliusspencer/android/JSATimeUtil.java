package nz.co.juliusspencer.android;

import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

public class JSATimeUtil {
	private static Map<String, Long> sTimeMap = new HashMap<String, Long>();			// the mapping of an id to the last time a message was logged
	private static final String DEFAULT_TIME_ID = "time";
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * log time
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public synchronized static void logTime() {
		logTime(DEFAULT_TIME_ID, null, JSATimeUtil.class.getSimpleName());
	}
	
	public synchronized static void logTime(Object message) {
		logTime(DEFAULT_TIME_ID, message, JSATimeUtil.class.getSimpleName());
	}
	
	public synchronized static void logTime(Object message, String tag) {
		logTime(DEFAULT_TIME_ID, message, tag);
	}
	
	public synchronized static void logTime(String id, Object message, String tag) {
		Long previousTime = sTimeMap.get(id);
		long currentTime = System.currentTimeMillis();
		sTimeMap.put(id, currentTime);
		String timeString = previousTime != null ? JSATimeUtil.humanReadableDuration(currentTime - previousTime, true) : null;
		if (timeString != null) message = (message == null) ? timeString : message + " (" + timeString + ")";
		Log.i(tag, message != null ? message.toString() : "");
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * human readable duration
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	/**
	 * {@link Inherited}
	 */
	public static String humanReadableDuration(long milliseconds) {
		return humanReadableDuration(milliseconds, false);
	}

	/**
	 * Return a human readable length of time for the given duration. 
	 * 
	 * For example: 
	 * 2 days 1 hour 46 minutes 1 minute 35 seconds 
	 * less than a second 
	 * 34 milliseconds
	 */
	public static String humanReadableDuration(long milliseconds, boolean includeMilliseconds) {
		long millis = milliseconds % 1000;
		milliseconds = milliseconds / 1000;
		long seconds = milliseconds % 60;
		milliseconds /= 60;
		long minutes = milliseconds % 60;
		milliseconds /= 60;
		long hours = milliseconds % 24;
		milliseconds /= 24;
		long days = milliseconds;
		List<String> list = new ArrayList<String>();
		if (days != 0) list.add(days + (days == 1 ? " day" : " days"));
		if (hours != 0) list.add(hours + (hours == 1 ? " hour" : " hours"));
		if (minutes != 0) list.add(minutes + (minutes == 1 ? " minute" : " minutes"));
		if (seconds != 0) list.add(seconds + (seconds == 1 ? " second" : " seconds"));
		if (!includeMilliseconds && list.size() == 0) return "less than a second";
		if (includeMilliseconds && (list.size() == 0 || millis != 0)) list.add(millis + (millis == 1 ? " millisecond" : " milliseconds"));
		return JSAArrayUtil.join(list, " ");
	}
}
