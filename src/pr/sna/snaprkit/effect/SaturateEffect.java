package pr.sna.snaprkit.effect;

import android.graphics.Bitmap;

/**
 * Provides the  Effect
 * @author julius
 *
 */
public class SaturateEffect {

	//  - Effect
	public static native void applyEffect(Bitmap source, float sFactor);

}
