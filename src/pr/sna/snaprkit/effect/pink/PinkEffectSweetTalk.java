package pr.sna.snaprkit.effect.pink;

import pr.sna.snaprkit.R;
import pr.sna.snaprkit.SnaprPhotoHelper;
import pr.sna.snaprkit.effect.CompositeEffect;
import pr.sna.snaprkit.effect.HSBEffect;
import pr.sna.snaprkit.effect.LevelsEffect;
import pr.sna.snaprkit.effect.PolynomialFunctionEffect;
import android.content.Context;
import android.graphics.Bitmap;

public class PinkEffectSweetTalk {

	public static void applyEffects(Bitmap image, Context context) {
		HSBEffect.applyEffect(image, 0, 0.83f, 1.0f);
		LevelsEffect.applyLevel(image, 0, 1, 255, 79, 255);

	    // ./im_fx_curves255 -c 0,0 23,23 231,241 255,255
	    double curvesRGB[] = { -0.563892000,0.614753000,0.949139000,-0.000000000 };
	    PolynomialFunctionEffect.applyEffect(image, 4, curvesRGB);

	    // ./im_fx_curves255 -c 0,0 30,39 106,143 255,255
	    double curvesR[] = { -0.863577000,0.625175000,1.238400000,-0.000000000 };
	    double curvesG[] = { 1.0,-0.000000000 };
	    // ./im_fx_curves255 -c 0,0 37,32 109,156 255,255
	    double curvesB[] = { -3.227110000,3.853430000,0.373681000,-0.000000000 };
	    PolynomialFunctionEffect.applyChannelsEffect(image, 4, curvesR, 2, curvesG, 4, curvesB);

		Bitmap temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_sweettalk);
		Bitmap bit0 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
		if(null!=bit0)
			CompositeEffect.applyImageEffect(image, bit0, 1.0f, CompositeEffect.BLEND_MODE_NORMAL);
		bit0.recycle();
	}
	
}
