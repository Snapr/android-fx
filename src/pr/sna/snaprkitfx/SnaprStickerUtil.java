package pr.sna.snaprkitfx;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import nz.co.juliusspencer.android.JSAFileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pr.sna.snaprkitfx.SnaprFilterUtil.BlendingMode;
import pr.sna.snaprkitfx.SnaprFilterUtil.OnImageLoadListener;
import pr.sna.snaprkitfx.tabletop.TabletopSurfaceView.GraphicElement;
import pr.sna.snaprkitfx.util.JsonUtil;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;


public abstract class SnaprStickerUtil {
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * sticker pack
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static final class StickerPack {
		private String mName;
		private String mDescription;
		private List<Sticker> mStickers;
		private Bitmap mThumbnail;
		
		public static StickerPack parse(Context context, String folder) throws IOException, JSONException, ParseException {
			return parse(context, folder, false);
		}
		
		public static StickerPack parse(Context context, String folder, boolean loadImages) throws IOException, JSONException, ParseException {
			String file = JsonUtil.loadJsonFile(context, folder, "sticker-pack.json");
			JSONObject json = new JSONObject(file).getJSONObject("sticker_pack");
			JSONArray stickers = json.getJSONArray("stickers");
			
			StickerPack pack = new StickerPack();
			pack.mName = json.getString("name");
			pack.mDescription = json.getString("description");
			pack.mThumbnail = loadImages ? JsonUtil.loadAssetBitmap(context, folder, "thumb.png", "thumb@2x.png", false) : null;
			pack.mStickers = new ArrayList<SnaprStickerUtil.Sticker>();
			
			ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			long deviceMemoryBytes = am.getMemoryClass() * JSAFileUtil.BYTES_PER_MB;
			long maxImageBytes = (long) (deviceMemoryBytes * SnaprImageEditFragmentUtil.STICKER_MEMORY_PERCENTAGE / 100f / stickers.length());
			
			for (int i = 0; i < stickers.length(); i++)
				pack.mStickers.add(Sticker.parse(context, folder, (JSONObject) stickers.get(i), loadImages, maxImageBytes));
			return pack;
		}
		
		public String getName() {
			return mName;
		}
		
		public String getDescription() {
			return mDescription;
		}
		
		public List<Sticker> getStickers() {
			return mStickers;
		}
		
		public Bitmap getThumbnail() {
			return mThumbnail;
		}
		
		public void loadImagesNoException(Context context, String folder, OnImageLoadListener listener) {
			try {
				loadImages(context, folder, listener);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void loadImages(Context context, String folder, OnImageLoadListener listener) throws IOException {
			if (folder == null) throw new IllegalArgumentException();
			if (mThumbnail == null) mThumbnail = JsonUtil.loadAssetBitmap(context, folder, "thumb.png", "thumb@2x.png");
			if (mThumbnail != null && listener != null) listener.onImageLoad(this, mThumbnail);
			for (Sticker sticker : mStickers) sticker.loadImages(context, folder, listener);
		}
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * sticker
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static final class Sticker implements GraphicElement {
		private String mName;
		private String mSlug;
		private int mOpacity;
		private BlendingMode mBlendingMode;
		private Bitmap mImage;
		private Bitmap mThumbnail;
		private long mMaxImageBytes;
		private SnaprSetting mSettings;
		
		private static Sticker parse(Context context, String folder, JSONObject json, boolean loadImages, long maxImageBytes) throws JSONException, IOException, ParseException {
			String stickerSlug = null;
			folder = folder + File.separator + "assets";
			String thumbnailFolder = folder + File.separator + "thumbs";
			
			try {
				stickerSlug = json.getString("slug");
				
				Sticker sticker = new Sticker();
				sticker.mMaxImageBytes = maxImageBytes;
				sticker.mName = json.getString("name");
				sticker.mSlug = json.getString("slug");
				sticker.mOpacity = json.getInt("opacity");
				sticker.mBlendingMode = BlendingMode.getBlendingMode(json.getString("blending_mode"));
				sticker.mImage = loadImages ? loadImageBitmap(context, folder, sticker.mSlug, sticker.mMaxImageBytes, sticker.mOpacity, sticker.mBlendingMode) : null;
				sticker.mThumbnail = loadImages ? JsonUtil.loadAssetBitmap(context, thumbnailFolder, sticker.mSlug + ".png", sticker.mSlug + "@2x.png") : null;
				
				// parse settings, or construct default (everything initialised to 'false' and 'null')
				sticker.mSettings = json.isNull("settings") ? SnaprSetting.getDefaultSettings(stickerSlug) : SnaprSetting.parse(context, json.getJSONObject("settings"), stickerSlug);
				
				return sticker;
				
			} catch (JSONException exception) {
				if (stickerSlug == null) throw exception;
				else throw new JSONException(exception.getMessage() + ": " + stickerSlug);
			} catch (IOException exception) {
				if (stickerSlug == null) throw exception;
				else throw new IOException(exception.getMessage() + ": " + stickerSlug);
			}
		}
		
		public String getName() {
			return mName;
		}

		public String getSlug() {
			return mSlug;
		}
		
		public int getOpacity() {
			return mOpacity;
		}
		
		public BlendingMode getBlendingMode() {
			return mBlendingMode;
		}
		
		public Bitmap getImage() {
			return mImage;
		}
		
		public Bitmap getThumbnail() {
			return mThumbnail;
		}
		
		public SnaprSetting getSettings() {
			return mSettings;
		}
		
		public void setSettings(SnaprSetting settings) {
			mSettings = settings;
		}
		
		public void loadImagesNoException(Context context, String folder, OnImageLoadListener listener) {
			try {
				loadImages(context, folder, listener);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void loadImages(Context context, String folder, OnImageLoadListener listener) throws IOException {
			if (context == null || folder == null) throw new IllegalArgumentException();
			folder = folder + File.separator + "assets";
			String thumbnailFolder = folder + File.separator + "thumbs";
			if (mImage == null) mImage = loadImageBitmap(context, folder, mSlug, mMaxImageBytes, mOpacity, mBlendingMode);
			if (mImage != null && listener != null) listener.onImageLoad(this, mThumbnail);
			if (mThumbnail == null) mThumbnail = JsonUtil.loadAssetBitmap(context, thumbnailFolder, mSlug + ".png", mSlug + "@2x.png");
			if (mThumbnail != null && listener != null) listener.onImageLoad(this, mImage);
		}
		
		private static Bitmap loadImageBitmap(Context context, String folder, String slug, long maxImageBytes, int opacity, BlendingMode blendingMode) throws IOException {
			Bitmap bitmap = JsonUtil.loadMemoryAwareAssetBitmap(context, folder, slug + ".png", maxImageBytes);
			Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
			Canvas canvas = new Canvas(result);
			
			// draw the opaque bitmap
			Paint paint = new Paint();
			paint.setAlpha((int) (opacity / 100f * 255));
			canvas.drawBitmap(bitmap, 0, 0, paint);
			bitmap.recycle();
			System.gc();
			
			return result;
		}

		@Override public Bitmap getBitmap() {
			return mImage;
		}
	}
	
}
