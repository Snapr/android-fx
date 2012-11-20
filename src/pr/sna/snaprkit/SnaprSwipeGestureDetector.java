package pr.sna.snaprkit;

import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;

public class SnaprSwipeGestureDetector extends SimpleOnGestureListener {
	
	private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private SnaprSwipeGestureListener mListener;
	
    public SnaprSwipeGestureDetector(SnaprSwipeGestureListener listener) {
    	super();
    	mListener = listener;
    }
    
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;
            // right to left swipe
            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
            	mListener.goPrevious();
            }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
            	mListener.goNext();
            }
        } catch (Exception e) {
            // nothing
        }
        return false;
    }

	@Override
	public boolean onDown(MotionEvent e) {
		return true;
	}
    
	public static interface SnaprSwipeGestureListener {
		void goNext();
		void goPrevious();
	}
	
	
}