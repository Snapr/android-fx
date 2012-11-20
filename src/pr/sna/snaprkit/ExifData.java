package pr.sna.snaprkit;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.location.Location;


public class ExifData
{
	// ------------------------------------------------------------------------
	// Members
	// ------------------------------------------------------------------------
	
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z";
	
	private Location mLocation = null;
	private Date mOriginalDateTime = null;
	private Date mModifyDateTime = null;

	// ------------------------------------------------------------------------
	// Constructors and Destructor
	// ------------------------------------------------------------------------
    
    public ExifData()
    {
    }
	
	// ------------------------------------------------------------------------
	// Getters / Setters
	// ------------------------------------------------------------------------
    
    public Location getLocation()
    {
		return mLocation;
	}

	public void setLocation(Location location)
	{
		mLocation = location;
	}

	public Date getOriginalDateTime()
	{
		return mOriginalDateTime;
	}

	public String getOriginalDateTimeString()
	{
		if (mOriginalDateTime == null)
		{
			return "";
		}
		else
		{
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
			return sdf.format(mOriginalDateTime);
		}
	}
	
	public void setOriginalDateTime(Date dateTime)
	{
		mOriginalDateTime = dateTime;
	}
	
	public Date getModifyDateTime()
	{
		return mOriginalDateTime;
	}
	
	public String getModifyDateTimeString()
	{
		if (mModifyDateTime == null)
		{
			return "";
		}
		else
		{
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
			return sdf.format(mModifyDateTime);
		}
	}

	public void setModifyDateTime(Date dateTime)
	{
		mOriginalDateTime = dateTime;
	}
}