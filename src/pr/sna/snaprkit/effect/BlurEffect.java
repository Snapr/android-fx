package pr.sna.snaprkit.effect;

import android.graphics.Bitmap;

/**
 * Provides the Blur Effect
 * 
 * @author julius
 *
 */
public class BlurEffect {

	//  - Effect
	public static native void applyEffect(Bitmap source, int radius);

}
