package pr.sna.snaprkit.effect;

import android.graphics.Bitmap;

/**
 * Provides the HSB Effect
 * @author julius
 *
 */
public class HSBEffect {

	// Hue Sat Bri - Effect
	public static native void applyEffect(Bitmap source, int h, float s, float b);
	
}
