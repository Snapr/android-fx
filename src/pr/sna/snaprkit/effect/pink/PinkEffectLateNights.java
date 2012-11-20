package pr.sna.snaprkit.effect.pink;

import pr.sna.snaprkit.R;
import pr.sna.snaprkit.SnaprPhotoHelper;
import pr.sna.snaprkit.effect.CompositeEffect;
import android.content.Context;
import android.graphics.Bitmap;

public class PinkEffectLateNights {

	public static void applyEffects(Bitmap image, Context context) {
		
		Bitmap temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_latenights);
		Bitmap bit0 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
		if(null!=bit0)
			CompositeEffect.applyImageEffect(image, bit0, 1.0f, CompositeEffect.BLEND_MODE_NORMAL);
		bit0.recycle();

	}
}
