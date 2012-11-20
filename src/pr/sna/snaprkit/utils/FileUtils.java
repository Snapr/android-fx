package pr.sna.snaprkit.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import pr.sna.snaprkit.Global;

public class FileUtils
{
	public static String getSnaprCacheDirectory(Context context)
	{
		return getDirectory(context.getCacheDir().getAbsolutePath() + "/snapr");
	}
	
	public static String getDCIMCameraDirectory()
	{
		String returnDir = null;
		
		// Get the camera picture directory
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO)
		{
		     // Only for Android versons older than Froyo
			returnDir = Global.DIR_DCIM + "/Camera";
		}
		else
		{
			// Froyo and above
			returnDir = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/Camera";
		}
		
		// Return
		return returnDir;
	}
	
    public static String getLogsDirectory()
    {
    	return getDirectory(Global.DIR_LOGS);
    }
    
    public static String getLogFileName()
    {
    	return Global.LOG_NAME_PREFIX + android.text.format.DateFormat.format(Global.IMAGE_NAME_DATE_FORMAT, new java.util.Date()).toString() + ".txt";
    }
    
    // Gets a standard directory; creates it if it does not exist
    // If it cannot create it, it defaults to the root of the SD card
    public static String getDirectory(String directory)
    {  	
    	// Check if it exists
    	if (pr.sna.snaprkit.utils.FileUtils.isDirectoryPresent(directory))
    	{
    		return directory;
    	}
    	else
    	{
    		// Attempt to create the directory
    		Boolean isCreated = pr.sna.snaprkit.utils.FileUtils.createDirectory(directory);
    		
    		// Check result
    		if(isCreated)
    			// Successfully created, so we can use it
    			return directory;
    		else
    			// Failed to create it, so default to SD card root
    			return Environment.getExternalStorageDirectory().getAbsolutePath();
    	}
    }
	
	// Check if picture queue directory exists
    public static boolean isDirectoryPresent(String directory)
    {
    	// Declare
    	boolean isPresent = false;
    	File path = null;

    	// Check for path
    	try
    	{
    		path = new File(directory);
    		isPresent = path.exists();
    	}
    	catch (Exception e)
    	{
    		if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + " Failed to get directory state (exists or not) " + path.toString());
    	}
    	
    	return isPresent;
    }
    
    // Create directory
    public static boolean createDirectory(String directory)
    {
    	// Declare
    	File path = null;
    	boolean isSuccess = false;
    	
    	// Attempt to create directory
    	try
    	{
    		path = new File(directory);
    		isSuccess = path.mkdirs();
    	}
    	catch (Exception e)
    	{
    		if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + " Failed to create directory " + path.toString());
    	}
    	
    	return isSuccess;
    }
    
    public static boolean copyDirectory(String sourceDirectory, String destinationDirectory)
    {
    	try
		{
			org.apache.commons.io.FileUtils.copyDirectory(new File(sourceDirectory), new File(destinationDirectory));
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
    	catch (NullPointerException e)
		{
			e.printStackTrace();
			return false;
		}
    }
    
	public static boolean isFilePresent(String fileName)
	{
		// Declare
		boolean exists = false;
		
		// Check for file
		try
		{
			File f = new File(fileName);
			exists = f.exists();
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed with error " + e);
		}
		
		// return
		return exists;
	}
	
	public static boolean copyFile(String sourceFileName, String destinationFileName)
	{
		try
		{
			org.apache.commons.io.FileUtils.copyFile(new File(sourceFileName), new File(destinationFileName));
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public static String getStringFromFile(String fileName)
	{
        StringBuilder contents = new StringBuilder();
        String sep = System.getProperty("line.separator");
         
        try
        { 
        	BufferedReader input =  new BufferedReader(new FileReader(fileName), 1024*8);
        	try
        	{
        		String line = null; 
        		while (( line = input.readLine()) != null)
        		{
        			contents.append(line);
        			contents.append(sep);
        		}
        	}
        	finally
        	{
        		input.close();
        	}
        }
        catch (FileNotFoundException ex)
        {
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() +  ": Couldn't find the file " + fileName + ex.toString());
        	return null;
        }
        catch (IOException ex)
        {
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() +  ": Error reading file " + fileName + ex.toString());
            return null;
        }
        
        return contents.toString();
	}
	
	public static void saveStringToFile(String string, String fileName)
	{
		// Try to write the content
		try
		{
			// open myfilename.txt for writing
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(fileName));
			// write the contents on mySettings to the file
			out.write(string);
			// close the file
			out.close();
		}
		catch (Exception e)
		{
			//do something if an IOException occurs.
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() +  ": Error writing file " + fileName + e.toString());
		}
	}
	
	public static boolean setFileLastModifiedDate(String fileName, Date newDate)
	{
		// Declare
		boolean success = false;
		
		try
		{
			File file = new File(fileName);
			file.setLastModified(newDate.getTime());
			success = true;
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed with error " + e);
		}
		
		// Return
		return success;
	}
	
	public static long getDirectorySize(String directoryName)
    {
    	try
    	{
    		File directory = new File(directoryName);
    		return org.apache.commons.io.FileUtils .sizeOf(directory);
    	}
    	catch (Exception e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed with error " + e);
    	}
    	
    	return -1;
    }
	
	/**
	 * @param directoryName  The directory to prune 
	 * @return               Indicates whether the operation succeeded
	 */
	public static boolean cleanDirectory(String directoryName)
	{
		// Declare
		boolean success = false;
		
		try
		{
			// Open the directory
			File directory = new File(directoryName);
			org.apache.commons.io.FileUtils.cleanDirectory(directory);
			success = true;
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() +": Failed with error " + e);
		}
		
		// Return
		return success;
	}
	
	public static boolean removeDiskFile(String fileName)
	{
		if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
		
		// Delete the file from queue folder
		try
		{
			File file = new File(fileName);
			file.delete();
			if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod() + ": Successfully deleted " + fileName);
			return true;
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Could not remove the file " + fileName + " from disk ");
		}
		
		if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
		
		return false;
	}
	
	public static File openLogFile()
	{
		if (Global.LOG_DISK)
		{
			String logFileName = Global.DIR_LOGS + "/" + FileUtils.getLogFileName();
			File logFile = new File(logFileName);
			if (!logFile.exists())
			{
				try
				{
					logFile.createNewFile();
				} 
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			return logFile;
		}
		else
		{
			return null;
		}
	}
}
