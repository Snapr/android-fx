package nz.co.juliusspencer.android;

public class JSAMathUtil {

	/** Return the degree value of the given radian angle. */
	public static float toDegrees(float radians) {
		return (float) (radians * 180 / Math.PI);
	}
	
	/** Return the radian value of the given degree angle. */
	public static float toRadians(float degrees) {
		return (float) (degrees * Math.PI / 180);
	}
	
}
