package pr.sna.snaprkit.effect.pink;

import pr.sna.snaprkit.R;
import pr.sna.snaprkit.SnaprPhotoHelper;
import pr.sna.snaprkit.effect.CompositeEffect;
import android.content.Context;
import android.graphics.Bitmap;

public class PinkEffectSummer {

	public static void applyEffects(Bitmap image, Context context) {
		
		CompositeEffect.applyColorEffect(image, 0x008aec, 0.3f, CompositeEffect.BLEND_MODE_NORMAL);
		
		Bitmap temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_summer_overlay);
		Bitmap bit0 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
		if(null!=bit0) CompositeEffect.applyImageEffect(image, bit0, 1.0f, CompositeEffect.BLEND_MODE_NORMAL);
		bit0.recycle();
		
		temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_summer_glow);
		Bitmap bit1 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
		if(null!=bit1) CompositeEffect.applyImageEffect(image, bit1, 0.3f, CompositeEffect.BLEND_MODE_SCREEN);
		bit1.recycle();
	}
}
