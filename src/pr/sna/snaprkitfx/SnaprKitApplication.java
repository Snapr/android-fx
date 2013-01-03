package pr.sna.snaprkitfx;

import android.app.Application;

public class SnaprKitApplication extends Application {

	private static SnaprKitApplication sInstance;
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constructor
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public SnaprKitApplication() {
		sInstance = this;
	}
	
	public static Application getInstance() {
		return sInstance;
	}
	
}
