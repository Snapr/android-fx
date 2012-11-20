package nz.co.juliusspencer.android;

import android.view.MotionEvent;

/**
 * The {@link JSAMotionEventUtil} class provides a set of utility methods to support common {@link MotionEvent} interactions.
 */

public class JSAMotionEventUtil {
	public static final int ACTION_MASK					= 0xff;
	public static final int ACTION_POINTER_INDEX_MASK	= 0xff00;
	public static final int ACTION_POINTER_INDEX_SHIFT	= 8;
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * get action masked
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/** 
	 * Return the masked action being performed, without pointer index information.
	 * Included to support all versions (equivalent method on {@link MotionEvent} API8+).
	 * 
	 * {@link MotionEvent.#getActionMasked()}
	 */
	public static int getActionMasked(MotionEvent event) {
		return event.getAction() & ACTION_MASK;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * get action index
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/** 
	 * Return the index of the pointer for the event. 
	 * Included to support all versions (equivalent method on {@link MotionEvent} API8+).
	 * 
	 * {@link MotionEvent.#getActionIndex()}
	 */
	public static int getActionIndex(MotionEvent event) {
		return (event.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * get pointer id
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/** Return the id of the pointer for the event. */
	public static int getPointerId(MotionEvent event) {
		return event.getPointerId(getActionIndex(event));
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * obtain
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/** Obtain a new {@link MotionEvent} by using the given event and substituting the given action. */
	public static MotionEvent obtain(MotionEvent event, int action) {
		if (event == null) throw new IllegalArgumentException();
		MotionEvent m = MotionEvent.obtain(event);
		int indexedAction = (event.getAction() & ~ACTION_MASK) + action; 
		m.setAction(indexedAction);
		return m;
	}

}
