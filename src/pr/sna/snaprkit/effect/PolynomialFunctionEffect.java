package pr.sna.snaprkit.effect;

import android.graphics.Bitmap;

/**
 * Provides the  Effect
 * @author julius
 *
 */
public class PolynomialFunctionEffect {

	//  - Effect

	public static native void applyEffect(Bitmap source,
			int n_params_rgb,
			double[] curvesRGB);

	public static native void applyBlend(Bitmap source,
			int n_params,
			double[] params,
			float alpha,
			int blendMode);

	public static native void applyChannelsEffect(Bitmap source,
			int n_params_r, double[] params_r,
			int n_params_g, double[] params_g,
			int n_params_b, double[] params_b);

	public static native void applyChannelsBlend(Bitmap source,
			int n_params_r, double[] params_r,
			int n_params_g, double[] params_g,
			int n_params_b, double[] params_b,
			float alpha, int blendMode);

}
