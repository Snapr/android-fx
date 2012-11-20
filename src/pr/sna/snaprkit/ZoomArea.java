package pr.sna.snaprkit;

public class ZoomArea
{	
	// ------------------------------------------------------------------------
	// Members
	// ------------------------------------------------------------------------
	
	// Zoom data
	private double mMinLatitude;
	private double mMinLongitude;
	private double mMaxLatitude;
	private double mMaxLongitude;
    
	// ------------------------------------------------------------------------
	// Constructor
	// ------------------------------------------------------------------------
    
	public ZoomArea()
	{
		
	}
	
    public ZoomArea(double minLatitude, double minLongitude, double maxLatitude, double maxLongitude)
    {
    	mMinLatitude = minLatitude;
    	mMinLongitude = minLongitude;
    	mMaxLatitude = maxLatitude;
    	mMaxLongitude = maxLongitude;
    }

	public double getMinLatitude()
	{
		return mMinLatitude;
	}

	public void setMinLatitude(double minLatitude)
	{
		this.mMinLatitude = minLatitude;
	}

	public double getMinLongitude()
	{
		return mMinLongitude;
	}

	public void setMinLongitude(double minLongitude)
	{
		this.mMinLongitude = minLongitude;
	}

	public double getMaxLatitude()
	{
		return mMaxLatitude;
	}

	public void setMaxLatitude(double maxLatitude)
	{
		this.mMaxLatitude = maxLatitude;
	}

	public double getMaxLongitude()
	{
		return mMaxLongitude;
	}

	public void setMaxLongitude(double maxLongitude)
	{
		this.mMaxLongitude = maxLongitude;
	}
	
	public double getLatitudeSpan()
	{
		return mMaxLatitude - mMinLatitude;
	}
	
	public double getLongitudeSpan()
	{
		return (mMaxLongitude - mMinLongitude);
	}
	
	public int getLatitudeSpanE6()
	{
		return (int)((mMaxLatitude - mMinLatitude)*1E6);
	}
	
	public int getLongitudeSpanE6()
	{
		return (int)((mMaxLongitude - mMinLongitude) * 1E6);
	}
	
	// ------------------------------------------------------------------------
	// Getters / Setters
	// ------------------------------------------------------------------------
    
    
}