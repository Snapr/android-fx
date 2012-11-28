package pr.sna.snaprkit.tabletop.sticker;

import pr.sna.snaprkit.SnaprStickerUtil.Sticker;
import pr.sna.snaprkit.tabletop.TabletopGraphic;
import pr.sna.snaprkit.tabletop.TabletopSurfaceView;
import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;

public class StickerTabletopSurfaceView extends TabletopSurfaceView {

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constructors
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public StickerTabletopSurfaceView(Context context) {
		super(context);
	}
	
	public StickerTabletopSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public StickerTabletopSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * new tabletop graphic
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	@Override protected synchronized TabletopGraphic newTabletopGraphic(GraphicElement element, int id, PointF center, int width) {
		if (!(element instanceof Sticker)) return super.newTabletopGraphic(element, id, center, width);
		Sticker sticker = (Sticker) element;
		return new StickerTabletopGraphic(getContext(), id, sticker, center, 0, width == 0 ? element.getBitmap().getWidth() : width);
	}

}
