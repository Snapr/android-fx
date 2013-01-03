package pr.sna.snaprkitfx;

import java.io.File;
import java.io.InputStream;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.os.Environment;

/**
 * Some useful methods for dealing with taking photos
 * @author julius
 *
 */
public class SnaprPhotoHelper {

	public static final String APPLICATION_NAME = "Snapr";
	public static final String TEMP_DIRECTORY = "Temp";

	// ASSET DIMENSIONS
	public static final int EFFECT_MIN_SYSTEM_MEMORY = 0;
	public static final int EFFECT_IMAGE_WIDTH = 800;
	public static final int EFFECT_IMAGE_HEIGHT = 800;
	
	// FILE SUFFIXES
	public static final String MEDIA_FILE_SUFFIX_JPG = ".jpg";

	// PLAYER CONTENT TYPES
	public static final String CONTENT_TYPE_JPG = "image/jpeg";

	// ---------------------------------
	public static String getAppFilePath() {
		StringBuilder sb = new StringBuilder();
		sb.append(Environment.getExternalStorageDirectory());
		sb.append(File.separator);
		sb.append(APPLICATION_NAME);
		return sb.toString();
	}
	// ---------------------------------
	public static String getAppTempFilePath() {
		StringBuilder sb = new StringBuilder();
		sb.append(Environment.getExternalStorageDirectory());
		sb.append(File.separator);
		sb.append(APPLICATION_NAME);
		sb.append(File.separator);
		sb.append(TEMP_DIRECTORY);
		sb.append(File.separator);
		return sb.toString();
	}
	// ---------------------------------

	public static Bitmap getBitmapFromAssets(Context context, int resId) {
		InputStream bitmapInputStream = context.getResources().openRawResource(resId);
		return BitmapFactory.decodeStream(bitmapInputStream);
	}
	
	public static Bitmap getBitmapFromAssets(Context context, int resId, int assetWidth, int assetHeight, int maxWidth, int maxHeight) {
		
		// return the image unscaled if the image width and height are less than requested
		if (assetWidth <= maxWidth && assetHeight <= maxHeight) return getBitmapFromAssets(context, resId);
		
		// calculate the desired scale (using powers of two to result in a faster, more accurate subsampling)
		double scaleValue = Math.max(maxWidth / (double) assetWidth, maxHeight / (double) assetHeight);
		int scale = (int) Math.pow(2, (int) Math.round(Math.log(scaleValue) / Math.log(0.5)));
		
		// ensure the scale is at least two to ensure the returned value is smaller than the requested size  
		InputStream bitmapInputStream = context.getResources().openRawResource(resId);
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = Math.max(2, scale);
		return BitmapFactory.decodeStream(bitmapInputStream, null, options);
	}	
	
	// ---------------------------------

	public static Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth, boolean recycle) {
		Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, newWidth, newHeight, false);
		if (recycle && bm != resizedBitmap) bm.recycle();
		if (resizedBitmap.getConfig() != Config.ARGB_8888) return convertBitmapToARGB_8888(resizedBitmap, true);
		else return resizedBitmap;
	}
	
	// ---------------------------------

	public static Bitmap convertBitmapToARGB_8888(Bitmap bm, boolean recycle) {
		Bitmap result = bm.copy(Bitmap.Config.ARGB_8888, true);
		if (!recycle) return result;
		bm.recycle();
		return result;
	}
	// ---------------------------------
	public static Bitmap cropBitmap(Bitmap bitmapToCrop, boolean recycle) {
		
		Bitmap b;
		
		if(bitmapToCrop.getWidth() > bitmapToCrop.getHeight()) {
			// Landscape
			b = Bitmap.createBitmap(
					bitmapToCrop, 
					bitmapToCrop.getWidth()/2 - bitmapToCrop.getHeight()/2,
					0,
					bitmapToCrop.getHeight(), 
					bitmapToCrop.getHeight()
					);
		} else {
			b = Bitmap.createBitmap(
					bitmapToCrop,
					0, 
					bitmapToCrop.getHeight()/2 - bitmapToCrop.getWidth()/2,
					bitmapToCrop.getWidth(),
					bitmapToCrop.getWidth() 
					);
		}

		if (recycle) bitmapToCrop.recycle();
		
		// Convert to ARGB_8888
		return SnaprPhotoHelper.convertBitmapToARGB_8888(b, recycle);
	}
	// ---------------------------------
	
	/**
	 * Get the maximum width an image is permitted to be when applying effects in order to stay within memory constraints.
	 * Return -1 if the device lacks the memory to perform the function.
	 */
	public static int getMaxImageWidth(Context context) {
		int memory = getSystemMemory(context);
		if (memory < EFFECT_MIN_SYSTEM_MEMORY) return -1;
		if (memory <= 16) return 300;
		return EFFECT_IMAGE_WIDTH;
	}

	public static int getSystemMemory(Context context) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		return manager.getMemoryClass();
	}
}
