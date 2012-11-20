package pr.sna.snaprkit;

public class SharedPictureInfo
{	
	// ------------------------------------------------------------------------
	// Members
	// ------------------------------------------------------------------------
	
	private String mFileName;
	private double mLatitude;
	private double mLongitude;

	public String getFileName()
	{
		return mFileName;
	}
		
	public void setFileName(String fileName)
	{
		mFileName = fileName;
	}
	
	public double getLatitude()
	{
		return mLatitude;
	}
	
	public void setLatitude(double latitude)
	{
		this.mLatitude = latitude;
	}
	
	public double getLongitude()
	{
		return mLongitude;
	}
	
	public void setLongitude(double longitude)
	{
		this.mLongitude = longitude;
	}
}