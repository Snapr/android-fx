package pr.sna.snaprkitfx;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import pr.sna.snaprkitfx.util.JsonUtil;
import android.content.Context;

/**
 * The {@code SnaprSetting} class provides a simple implementation that can be used to configure a filter or sticker
 * in terms of visibility and whether it's locked or not. The class supports parsing from correctly formatted json,
 * and can also be constructed manually by invoking the static {@link #getSettings(String, boolean, String, boolean, Date, Date)}
 * method. 
 * 
 * {@link #getDefaultSettings(String)} returns a default configuration, with all flags initialised to {@code false} and
 * all other fields to {@code null} - this is only a helper for the parsing framework and should not be called called manually.
 * @author mhelder
 */
public final class SnaprSetting implements Serializable {
	private static final long serialVersionUID = -4356276975967816716L;
	
	private final String mSlug;
	private boolean mHidden;
	private boolean mLocked;
	private String mUnlockMessage;
	private Date mShowDate;
	private Date mHideDate;
	
	/* package */ SnaprSetting(String slug) {
		mSlug = slug;
	}

	/* package */ static SnaprSetting parse(Context context, JSONObject json, String slug) throws JSONException, IOException, ParseException {
		SnaprSetting settings = new SnaprSetting(slug);
		settings.mHidden = json.isNull("hidden") ? false : json.getBoolean("hidden");
		settings.mLocked = json.isNull("locked") ? false : json.getBoolean("locked");
		settings.mUnlockMessage = json.isNull("unlock_message") ? null : json.getString("unlock_message");

		String showDate = json.isNull("show_date") ? null : json.getString("show_date");
		String hideDate = json.isNull("hide_date") ? null : json.getString("hide_date");
		if (showDate != null) settings.mShowDate = JsonUtil.parseJsonDate(showDate);
		if (hideDate != null) settings.mHideDate = JsonUtil.parseJsonDate(hideDate);

		return settings;
	}

	/* package */ static SnaprSetting getDefaultSettings(String slug) {
		return getSettings(slug, false, null, false, null, null);
	}
	
	public static SnaprSetting getSettings(String slug, boolean locked, String unlockMessage, boolean hidden, Date showDate, Date hideDate) {
		if (locked && unlockMessage == null) throw new IllegalArgumentException("A locked effect must provide an unlock message");
		SnaprSetting settings = new SnaprSetting(slug);
		settings.mHidden = hidden;
		settings.mLocked = locked;
		settings.mHideDate = hideDate;
		settings.mShowDate = showDate;
		settings.mUnlockMessage = unlockMessage;
		return settings;
	}
	
	public String getSlug() {
		return mSlug;
	}
	
	public boolean isVisible() {
		if (mLocked || !mHidden) return true;
		// consider permanently locked (until indicated otherwise by parent app) if there is no show date specified
		if (mShowDate == null) return false;
		Date now = Calendar.getInstance().getTime();

		// test if we're passed the show date
		boolean afterShowDate = mShowDate.before(now);
		// test if we're before the hide date - if no hide date is specified, then this is always the case
		boolean beforeHideDate = mHideDate != null ? mHideDate.after(now) : true;  
		return afterShowDate && beforeHideDate;
	}
	
	public boolean isLocked() {
		 return mLocked;
	}
	
	public String getUnlockMessage() {
		return mUnlockMessage;
	}
	
	public Date getShowDate() {
		return mShowDate;
	}
	
	public Date getHideDate() {
		return mHideDate;
	}
}