package pr.sna.snaprkitfx;

import pr.sna.snaprkitfx.SnaprImageEditFragment.CropInformation;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class SnaprImageView extends ImageView implements OnTouchListener {

	private ScaleGestureDetector mScaleDetector;
	
	private CropInformation mCropInformation;
	private float mLastTouchX, mLastTouchY, mLastPivotX, mLastPivotY;
	
	private float[] mGrid = null;
	private Paint mGridPaint;
	
	public SnaprImageView(Context context) {
		super(context);
		init(context);
	}

	public SnaprImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public SnaprImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	public void setCropInformation(CropInformation cropInformation) {
		mCropInformation = cropInformation;
	}

	private void init(Context context) {
		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		mGridPaint = new Paint();
		mGridPaint.setColor(0xFFFFFFFF);
		mGridPaint.setStyle(Style.STROKE);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
		
		switch (event.getAction()) { 
	    case MotionEvent.ACTION_DOWN: {
	        final float x = event.getX();
	        final float y = event.getY();
	            
	        // Remember where we started (for dragging)
	        mLastTouchX = x;
	        mLastTouchY = y;
	        break;
	    }
	            
	    case MotionEvent.ACTION_MOVE: {
	    	if(mCropInformation == null) break;
	        final float x = event.getX();
	        final float y = event.getY();
	            
	        // Calculate the distance moved
	        float dx = x - mLastTouchX;
	        float dy = y - mLastTouchY;
	        
	        dx = mCropInformation.getScaleFactor()*5*dx;
	        dy = mCropInformation.getScaleFactor()*5*dy;

	        if(event.getPointerCount() == 1) {
	        	mCropInformation.setPivotX(mCropInformation.getPivotX() - dx);
	        	mCropInformation.setPivotY(mCropInformation.getPivotY() - dy);
	        }

	        invalidate();

	        // Remember this touch position for the next move event
	        mLastTouchX = x;
	        mLastTouchY = y;

	        break;
	    }
	    case MotionEvent.ACTION_UP: {
	    	if(mCropInformation == null) break;
    		mLastPivotX = mCropInformation.getPivotX();
    		mLastPivotY = mCropInformation.getPivotY();
	    	
	    	break;
	    }
	    }   
		
		return super.onTouchEvent(event);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if(mCropInformation != null) {
			//Perform the cropping/scale attributes on the image
			if(mCropInformation.getPivotX() < 0) mCropInformation.setPivotX(0);
			else if (mCropInformation.getPivotX() > canvas.getWidth()) mCropInformation.setPivotX(canvas.getWidth());
			if(mCropInformation.getPivotY() < 0) mCropInformation.setPivotY(0);
			else if (mCropInformation.getPivotY() > canvas.getHeight()) mCropInformation.setPivotY(canvas.getHeight());
	
		    canvas.save();
		    canvas.scale(mCropInformation.getScaleFactor(), mCropInformation.getScaleFactor(), mCropInformation.getPivotX(), mCropInformation.getPivotY());
		}
	    super.onDraw(canvas);
	    
	    canvas.restore();
	    
	    if(mGrid == null) {
	    	mGrid = new float[] { 0.0f, canvas.getHeight()/3, canvas.getWidth(), canvas.getHeight()/3, 
	    						  0.0f, (canvas.getHeight()/3)*2, canvas.getWidth(), (canvas.getHeight()/3)*2,
	    						  canvas.getWidth()/3, 0.0f, canvas.getWidth()/3, canvas.getHeight(),
	    						  (canvas.getWidth()/3)*2, 0.0f, (canvas.getWidth()/3)*2, canvas.getHeight() };
	    }
	    
	    canvas.drawLines(mGrid, 0, 16, mGridPaint);
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			if(mCropInformation == null) return true;
			
			float scaleFactor = mCropInformation.getScaleFactor();
			float pivotX = mCropInformation.getPivotX();
			float pivotY = mCropInformation.getPivotY();
			
			scaleFactor *= detector.getScaleFactor();

			// Don't let the object get too small or too large.
			scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 3.0f));
			
			if(pivotX == 0 && pivotY == 0) {
				pivotX = detector.getFocusX();
				pivotY = detector.getFocusY();
			} else {
				pivotX = mLastPivotX + (detector.getFocusX() - mLastPivotX)/scaleFactor;
				pivotY = mLastPivotY + (detector.getFocusY() - mLastPivotY)/scaleFactor;
			}
			
			mCropInformation.setPivotX(pivotX);
			mCropInformation.setPivotY(pivotY);
			mCropInformation.setScaleFactor(scaleFactor);

			invalidate();
			return true;
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return onTouchEvent(event);
	}

}
