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
	
	/** Return the minimum value from the given values. */
	public static int min(int... values) {
		if (values.length == 0) throw new IllegalArgumentException();
		if (values.length == 1) return values[0];
		int min = values[0];
		for (int i = 0; i < values.length; i++)
			min = Math.min(min, values[i]);
		return min;
	}

}
