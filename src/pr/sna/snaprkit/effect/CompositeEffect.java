package pr.sna.snaprkit.effect;

import android.graphics.Bitmap;

/**
 * Provides the  Effect
 * @author julius
 *
 */
public class CompositeEffect {

	/** alpha [0,1] */
	public static native void applyImageEffect(Bitmap source, Bitmap overlay, float alpha, int blendMode);
	
	/** alpha [0,1] */
	public static native void applyAlphaEffect(Bitmap source, Bitmap overlay, float alpha, int blendMode);
	
	/** alpha [0,1] */
	public static native void applyColorEffect(Bitmap source, int colour, float alpha, int blendMode);

	public static final int BLEND_MODE_NORMAL = 0;
	public static final int BLEND_MODE_MULTIPLY = 1;
	public static final int BLEND_MODE_OVERLAY = 2;
	public static final int BLEND_MODE_SCREEN = 3;
	public static final int BLEND_MODE_DARKEN = 4;
	public static final int BLEND_MODE_LIGHTEN = 5;
	public static final int BLEND_MODE_COLOR_DODGE = 6;
	public static final int BLEND_MODE_COLOR_BURN = 7;
	public static final int BLEND_MODE_LINEAR_DODGE = 8;
	public static final int BLEND_MODE_LINEAR_BURN = 9;
	public static final int BLEND_MODE_SOFT_LIGHT = 10;
	public static final int BLEND_MODE_HARD_LIGHT = 11;
	public static final int BLEND_MODE_DIFFERENCE = 12;
	public static final int BLEND_MODE_EXCLUSION = 13;
	public static final int BLEND_MODE_HUE = 14;
	public static final int BLEND_MODE_SATURATION = 15;
	public static final int BLEND_MODE_COLOR = 16;
	public static final int BLEND_MODE_LUMINOSITY = 17;
	public static final int BLEND_MODE_ADD = 18;
	public static final int BLEND_MODE_SUBTRACT = 19;
	public static final int BLEND_MODE_DIVIDE = 20;

}
