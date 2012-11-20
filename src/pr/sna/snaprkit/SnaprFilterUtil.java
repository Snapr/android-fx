package pr.sna.snaprkit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.interpolation.NevilleInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunctionLagrangeForm;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pr.sna.snaprkit.effect.BlurEffect;
import pr.sna.snaprkit.effect.CompositeEffect;
import pr.sna.snaprkit.effect.HSBEffect;
import pr.sna.snaprkit.effect.LevelsEffect;
import pr.sna.snaprkit.effect.PolynomialFunctionEffect;
import pr.sna.snaprkit.effect.SaturateEffect;
import pr.sna.snaprkit.utils.JsonUtil;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

public abstract class SnaprFilterUtil {

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constants
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private static final boolean DEBUG = false;
	private static final String LOG_TAG = "SnaprFilterUtil";

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * blending mode
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static enum BlendingMode {
		NORMAL			("normal", CompositeEffect.BLEND_MODE_NORMAL),
		MULTIPLY		("multiply", CompositeEffect.BLEND_MODE_MULTIPLY),
		SCREEN			("screen", CompositeEffect.BLEND_MODE_SCREEN),
		OVERLAY			("overlay", CompositeEffect.BLEND_MODE_OVERLAY),
		DARKEN			("darken", CompositeEffect.BLEND_MODE_DARKEN),
		LIGHTEN			("lighten", CompositeEffect.BLEND_MODE_LIGHTEN),
		COLOR_DODGE		("color_dodge", CompositeEffect.BLEND_MODE_COLOR_DODGE),
		COLOR_BURN		("color_burn", CompositeEffect.BLEND_MODE_COLOR_BURN),
		SOFT_LIGHT		("soft_light", CompositeEffect.BLEND_MODE_SOFT_LIGHT),
		HARD_LIGHT		("hard_light", CompositeEffect.BLEND_MODE_HARD_LIGHT),
		DIFFERENCE		("difference", CompositeEffect.BLEND_MODE_DIFFERENCE),
		EXCLUSION		("exclusion", CompositeEffect.BLEND_MODE_EXCLUSION),
		HUE				("hue", CompositeEffect.BLEND_MODE_HUE),
		SATURATION		("saturation", CompositeEffect.BLEND_MODE_SATURATION),
		COLOR			("color", CompositeEffect.BLEND_MODE_COLOR),
		LUMINOSITY		("luminosity", CompositeEffect.BLEND_MODE_LUMINOSITY),
		SUBTRACT		("subtract", CompositeEffect.BLEND_MODE_SUBTRACT),
		ADD				("add", CompositeEffect.BLEND_MODE_ADD),
		DIVIDE			("divide", CompositeEffect.BLEND_MODE_DIVIDE),
		LINEAR_BURN		("linear_burn", CompositeEffect.BLEND_MODE_LINEAR_BURN);

		private final String mTag;
		private final int mCompositeBlendMode;

		private BlendingMode(String tag) { mTag = tag; mCompositeBlendMode = CompositeEffect.BLEND_MODE_MULTIPLY; }
		private BlendingMode(String tag, int compositeBlendMode) { mTag = tag; mCompositeBlendMode = compositeBlendMode; }

		public static BlendingMode getBlendingMode(String tag) {
			for (BlendingMode mode : BlendingMode.values())
				if (mode.mTag.equals(tag)) return mode;
			return null;
		}
		
		public int getCompositeBlendMode() {
			return mCompositeBlendMode;
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * filter pack
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static final class FilterPack {
		protected String mName;
		protected String mDescription;
		protected List<Filter> mFilters;
		protected Bitmap mThumbnail;

		public static FilterPack parse(Context context, String folder) throws IOException, JSONException {
			return parse(context, folder, true);
		}

		public static FilterPack parse(Context context, String folder, boolean loadImages) throws IOException, JSONException {
			if (context == null || folder == null) throw new IllegalArgumentException();
			String file = JsonUtil.loadJsonFile(context, folder, "filter-pack.json");
			JSONObject json = new JSONObject(file).getJSONObject("filter_pack");
			JSONArray filters = json.getJSONArray("filters");
			FilterPack pack = new FilterPack();
			pack.mName = json.getString("name");
			pack.mDescription = json.getString("description");
			pack.mThumbnail = loadImages ? JsonUtil.loadAssetBitmap(context, folder, "thumb.png", "thumb@2x.png") : null;
			pack.mFilters = new ArrayList<SnaprFilterUtil.Filter>();
			for (int i = 0; i < filters.length(); i++) {
				String slug = ((JSONObject) filters.get(i)).getString("slug");
				pack.mFilters.add(Filter.parse(context, folder, slug, loadImages));
			}

			return pack;
		}

		public String getName() {
			return mName;
		}

		public String getDescription() {
			return mDescription;
		}

		public List<Filter> getFilters() {
			return mFilters;
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
			for (Filter filter : mFilters) filter.loadImages(context, folder, listener);
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * filter
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static final class Filter {
		protected String mName;
		protected String mSlug;
		protected List<Layer> mLayers;
		protected Bitmap mThumbnail;

		private static Filter parse(Context context, String folder, String slug, boolean loadImages) throws JSONException, IOException {
			String filterSlug = null;

			try {
				folder = folder + File.separator + "filters" + File.separator + slug;
				String file = JsonUtil.loadJsonFile(context, folder, "filter.json");
				JSONObject json = new JSONObject(file).getJSONObject("filter");
				JSONArray layers = json.getJSONArray("layers");
				filterSlug = json.getString("slug");

				Filter filter = new Filter();
				filter.mName = json.getString("name");
				filter.mSlug = json.getString("slug");
				filter.mThumbnail = loadImages ? JsonUtil.loadAssetBitmap(context, folder, "thumb.png", "thumb@2x.png") : null;
				filter.mLayers = new ArrayList<SnaprFilterUtil.Layer>();
				for (int i = 0; i < layers.length(); i++)
					filter.mLayers.add(Layer.parse(context, folder, (JSONObject) layers.get(i)));

				return filter;

			} catch (JSONException exception) {
				if (filterSlug == null) throw exception;
				else throw new JSONException(exception.getMessage() + ": " + filterSlug);
			} catch (IOException exception) {
				if (filterSlug == null) throw exception;
				else throw new IOException(exception.getMessage() + ": " + filterSlug);
			}
		}

		public String getName() {
			return mName;
		}

		public String getSlug() {
			return mSlug;
		}

		public Bitmap getThumbnail() {
			return mThumbnail;
		}

		public void apply(Context context, Bitmap bitmap) throws IOException {
			for (Layer layer : mLayers)
				layer.apply(context, bitmap);
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
			folder = folder + File.separator + "filters" + File.separator + mSlug;
			if (mThumbnail == null) mThumbnail = JsonUtil.loadAssetBitmap(context, folder, "thumb.png", "thumb@2x.png");
			if (mThumbnail != null && listener != null) listener.onImageLoad(this, mThumbnail);
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * layer
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static abstract class Layer {
		protected int mOpacity;
		protected BlendingMode mBlendingMode;
		protected boolean mMaskImage;

		private static Layer parse(Context context, String folder, JSONObject json) throws JSONException, IOException {
			String type = json.getString("type");
			if (type.equals("adjustment")) return AdjustmentLayer.parse(json).fill(json);
			if (type.equals("color")) return ColorLayer.parse(json).fill(json);
			if (type.equals("image")) return ImageLayer.parse(context, folder, json).fill(json);
			throw new IllegalArgumentException();
		}

		/* internal */ Layer fill(JSONObject json) throws JSONException {
			mOpacity = json.getInt("opacity");
			mBlendingMode = BlendingMode.getBlendingMode(json.getString("blending_mode"));
			mMaskImage = json.getBoolean("mask_image");
			return this;
		}

		public abstract void apply(Context context, Bitmap bitmap) throws IOException;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * adjustment layer
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static final class AdjustmentLayer extends Layer {
		protected Adjustment mAdjustment;

		private static AdjustmentLayer parse(JSONObject json) throws JSONException {
			AdjustmentLayer layer = new AdjustmentLayer();
			layer.mAdjustment = Adjustment.parse(json.getJSONObject("adjustment"));
			return layer;
		}

		@Override public void apply(Context context, Bitmap bitmap) {
			mAdjustment.apply(context, bitmap);
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * color layer
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static final class ColorLayer extends Layer {
		protected int mColor;

		private static ColorLayer parse(JSONObject json) throws JSONException {
			ColorLayer layer = new ColorLayer();
			layer.mColor = JsonUtil.parseColorArray(json.getJSONObject("color"), "rgb");
			return layer;
		}

		@Override public void apply(Context context, Bitmap bitmap) {
			if (DEBUG) Log.i(LOG_TAG, "applying composite effect: " + mColor + " " + mOpacity / 100f + " " + mBlendingMode.mCompositeBlendMode);
			CompositeEffect.applyColorEffect(bitmap, mColor, mOpacity / 100f, mBlendingMode.mCompositeBlendMode);
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * image layer
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static final class ImageLayer extends Layer {
		protected String mImageFolder;
		protected String mImage;
		protected boolean mScale;
		protected int mTop;
		protected int mLeft;

		private static ImageLayer parse(Context context, String folder, JSONObject json) throws JSONException, IOException {
			ImageLayer layer = new ImageLayer();
			json = json.getJSONObject("image");
			layer.mImageFolder = folder;
			layer.mImage = json.getString("image");
			layer.mScale = json.getBoolean("scale");
			layer.mTop = json.has("top") ? json.getInt("top") : 0;
			layer.mLeft = json.has("left") ? json.getInt("left") : 0;
			return layer;
		}

		@Override public void apply(Context context, Bitmap bitmap) throws IOException {
			Bitmap image = JsonUtil.loadAssetBitmap(context, mImageFolder, mImage);
			Bitmap image2 = SnaprPhotoHelper.getResizedBitmap(image, bitmap.getHeight(), bitmap.getHeight());
			image.recycle();
			if (DEBUG) Log.i(LOG_TAG, "applying image effect: " + mOpacity / 100f + " " + mBlendingMode.mCompositeBlendMode);
			CompositeEffect.applyImageEffect(bitmap, image2, mOpacity / 100f, mBlendingMode.mCompositeBlendMode);
			image2.recycle();
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * adjustment
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static abstract class Adjustment {

		private static Adjustment parse(JSONObject json) throws JSONException {
			String type = json.getString("type");
			if (type.equals("levels")) return LevelsAdjustment.parse(json);
			if (type.equals("curves")) return CurvesAdjustment.parse(json);
			if (type.equals("saturation")) return SaturationAdjustment.parse(json);
			if (type.equals("lightness")) return LightnessAdjustment.parse(json);
			if (type.equals("blur")) return BlurAdjustment.parse(json);
			if (type.equals("hue")) return HueAdjustment.parse(json);
			throw new IllegalArgumentException();
		}

		public abstract void apply(Context context, Bitmap bitmap);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * levels adjustment
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private static final class LevelsAdjustment extends Adjustment {
		protected int mBlack;
		protected float mMid;
		protected int mWhite;

		private static LevelsAdjustment parse(JSONObject json) throws JSONException {
			LevelsAdjustment a = new LevelsAdjustment();
			a.mBlack = json.getInt("black");
			a.mMid = (float) json.getDouble("mid");
			a.mWhite = json.getInt("white");
			return a;
		}

		@Override public void apply(Context context, Bitmap bitmap) {
			if (DEBUG) Log.i(LOG_TAG, "applying levels adjustment: " + mBlack + " " + mMid + " " + mWhite);
			LevelsEffect.applyLevel(bitmap, mBlack, mMid, mWhite, 0, 255);
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * curves adjustment
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private static final class CurvesAdjustment extends Adjustment {
		protected int[] mRGB;
		protected int[] mRed;
		protected int[] mGreen;
		protected int[] mBlue;

		protected double[] mRGBCo;
		protected double[] mRedCo;
		protected double[] mGreenCo;
		protected double[] mBlueCo;

		private static CurvesAdjustment parse(JSONObject json) throws JSONException {

			CurvesAdjustment a = new CurvesAdjustment();
			a.mRGB = JsonUtil.flattenIntArray(json, "rgb");
			a.mRed = JsonUtil.flattenIntArray(json, "red");
			a.mGreen = JsonUtil.flattenIntArray(json, "green");
			a.mBlue = JsonUtil.flattenIntArray(json, "blue");

			if(DEBUG)
				Log.i(LOG_TAG,json.toString());

			double[] mRGBX;
			double[] mRedX;
			double[] mGreenX;
			double[] mBlueX;

			double[] mRGBY;
			double[] mRedY;
			double[] mGreenY;
			double[] mBlueY;


			// Create the arrays for the x and Y points
			mRGBX = new double[a.mRGB.length/2];
			mRGBY = new double[a.mRGB.length/2];
			mRedX = new double[a.mRed.length/2];
			mRedY = new double[a.mRed.length/2];
			mGreenX = new double[a.mGreen.length/2];
			mGreenY = new double[a.mGreen.length/2];
			mBlueX = new double[a.mBlue.length/2];
			mBlueY = new double[a.mBlue.length/2];

			NevilleInterpolator ni = new NevilleInterpolator();
			// Fill the arrays
			if(mRGBX.length>=2) {
				fillArray(a.mRGB, mRGBX, mRGBY);
				if(DEBUG)
					Log.i(LOG_TAG,"rgbx: "+getStringFromArray(mRGBX));
				if(DEBUG)
					Log.i(LOG_TAG,"rgby: "+getStringFromArray(mRGBY));

				PolynomialFunctionLagrangeForm interpolatedRGB = ni.interpolate(mRGBX, mRGBY);
				a.mRGBCo = interpolatedRGB.getCoefficients();
			}

			if(mRedX.length>=2) {
				fillArray(a.mRed, mRedX, mRedY);
				if(DEBUG)
					Log.i(LOG_TAG,"redx: "+getStringFromArray(mRedX));
				if(DEBUG)
					Log.i(LOG_TAG,"redy: "+getStringFromArray(mRedY));

				PolynomialFunctionLagrangeForm interpolatedRed = ni.interpolate(mRedX, mRedY);
				a.mRedCo = interpolatedRed.getCoefficients();
			}

			if(mGreenX.length>=2) {
				fillArray(a.mGreen, mGreenX, mGreenY);
				if(DEBUG)
					Log.i(LOG_TAG,"grex: "+getStringFromArray(mGreenX));
				if(DEBUG)
					Log.i(LOG_TAG,"grey: "+getStringFromArray(mGreenY));
				PolynomialFunctionLagrangeForm interpolatedGreen = ni.interpolate(mGreenX, mGreenY);
				a.mGreenCo = interpolatedGreen.getCoefficients();
			}

			if(mBlueX.length>=2) {
				fillArray(a.mBlue, mBlueX, mBlueY);
				if(DEBUG)
					Log.i(LOG_TAG,"blux: "+getStringFromArray(mBlueX));
				if(DEBUG)
					Log.i(LOG_TAG,"bluy: "+getStringFromArray(mBlueY));
				PolynomialFunctionLagrangeForm interpolatedBlue = ni.interpolate(mBlueX, mBlueY);
				a.mBlueCo = interpolatedBlue.getCoefficients();
			}

			return a;
		}

		private static String getStringFromArray(int[] array) {
			StringBuilder rgbs = new StringBuilder();
			for(int i=0; i<array.length; i++) {
				if(0!=i)
					rgbs.append(", ");
				rgbs.append(array[i]);
			}
			return rgbs.toString();
		}

		private static String getStringFromArray(double[] array) {
			StringBuilder rgbs = new StringBuilder();
			for(int i=0; i<array.length; i++) {
				if(0!=i)
					rgbs.append(", ");
				rgbs.append(array[i]);
			}
			return rgbs.toString();
		}

		private static void fillArray(int[] sourceArray, double[] destinationXArray, double[] destinationYArray) {
			for(int i=0; i<sourceArray.length/2; i++) {
				destinationXArray[i] = (double)sourceArray[2*i] / 255.0; 
			}
			for(int i=0; i<sourceArray.length/2; i++) {
				if(i==0)
					destinationYArray[i] = (double)sourceArray[i+1] / 255.0;
				else
					destinationYArray[i] = (double)sourceArray[2*(i+1)-1] / 255.0; 
			}
		}

		@Override public void apply(Context context, Bitmap bitmap) {

			if (DEBUG) {
				StringBuilder rgbs = new StringBuilder();
				if(null!=mRGBCo) {
					for(int i=0; i<mRGBCo.length; i++) {
						if(0!=i)
							rgbs.append(", ");
						rgbs.append(mRGBCo[i]);
					}
				}
				StringBuilder rs = new StringBuilder();
				if(null!=mRedCo) {
					for(int i=0; i<mRedCo.length; i++) {
						if(0!=i)
							rs.append(", ");
						rs.append(mRedCo[i]);
					}
				}
				StringBuilder gs = new StringBuilder();
				if(null!=mGreenCo) {
					for(int i=0; i<mGreenCo.length; i++) {
						if(0!=i)
							gs.append(", ");
						gs.append(mGreenCo[i]);
					}
				}
				StringBuilder bs = new StringBuilder();
				if(null!=mBlueCo) {
					for(int i=0; i<mBlueCo.length; i++) {
						if(0!=i)
							bs.append(", ");
						bs.append(mBlueCo[i]);
					}
				}
				Log.i(LOG_TAG, "applying curves adjustment red  : "+rs.toString());
				Log.i(LOG_TAG,"rps: "+getStringFromArray(mRed));
				Log.i(LOG_TAG, "applying curves adjustment green: "+gs.toString());
				Log.i(LOG_TAG,"gps: "+getStringFromArray(mGreen));
				Log.i(LOG_TAG, "applying curves adjustment blue : "+bs.toString());
				Log.i(LOG_TAG,"bps: "+getStringFromArray(mBlue));
				Log.i(LOG_TAG, "applying curves adjustment rgb  : "+rgbs.toString());
				Log.i(LOG_TAG,"aps: "+getStringFromArray(mRGB));
			}

			double[] mRedCo = reverse(this.mRedCo);
			double[] mBlueCo = reverse(this.mBlueCo);
			double[] mGreenCo = reverse(this.mGreenCo);
			double[] mRGBCo = reverse(this.mRGBCo);



			if(null!=mRedCo)
				PolynomialFunctionEffect.applyChannelsEffect(bitmap, mRedCo.length, mRedCo, mGreenCo.length, mGreenCo, mBlueCo.length, mBlueCo);
			if(null!=mRGBCo)
				PolynomialFunctionEffect.applyEffect(bitmap, mRGBCo.length, mRGBCo);
		}

		private double[] reverse(double[] array) {
			if(null==array) return new double[] { 1.0, 0.0 };
			Log.i(LOG_TAG, getStringFromArray(array));
			double[] newArray = new double[array.length];
			int j=0;
			for(int i=array.length-1; i>=0; i--) {
				newArray[j] = array[i];
				j++;
			}
			Log.i(LOG_TAG, getStringFromArray(newArray));
			return newArray;
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * saturation adjustment
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private static final class SaturationAdjustment extends Adjustment {
		protected float mAmount;

		private static SaturationAdjustment parse(JSONObject json) throws JSONException {
			SaturationAdjustment a = new SaturationAdjustment();
			a.mAmount = (float) json.getDouble("amount");
			return a;
		}

		@Override public void apply(Context context, Bitmap bitmap) {
			if (DEBUG) Log.i(LOG_TAG, "applying saturation adjustment: " + ((mAmount / 100f)+1f) + " ");
			SaturateEffect.applyEffect(bitmap, (mAmount / 100f)+1f);
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * lightness adjustment
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private static final class LightnessAdjustment extends Adjustment {
		protected float mAmount;

		private static LightnessAdjustment parse(JSONObject json) throws JSONException {
			LightnessAdjustment a = new LightnessAdjustment();
			a.mAmount = (float) json.getDouble("amount");
			return a;
		}

		@Override public void apply(Context context, Bitmap bitmap) {
			if (DEBUG) Log.i(LOG_TAG, "applying lightness adjustment: " + (1+ mAmount / 100));
			HSBEffect.applyEffect(bitmap, 0, 1f, 1+ mAmount / 100); // hue 0: neutral, saturation 1.0: neutral, brightness 1.0: neutral
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * blur adjustment
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private static final class BlurAdjustment extends Adjustment {
		protected int mAmount;

		private static BlurAdjustment parse(JSONObject json) throws JSONException {
			BlurAdjustment a = new BlurAdjustment();
			a.mAmount = (int) json.getDouble("amount");
			return a;
		}

		@Override public void apply(Context context, Bitmap bitmap) {
			if (DEBUG) Log.i(LOG_TAG, "applying blur adjustment: " + mAmount);
			BlurEffect.applyEffect(bitmap, mAmount);
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * hue adjustment
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private static final class HueAdjustment extends Adjustment {
		protected int mAmount;

		private static HueAdjustment parse(JSONObject json) throws JSONException {
			HueAdjustment a = new HueAdjustment();
			a.mAmount = json.getInt("amount");
			return a;
		}

		@Override public void apply(Context context, Bitmap bitmap) {
			if (DEBUG) Log.i(LOG_TAG, "applying hue adjustment: " + mAmount);
			HSBEffect.applyEffect(bitmap, mAmount, 1f, 1f); // hue 0: neutral, saturation 1.0: neutral, brightness 1.0: neutral
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on image load listener
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static interface OnImageLoadListener {
		void onImageLoad(Object parent, Bitmap image);
	}

}
