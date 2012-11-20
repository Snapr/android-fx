package pr.sna.snaprkit.effect.pink;

import pr.sna.snaprkit.R;
import pr.sna.snaprkit.SnaprPhotoHelper;
import pr.sna.snaprkit.effect.CompositeEffect;
import pr.sna.snaprkit.effect.HSBEffect;
import pr.sna.snaprkit.effect.LevelsEffect;
import pr.sna.snaprkit.effect.SaturateEffect;
import android.content.Context;
import android.graphics.Bitmap;

public class PinkEffectBonBon {
	
	public static void applyEffects(Bitmap image, Context context) {
		
		
		SaturateEffect.applyEffect(image, 0.04f);
		LevelsEffect.applyLevel(image, 0, 1.0f, 255, 153, 255);
		LevelsEffect.applyLevel(image, 164, 1.0f, 241, 0, 255);
		
		CompositeEffect.applyColorEffect(image, 0xbff8da, 1.0f, CompositeEffect.BLEND_MODE_OVERLAY);

		Bitmap temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_bonbon_red);
		Bitmap bit0 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
	    if(null!=bit0)
	    	CompositeEffect.applyImageEffect(image, bit0, 1.0f, CompositeEffect.BLEND_MODE_SCREEN);
		bit0.recycle();

		HSBEffect.applyEffect(image, -26, 1.19f, 1.0f);
	    
		temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_bonbon_pink);
		Bitmap bit1 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
	    if(null!=bit1)
	    	CompositeEffect.applyImageEffect(image, bit1, 0.37f, CompositeEffect.BLEND_MODE_MULTIPLY);
		bit1.recycle();

	}
	
}
