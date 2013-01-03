package pr.sna.snaprkitfx.effect;

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
