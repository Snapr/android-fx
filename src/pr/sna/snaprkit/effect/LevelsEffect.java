package pr.sna.snaprkit.effect;

import android.graphics.Bitmap;

/**
 * Provides the  Effect
 * @author julius
 *
 */
public class LevelsEffect {

//	XKLevelImage((PixelPacket *)pixels, info.width, info.height, info.stride, 120, 1.0, 255, 0, 255);
	//  - Effect
	public static native void applyLevel(Bitmap source,
			int black_point, 
			float gamma,
			int white_point,
			int black_out,
			int white_out);
	
	public static native void applyLevelBlend(Bitmap source,
			int black_point,
			float gamma,
			int white_point,
			int black_out,
			int white_out,
			float alpha,
			int colorBlend);

	public static native void applyLevelChannels(Bitmap source,
			int black_point, float gamma, int white_point, int black_out, int white_out,
			int black_point_r, float gamma_r, int white_point_r, int black_out_r, int white_out_r,
			int black_point_g, float gamma_g, int white_point_g, int black_out_g, int white_out_g,
			int black_point_b, float gamma_b, int white_point_b, int black_out_b, int white_out_b);

	public static native void applyLevelChannelsBlend(Bitmap source,
			 int black_point, float gamma, int white_point, int black_out, int white_out,
			 int black_point_r, float gamma_r, int white_point_r, int black_out_r, int white_out_r,
			 int black_point_g, float gamma_g, int white_point_g, int black_out_g, int white_out_g,
			 int black_point_b, float gamma_b, int white_point_b, int black_out_b, int white_out_b,
			 float alpha, int colorBlend);

	
}
