package pr.sna.snaprkit.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.SanselanFixes;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.GPSTagConstants;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

import pr.sna.snaprkit.ExifData;
import pr.sna.snaprkit.Global;
import android.location.Location;
import android.media.ExifInterface;

public class CameraUtils
{
    public static String getPictureName()
    {
    	return Global.IMAGE_NAME_PREFIX + android.text.format.DateFormat.format(Global.IMAGE_NAME_DATE_FORMAT, new java.util.Date()).toString() + ".jpg";
    }
	
    public static String getDisabledProvidersString(boolean networkEnabled, boolean wifiEnabled, boolean gpsEnabled)
    {	
    	Vector<String> disabledProviders = new Vector<String>();
    	
    	if (!networkEnabled)
    	{
    		disabledProviders.add("network");
    	}
    	
    	if (!wifiEnabled)
    	{
    		disabledProviders.add("WiFi");
    	}
    	
    	if (!gpsEnabled)
    	{
    		disabledProviders.add("GPS");
    	}
    	
    	// Create string
    	if (disabledProviders.size() == 0)
    	{
    		return null;
    	}
    	else if (disabledProviders.size() == 1)
    	{
    		return disabledProviders.firstElement();
    	}
    	else
    	{
        	String returnString = "";
        	
        	// Concatenate all but the last element using ", "
        	for (int i=0; i<disabledProviders.size() - 1; i++)
        	{
        		returnString = returnString + disabledProviders.get(i) + ", ";
        	}
        	
        	// Remove last ", "
        	returnString = returnString.substring(0, returnString.length() -2);
        	
        	// Concatenate last element using " and "
        	returnString = returnString + " and " + disabledProviders.lastElement();
        	
        	// Return
        	return returnString;
    	}
    }
    
    public static ExifData getExifData(String fileName)
    {
    	// Due to various bugs in both the native and Sanselan EXIF
    	// libraries, we need to read using both libraries to make
    	// sure we always get the location information stored
    	// in the picture
    	
    	// Declare
    	ExifData exifDataAndroid;
    	ExifData exifDataSanselan;
    	
    	// Try to read the data using the Sanselan library first
    	exifDataSanselan = getExifDataSanselan(fileName);
    	
    	// Check results
    	if (exifDataSanselan == null || exifDataSanselan.getLocation() == null)
    	{
        	exifDataAndroid = getExifDataAndroid(fileName);
    		if (exifDataAndroid == null || exifDataAndroid.getLocation() == null)
    		{
    			// Prefer Sanselan data with multiple timestamps
    			return exifDataSanselan;
    		}
    		else
    		{
    			// Prefer Android data with location info
    			return exifDataAndroid;
    		}
    	}
    	else
    	{
    		return exifDataSanselan;
    	}
    }
        
    // 
    private static ExifData getExifDataSanselan(String fileName)
    {
    	// Declare
    	Location exifLocation = null;
    	Date originalDateTime = null;
    	Date modifyDateTime = null;
    	ExifData returnData = new ExifData();
    	
		IImageMetadata metadata = null;
		File file = null;
    	
    	// Create a file object
    	file = new File(fileName);
		
		try
		{
			metadata = Sanselan.getMetadata(file);
        }
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed with error " + e);
		}
 
        if (metadata instanceof JpegImageMetadata)
        {
        	JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
        	
        	// Parse EXIF modify date
        	modifyDateTime = ImageUtils.parseExifDateMultiFormat(jpegMetadata, TiffConstants.TIFF_TAG_DATE_TIME);
        	
        	// Save modify date
        	if (modifyDateTime != null)
        	{
        		returnData.setModifyDateTime(modifyDateTime);
        	}
        	
        	// Parse EXIF original date
        	originalDateTime = ImageUtils.parseExifDateMultiFormat(jpegMetadata, TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
        	
        	// Save original date
        	if (originalDateTime != null)
        	{
        		returnData.setOriginalDateTime(originalDateTime);
        	}
        	
        	// Simple interface to GPS data
        	TiffImageMetadata exifMetadata = jpegMetadata.getExif();
        	
        	if (exifMetadata != null)
        	{
        		try 
        		{        			
        			// Get the GPS data
        			TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
        			if (null != gpsInfo)
        			{
        				// Get the longitude and latitude
        				double longitude = gpsInfo.getLongitudeAsDegreesEast();
        				double latitude = gpsInfo.getLatitudeAsDegreesNorth();
        				
        				// Enter them in the return object
        				exifLocation = new Location("exif");
        				exifLocation.setLatitude(latitude);
        				exifLocation.setLongitude(longitude);
        				exifLocation.setAltitude(0);
        				
        				// Add it to the return data
        				returnData.setLocation(exifLocation);
        			}
        		}
        		catch (ImageReadException e)
        		{
        			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Could not read GPS data: " +  e.toString());
        		}
        	}
        }
        
        return returnData;
    }
    
    private static ExifData getExifDataAndroid(String fileName)
    {
    	// Declare
    	Location exifLocation = null;
    	Date originalDateTime = null;
    	ExifData returnData = new ExifData();
    	
    	try
		{
    		// Get interface
			ExifInterface exif = new ExifInterface(fileName);
			
			// Get latitude and longitude
			float[] latLong = new float[2];
			if (exif.getLatLong(latLong))
			{
				// Save latitude and longitude to Location
				exifLocation = new Location("exif");
				exifLocation.setLatitude(latLong[0]);
				exifLocation.setLongitude(latLong[1]);
				exifLocation.setAltitude(0);
			}
			
			// Set location
			returnData.setLocation(exifLocation);
			
			// Get the date and time
			originalDateTime = ImageUtils.parseExifDateMultiFormat(exif, ExifInterface.TAG_DATETIME);
			
			// Set the date and time
			if (originalDateTime != null)
			{
				returnData.setOriginalDateTime(originalDateTime);
			}
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + "Failed with error " + e);
		}
    	
    	// Return
    	return returnData;
    }
    
    /**
	 * Tag the picture with the GPS information
	 * @param fileName Picture filename
	 * @param location GPS location
	 * @return Returns a boolean indicating whether it succeeded
	 * Based on Sanselan usage example; uses openstreetmap Sanselan fixes
	 */
	public static boolean geotagPicture(String fileName, Location location)
    {
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
		
		// Exit immediately on incorrect parameters
		if (fileName == null || location == null)
		{
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
			return false;
		}
		
		try
		{
			File jpegSrcFile = new File(fileName);
			OutputStream os = null;
			
	        try {
	            TiffOutputSet outputSet = null;
	            
	            // note that metadata might be null if no metadata is found.
	            IImageMetadata metadata = Sanselan.getMetadata(jpegSrcFile);
	            JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
	            if (null != jpegMetadata)
	            {
	            	// note that exif might be null if no Exif metadata is found.
	                TiffImageMetadata exif = jpegMetadata.getExif();
	
	                if (null != exif)
	                {
	                	// TiffImageMetadata class is immutable (read-only).
	                	// TiffOutputSet class represents the Exif data to write.
	                	//
	                	// Usually, we want to update existing Exif metadata by
	                	// changing
	                	// the values of a few fields, or adding a field.
	                	// In these cases, it is easiest to use getOutputSet() to
	                	// start with a "copy" of the fields read from the image.
	                    outputSet = exif.getOutputSet();
	                }
	            }
	
	            if (null == outputSet)
	            {
	                outputSet = new TiffOutputSet();
	            }
	            
	            // Format date
	            long timestamp = location.getTime();
	            Date time = new Date(timestamp);
	            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy:MM:dd");
				sdfDate.setTimeZone(TimeZone.getTimeZone("UTC"));
				String dateStampString = sdfDate.format(time);
	
				// Get the time hours minutes and seconds
	            final int hour =   time.getHours();
	            final int minute = time.getMinutes();
	            final int second = time.getSeconds();
	
	            /*
	            // Get the altitude and altitude reference
	            double altitude = location.getAltitude();
	            Byte altitudeRef = (byte)((altitude >= 0)? 0:1);
	            
	            // Get provider
	            String provider = location.getProvider();
	            
	            // Get accuracy
	            double accuracy = location.getAccuracy();
	            */
	            
	            // Set the latitude and longitude
				double latitude = location.getLatitude();
				double longitude = location.getLongitude();
	            
	            // Open the GPS directory
	            TiffOutputDirectory exifDirectory = outputSet.getOrCreateGPSDirectory();
	            
	            // Add time field as array of doubles
	            Double[] timeStamp = {Double.valueOf(hour), Double.valueOf(minute), Double.valueOf(second)};
	            TiffOutputField gpsTimeStamp = TiffOutputField.create(
	                    GPSTagConstants.GPS_TAG_GPS_TIME_STAMP,
	                    outputSet.byteOrder, timeStamp);
	            exifDirectory.removeField(GPSTagConstants.GPS_TAG_GPS_TIME_STAMP);
	            exifDirectory.add(gpsTimeStamp);
	
	            // Add date field as string
	            TiffOutputField gpsDateStamp = SanselanFixes.create(
	                    GPSTagConstants.GPS_TAG_GPS_DATE_STAMP,
	                    outputSet.byteOrder, dateStampString);
	            exifDirectory.removeField(GPSTagConstants.GPS_TAG_GPS_DATE_STAMP);
	            exifDirectory.add(gpsDateStamp);
	
	            /*
	            // Add altitude
	            TiffOutputField gpsAltitude = TiffOutputField.create(
	                    GPSTagConstants.GPS_TAG_GPS_ALTITUDE,
	                   outputSet.byteOrder, altitude);
	            exifDirectory.removeField(GPSTagConstants.GPS_TAG_GPS_ALTITUDE);
	            exifDirectory.add(gpsAltitude);
	            
	            // Add altitude ref as byte
	            TiffOutputField gpsAltitudeRef = TiffOutputField.create(
	                    GPSTagConstants.GPS_TAG_GPS_ALTITUDE_REF,
	                   outputSet.byteOrder, altitudeRef);
	            exifDirectory.removeField(GPSTagConstants.GPS_TAG_GPS_ALTITUDE_REF);
	            exifDirectory.add(gpsAltitudeRef);
	            
	            // Add provider as string
	            TiffOutputField gpsProcessingMethod = SanselanFixes.create(
	                    GPSTagConstants.GPS_TAG_GPS_PROCESSING_METHOD,
	                    outputSet.byteOrder, provider);
	            exifDirectory.removeField(GPSTagConstants.GPS_TAG_GPS_PROCESSING_METHOD);
	            exifDirectory.add(gpsProcessingMethod);
	            
	            // Add accuracy
	            TiffOutputField gpsAccuracy = TiffOutputField.create(
	                    GPSTagConstants.GPS_TAG_GPS_DOP,
	                   outputSet.byteOrder, accuracy);
	            exifDirectory.removeField(GPSTagConstants.GPS_TAG_GPS_DOP);
	            exifDirectory.add(gpsAccuracy);
	            */
	            
	            // Add GPS latitude and longitude
	            SanselanFixes.setGPSInDegrees(outputSet, longitude, latitude);
	
	            // Create output temporary file
	            File jpegDestFile = new File(fileName + ".tmp");
	            os = new FileOutputStream(jpegDestFile);
	            os = new BufferedOutputStream(os);
	
	            // Update metadata
	            new ExifRewriter().updateExifMetadataLossless(jpegDestFile, os, outputSet);
	
	            // Rename temporary output to source filename
	            if (jpegSrcFile.delete())
	            {
	            	jpegDestFile.renameTo(jpegSrcFile);
	            }
	            
	            // Close
	            os.close();
	            os = null;
	            
	            // Return
	            if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
	            return true;
	        }
	        finally
	        {
	            if (os != null)
	            {
	                try
	                {
	                    os.close();
	                }
	                catch (IOException e)
	                {
	                }
	            }
	        }
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Failed due to error " + e.toString());
		}
		
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
		
		return false;
    }
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * copy exif data
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/** 
	 * Copy the exif data from the given source file to the given destination file. 
	 * Return whether or not the copy was successful.
	 */
	public static boolean copyExifData(File src, File destination) {
		if (src == null || destination == null) throw new IllegalArgumentException();
		if (!src.exists() || !destination.exists()) throw new IllegalArgumentException();
		Object metadata = null;
		
		try {
			metadata = Sanselan.getMetadata(src);
			if (metadata == null) return false;
			if (!(metadata instanceof JpegImageMetadata)) return false;
		} catch (Exception e) {
			return false;
		}
		
		JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
		List<TiffField> fields = null;
		
		try {
			fields = new ArrayList<TiffField>();
			List<?> rawFields = jpegMetadata.getExif().getAllFields();
			if (rawFields == null) return false;
			for (Object rawField : rawFields) if (rawField instanceof TiffField) fields.add((TiffField) rawField);
			if (fields.size() == 0) return false;
//			ArrayList items = jpegMetadata.getExif().getItems();
		} catch (Exception e) {
			return false;
		}
		
		ExifInterface destinationInterface = null;
		
		try {
			destinationInterface = new ExifInterface(destination.getAbsolutePath());
			for (TiffField field : fields) {
				Object value = field.getValue();
				if (value != null) destinationInterface.setAttribute(field.getTagName(), value.toString());
			}
			
			destinationInterface.saveAttributes();
		} catch (ImageReadException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}
}
