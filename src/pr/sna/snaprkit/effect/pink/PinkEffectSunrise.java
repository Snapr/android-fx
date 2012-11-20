package pr.sna.snaprkit.effect.pink;

import pr.sna.snaprkit.R;
import pr.sna.snaprkit.SnaprPhotoHelper;
import pr.sna.snaprkit.effect.CompositeEffect;
import pr.sna.snaprkit.effect.HSBEffect;
import pr.sna.snaprkit.effect.LevelsEffect;
import pr.sna.snaprkit.effect.PolynomialFunctionEffect;
import android.content.Context;
import android.graphics.Bitmap;

public class PinkEffectSunrise {

	public static void applyEffects(Bitmap image, Context context) {
		HSBEffect.applyEffect(image, -0, 0.32f, 1.0f);
		LevelsEffect.applyLevel(image, 0,   1.0f, 255, 159, 255);
		LevelsEffect.applyLevel(image, 147, 1.0f, 235,   0, 255);

		double curvesRGB[] = { -0.987851000,1.106740000,0.881108000,-0.000000000 };
		PolynomialFunctionEffect.applyEffect(image, 4, curvesRGB);

		Bitmap temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_sunrise_colours);
		Bitmap bit0 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
		if(null!=bit0)
			CompositeEffect.applyImageEffect(image, bit0, 1.0f, CompositeEffect.BLEND_MODE_MULTIPLY);
		bit0.recycle();

		temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_sunrise);
		Bitmap bit1 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
		if(null!=bit1)
			CompositeEffect.applyImageEffect(image, bit1, 1.0f, CompositeEffect.BLEND_MODE_NORMAL);
		bit1.recycle();
	}
}
