package pr.sna.snaprkitfx.tabletop.sticker;

import pr.sna.snaprkitfx.SnaprImageEditFragmentUtil;
import pr.sna.snaprkitfx.SnaprFilterUtil.BlendingMode;
import pr.sna.snaprkitfx.SnaprStickerUtil.Sticker;
import pr.sna.snaprkitfx.effect.CompositeEffect;
import pr.sna.snaprkitfx.tabletop.TabletopGraphic;
import pr.sna.snaprkitfx.tabletop.TabletopSurfaceView.BitmapCanvas;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;

public class StickerTabletopGraphic extends TabletopGraphic {
	private final Sticker mSticker;
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constructors
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public StickerTabletopGraphic(Context context, int id, Sticker sticker) {
		this(context, id, sticker, new PointF());
	}

	public StickerTabletopGraphic(Context context, int id, Sticker sticker, PointF center) {
		this(context, id, sticker, center, 0, sticker.getBitmap().getWidth(), sticker.getBitmap().getHeight());
	}
	
	public StickerTabletopGraphic(Context context, int id, Sticker sticker, PointF center, float rotation) {
		this(context, id, sticker, center, rotation, sticker.getBitmap().getWidth(), sticker.getBitmap().getHeight());
	}
	
	public StickerTabletopGraphic(Context context, int id, Sticker sticker, PointF center, float rotation, float width) {
		this(context, id, sticker, center, rotation, width, width / sticker.getBitmap().getWidth() * sticker.getBitmap().getHeight());
	}
	
	public StickerTabletopGraphic(Context context, int id, Sticker sticker, PointF center, float rotation, float width, float height) {
		this(context, id, sticker, center.x, center.y, rotation, width, height);
	}
	
	public StickerTabletopGraphic(Context context, int id, Sticker sticker, float centerX, float centerY, float rotation, float width, float height) {
		super(context, id, sticker.getBitmap(), centerX, centerY, rotation, width, height);
		mSticker = sticker;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * draw
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	@Override public void onDraw(Canvas canvas, DrawContext context) {
		boolean isBitmapCanvas = canvas instanceof BitmapCanvas;
		boolean hasBlendMode = false;
		
		// draw the sticker directly onto the canvas if no blending is required
		if (!isBitmapCanvas || !hasBlendMode) {
			super.onDraw(canvas, context);
			return;
		}
		
		// get the bitmap from the canvas
		BitmapCanvas bc = (BitmapCanvas) canvas;
		Bitmap bitmap = bc.getBitmap();
		
		// draw the sticker into a bitmap of the same size as the canvas
		Bitmap transparentBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), SnaprImageEditFragmentUtil.PREFERRED_BITMAP_CONFIG);
		Canvas transparentCanvas = new Canvas(transparentBitmap);
		super.onDraw(transparentCanvas, context);
		
		// draw the sticker into the canvas
		CompositeEffect.applyImageEffect(bitmap, transparentBitmap, 1f, BlendingMode.NORMAL.getCompositeBlendMode());
		
		// recycle the transparent bitmap
		transparentBitmap.recycle();
		transparentBitmap = null;
		transparentCanvas = null;
	}
	
}
