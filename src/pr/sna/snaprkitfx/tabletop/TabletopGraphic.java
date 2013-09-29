package pr.sna.snaprkitfx.tabletop;

import nz.co.juliusspencer.android.JSAGeometryUtil;
import nz.co.juliusspencer.android.JSAMathUtil;
import nz.co.juliusspencer.android.JSAMotionEventUtil;
import pr.sna.snaprkitfx.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.FloatMath;
import android.view.MotionEvent;

/**
 * The {@link TabletopGraphic} is an object used to display and help manipulate a single graphic stored on a {@link TabletopSurfaceView}.
 * 
 * @see TabletopSurfaceView
 */

@SuppressLint("WrongCall")
public class TabletopGraphic implements Cloneable {
	private final int mId;					// unique id assigned to the graphic by the surface
	private final Bitmap mBitmap;			// the original bitmap being rendered
	private /* final */ Matrix mMatrix;		// the matrix used to draw the bitmap (non-final to support clone)
	private /* final */ PointF mCenter;		// the center point of the graphic (non-final to support clone)
	private float mRotation;				// the clockwise rotation (in radians) of the graphic
	private float mWidth;					// the width of the current render
	private float mHeight;					// the height of the current render
	private RectF mCropRegion;				// the region to crop the bitmap to
	private float mBorder;					// the border around the image that ensures that the handles do not overlap
	
	private final GeometryHelper mGeometryHelper = new GeometryHelper();
	
	private State mState = State.INACTIVE;	// the current state of the graphic
	private boolean mRefreshMatrix;			// whether or not the matrix needs to be refreshed (before the next draw)
	private PointF mLastInteractionPoint;	// the point at with the user last interacted with the graphic (when editing)
	
	private boolean mReactivatePinned;		// whether or not pinned graphic can be reactivated with user interaction (mouse down)
	
	private final Paint mBoundingBoxPaint;	// the paint used to draw the bounding box (reused for optimisation)
	private final Paint mBitmapPaint;		// the paint used to draw the bitmap (reused for optimisation)
	private final Path mBoundingBoxPath;	// the path used to draw the bounding box (reused for optimisation)
	
	private boolean mForceBoundingBoxDraw;	// whether or not the force the graphic to draw the bounding box (regardless of state)
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constants (pseudo constants)
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private static Bitmap DELETE_BITMAP_UP;			// the bitmap for the delete button
	private static Bitmap DELETE_BITMAP_DOWN;		// the bitmap for the delete button (down state)
	
	private static Bitmap ROTATION_BITMAP_UP;		// the bitmap for the rotation button
	private static Bitmap ROTATION_BITMAP_DOWN;		// the bitmap for the rotation button (down state)
	
	private static Bitmap PIN_BITMAP_UP;			// the bitmap for the pin button
	private static Bitmap PIN_BITMAP_DOWN;			// the bitmap for the pin button (down state)
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constants
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private static final float MIN_RADIUS = 50;
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * state
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static enum State {
		DISABLED,			// the graphic cannot be interacted with
		INACTIVE,			// the graphic is not being interacted with
		DELETING,			// the user is currently pressing the delete button
		DELETED,			// the user has pressed the delete button
		PINNING,			// the user is currently pressing the pin button
		PINNED,				// the user has pressed the pin button
		TRANSLATING,		// the user is currently translating the graphic
		ROTATING			// the user is currently rotating the graphic
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constructors
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public TabletopGraphic(Context context, int id, Bitmap bitmap) {
		this(context, id, bitmap, new PointF());
	}

	public TabletopGraphic(Context context, int id, Bitmap bitmap, PointF center) {
		this(context, id, bitmap, center, 0, bitmap.getWidth(), bitmap.getHeight());
	}
	
	public TabletopGraphic(Context context, int id, Bitmap bitmap, PointF center, float rotation) {
		this(context, id, bitmap, center, rotation, bitmap.getWidth(), bitmap.getHeight());
	}
	
	public TabletopGraphic(Context context, int id, Bitmap bitmap, PointF center, float rotation, float width) {
		this(context, id, bitmap, center, rotation, width, width / bitmap.getWidth() * bitmap.getHeight());
	}
	
	public TabletopGraphic(Context context, int id, Bitmap bitmap, PointF center, float rotation, float width, float height) {
		this(context, id, bitmap, center.x, center.y, rotation, width, height);
	}
	
	public TabletopGraphic(Context context, int id, Bitmap bitmap, float centerX, float centerY, float rotation, float width, float height) {
		if (context == null || bitmap == null) throw new IllegalArgumentException();
		if (bitmap.getWidth() == 0 || bitmap.getHeight() == 0) throw new IllegalArgumentException();
		if (width <= 0 || height <= 0) throw new IllegalArgumentException();
		initialiseBitmaps(context);
		mId = id;
		mBitmap = bitmap;
		mMatrix = new Matrix();
		mCenter = new PointF(centerX, centerY);
		mRotation = rotation;
		mWidth = width;
		mHeight = height;
		mBorder = computeBorder();
		notifyMatrixRefresh();
		
		mBoundingBoxPaint = new Paint();
		mBoundingBoxPaint.setColor(0xFFFFFFFF);
		mBoundingBoxPaint.setStrokeWidth(2f);
		mBoundingBoxPaint.setStyle(Paint.Style.STROKE);
		mBoundingBoxPaint.setAntiAlias(true);
		
		mBoundingBoxPath = new Path();
		
		mBitmapPaint = new Paint();
		mBitmapPaint.setFilterBitmap(true);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * border computation
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private float computeBorder()
	{
		// Assumption: down state images have the same size as up state images
		
		float values[] = {	DELETE_BITMAP_UP.getWidth()/2, DELETE_BITMAP_UP.getHeight()/2,
							ROTATION_BITMAP_UP.getWidth()/2, ROTATION_BITMAP_UP.getHeight()/2,
							PIN_BITMAP_UP.getWidth()/2, PIN_BITMAP_UP.getHeight()/2}; 
		
		return findLargestFloatArrayValue(values);
	}
	
	private float findLargestFloatArrayValue(float values[])
	{
		float returnValue = -1 * Float.MAX_VALUE;
		for (float f: values)
		{
			if (f > returnValue) returnValue = f;
		}
		
		return returnValue;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * crop region
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public RectF getCropRegion() {
		return mCropRegion;
	}
	
	public void setCropRegion(RectF rect) {
		mCropRegion = rect;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * reactivate pinned
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public boolean getReactivatePinned() {
		return mReactivatePinned;
	}
	
	public void setReactivatePinned(boolean reactivate) {
		mReactivatePinned = reactivate;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * force bounding box draw
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public boolean getForceBoundingBoxDraw() {
		return mForceBoundingBoxDraw;
	}
	
	public void setForceBoundingBoxDraw(boolean force) {
		mForceBoundingBoxDraw = force;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * center
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public float getCenterX() {
		return mCenter.x;
	}
	
	public void setCenterX(float x) {
		mCenter.x = x;
		notifyMatrixRefresh();
	}
	
	public float getCenterY() {
		return mCenter.y;
	}
	
	public void setCenterY(float y) {
		mCenter.y = y;
		notifyMatrixRefresh();
	}
	
	public PointF getCenter() {
		return new PointF(mCenter.x, mCenter.y);
	}
	
	public void setCenter(PointF center) {
		if (center == null) throw new IllegalArgumentException();
		mCenter.set(center);
		notifyMatrixRefresh();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * rotation
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public float getRotation() {
		return mRotation;
	}
	
	public void setRotation(float rotation) {
		mRotation = rotation;
		notifyMatrixRefresh();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * width
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public float getWidth() {
		return mWidth;
	}
	
	public void setWidth(float width) {
		mWidth = width;
		notifyMatrixRefresh();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * height
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public float getHeight() {
		return mHeight;
	}
	
	public void setHeight(float height) {
		mHeight = height;
		notifyMatrixRefresh();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * pin
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public void pin() {
		mState = State.PINNED;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * is pinned
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public boolean isPinned() {
		return mState.equals(State.PINNED);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * state
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public State getState() {
		return mState;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * notify matrix refresh
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private void notifyMatrixRefresh() {
		mRefreshMatrix = true;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * refresh matrix
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void refreshMatrix() {
		mMatrix.set(null);
		int w2 = mBitmap.getWidth() / 2;
		int h2 = mBitmap.getHeight() / 2;
		mMatrix.postScale(mWidth / mBitmap.getWidth(), mHeight / mBitmap.getHeight(), w2, h2);
		mMatrix.postRotate(JSAMathUtil.toDegrees(mRotation), w2, h2);
		mMatrix.postTranslate(mCenter.x - w2, mCenter.y - h2);
		mRefreshMatrix = false;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * draw
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public void onDraw(Canvas canvas, DrawContext context) {
		boolean isPinned = mState.equals(State.PINNED);
		boolean isDeleted = mState.equals(State.DELETED);
		boolean isDisabled = mState.equals(State.DISABLED);
		
		// draw the bitmap
		if (mRefreshMatrix) refreshMatrix();
		if (!isDeleted) canvas.drawBitmap(mBitmap, mMatrix, mBitmapPaint);
		
		// cache calculation
		mGeometryHelper.setBounds(mRotation, mWidth + mBorder*2, mHeight + mBorder*2);
		
		// create the bounding box path
		mBoundingBoxPath.reset();
		mBoundingBoxPath.moveTo(mGeometryHelper.getCorner(0, mCenter).x, mGeometryHelper.getCorner(0, mCenter).y); 		// top left
		mBoundingBoxPath.lineTo(mGeometryHelper.getCorner(1, mCenter).x, mGeometryHelper.getCorner(1, mCenter).y); 		// top right
		mBoundingBoxPath.lineTo(mGeometryHelper.getCorner(2, mCenter).x, mGeometryHelper.getCorner(2, mCenter).y); 		// bottom right
		mBoundingBoxPath.lineTo(mGeometryHelper.getCorner(3, mCenter).x, mGeometryHelper.getCorner(3, mCenter).y); 		// bottom left
		mBoundingBoxPath.lineTo(mGeometryHelper.getCorner(0, mCenter).x, mGeometryHelper.getCorner(0, mCenter).y); 		// top left (again)
		
		// draw the bounding box
		boolean forceBoundingBoxDraw = mForceBoundingBoxDraw && !(context instanceof ScaledDrawContext) && !isPinned;
		if (!isDeleted && !isDisabled && !isPinned || forceBoundingBoxDraw) canvas.drawPath(mBoundingBoxPath, mBoundingBoxPaint);
		
		// draw the rotation button
		PointF point = mGeometryHelper.getCorner(2, mCenter);
		Bitmap bitmap = mState.equals(State.ROTATING) ? ROTATION_BITMAP_DOWN : isDeleted || isDisabled || isPinned ? null : ROTATION_BITMAP_UP;
		if (bitmap != null) canvas.drawBitmap(bitmap, point.x - bitmap.getWidth() / 2, point.y - bitmap.getHeight() / 2, mBitmapPaint);
		
		// draw the delete button (top left; don't draw if the graphic considers itself deleted)
		point = mGeometryHelper.getCorner(0, mCenter);
		bitmap = mState.equals(State.DELETING) ? DELETE_BITMAP_DOWN : isDeleted || isDisabled || isPinned ? null : DELETE_BITMAP_UP;
		if (bitmap != null) canvas.drawBitmap(bitmap, point.x - bitmap.getWidth() / 2, point.y - bitmap.getHeight() / 2, mBitmapPaint);
		
		// draw the pin button (top right; don't draw if the graphic considers itself deleted)
		point = mGeometryHelper.getCorner(1, mCenter);
		bitmap = mState.equals(State.PINNING) ? PIN_BITMAP_DOWN : isDeleted || isDisabled || isPinned ? null : PIN_BITMAP_UP;
		if (bitmap != null) canvas.drawBitmap(bitmap, point.x - bitmap.getWidth() / 2, point.y - bitmap.getHeight() / 2, mBitmapPaint);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on touch event
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/** 
	 * Handle touch events from the user. 
	 * @return True if the graphic should receive future touch events.
	 */
	public boolean onTouchEvent(MotionEvent event) {
		int action = JSAMotionEventUtil.getActionMasked(event);
		
		switch (action) {
		case MotionEvent.ACTION_CANCEL:
			return onTouchEventActionCancel(event);
		case MotionEvent.ACTION_UP:
			return onTouchEventActionUp(event);
		case MotionEvent.ACTION_DOWN:
			return onTouchEventActionDown(event);
		case MotionEvent.ACTION_MOVE:
			return onTouchEventActionMove(event);
		}
		
		return true;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on touch event: cancel
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private boolean onTouchEventActionCancel(MotionEvent event) {
		mState = State.INACTIVE;
		return false;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on touch event: down
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private boolean onTouchEventActionDown(MotionEvent event) {
		int pointerIndex = JSAMotionEventUtil.getActionIndex(event);
		float x = event.getX(pointerIndex);
		float y = event.getY(pointerIndex);

		// unpin a pinned graphic and start translating
		if (mReactivatePinned && mState.equals(State.PINNED)) {
			if (!withinBoundsGraphic(x, y)) return false;
			mState = State.TRANSLATING;
			mLastInteractionPoint = new PointF(x, y);
			return true;
		}
		
		// return false, indicating that the graphic shouldn't receive further events, if the graphic is not inactive
		if (!mState.equals(State.INACTIVE)) { 
			return false;
		}
		
		// return true and start deleting if the touch is within the delete button bounds
		if (withinBoundsDeleteButton(x, y)) {
			mState = State.DELETING;
			return true;
		}
		
		// return true and start rotating if the touch is within the rotate button bounds
		if (withinBoundsRotationButton(x, y)) {
			mState = State.ROTATING;
			mLastInteractionPoint = new PointF(x, y);
			return true;
		}
		
		// return true and start pinning if the touch is within the pin button bounds
		if (withinBoundsPinButton(x, y)) {
			mState = State.PINNING;
			mLastInteractionPoint = new PointF(x, y);
			return true;
		}
		
		// return true and start translating if the touch is within the graphic button bounds
		if (withinBoundsGraphic(x, y)) {
			mState = State.TRANSLATING;
			mLastInteractionPoint = new PointF(x, y);
			return true;
		}
		
		return false;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on touch event: move
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private boolean onTouchEventActionMove(MotionEvent event) {
		int pointerIndex = JSAMotionEventUtil.getActionIndex(event);
		float x = event.getX(pointerIndex);
		float y = event.getY(pointerIndex);
		
		if (mState.equals(State.DELETING)) {
			if (withinBoundsDeleteButton(x, y)) return true;
			mState = State.INACTIVE;
			return false;
		}
		
		if (mState.equals(State.PINNING)) {
			if (withinBoundsPinButton(x, y)) return true;
			mState = State.INACTIVE;
			return false;
		}
		
		if (mState.equals(State.TRANSLATING)) {
			setCenterX(mCenter.x + x - mLastInteractionPoint.x);
			setCenterY(mCenter.y + y - mLastInteractionPoint.y);
			mLastInteractionPoint = new PointF(x, y);
			return true;
		}
		
		if (mState.equals(State.ROTATING)) {
			float angle = (float) JSAGeometryUtil.angleBetweenPoints(mCenter.x, mCenter.y, mLastInteractionPoint.x, mLastInteractionPoint.y, x, y);
			setRotation(mRotation + angle);
			
			float lastRadius = Math.max(MIN_RADIUS, (float) JSAGeometryUtil.distance(mCenter.x, mCenter.y, mLastInteractionPoint.x, mLastInteractionPoint.y));
			float radius = Math.max(MIN_RADIUS, (float) JSAGeometryUtil.distance(mCenter.x, mCenter.y, x, y));
			setWidth(mWidth * radius / lastRadius);
			setHeight(mHeight * radius / lastRadius);
			
			mLastInteractionPoint = new PointF(x, y);
			return true;
		}
		
		return true;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on touch event: up
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private boolean onTouchEventActionUp(MotionEvent event) {
		if (mState.equals(State.ROTATING) || mState.equals(State.TRANSLATING)) mState = State.INACTIVE;
		else if (mState.equals(State.DELETING)) mState = State.DELETED;
		else if (mState.equals(State.PINNING)) mState = State.PINNED;
		else mState = State.INACTIVE;
		return false;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * within bounds
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/** Get whether or not the given point lays within the bounds of the delete button. */
	private boolean withinBoundsDeleteButton(float x, float y) {
		mGeometryHelper.setBounds(mRotation, mWidth + mBorder*2, mHeight + mBorder*2);
		int w2 = DELETE_BITMAP_DOWN.getWidth() / 2;
		int h2 = DELETE_BITMAP_DOWN.getHeight() / 2;
		PointF center = mGeometryHelper.getCorner(0, mCenter);
		RectF rect = new RectF(center.x - w2, center.y - h2, center.x + w2, center.y + h2);
		return rect.contains(x, y);
	}
	
	/** Get whether or not the given point lays within the bounds of the rotation button. */
	private boolean withinBoundsRotationButton(float x, float y) {
		mGeometryHelper.setBounds(mRotation, mWidth + mBorder*2, mHeight + mBorder*2);
		int w2 = ROTATION_BITMAP_DOWN.getWidth() / 2;
		int h2 = ROTATION_BITMAP_DOWN.getHeight() / 2;
		PointF center = mGeometryHelper.getCorner(2, mCenter);
		RectF rect = new RectF(center.x - w2, center.y - h2, center.x + w2, center.y + h2);
		return rect.contains(x, y);
	}
	
	/** Get whether or not the given point lays within the bounds of the pin button. */
	private boolean withinBoundsPinButton(float x, float y) {
		mGeometryHelper.setBounds(mRotation, mWidth + mBorder*2, mHeight + mBorder*2);
		int w2 = PIN_BITMAP_DOWN.getWidth() / 2;
		int h2 = PIN_BITMAP_DOWN.getHeight() / 2;
		PointF center = mGeometryHelper.getCorner(1, mCenter);
		RectF rect = new RectF(center.x - w2, center.y - h2, center.x + w2, center.y + h2);
		return rect.contains(x, y);
	}
	
	/** Get whether or not the given point lays within the bounds of the graphic. */
	private boolean withinBoundsGraphic(float x, float y) {
		mGeometryHelper.setBounds(mRotation, mWidth + mBorder*2, mHeight + mBorder*2);
		PointF[] points = mGeometryHelper.getCorners(mCenter);
		return contains(points, new PointF(x, y));
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * clone
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	@Override protected TabletopGraphic clone() {
		TabletopGraphic clone;
		try { clone = (TabletopGraphic) super.clone();
		} catch (CloneNotSupportedException e) { throw new IllegalStateException(); }
		mMatrix = new Matrix(mMatrix);
		mCenter = new PointF(mCenter.x, mCenter.y);
		mLastInteractionPoint = mLastInteractionPoint != null ? new PointF(mLastInteractionPoint.x, mLastInteractionPoint.y) : null;
		return clone;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * scale
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public void scale(float scaleX, float scaleY) {
		setCenterX(getCenterX() * scaleX);
		setCenterY(getCenterY() * scaleY);
		setWidth(getWidth() * scaleX); // XXX: incorrect unless scale x and y are equal or rotation zero
		setHeight(getHeight() * scaleY);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * initialise bitmaps
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void initialiseBitmaps(Context context) {
		if (ROTATION_BITMAP_UP != null) return;
		
		ROTATION_BITMAP_UP = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.snaprkitfx_btn_sticker_handle_normal)).getBitmap();
		ROTATION_BITMAP_DOWN = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.snaprkitfx_btn_sticker_handle_down)).getBitmap();
		
		DELETE_BITMAP_UP = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.snaprkitfx_btn_sticker_delete_normal)).getBitmap();
		DELETE_BITMAP_DOWN = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.snaprkitfx_btn_sticker_delete_down)).getBitmap();
		
		PIN_BITMAP_UP = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.snaprkitfx_btn_sticker_pin_normal)).getBitmap();
		PIN_BITMAP_DOWN = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.snaprkitfx_btn_sticker_pin_down)).getBitmap();
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * geometry helper
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	/*
	 * The GeometryHelper is a class used to efficiently calculate the corners of a bounding box (with center 0,0) given its current width,
	 * height and rotation. The indices clockwise from top left; as follows:
	 * 
	 * 0: top left
	 * 1: top right
	 * 2: bottom right
	 * 3: bottom left
	 */
	
	private static final class GeometryHelper {
		private float mRotation;
		private float mWidth;
		private float mHeight;

		public void setBounds(float rotation, float width, float height) {
			if (mRotation == rotation && mWidth == width && mHeight == height) return;
			mRotation = rotation;
			mWidth = width;
			mHeight = height;
		}
		
		public PointF getCorner(int index, PointF center) {
			float r = getRadius();
			float pi = (float) Math.PI;
			float angle = (float) Math.atan(mHeight / mWidth);
			
			if (index == 0) return new PointF(center.x + r * FloatMath.cos(mRotation + angle + pi), center.y + r * FloatMath.sin(mRotation + angle + pi)); 	// top left
			if (index == 1) return new PointF(center.x + r * FloatMath.cos(mRotation - angle), center.y + r * FloatMath.sin(mRotation - angle)); 			// top right
			if (index == 2) return new PointF(center.x + r * FloatMath.cos(mRotation + angle), center.y + r * FloatMath.sin(mRotation + angle)); 			// bottom right
			return new PointF(center.x + r * FloatMath.cos(mRotation + pi - angle), center.y + r * FloatMath.sin(mRotation + pi - angle)); 					// bottom left
		}
		
		public PointF[] getCorners(PointF center) {
			return new PointF[] { getCorner(0, center), getCorner(1, center), getCorner(2, center), getCorner(3, center) };
		}
		
		public float getRadius() {
			return FloatMath.sqrt((float) Math.pow(mWidth / 2, 2) + (float) Math.pow(mHeight / 2, 2));
		}
		
		@SuppressWarnings("unused") public float getWidth() {
			return Math.abs(getCorner(0, new PointF()).x - getCorner(2, new PointF()).x);
		}
		
		@SuppressWarnings("unused") public float getHeight() {
			return Math.abs(getCorner(0, new PointF()).y - getCorner(2, new PointF()).y);
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * contains
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private boolean contains(PointF[] shape, PointF test) {
		boolean result = false;
		int i, j;
		
		for (i = 0, j = shape.length - 1; i < shape.length; j = i++)
			if ((shape[i].y > test.y) != (shape[j].y > test.y) && (test.x < (shape[j].x - shape[i].x) * (test.y - shape[i].y) / (shape[j].y - shape[i].y) + shape[i].x))
				result = !result;
		
		return result;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on draw scaled
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static void onDrawScaled(TabletopGraphic graphic, Canvas canvas, float scaleX, float scaleY, float offsetX, float offsetY) {
		onDrawScaled(graphic, canvas, scaleX, scaleY, offsetX, offsetY, new ScaledDrawContext());
	}
	
	public static void onDrawScaled(TabletopGraphic graphic, Canvas canvas, float scaleX, float scaleY, float offsetX, float offsetY, DrawContext context) {
		TabletopGraphic clone = graphic.clone();
		clone.setCenterX(clone.getCenterX() + offsetX);
		clone.setCenterY(clone.getCenterY() + offsetY);
		clone.scale(scaleX, scaleY);
		clone.mState = State.PINNED;
		clone.onDraw(canvas, context);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * equals
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	@Override public boolean equals(Object o) {
		if (!(o instanceof TabletopGraphic)) return false;
		TabletopGraphic graphic = (TabletopGraphic) o;
		return graphic.mId == mId;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * hash code
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	@Override public int hashCode() {
		return mId;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * in identical position
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public boolean inIdenticalPosition(TabletopGraphic graphic) {
		if (graphic == null) throw new IllegalArgumentException();
		if (!graphic.mCenter.equals(mCenter.x, mCenter.y)) return false;
		if (graphic.mRotation != mRotation) return false;
		if (graphic.mWidth != mWidth) return false;
		if (graphic.mHeight != mHeight) return false;
		return true;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * draw context
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static class DrawContext { }
	public static class ScaledDrawContext extends DrawContext { }
	
}
