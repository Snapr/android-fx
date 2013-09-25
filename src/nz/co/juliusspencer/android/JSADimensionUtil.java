package nz.co.juliusspencer.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.WindowManager;

public class JSADimensionUtil {

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * default display dimensions
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	/** Return the width of the default display. */
	public static int getDefaultDisplayWidth(Context context) {
		return getDefaultDisplayDimensions(context).getA();
	}
	
	/** Return the height of the default display. */
	public static int getDefaultDisplayHeight(Context context) {
		return getDefaultDisplayDimensions(context).getB();
	}
	
	/** Return the width and height of the default display (in a tuple). */
	@SuppressLint("NewApi") public static JSATuple<Integer, Integer> getDefaultDisplayDimensions(Context context) {
		WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Point dimens = new Point();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) manager.getDefaultDisplay().getSize(dimens);
		else dimens.set(manager.getDefaultDisplay().getWidth(), manager.getDefaultDisplay().getHeight());
		return new JSATuple<Integer, Integer>(dimens.x, dimens.y);
	}
	
}