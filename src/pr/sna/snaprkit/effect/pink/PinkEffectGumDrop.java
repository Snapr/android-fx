package pr.sna.snaprkit.effect.pink;

import pr.sna.snaprkit.R;
import pr.sna.snaprkit.SnaprPhotoHelper;
import pr.sna.snaprkit.effect.CompositeEffect;
import pr.sna.snaprkit.effect.HSBEffect;
import android.content.Context;
import android.graphics.Bitmap;

public class PinkEffectGumDrop {

	public static void applyEffects(Bitmap image, Context context) {
		HSBEffect.applyEffect(image, -26, 0.81f, 1.0f);

		// Get overlay in bytes and supply blendMode
		Bitmap temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_gumdrop);
		Bitmap bit0 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
		if(null!=bit0)
			CompositeEffect.applyImageEffect(image, bit0, 1, CompositeEffect.BLEND_MODE_NORMAL);
		bit0.recycle();
	}

}
