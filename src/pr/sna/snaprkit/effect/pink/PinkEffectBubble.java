package pr.sna.snaprkit.effect.pink;

import pr.sna.snaprkit.R;
import pr.sna.snaprkit.SnaprPhotoHelper;
import pr.sna.snaprkit.effect.CompositeEffect;
import pr.sna.snaprkit.effect.HSBEffect;
import pr.sna.snaprkit.effect.LevelsEffect;
import android.content.Context;
import android.graphics.Bitmap;

public class PinkEffectBubble {

	public static void applyEffects(Bitmap image, Context context) {
		HSBEffect.applyEffect(image, 0, 1.28f, 1.0f);
		LevelsEffect.applyLevel(image, 0, 0.96f, 255, 0, 255);
		
		Bitmap temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_bubble_overlay);
		Bitmap bit0 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
		if(null!=bit0) CompositeEffect.applyImageEffect(image, bit0, 1.0f, CompositeEffect.BLEND_MODE_OVERLAY);
		bit0.recycle();
	}
}
