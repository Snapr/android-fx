package pr.sna.snaprkit.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;

import android.media.ExifInterface;

/**
 * @author JSA
 */
public class CameraUtil {
	
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
