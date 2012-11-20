package pr.sna.snaprkit.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.TagInfo;

import pr.sna.snaprkit.Global;

import android.media.ExifInterface;

public class ImageUtils
{
	// ------------------------------------------------------------------------
	// Picture store functions
	// ------------------------------------------------------------------------
		
	// Gets the last image id from the file system
	// Used on low end devices that fail to insert the camera picture 
	// in the MediaStore
	public static String getLastImagePath()
	{
		// Declare
		String imageFolder = FileUtils.getDCIMCameraDirectory();
		
		// Loop through files and find the last modified file
		return getLastModifiedFile(imageFolder);
	}
	
	public static String getLastModifiedFile(String directory)
	{
		File dir = new File(directory);

		File[] files = dir.listFiles();
		if (files.length == 0)
		{
		    return null;
		}

		File lastModifiedFile = files[0];
		for (int i = 1; i < files.length; i++)
		{
		   if (lastModifiedFile.lastModified() < files[i].lastModified())
		   {
		       lastModifiedFile = files[i];
		   }
		}
		
		// Return
		return lastModifiedFile.getAbsolutePath();
	}
	
	// ------------------------------------------------------------------------
	// Thumbnail functions
	// ------------------------------------------------------------------------
	
	public static String getThumbFileName(String fileName)
	{
		File file = new File(fileName);
		return file.getAbsolutePath() + "/thumb_" + file.getName();
	}
	
	// ------------------------------------------------------------------------
	// EXIF Date Parsing for Sanselan
	// ------------------------------------------------------------------------
	
	public static Date parseExifDate(JpegImageMetadata jpegMetadata, TagInfo fieldId, String fieldFormat)
    {
    	TiffField field;
    	String fieldString;
    	Date returnDateTime = null;
    	
		// Get modify date time
		try
		{
	    	field = jpegMetadata.findEXIFValue(fieldId);
			fieldString = field.getStringValue(); // EXIF stores dates as string with no timezone, so assume current timezone
			SimpleDateFormat sdf = new SimpleDateFormat(fieldFormat);
			returnDateTime = sdf.parse(fieldString);
		}
		catch (Exception e)
		{
			// Log
			if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + " Failed to parse date with taginfo " + fieldId + " and format " + fieldFormat);
			if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + " Got error " + e);
		}
		
		return returnDateTime;
    }
    
    public static Date parseExifDateMultiFormat(JpegImageMetadata jpegMetadata, TagInfo fieldId)
    {
    	// Declare
    	Date returnDate = null;
    	
    	// Get date time - use regular format
    	returnDate = parseExifDate(jpegMetadata, fieldId, "yyyy:MM:dd HH:mm:ss");
    	
    	// Get date time - use HTC Sensation erroneous format
    	if (returnDate == null)
    	{
    		returnDate = parseExifDate(jpegMetadata, fieldId, "yyyy/MM/dd HH:mm:ss");
    	}
    	
    	// Return
    	return returnDate;
    }
	
	// ------------------------------------------------------------------------
	// EXIF Date Parsing for ExifInterface
	// ------------------------------------------------------------------------
	
	public static Date parseExifDate(ExifInterface exif, String fieldId, String fieldFormat)
    {
    	Date returnDateTime = null;
    	
		// Get modify date time
		try
		{
			String dateTimeString = exif.getAttribute(fieldId);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
			returnDateTime = sdf.parse(dateTimeString);
		}
		catch (Exception e)
		{
			// Log
			if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + " Failed to parse date with taginfo " + fieldId + " and format " + fieldFormat);
			if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + " Got error " + e);
		}
		
		return returnDateTime;
    }
    
    public static Date parseExifDateMultiFormat(ExifInterface exif, String fieldId)
    {
    	// Declare
    	Date returnDate = null;
    	
    	// Get date time - use regular format
    	returnDate = parseExifDate(exif, fieldId, "yyyy:MM:dd HH:mm:ss");
    	
    	// Get date time - use HTC Sensation erroneous format
    	if (returnDate == null)
    	{
    		returnDate = parseExifDate(exif, fieldId, "yyyy/MM/dd HH:mm:ss");
    	}
    	
    	// Return
    	return returnDate;
    }
}
