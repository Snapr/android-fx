package pr.sna.snaprkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nz.co.juliusspencer.android.JSAFileUtil;
import nz.co.juliusspencer.android.JSAImageUtil;
import nz.co.juliusspencer.android.JSAMathUtil;
import nz.co.juliusspencer.android.JSATuple;
import pr.sna.snaprkit.tabletop.TabletopSurfaceView;
import pr.sna.snaprkit.util.CameraUtil;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.FloatMath;
import android.view.WindowManager;

public class SnaprImageEditFragmentUtil {
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constants
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static final File TEMP_IMAGE_DIR = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + ".snapr", "images");
	public static final String ORIGINAL_IMAGE_FILENAME = "original.jpg";
	public static final String ORIGINAL_STICKERS_IMAGE_FILENAME = "original_stickers.jpg";
	
	public static final Bitmap.Config PREFERRED_BITMAP_CONFIG = Bitmap.Config.ARGB_8888;
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constants: memory
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static final int MAX_CONCURRENT_IMAGES_IN_MEMORY = 5; 	// the maximum number of raw images allows within the allocated memory
	public static final float IMAGE_MEMORY_PERCENTAGE = 60f; 		// the percentage of total memory raw images can occupy
	public static final float STICKER_MEMORY_PERCENTAGE = 20f; 		// the percentage of total memory raw sticker images can occupy
	public static final int MAX_FINAL_IMAGE_WIDTH = 900; 			// the maximum width of the final rendered image
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * save edited bitmap to file async task
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static class SaveEditedBitmapToFileAsyncTask extends AsyncTask<Void, Void, JSATuple<File, Boolean>> {
		private final Context mContext;
		private final String mOriginalFilePath;
		private final String mSaveFilePath;
		private final SnaprEffect mEffect;
		private final TabletopSurfaceView mTabletop;
		private final int mTabletopWidth;
		private final int mTabletopHeight;
		
		public SaveEditedBitmapToFileAsyncTask(Context context, String originalFilePath, String saveFilenPath, SnaprEffect effect, TabletopSurfaceView tabletop) {
			if (context == null || saveFilenPath == null || tabletop == null) throw new IllegalArgumentException();
			mContext = context;
			mOriginalFilePath = originalFilePath;
			mSaveFilePath = saveFilenPath;
			mEffect = effect;
			mTabletop = tabletop;
			mTabletopWidth = tabletop.getWidth();
			mTabletopHeight = tabletop.getHeight();
		}
		
		@SuppressLint("NewApi") @Override protected JSATuple<File, Boolean> doInBackground(Void... params) {
			File file = new File(mSaveFilePath);

			try {

				// ensure the base output directory exists
				File baseDir = file.getParentFile();
				baseDir.mkdirs();

				// calculate the maximum number of bytes the bitmap can be
				ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
				long deviceMemoryBytes = am.getMemoryClass() * JSAFileUtil.BYTES_PER_MB;
				
				// we allow a small number of concurrent images to fit within memory
				long maxImageBytes = (long) (deviceMemoryBytes * IMAGE_MEMORY_PERCENTAGE / 100f) / MAX_CONCURRENT_IMAGES_IN_MEMORY;
				
				// calculate the maximum width a square image can be to fit within memory constraints
				float rawMaxMemoryWidth = FloatMath.sqrt(maxImageBytes / 4); // approximate
				
				// round the width down to the nearest hundred pixels (to give a nice even number of the final output)
				int maxMemoryWidth = (int) (FloatMath.floor(rawMaxMemoryWidth / 100f)) * 100;
				
				// retrieve the dimensions of the original file
				JSATuple<Integer, Integer> dimens = JSAImageUtil.getBitmapImageDimensions(new File(mOriginalFilePath));
				
				// ensure the maximum width doesn't exceed the size of the original file
				int maxWidth = JSAMathUtil.min(dimens.getA(), dimens.getB(), maxMemoryWidth, MAX_FINAL_IMAGE_WIDTH);
				
				// construct the options to load the bitmap into memory (slightly larger than required)
				Options opts = new Options();
				opts.inSampleSize = JSAImageUtil.getLoadImageScale(new File(mOriginalFilePath), maxWidth, maxWidth, false);
				opts.inDither = true;
				opts.inPreferredConfig = PREFERRED_BITMAP_CONFIG;
				if (Build.VERSION.SDK_INT >= 11) opts.inMutable = true;
				
				// load the bitmap into memory
				Bitmap bitmap = JSAImageUtil.loadImageFile(new File(mOriginalFilePath), opts);
				
				// crop the bitmap to square
				bitmap = SnaprPhotoHelper.cropBitmap(bitmap, true);

				// draw the stickers on the bitmap
				bitmap = mTabletop.drawOnBitmap(bitmap, true, mTabletopWidth, mTabletopHeight);
				
				// apply the effect to the bitmap (if required)
				if (mEffect != null) mEffect.getFilter().apply(mContext.getApplicationContext(), bitmap);
				
				// save the bitmap to file
				FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
				bitmap.compress(CompressFormat.JPEG, 100, fos);

				// tell the media scanner about the new file so that it is immediately available to the user.
				MediaScannerConnection.scanFile(SnaprKitApplication.getInstance(), new String[] { file.toString() }, null, null);
				
			} catch (Exception exception) {
				exception.printStackTrace();
				return new JSATuple<File, Boolean>(file, false);
			}

			try {
				// copy the exif data from the source to the destination
				if (!mOriginalFilePath.equals(mSaveFilePath)) CameraUtil.copyExifData(new File(mOriginalFilePath), new File(mSaveFilePath));
			} catch (Exception exception) {
				exception.printStackTrace();
			}

			
			return new JSATuple<File, Boolean>(file, true);
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * save temp image
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static boolean saveTempImage(Bitmap bitmap) {
		return saveTempImage(bitmap, ORIGINAL_IMAGE_FILENAME);
	}
	
	public static boolean saveTempImage(Bitmap bitmap, String filename) {
		File file = new File(TEMP_IMAGE_DIR, filename);
		return saveImage(bitmap, file);
	}
	
	public static boolean saveImage(Bitmap bitmap, File file) {
		if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) return false; // make sure the parent directory exists
		FileOutputStream fos;

		try {
			fos = new FileOutputStream(file.getAbsolutePath());
			bitmap.compress(CompressFormat.JPEG, 100, fos);
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * load temp image
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static Bitmap loadTempImage() {
		return loadTempImage(ORIGINAL_IMAGE_FILENAME);
	}
	
	public static Bitmap loadTempImage(String filename) {
		File file = new File(TEMP_IMAGE_DIR, filename);
		return loadImage(file);
	}
	
	@SuppressLint("NewApi") public static Bitmap loadImage(File file) {
		FileInputStream fis;
		
		try {
			fis = new FileInputStream(file.getAbsolutePath());
			Options opts = new Options();
			opts.inPreferredConfig = PREFERRED_BITMAP_CONFIG;
			if (Build.VERSION.SDK_INT >= 11) opts.inMutable = true;
			return BitmapFactory.decodeStream(fis, null, opts);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * prepare original temp image
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	/** Save the given original image by resizing and saving out to an expected (temporary) location. */
	@SuppressLint("NewApi") public static Bitmap saveOriginalTempImage(Context context, String originalFilePath, long imageRequestTimestamp) {
		@SuppressWarnings("deprecation") int length = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
		
		Options opts = new Options();
		opts.inSampleSize = JSAImageUtil.getLoadLargerImageScale(new File(originalFilePath), length, length, false);
		opts.inDither = true;
		opts.inPreferredConfig = PREFERRED_BITMAP_CONFIG;
		if (Build.VERSION.SDK_INT >= 11) opts.inMutable = true;
		
		// load a larger version of the bitmap into memory
		Bitmap bitmap = JSAImageUtil.loadImageFile(new File(originalFilePath), opts); 

		// return if the bitmap could not be loaded
		if (bitmap == null) return null;
		
		// calculate the maximum number of bytes the bitmap can be
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		long deviceMemoryBytes = am.getMemoryClass() * JSAFileUtil.BYTES_PER_MB;
		
		// we allow a small number of concurrent images to fit within memory
		long maxImageBytes = (long) (deviceMemoryBytes * IMAGE_MEMORY_PERCENTAGE / 100f) / MAX_CONCURRENT_IMAGES_IN_MEMORY;
		long rawBytes = bitmap.getWidth() * bitmap.getHeight() * 4; // approximate
		float memoryScale = Math.min(FloatMath.sqrt(maxImageBytes / (float) rawBytes), 1f);
		
		// calculate the scale the bitmap should be to fit the device width perfectly (maintaining minimum width and height larger than device width)
		float deviceScale = Math.min(length / (float) Math.min(bitmap.getWidth(), bitmap.getHeight()), 1f);
		
		// set the scale to be the smaller of the two restrictions
		float scale = Math.min(deviceScale, memoryScale);
		
		// scale down the bitmap if necessary to fit within the memory restriction
		Bitmap temp = scale < 1f ? Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * scale), (int) (bitmap.getHeight() * scale), true) : bitmap;
		
		// return if the bitmap was not scaled
		if (temp == null) return null;
		
		// recycle the original bitmap (if scaled down)
		if (temp != bitmap) bitmap.recycle();
		
		// assign the temp bitmap to the bitmap
		bitmap = temp;
		
		// Check the orientation of the photo in the Mediastore
		try {
			int orientation = 0;
			
			/*
			 * Because of a bug on some Android devices, we're not querying the database on the location or name of the file, but request
			 * all images added after a specific timestamp (that gets initialised whenever the camera intent is fired off). This will give
			 * us either one or two results. The case with a single result is trivial. However, when two results are returned, it means that
			 * the image stored out the requested output location doesn't contain proper exif data or values in the MediaStore. Hence, we're 
			 * grabbing those from the 'other' result by means of a simple Math.max() call (which works since the incorrect orientation data
			 * defaults to '0').
			 * 
			 * See: http://code.google.com/p/android/issues/detail?id=19268
			 */
			// list to store the orientations
			List<Integer> orientations = new ArrayList<Integer>();
			Cursor cursor = null;
			try {
				
				// query params
				String[] proj = { MediaStore.Images.Media.ORIENTATION};
				String selection = imageRequestTimestamp > 0 ? MediaStore.Images.Media.DATE_ADDED + ">=?" : MediaStore.Images.Media.DATA + "=?";
				String[] selectionArgs = { imageRequestTimestamp > 0 ? Long.toString(imageRequestTimestamp / 1000l) : originalFilePath };
				String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
				
				// execute query
		        cursor = SnaprKitApplication.getInstance().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, selection, selectionArgs, sortOrder);
		        cursor.moveToFirst();
				while(!cursor.isAfterLast()) {
					orientations.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION)));
					cursor.moveToNext();
				}
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    		throw new RuntimeException("couldn't initialize media store cursor!");
	    	} finally {
	    		if (cursor != null && !cursor.isClosed()) cursor.close();
	    	}

			// get orientation from single result, or the largest from two
			if (!orientations.isEmpty()) orientation = orientations.size() == 1 ? orientations.get(0) : Math.max(orientations.get(0), orientations.get(1));
			
			if(orientation!=0) {
				// Rotate and save
				Matrix m = new Matrix();
				m.preRotate(orientation);
				int width = bitmap.getWidth();
				int height = bitmap.getHeight();
				Bitmap b = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, false);
				OutputStream out;
				out = new FileOutputStream(originalFilePath);
				b.compress(CompressFormat.JPEG, 100, out);
				ExifInterface eiw = new ExifInterface(originalFilePath);
				eiw.setAttribute(ExifInterface.TAG_ORIENTATION, "0");
				eiw.saveAttributes();

				// Tell the media scanner about the new file so that it is
				// immediately available to the user.
				MediaScannerConnection.scanFile(
						SnaprKitApplication.getInstance(),
						new String[] { originalFilePath.toString() }, null,
						new MediaScannerConnection.OnScanCompletedListener() {
							public void onScanCompleted(String path, Uri uri) {
							}
						});

				bitmap = b;
			}			
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		bitmap = SnaprPhotoHelper.cropBitmap(bitmap, true);
		if (!saveTempImage(bitmap)) return null;

		return bitmap;
	}
	
}
