package nz.co.juliusspencer.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

public class JSAImageUtil {

	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * get load image scale (max width, height)
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/** {@see getLoadImageScale(int, int, int, int)} */
	public static int getLoadImageScale(String path, int maxWidth, int maxHeight) {
		if (path == null) throw new IllegalArgumentException();
		return getLoadImageScale(new File(path), maxWidth, maxHeight);
	}
	
	/** {@see getLoadImageScale(int, int, int, int)} */
	public static int getLoadImageScale(File file, int maxWidth, int maxHeight) {
		if (file == null) throw new IllegalArgumentException();
		JSATuple<Integer, Integer> dimensions = getBitmapImageDimensions(file);
		return getLoadImageScale(dimensions.getA(), dimensions.getB(), maxWidth, maxHeight);
	}
	

	/** 
	 * Get the scale to apply to the image when loading a file into memory to be displayed with the given maximum width and height.
	 * If the image is smaller than the given width and height, the image will be returned unscaled.
	 * If the image is larger than the given width and/or height, the image will be scaled down in powers of two until the image is within the given bounds.
	 * 
	 * This method is useful to ensure the image does not exceed the virtual machine bitmap cache size (for example, displaying a camera image on screen).
	 */
	public static int getLoadImageScale(int imageWidth, int imageHeight, int maxWidth, int maxHeight) {
		if (maxWidth <= 0 || maxHeight <= 0) throw new InvalidParameterException("maxWidth and maxHeight must be positive");

		// return the image unscaled if the image width and height are less than requested
		if (imageWidth <= maxWidth && imageHeight <= maxHeight) return 1;
		
		// calculate the desired scale (using powers of two to result in a faster, more accurate subsampling)
		double scaleValue = Math.min(maxWidth / (double) imageWidth, maxHeight / (double) imageHeight);
		int scale = (int) Math.pow(2, (int) Math.ceil(Math.log(scaleValue) / Math.log(0.5)));
		
		// ensure the scale is at least two to ensure the returned value is smaller than the requested size  
		return Math.max(2, scale);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * get load larger image scale (min width, height)
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/** {@see getLoadLargerImageScale(File, int, int)} */
	public static int getLoadLargerImageScale(String path, int minWidth, int minHeight) {
		if (path == null) throw new IllegalArgumentException();
		return getLoadLargerImageScale(new File(path), minWidth, minHeight);
	}
	
	/** {@see getLoadLargerImageScale(File, int, int)} */
	public static int getLoadLargerImageScale(File file, int minWidth, int minHeight) {
		if (file == null) throw new IllegalArgumentException();
		JSATuple<Integer, Integer> dimensions = getBitmapImageDimensions(file);
		return getLoadLargerImageScale(dimensions.getA(), dimensions.getB(), minWidth, minHeight);
	}
	
	/** 
	 * Get the scale to apply to the image when loading a file into memory to be displayed slightly larger than (within double of) the given minimum width and 
	 * height.
	 * 
	 * If the image is smaller than the given width and height, the image will be returned unscaled.
	 * If the image is larger than the given width and height, the image will be scaled down in powers of two until the largest value where both image width and 
	 * height are greater than the given bounds.
	 * 
	 * This method is useful when displaying larger images in a thumbnail where the image is requested to be displayed cropped. Unlike the {@code loadImageFile}
	 * method, this method will ensure the resulting image does not require upscaling to fit the requested bounds.
	 */
	public static int getLoadLargerImageScale(int imageWidth, int imageHeight, int minWidth, int minHeight) {
		if (minWidth <= 0 || minHeight <= 0) throw new InvalidParameterException("minWidth and minHeight must be positive");
		
		// return the image unscaled if the image width and height are less than requested
		if (imageWidth <= minWidth && imageHeight <= minHeight) return 1;
		
		// calculate and return the image at the requested scale (using powers of two to result in a faster, more accurate subsampling)
		double scaleValue = Math.max(minWidth / (double) imageWidth, minHeight / (double) imageHeight);
		return (int) Math.pow(2, (int) Math.floor(Math.log(scaleValue) / Math.log(0.5)));
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * get bitmap image dimensions
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/** Get the dimensions of an image saved as a file using the bitmap bounds. Return null if no information is available. */
	public static JSATuple<Integer, Integer> getBitmapImageDimensions(File file) {
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(file);
			return getBitmapImageDimensions(stream);
		} catch (FileNotFoundException e) {
			return null;
		} finally {
			try { if (stream != null) stream.close();
			} catch (IOException exception) { }
		}
	}
	
	/** Get the dimensions of an image saved as a file using the bitmap bounds. Return null if no information is available. */
	public static JSATuple<Integer, Integer> getBitmapImageDimensions(InputStream stream) {
		if (stream == null) throw new IllegalArgumentException();
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(stream, null, options);
		return new JSATuple<Integer, Integer>(options.outWidth, options.outHeight);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * load image file (options)
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	/** Load the image file at the given location into a bitmap using the options supplied. Return null in the event of an exception. */
	public static Bitmap loadImageFile(File file, Options options) {
		try {
			return loadImageFileWithException(file, options);
		} catch (IOException exception) {
			return null;
		}
	}
	
	/** Load the image file at the given location into a bitmap using the options supplied. */
	public static Bitmap loadImageFileWithException(File file, Options options) throws IOException {
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(file);
			return BitmapFactory.decodeStream(stream, null, options);
		} finally {
			try {
				if (stream != null) stream.close();
			} catch (IOException exception) { }
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * load image stream (options)
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/** Get the dimensions of an image saved as a file using the bitmap bounds. Return null if no information is available. */
	public static Bitmap loadImageStream(InputStream stream, Options options) {
		if (stream == null) throw new IllegalArgumentException();
		return BitmapFactory.decodeStream(stream, null, options);
	}

}
