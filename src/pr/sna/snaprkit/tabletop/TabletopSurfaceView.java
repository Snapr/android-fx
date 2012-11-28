package pr.sna.snaprkit.tabletop;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nz.co.juliusspencer.android.JSAFileUtil;
import nz.co.juliusspencer.android.JSAMotionEventUtil;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * The {@link TabletopSurfaceView} is an extension of {@link SurfaceView} and is used to support the manipulation (translate, rotate, pin, 
 * etc.) of graphics through user interaction on a transparent plane.
 * 
 * The {@link TabletopSurfaceView} uses {@link TabletopGraphic} objects to display and manipulate. {@link TabletopGraphic} objects can be
 * translated, rotated and pinned. When pinned, graphics are put in a background stack. {@link TabletopGraphic} objects can optionally be
 * restored from being pinned {@link TabletopGraphic.#setReactivatePinned(boolean)} through user interaction again. When unpinned, graphics
 * are moved to the top of the foreground stack.
 * 
 * {@link TabletopGraphic} objects can be added in two ways, either programmatically through {@link #addGraphic(Bitmap)} (placing the given 
 * graphic in the center of the surface) or through user interaction through setting {@link #setTouchElement(Bitmap)} and placing the graphic
 * on the surface when the user touches an empty space.
 */

public class TabletopSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
	private final List<TabletopGraphic> mBackgroundGraphics = new ArrayList<TabletopGraphic>();		// graphics that cannot directly being interacted with
	private final List<TabletopGraphic> mForegroundGraphics = new ArrayList<TabletopGraphic>();		// graphics that may be directly interacted with
	private TabletopThread mThread;
	
	private int mNextGraphicId;						// the id to assign to the next graphic created 
	private TabletopGraphic mActiveGraphic; 		// the graphic currently being edited by the user
	private int mActiveGraphicPointerId;			// the id of the pointer being used to edit the graphic
	private GraphicElement mTouchElement;			// the graphic (retrieved from assets) to add when the user touches the blank surface
	
	private RectF mCropRegion;						// the crop region of the surface (not yet implemented)
	private /* final */ Paint mCropRegionPaint;		// the paint to draw the crop region with
	
	private boolean mInteractionEnabled = true;		// whether or not interaction is currently enabled for the surface
	
	private int mInteractionCount;											// the number of times the user has interacted with the surface
	private Runnable mNonInteractionRunnable;								// the runnable used to track non-interaction timeouts
	private TabletopListener mTabletopListener;								// the listener notified when the user is no longer interacting
	private long mNonInteractionTimeout = DEFAULT_NON_INTERACTION_TIMEOUT;	// the length of time after the last user interaction to dispatch an event
	
	private int mMaxGraphicCount = DEFAULT_MAX_GRAPHIC_COUNT;				// the maximum number of graphics permitted to be added
	
	private final Handler mUiThreadHandler = new Handler();					// handler for the ui thread
	
	private boolean mAutoPinGraphics;				// whether or not only the most recently interacted graphic should be active (all others pinned)
	private boolean mDrawGraphics;					// whether or not to draw graphics on the tabletop (during the update loop)
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constants
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private static final int DEFAULT_MAX_GRAPHIC_COUNT = 10;
	private static final long DEFAULT_NON_INTERACTION_TIMEOUT = 1000;
	
	private static final boolean DEBUG = false;
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constructors
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public TabletopSurfaceView(Context context) {
		super(context);
		initialise();
	}
	
	public TabletopSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialise();
	}
	
	public TabletopSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialise();
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * initialise
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void initialise() {
		getHolder().addCallback(this);
		setFocusable(true);
		mCropRegionPaint = new Paint();
		mCropRegionPaint.setColor(0xFFFFFFFF);
		mCropRegionPaint.setStyle(Style.STROKE);
		
		setZOrderOnTop(true);
		getHolder().setFormat(PixelFormat.TRANSPARENT);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * touch bitmap
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public GraphicElement getTouchElement() {
		return mTouchElement;
	}
	
	public void setTouchElement(String asset) throws IOException {
		Bitmap bitmap = loadBitmap(getContext(), asset);
		setTouchElement(new BitmapGraphicElement(bitmap));
	}
	
	public void setTouchElement(GraphicElement element) {
		mTouchElement = element;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * interaction enabled
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public boolean isInteractionEnabled() {
		return mInteractionEnabled;
	}
	
	public void setInteractionEnabled(boolean enabled) {
		mInteractionEnabled = enabled;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * graphic count
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public int getGraphicCount() {
		return mBackgroundGraphics.size() + mForegroundGraphics.size();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * interaction count
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public int getInteractionCount() {
		return mInteractionCount;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * non interaction listener
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public void setOnNonInteractionListener(TabletopListener listener) {
		mTabletopListener = listener;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * autopin graphics
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public boolean getAutoPinGraphics() {
		return mAutoPinGraphics;
	}
	
	public void setAutoPinGraphics(boolean autopin) {
		mAutoPinGraphics = autopin;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * pin all graphics
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public synchronized void pinAllGraphics() {
		pinAllGraphics(null);
	}

	public synchronized void pinAllGraphics(List<TabletopGraphic> excluding) {
		for (TabletopGraphic graphic : mBackgroundGraphics)
			if (excluding == null || !excluding.contains(graphic))
				graphic.pin();
		for (TabletopGraphic graphic : mForegroundGraphics)
			if (excluding == null || !excluding.contains(graphic))
				graphic.pin();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on touch event
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	@Override public synchronized boolean onTouchEvent(MotionEvent event) {
		int action = JSAMotionEventUtil.getActionMasked(event);
		int pointerId = event.getPointerId(JSAMotionEventUtil.getActionIndex(event));
		
		// dispatch the cancel action to the active graphic if interaction is not enabled
		if (!mInteractionEnabled) {
			if (mActiveGraphic != null) mActiveGraphic.onTouchEvent(JSAMotionEventUtil.obtain(event, MotionEvent.ACTION_CANCEL));
			mActiveGraphic = null;
			return true;
		}
		
		// return if the event is from another pointer than the one currently editing
		if (mActiveGraphic != null && pointerId != mActiveGraphicPointerId) return true;
		
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
		if (mActiveGraphic != null) mActiveGraphic.onTouchEvent(event);
		mActiveGraphic = null;
		return true;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on touch event: down
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private synchronized boolean onTouchEventActionDown(MotionEvent event) {
		int graphicCount = mBackgroundGraphics.size() + mForegroundGraphics.size();
		int pointerIndex = JSAMotionEventUtil.getActionIndex(event);
		int pointerId = event.getPointerId(pointerIndex);
		
		// forward the event to the active graphic (if available)
		if (mActiveGraphic != null) { 
			boolean handle = mActiveGraphic.onTouchEvent(event);
			if (!handle) mActiveGraphic = null;
			return true;
		} 
		
		// forward the event to all foreground graphics, setting the active graphic to the first responder
		for (int i = mForegroundGraphics.size() - 1; i >= 0; i--) {
			TabletopGraphic graphic = mForegroundGraphics.get(i);
			if (!graphic.onTouchEvent(event)) continue;
			setActiveGraphic(graphic, pointerId);
			registerInteraction();
			return true;
		}
		
		// forward the event to all background graphics, setting the active graphic to the first responder
		for (int i = mBackgroundGraphics.size() - 1; i >= 0; i--) {
			TabletopGraphic graphic = mBackgroundGraphics.get(i);
			if (!graphic.onTouchEvent(event)) continue;
			setActiveGraphic(graphic, pointerId);
			registerInteraction();
			return true;
		}
		
		// return if there is no graphic to add
		if (mTouchElement == null) return true;
		
		// return if the maximum number of graphics have been added
		if (graphicCount >= mMaxGraphicCount) return true;
		
		// create and add a new graphic to the surface
		PointF center = new PointF(event.getX(pointerIndex), event.getY(pointerIndex));
		TabletopGraphic graphic = addGraphic(mTouchElement, center, false);
		
		// set the newly added graphic as the active graphic (if possible)
		boolean handle = graphic.onTouchEvent(event);
		if (handle) setActiveGraphic(graphic, pointerId);
		if (handle) registerInteraction();
		return true;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on touch event: move
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private boolean onTouchEventActionMove(MotionEvent event) {
		if (mActiveGraphic == null) return true;
		boolean handle = mActiveGraphic.onTouchEvent(event);
		if (!handle) mActiveGraphic = null;
		return true;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on touch event: up
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private synchronized boolean onTouchEventActionUp(MotionEvent event) {
		if (mActiveGraphic == null) return true;
		mActiveGraphic.onTouchEvent(event);
		TabletopGraphic.State state = mActiveGraphic.getState();
		startNonInteractionTimeout();
		
		if (state.equals(TabletopGraphic.State.DELETED)) {
			mBackgroundGraphics.remove(mActiveGraphic);
			mForegroundGraphics.remove(mActiveGraphic);
		}
		
		if (state.equals(TabletopGraphic.State.PINNED) || state.equals(TabletopGraphic.State.DISABLED)) {
			mForegroundGraphics.remove(mActiveGraphic);
			mBackgroundGraphics.add(mActiveGraphic);
		}
		
		mActiveGraphic = null;
		return true;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * register interaction
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void registerInteraction() {
		mInteractionCount = mInteractionCount + 1;
		if (mTabletopListener != null) mTabletopListener.onInteraction(mInteractionCount);
		if (mNonInteractionRunnable != null) mUiThreadHandler.removeCallbacks(mNonInteractionRunnable);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * start non interaction timeout
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void startNonInteractionTimeout() {
		final int count = mInteractionCount;
		
		if (mNonInteractionRunnable != null) mUiThreadHandler.removeCallbacks(mNonInteractionRunnable); 
		else mNonInteractionRunnable = new Runnable() {
			@Override public void run() { if (mTabletopListener != null) mTabletopListener.onNonInteraction(count); }
		};
		
		mUiThreadHandler.postDelayed(mNonInteractionRunnable, mNonInteractionTimeout);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * set active graphic
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private synchronized void setActiveGraphic(TabletopGraphic graphic, int pointerId) {
		
		// pin all other graphics
		List<TabletopGraphic> others = new ArrayList<TabletopGraphic>();
		others.add(graphic);
		pinAllGraphics(others);
		
		// set the active graphic and pointer id
		mActiveGraphicPointerId = pointerId;
		mActiveGraphic = graphic;
		
		// move the graphic to the top of the foreground list
		mBackgroundGraphics.remove(graphic);
		mForegroundGraphics.remove(graphic);
		mForegroundGraphics.add(graphic);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * add graphic (bitmap)
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public synchronized TabletopGraphic addGraphic(Bitmap bitmap, int width) {
		return addGraphic(new BitmapGraphicElement(bitmap), width);
	}
	
	protected synchronized TabletopGraphic addGraphic(Bitmap bitmap, PointF center, boolean registerInteraction) {
		return addGraphic(new BitmapGraphicElement(bitmap), center, 0, registerInteraction);
	}
	
	protected synchronized TabletopGraphic addGraphic(Bitmap bitmap, PointF center, int width, boolean registerInteraction) {
		return addGraphic(new BitmapGraphicElement(bitmap), center, width, registerInteraction);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * add graphic (element)
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public synchronized TabletopGraphic addGraphic(GraphicElement element, int width) {
		int graphicCount = mBackgroundGraphics.size() + mForegroundGraphics.size();
		
		// return if the maximum number of graphics have been added
		if (graphicCount >= mMaxGraphicCount) return null;
		
		// return the newly added graphic at the center of the surface
		PointF center = new PointF(getWidth() / 2, getHeight() / 2);
		return addGraphic(element, center, width, true);
	}
	
	protected synchronized TabletopGraphic addGraphic(GraphicElement element, PointF center, boolean registerInteraction) {
		return addGraphic(element, center, 0, registerInteraction);
	}
	
	protected synchronized TabletopGraphic addGraphic(GraphicElement element, PointF center, int width, boolean registerInteraction) {
		if (mAutoPinGraphics) pinAllGraphics(); // pin all existing graphics if required
		TabletopGraphic graphic = newTabletopGraphic(element, mNextGraphicId++, center, width);
		graphic.setCropRegion(mCropRegion);
		graphic.setReactivatePinned(true);
		mForegroundGraphics.add(graphic);
		if (registerInteraction) registerInteraction();
		if (registerInteraction) startNonInteractionTimeout();
		return graphic;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * new tabletop graphic
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	protected synchronized TabletopGraphic newTabletopGraphic(GraphicElement element, int id, PointF center, int width) {
		return new TabletopGraphic(getContext(), id, element.getBitmap(), center, 0, width == 0 ? element.getBitmap().getWidth() : width);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * draw graphics
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public boolean getDrawGraphics() {
		return mDrawGraphics;
	}
	
	public void setDrawGraphics(boolean draw) {
		mDrawGraphics = draw;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on draw
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	@Override public synchronized void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		// return if the canvas is invalid
		if (canvas == null) return;

		// clear the canvas
		canvas.drawColor(0, PorterDuff.Mode.CLEAR);
		
		// draw the crop region
		if (mCropRegion != null && mCropRegionPaint != null) canvas.drawRect(mCropRegion, mCropRegionPaint);
		
		// draw the background region (debug)
		if (DEBUG) canvas.drawColor(0x22FFFFFF);
		
		// return if no drawing is required
		if (!mDrawGraphics) return;
			
		// draw the background graphics
		for (TabletopGraphic graphic : mBackgroundGraphics) 
			graphic.onDraw(canvas, null);
		
		// draw the foreground graphics
		for (TabletopGraphic graphic : mForegroundGraphics) 
			graphic.onDraw(canvas, null);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * surface holder callbacks
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	@Override public void surfaceCreated(SurfaceHolder holder) {
		if (mThread != null) mThread.setRunning(false);
		mThread = new TabletopThread(this);
		mThread.setRunning(true);
		mThread.start();
	}

	@Override public void surfaceDestroyed(SurfaceHolder holder) {
		if (!mThread.isRunning()) return;
		mThread.setRunning(false);
		mThread = null;
	}

	@Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * tabletop thread
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	/* package */ class TabletopThread extends Thread {
		private final TabletopSurfaceView mView;
		private boolean mIsRunning;

		public TabletopThread(TabletopSurfaceView view) {
			mView = view;
		}

		public boolean isRunning() {
			return mIsRunning;
		}
		
		public void setRunning(boolean running) {
			mIsRunning = running;
		}

		@Override public void run() {
			SurfaceHolder holder = mView.getHolder();
			Canvas canvas = null;
			while (mIsRunning) {
				try {
					canvas = holder.lockCanvas(null);
					mView.onDraw(canvas);
				} finally {
					if (canvas != null) holder.unlockCanvasAndPost(canvas);
				}
			}
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * load bitmap
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private static Bitmap loadBitmap(Context context, String graphic) throws IOException {
		InputStream is = context.getAssets().open(graphic, AssetManager.ACCESS_RANDOM);
		ByteArrayOutputStream stream = JSAFileUtil.readFileStream(is);
		byte[] bytes = stream.toByteArray();
		return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);		
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * draw on bitmap
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/** 
	 * Draw the tabletop graphics on the given bitmap. 
	 * Scale the graphics on the surface appropriately to match the given bitmap.
	 * Scale the graphics such that the graphics are drawn on the given bitmap in the center of the surface.
	 * Return the modified bitmap.
	 */
	public synchronized Bitmap drawOnBitmap(Bitmap bitmap, boolean recycle) {
		if (bitmap == null) throw new IllegalArgumentException();
		
		// copy and and recycle the bitmap if not mutable
		if (!bitmap.isMutable()) {
			Bitmap originalBitmap = bitmap;
			bitmap = bitmap.copy(bitmap.getConfig(), true);
			if (recycle) originalBitmap.recycle();
		}
		
		// cache values used during drawing
		float scale = Math.min(getWidth() / (float) bitmap.getWidth(), getHeight() / (float) bitmap.getHeight());
		float offsetX = -(getWidth() - bitmap.getWidth() * scale) / 2;
		float offsetY = -(getHeight() - bitmap.getHeight() * scale) / 2;
		Canvas canvas = new BitmapCanvas(bitmap);
		float inverseScale = 1 / scale;
		
		// draw the background graphics
		for (TabletopGraphic graphic : mBackgroundGraphics)
			TabletopGraphic.onDrawScaled(graphic, canvas, inverseScale, inverseScale, offsetX, offsetY);

		// draw the foreground graphics
		for (TabletopGraphic graphic : mForegroundGraphics)
			TabletopGraphic.onDrawScaled(graphic, canvas, inverseScale, inverseScale, offsetX, offsetY);
		
		
		return bitmap;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * tabletop listener
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static interface TabletopListener {
		void onInteraction(int interactionCount);
		void onNonInteraction(int interactionCount);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * graphic element
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static interface GraphicElement {
		Bitmap getBitmap();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * bitmap graphic element
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static class BitmapGraphicElement implements GraphicElement {
		private final Bitmap mBitmap;
		
		public BitmapGraphicElement(Bitmap bitmap) {
			mBitmap = bitmap;
		}

		@Override public Bitmap getBitmap() {
			return mBitmap;
		}
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * bitmap canvas
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static class BitmapCanvas extends Canvas {
		private final Bitmap mBitmap;
		
		public BitmapCanvas(Bitmap bitmap) {
			super(bitmap);
			mBitmap = bitmap;
		}
		
		public Bitmap getBitmap() {
			return mBitmap;
		}
	}
	
}
