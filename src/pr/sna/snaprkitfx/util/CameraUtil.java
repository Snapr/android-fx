package pr.sna.snaprkitfx.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

/**
 * @author JSA
 */
public class CameraUtil {
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * copy exif data
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private static TiffOutputSet getSanselanOutputSet(File jpegImageFile) 
			throws IOException, ImageReadException, ImageWriteException
	{
		TiffOutputSet outputSet = null;
		
		// note that metadata might be null if no metadata is found.
		IImageMetadata metadata = Sanselan.getMetadata(jpegImageFile);
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

		// if file does not contain any exif metadata, we create an empty
		// set of exif metadata. Otherwise, we keep all of the other
		// existing tags.
		if (null == outputSet)
			outputSet = new TiffOutputSet();
		
		// Return
		return outputSet;
	}
	
	/**
	 * Copies the EXIF data from the source file to the destination file using Sanselan
	 * 
	 * @param sourceFile - Source image file
	 * @param destFile   - Destination image file
	 */
	public static void copyExifData(File sourceFile, File destFile)
	{
		copyExifData(sourceFile, destFile, false, null);
	}
	
	/**
	 * Copies the EXIF data from the source file to the destination file using Sanselan
	 * 
	 * Copying the EXIF data directly from source to destination does not work in Sanselan -- 
	 * Sanselan copies the entire source image data, not just EXIF data, and erases any destination
	 * image changes (such as image rotation between source and destination). To get around that, 
	 * we read the source EXIF OutputSet and the dest file EXIF OutputSet. We loop through all EXIF 
	 * directories and fields in the source OutputSet, manually copying the data to the destination
	 * OutputSet. Then, we apply the destination OutputSet onto a new temp file. The temp file will 
	 * now have the correct image and the correct EXIF data as well. The final setp is to replace the 
	 * dest file with the temp file.
	 * 
	 * @param sourceFile - Source image file
	 * @param destFile   - Destination image file
	 * @param preserveExistingFields - Preserve fields which already exist in the destination
	 * @param excludedFields - List of fields to avoid copying from source to destination
	 */
	public static void copyExifData(File sourceFile, File destFile, boolean preserveExistingFields, List<TagInfo> excludedFields)
	{
		String tempFileName = destFile.getAbsolutePath() + ".tmp";
		File tempFile = null;
		OutputStream tempStream = null;
		
		try
		{
			tempFile = new File (tempFileName);
			
			TiffOutputSet sourceSet = getSanselanOutputSet(sourceFile);
			TiffOutputSet destSet = getSanselanOutputSet(destFile);
			
			destSet.getOrCreateExifDirectory();
			
			// Go through the source directories
			List<?> sourceDirectories = sourceSet.getDirectories(); 
			for (int i=0; i<sourceDirectories.size(); i++)
			{
				TiffOutputDirectory sourceDirectory = (TiffOutputDirectory)sourceDirectories.get(i);
				TiffOutputDirectory destinationDirectory = getOrCreateExifDirectory(destSet, sourceDirectory);
				
				if (destinationDirectory == null) continue; // failed to create
				
				// Loop the fields
				List<?> sourceFields = sourceDirectory.getFields();
				for (int j=0; j<sourceFields.size(); j++)
				{
					// Get the source field
					TiffOutputField sourceField = (TiffOutputField) sourceFields.get(j);
					
					// Check exclusion list
					if (excludedFields != null && excludedFields.contains(sourceField.tagInfo))
					{
						destinationDirectory.removeField(sourceField.tagInfo);
						continue;
					}
					
					// Check field preservation
					if (preserveExistingFields && (destinationDirectory.findField(sourceField.tagInfo) != null))
					{
						continue;
					}
					
					// Remove any existing field
					destinationDirectory.removeField(sourceField.tagInfo);
					
					// Add field 
					destinationDirectory.add(sourceField);
				}
			}
			
			// Save data to destination
			tempStream = new BufferedOutputStream(new FileOutputStream(tempFile));
			new ExifRewriter().updateExifMetadataLossy(destFile, tempStream, destSet);
			tempStream.close();
			
			// Replace file
			if (destFile.delete())
			{
				tempFile.renameTo(destFile);
			}
		}
		catch (ImageReadException exception)
		{
			exception.printStackTrace();
		}
		catch (ImageWriteException exception)
		{
			exception.printStackTrace();
		}
		catch (IOException exception)
		{
			exception.printStackTrace();
		}
		finally
		{
			if (tempStream != null)
			{
				try
				{
					tempStream.close();
				}
				catch (IOException e)
				{
				}
			}
			
			if (tempFile != null)
			{
				if (tempFile.exists()) tempFile.delete();
			}
		}
	}
	
	private static TiffOutputDirectory getOrCreateExifDirectory(TiffOutputSet outputSet, TiffOutputDirectory outputDirectory)
	{
		TiffOutputDirectory result = outputSet.findDirectory(outputDirectory.type);
		if (null != result)
			return result;
		result = new TiffOutputDirectory(outputDirectory.type);
		try {
			outputSet.addDirectory(result);
		}
		catch (ImageWriteException e)
		{
			return null;
		}
		return result;
	}
}
