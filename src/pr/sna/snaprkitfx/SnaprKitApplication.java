package pr.sna.snaprkitfx;

import android.app.Application;

@Deprecated public class SnaprKitApplication extends Application {

	private static SnaprKitApplication sInstance;
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constructor
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	@Deprecated public SnaprKitApplication() {
		sInstance = this;
	}
	
	@Deprecated public static Application getInstance() {
		return sInstance;
	}
	
}
