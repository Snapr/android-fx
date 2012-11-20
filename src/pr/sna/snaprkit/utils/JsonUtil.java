package pr.sna.snaprkit.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nz.co.juliusspencer.android.JSAArrayUtil;
import nz.co.juliusspencer.android.JSAFileUtil;
import nz.co.juliusspencer.android.JSAImageUtil;
import nz.co.juliusspencer.android.JSATuple;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

public abstract class JsonUtil {
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * load json file
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static String loadJsonFile(Context context, String folder, String filename) throws IOException {
		InputStream stream = context.getAssets().open(folder + File.separator + filename, AssetManager.ACCESS_RANDOM);
		try {
			ByteArrayOutputStream out = JSAFileUtil.readFileStream(stream);
			return out.toString();
		} finally {
			if (stream != null) stream.close();
		}
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * load memory aware bitmap
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static Bitmap loadMemoryAwareAssetBitmap(Context context, String folder, String filename, long maxBytes) throws IOException {
		JSATuple<Integer, Integer> dimens = getAssetBitmapDimensions(context, folder, filename, filename);
		long rawBytes = dimens.getA() * dimens.getB() * 4;
		float scale = Math.min((float) Math.sqrt(maxBytes / (float) rawBytes), 1f);
		Bitmap bitmap = loadAssetBitmap(context, folder, filename);
		Bitmap result = Bitmap.createScaledBitmap(bitmap, (int) (dimens.getA() * scale), (int) (dimens.getB() * scale), true);
		bitmap.recycle();
		System.gc();
		return result;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * get bitmap dimensions
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static JSATuple<Integer, Integer> getAssetBitmapDimensions(Context context, String folder, String filenameMdpi, String filenameHdpi) throws IOException {
		int density = context.getResources().getDisplayMetrics().densityDpi;
		String filename = density <= DisplayMetrics.DENSITY_MEDIUM ? filenameMdpi : filenameHdpi;
		InputStream stream = null;
		try {
			stream = context.getAssets().open(folder + File.separator + filename, AssetManager.ACCESS_RANDOM);
			return JSAImageUtil.getBitmapImageDimensions(stream);
		} finally {
			try { if (stream != null) stream.close();
			} catch (IOException exception) { }
		}
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * load bitmap
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static Bitmap loadAssetBitmap(Context context, String folder, String filename) throws IOException {
		return loadAssetBitmap(context, folder, filename, filename);
	}
	
	public static Bitmap loadAssetBitmap(Context context, String folder, String filenameMdpi, String filenameHdpi) throws IOException {
		return loadAssetBitmap(context, folder, filenameMdpi, filenameHdpi, true);
	}
	
	public static Bitmap loadAssetBitmap(Context context, String folder, String filenameMdpi, String filenameHdpi, boolean required) throws IOException {
		int density = context.getResources().getDisplayMetrics().densityDpi;
		String filename = density <= DisplayMetrics.DENSITY_MEDIUM ? filenameMdpi : filenameHdpi;
		InputStream stream = context.getAssets().open(folder + File.separator + filename, AssetManager.ACCESS_RANDOM);
		try {
			ByteArrayOutputStream out = JSAFileUtil.readFileStream(stream);
			byte[] bytes = out.toByteArray();
			return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		} catch (IOException exception) {
			if (!required) return null;
			throw exception;
		} finally {
			if (stream != null) stream.close();
		}
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * parse color array
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static int parseColorArray(JSONObject json, String tag) throws JSONException {
		int[] values = flattenIntArray(json, tag);
		return (values[0] << 16) + (values[1] << 8) + values[2];
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * flatten int array
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static int[] flattenIntArray(JSONObject json, String tag) throws JSONException {
		return JSAArrayUtil.toPrimitiveInt(flattenIntArrayInner(json.getJSONArray(tag)));
	}
	
	public static List<Integer> flattenIntArrayInner(JSONArray array) throws JSONException {
		List<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < array.length(); i++) {
			Object item = array.get(i);
			if (item instanceof JSONArray) result.addAll(flattenIntArrayInner((JSONArray) item));
			else if (item instanceof Integer) result.add((Integer) item);
			else throw new IllegalArgumentException();
		}
		return result;
	}
	
}
