package nz.co.juliusspencer.android;

public class JSAGeometryUtil {

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * angle between points
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/**
	 * Return the angle (in radians) of the arc between the lines "center to p1" and "center to p2".
	 * 
	 * @param cx The x point of the center point of the arc for the angle
	 * @param cy The y point of the center point of the arc for the angle
	 * @param p1x The x point of the outer point of the first line "center to p1"
	 * @param p1y The y point of the outer point of the first line "center to p1"
	 * @param p2x The x point outer point of the second line "center to p2"
	 * @param p2y The y point outer point of the second line "center to p2"
	 * 
	 * @return The anticlockwise angle in radians of the arc between the lines "center to p1" and "center to p2". 
	 */
	public static double angleBetweenPoints(double cx, double cy, double p1x, double p1y, double p2x, double p2y) {
		return Math.atan2(p2y - cy, p2x - cx) - Math.atan2(p1y - cy, p1x - cx);
	}
	
	/** Return the distance between the two points. */
	public static double distance(double p1x, double p1y, double p2x, double p2y) {
		return Math.sqrt(Math.pow(p2x - p1x, 2) + Math.pow(p2y - p1y, 2));
	}
	
}
