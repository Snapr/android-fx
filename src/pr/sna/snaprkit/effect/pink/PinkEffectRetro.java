package pr.sna.snaprkit.effect.pink;

import pr.sna.snaprkit.R;
import pr.sna.snaprkit.SnaprPhotoHelper;
import pr.sna.snaprkit.effect.CompositeEffect;
import pr.sna.snaprkit.effect.HSBEffect;
import android.content.Context;
import android.graphics.Bitmap;

public class PinkEffectRetro {

	public static void applyEffects(Bitmap image, Context context) {
		HSBEffect.applyEffect(image, 0, 0.0f, 1.0f);
		
		Bitmap temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_retro_vignette);
		Bitmap bit0 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
		if(null!=bit0) CompositeEffect.applyImageEffect(image, bit0, 0.43f, CompositeEffect.BLEND_MODE_MULTIPLY);
		bit0.recycle();
		
		temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_retro_border);
		Bitmap bit1 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
		if(null!=bit1) CompositeEffect.applyImageEffect(image, bit1, 1.0f, CompositeEffect.BLEND_MODE_NORMAL);
		bit1.recycle();
	}
}
