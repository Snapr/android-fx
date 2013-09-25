package pr.sna.snaprkitfx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nz.co.juliusspencer.android.JSADimensionUtil;
import nz.co.juliusspencer.android.JSAObjectUtil;
import nz.co.juliusspencer.android.JSATimeUtil;
import nz.co.juliusspencer.android.JSATuple;
import pr.sna.snaprkitfx.SnaprFilterUtil.Filter;
import pr.sna.snaprkitfx.SnaprFilterUtil.FilterPack;
import pr.sna.snaprkitfx.SnaprFilterUtil.OnImageLoadListener;
import pr.sna.snaprkitfx.SnaprImageEditFragmentActivity.LaunchMode;
import pr.sna.snaprkitfx.SnaprStickerUtil.Sticker;
import pr.sna.snaprkitfx.SnaprStickerUtil.StickerPack;
import pr.sna.snaprkitfx.tabletop.TabletopSurfaceView;
import pr.sna.snaprkitfx.tabletop.TabletopSurfaceView.TabletopListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SnaprImageEditFragment extends Fragment implements TabletopListener {
	private static int STICKER_PACK_1 = 0;
	private static int STICKER_PACK_2 = 1;
	
	private FragmentListener mFragmentListener;

	private FilterPack mFilterPack;
	private List<StickerPack> mStickerPacks;
	
	private File mOriginalFile;					// the location of the original, unscaled image
	private File mSaveFile;						// the location to save the modified image to
	
	private Bitmap mBaseBitmap;								// the original, scaled bitmap (possibly with an effect applied)
	private Bitmap mComposedBitmap;							// the original, scaled bitmap (possibly with stickers and an effect applied)
	private ComposeBitmapAsyncTask mComposeBitmapAsyncTask;	// the currently running compose bitmap task
	
	private Filter mBaseBitmapFilter;						// the effect currently applied to the base bitmap
	private int mComposedBitmapInteractionCount;			// the interaction count of the tabletop when the composed bitmap was created
	
	private Filter mAppliedFilter;
	private Sticker mLastAppliedSticker;
	private InteractionState mInteractionState = InteractionState.SHOWING_FILTERS;	// the current state of the fragment interaction
	private String mLaunchStickerPackSlug;
	private int mCurrentStickerPack;
	
	private String mFilterPackLocation = FILTER_PACK_PATH_DEFAULT;		// the location (under assets) where the filter packs will be loaded from
	private List<String> mStickerPackLocations;							// the locations (under assets) where the sticker packs will be loaded from
	
	private float mImageAspectRatio = 1.0f;								// The desired image aspect ratio for the image
	
	private Handler mUiThreadHandler;			// handler to run actions on the ui thread
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * runnable
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private Runnable mHideStickerLockMessageRunnable;
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * views
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private boolean mViewsInitialised;
	
	private ImageView mEditedImageView;
	private TabletopSurfaceView mTabletop;
	private TextView mMessageTextView;

	private View mFilterContainerRoot;
	private ViewGroup mFilterContainer;
	private View mFilterButton;
	private View mButtonDivider;
	
	private ViewGroup mStickerContainer;
	private ViewGroup mSticker2Container;
	private View mStickerContainerRoot;
	private View mSticker2ContainerRoot;
	private View mStickerButton;
	private View mSticker2Button;
	
	private View mCancelButton;
	private View mNextButton;
	
	private ViewTreeObserver.OnGlobalLayoutListener mMessageTextViewLayoutListener;
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constants
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private static final boolean DEBUG = false;
	
	// default sticker & filter paths
	private static final String FILTER_PACK_PATH_DEFAULT = "filter_packs/defaults";
	private static final String STICKER_PACK_PATH_DEFAULT = "sticker_packs/defaults";
	
	// the canvas size stickers have been created for (and will be scaled for)
	private static final float TARGET_CANVAS_SIZE_PX = 800;

	// the time a sticker lock message will display (after which a short fade out animation occurs)
	private static final long HIDE_LOCK_MESSAGE_DELAY_MS = 4000;
	
	private static enum InteractionState {
		SHOWING_FILTERS,
		SHOWING_STICKERS
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on create view
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.snaprkitfx_edit_fragment, container, false);
	}

	@Override public void onActivityCreated(Bundle savedInstanceState) {
		Bundle extras = getActivity().getIntent().getExtras();
		super.onActivityCreated(savedInstanceState);
		super.setRetainInstance(true);
		mUiThreadHandler = new Handler();
		
		mFilterPackLocation = extras.getString(SnaprImageEditFragmentActivity.EXTRA_FILTER_PACK_PATH);
		mStickerPackLocations = extras.getStringArrayList(SnaprImageEditFragmentActivity.EXTRA_STICKER_PACK_PATHS);
		
		if (mFilterPackLocation == null) mFilterPackLocation = FILTER_PACK_PATH_DEFAULT;
		if (mStickerPackLocations == null)
		{
			mStickerPackLocations = new ArrayList<String>();
			mStickerPackLocations.add(STICKER_PACK_PATH_DEFAULT);
		}
		
		mImageAspectRatio = extras.getFloat(SnaprImageEditFragmentActivity.EXTRA_IMAGE_ASPECT_RATIO);
		if (mImageAspectRatio == 0) mImageAspectRatio = 1.0f;  
		
		mButtonDivider = getView().findViewById(R.id.button_divider);
		mEditedImageView = (ImageView) getView().findViewById(R.id.edited_image);
		mTabletop = (TabletopSurfaceView) getView().findViewById(R.id.tabletop);
		mMessageTextView = (TextView) getView().findViewById(R.id.message_textview);
		initialiseMessageTextView();
		
		mFilterContainer = (ViewGroup) getView().findViewById(R.id.filter_container);
		mStickerContainer = (ViewGroup) getView().findViewById(R.id.sticker_container);
		mSticker2Container = (ViewGroup) getView().findViewById(R.id.sticker2_container);
		
		mCancelButton = getView().findViewById(R.id.cancel_button);
		mCancelButton.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				mFragmentListener.onCancel();
			}
		});

		mNextButton = getView().findViewById(R.id.next_button);
		mNextButton.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				onNextButtonClick();
			}
		});
		
		// cache the filter and sticker container root views
		mFilterContainerRoot = getView().findViewById(R.id.filter_container_root);
		mStickerContainerRoot = getView().findViewById(R.id.sticker_container_root);
		mSticker2ContainerRoot = getView().findViewById(R.id.sticker2_container_root);
		
		// handle filter button clicks by showing the filters
		mFilterButton = getView().findViewById(R.id.filter_button);
		mFilterButton.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				onFilterButtonClick();
			}
		});
		
		// handle sticker button clicks by showing the stickers
		mStickerButton = getView().findViewById(R.id.sticker_button);
		mStickerButton.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				onStickerButtonClick(STICKER_PACK_1);
			}
		});
		
		// handle sticker button clicks by showing the stickers
		mSticker2Button = getView().findViewById(R.id.sticker2_button);
		mSticker2Button.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				onStickerButtonClick(STICKER_PACK_2);
			}
		});
		
		// initialise the tabletop
		mTabletop.setTabletopListener(this);
		mTabletop.setAutoPinGraphics(true);
		
		final String saveFile = extras.getString(SnaprImageEditFragmentActivity.EXTRA_OUTPUT);
		final String originalFile = getActivity().getIntent().getStringExtra(SnaprImageEditFragmentActivity.EXTRA_FILEPATH);
		final boolean isPhotoTaken = getActivity().getIntent().getBooleanExtra(SnaprImageEditFragmentActivity.EXTRA_TOOK_PHOTO, true);
		final long photoTimestamp = getActivity().getIntent().getLongExtra(SnaprImageEditFragmentActivity.EXTRA_TOOK_PHOTO_TIMESTAMP, -1);
		
		// determine launch mode (as exposed to the parent app); internally known as interaction state
		String launchModeString = getActivity().getIntent().getStringExtra(SnaprImageEditFragmentActivity.EXTRA_LAUNCH_MODE);
		LaunchMode launchMode = launchModeString != null ? LaunchMode.valueOf(launchModeString) : LaunchMode.FILTERS;
		if (launchMode == LaunchMode.FILTERS)
			mInteractionState = InteractionState.SHOWING_FILTERS;
		else
		{
			mInteractionState = InteractionState.SHOWING_STICKERS;
			mLaunchStickerPackSlug = getActivity().getIntent().getStringExtra(SnaprImageEditFragmentActivity.EXTRA_LAUNCH_STICKER_PACK);
		}
		
		// throw an exception if the original file is missing
		if (originalFile == null) throw new IllegalArgumentException("original file must not be null");
		
		// throw an exception if the save file is missing
		if (saveFile == null) throw new IllegalArgumentException("save file must not be null");
		
		// cache values of the original and save files
		mOriginalFile = new File(originalFile);
		mSaveFile = new File(saveFile);
		
		// throw an exception if the photo was taken and the time stamp is missing
		if (isPhotoTaken && photoTimestamp == -1) throw new IllegalArgumentException("time stamp of image request missing!");
		
		// tell the media scanner about the new file if the photo was taken, otherwise prepare the image immediately
		if (isPhotoTaken) MediaScannerConnection.scanFile(getActivity().getApplicationContext(), new String[] { originalFile }, null,
				new MediaScannerConnection.OnScanCompletedListener() {
						public void onScanCompleted(String path, Uri uri) { onOriginalBitmapAvailable(originalFile, photoTimestamp); } });
		else onOriginalBitmapAvailable(originalFile, photoTimestamp);

		// initialise the effects and stickers (on a background thread)
		new LoadStickersFiltersAsyncTask().execute();
		
		mViewsInitialised = true;
		updateView();
	}
	
	private int findStickerPackIndexBySlug(List<StickerPack> stickerPacks, String slug, int defaultIndex)
	{
		if (stickerPacks == null) return defaultIndex;
		for (int i =0; i< stickerPacks.size(); i++)
		{
			if (stickerPacks.get(i).getSlug() != null && stickerPacks.get(i).getSlug().equals(slug))
			{
				return i;
			}
		}
		
		return defaultIndex;
	}
	
	private void initialiseMessageTextView() {
		// attach view tree observer so we can keep track of the bounds and ensure a square background
		mMessageTextViewLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override public void onGlobalLayout() { onMessageTextViewLayout(); } };
		mMessageTextView.getViewTreeObserver().addOnGlobalLayoutListener(mMessageTextViewLayoutListener);

		/* bit of an ugly hack to scale down text based on screen aspect ratio */
		JSATuple<Integer, Integer> dimens = JSADimensionUtil.getDefaultDisplayDimensions(getActivity());
		int screenWidth = Math.max(dimens.getA(), dimens.getB());
		int screenHeight = Math.min(dimens.getA(), dimens.getB());
		float screenAspectRatio = (float) screenWidth / screenHeight;
		float scale = screenAspectRatio > 1.67f ? 1 : screenAspectRatio > 1.49f ? 0.8f : 0.65f; 
		
		float defaultTextSize = getResources().getDimension(R.dimen.message_text_size);
		float defaultPadding = getResources().getDimension(R.dimen.message_text_padding);
		float defaultMargin = getResources().getDimension(R.dimen.message_container_margin);
		int padding = (int) (defaultPadding * scale);
		int margin = (int) (defaultMargin * scale * scale);
		mMessageTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, (defaultTextSize * scale));
		mMessageTextView.setPadding(padding, (int) (padding / scale / scale), padding, padding);
		((MarginLayoutParams) mMessageTextView.getLayoutParams()).bottomMargin = margin;
		((MarginLayoutParams) mMessageTextView.getLayoutParams()).leftMargin = margin;
		((MarginLayoutParams) mMessageTextView.getLayoutParams()).rightMargin = margin;
		((MarginLayoutParams) mMessageTextView.getLayoutParams()).topMargin = margin;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * life cycle
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	@Override public void onDestroyView() {
		super.onDestroyView();
		if (mMessageTextViewLayoutListener != null) {
			mMessageTextView.getViewTreeObserver().removeGlobalOnLayoutListener(mMessageTextViewLayoutListener);
			mMessageTextViewLayoutListener = null;
		}
		
		// remove any pending runnables
		cancelHideStickerLockMessageRunnable(false);
	}
	
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on original available bitmap
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void onOriginalBitmapAvailable(String originalFilepath, long photoTimestamp) {
		mBaseBitmap = SnaprImageEditFragmentUtil.saveOriginalTempImage(getActivity(), originalFilepath, mImageAspectRatio, photoTimestamp);
		if (mBaseBitmap == null) {
			Toast.makeText(getActivity(), R.string.snaprkitfx_unable_to_load_image_try_another_, Toast.LENGTH_SHORT).show();
			mFragmentListener.onCancel();		
		} else {
			mFragmentListener.onAddAnalytic(SnaprImageEditFragmentActivity.ANALYTIC_PAGE_LOADED);
			mUiThreadHandler.post(new Runnable() { // run on ui thread to ensure initialisation from media scanner thread is correct 
				@Override public void run() {
					updateViewEditedImageView();
				}
			});
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on message textview layout
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void onMessageTextViewLayout() {
		final int width = mMessageTextView.getMeasuredWidth();
		final int height = mMessageTextView.getMeasuredHeight();
		if (width == 0 || height == 0) return;
		
		int dimen = Math.min(width, height);
		mMessageTextView.getLayoutParams().width = dimen;
		mMessageTextView.getLayoutParams().height = dimen;
		mMessageTextView.requestLayout();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * initialise effects
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private FilterPack inflateEffects(String filterPackLocation) {
		try { // load the filter packs from file
			return FilterPack.parse(getActivity().getApplicationContext(), filterPackLocation, false);
		} catch (Exception exception) {
			Log.e(SnaprImageEditFragment.class.getSimpleName(), "error inflating effects", exception);
			return null;
		}
	}
	
	private void initialiseEffectViews() {
		if (!isAdded() || !mViewsInitialised) return;
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		// create the thumbnail views for each effect
		for (Filter filter : mFilterPack.getFilters()) {
			View view = inflater.inflate(R.layout.snaprkitfx_effect_item, null);
			view.setTag(filter);
			
			ImageView image = (ImageView) view.findViewById(R.id.effect_imageview);
			if (filter.getThumbnail() != null) image.setImageBitmap(filter.getThumbnail());
			else image.setImageResource(R.drawable.snaprkitfx_ic_effect_placeholder);
			image.setTag(filter);

			TextView tv = ((TextView) view.findViewById(R.id.effect_textview));
			tv.setText(filter.getName());

			image.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View view) {
					onEffectClick(view);
				}
			});

			mFilterContainer.addView(view);
		}
	}
	
	private void updateEffectView(Filter filter) {
		View view = mFilterContainer.findViewWithTag(filter);
		if (view == null) return;
		ImageView image = (ImageView) view.findViewById(R.id.effect_imageview);
		if (filter.getThumbnail() != null) image.setImageBitmap(filter.getThumbnail());
		else image.setImageResource(R.drawable.snaprkitfx_ic_effect_placeholder);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * initialise stickers
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private StickerPack inflateStickers(String stickerPackLocation) {
		try { // load the sticker packs from file
			return StickerPack.parse(getActivity().getApplicationContext(), stickerPackLocation, false);
		} catch (Exception exception) {
			Log.e(SnaprImageEditFragment.class.getSimpleName(), "error inflating stickers", exception);
			return null;
		}
	}
	
	private int getNumVisibleStickerPacks(List<StickerPack> stickerPacks)
	{
		int numVisible = 0;
		
		for (StickerPack stickerPack : stickerPacks)
		{
			if (stickerPack.getSettings().isVisible()) numVisible++;
		}
		
		return numVisible;
	}
	
	private void initialiseStickerPackViews(int stickerPackIndex, StickerPack stickerPack) {
		LayoutInflater inflater = getActivity().getLayoutInflater();

		// create the thumbnail views for each sticker		
		for (Sticker sticker : stickerPack.getStickers()) {
			View view = inflater.inflate(R.layout.snaprkitfx_effect_item, null);
			view.setTag(sticker);
			
			ImageView image = (ImageView) view.findViewById(R.id.effect_imageview);
			if (sticker.getThumbnail() != null) image.setImageBitmap(sticker.getThumbnail());
			else image.setImageResource(R.drawable.snaprkitfx_ic_sticker_placeholder);
			image.setTag(sticker);
			
			TextView tv = ((TextView) view.findViewById(R.id.effect_textview));
			tv.setText(sticker.getName());
			
			image.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View view) {
					onStickerClick(view);
				}
			});
			
			if (stickerPackIndex == 0)
				mStickerContainer.addView(view);
			else
				mSticker2Container.addView(view);
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 *  update sticker
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void updateLastAppliedSticker() {
		boolean updateView = mLastAppliedSticker != null && mLastAppliedSticker.getSettings().isLocked(); 
		mLastAppliedSticker = null;
		if (updateView) updateView();
	}
	
	private void updateStickerView(int stickerPack, Sticker sticker) {
		View view = null;
		if (stickerPack == 0)
		{
			view = mStickerContainer.findViewWithTag(sticker);
		}
		else
		{
			view = mSticker2Container.findViewWithTag(sticker);
		}
		if (view == null) return;
		ImageView image = (ImageView) view.findViewById(R.id.effect_imageview);
		view.setVisibility(sticker.getSettings().isVisible() ? View.VISIBLE : View.GONE);
		if (sticker.getThumbnail() != null) image.setImageBitmap(sticker.getThumbnail());
		else image.setImageResource(R.drawable.snaprkitfx_ic_sticker_placeholder);
		view.findViewById(R.id.locked_overlay_imageview).setVisibility(sticker.getSettings().isLocked() ? View.VISIBLE : View.INVISIBLE);
	}
	
	private boolean hasFilters()
	{
		return (mFilterPack!=null &&  mFilterPack.getFilters()!=null && mFilterPack.getFilters().size()!=0);
	}
	
	private boolean hasStickers()
	{
		if (mStickerPacks == null) return false;
		
		for (StickerPack stickerPack: mStickerPacks)
		{
			if (stickerPack != null && stickerPack.getStickers() != null && stickerPack.getStickers().size() > 0) return true;
		}
		
		return false;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 *  update view
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void updateView() {
		if (!isAdded() || !mViewsInitialised) return;
		boolean isShowingFilters = mInteractionState.equals(InteractionState.SHOWING_FILTERS);
		boolean isShowingStickers1 = !isShowingFilters && (mCurrentStickerPack == 0);
		boolean isShowingStickers2 = !isShowingFilters && (mCurrentStickerPack == 1);
		boolean isFilterLocked = mAppliedFilter != null ? mAppliedFilter.getSettings().isLocked() : false;
		boolean isStickerLocked = mLastAppliedSticker != null ? mLastAppliedSticker.getSettings().isLocked() : false;
		boolean hasStickers = hasStickers();
		boolean hasFilters = hasFilters();
		int numVisibleStickerPacks = hasStickers?getNumVisibleStickerPacks(mStickerPacks):0;
		boolean hasStickers1 = (numVisibleStickerPacks > 0);
		boolean hasStickers2 = (numVisibleStickerPacks > 1);

		// reset last selected sticker if we're not displaying stickers anymore
		if (isShowingFilters && mLastAppliedSticker != null) mLastAppliedSticker = null;
		
		// update the filter or sticker containers
		mFilterContainerRoot.setVisibility(hasFilters && isShowingFilters ? View.VISIBLE : View.GONE);
		mStickerContainerRoot.setVisibility(hasStickers1 && isShowingStickers1 ? View.VISIBLE : View.GONE);
		mSticker2ContainerRoot.setVisibility(hasStickers2 && isShowingStickers2 ? View.VISIBLE : View.GONE);
		
		// update the filter and sticker buttons visibility and section
		mFilterButton.setVisibility(hasFilters && hasStickers ? View.VISIBLE : View.GONE);
		mFilterButton.setSelected(isShowingFilters);
		mStickerButton.setVisibility(hasStickers && hasFilters? View.VISIBLE : View.GONE);
		mStickerButton.setSelected(isShowingStickers1);
		mSticker2Button.setVisibility(hasStickers2 && hasFilters? View.VISIBLE : View.GONE);
		mSticker2Button.setSelected(isShowingStickers2);
		
		// update the filter and sticker buttons backgrounds
		if (numVisibleStickerPacks == 2)
		{
			mFilterButton.setBackgroundResource(R.drawable.snaprkitfx_btn_3btn_filter);
			mStickerButton.setBackgroundResource(R.drawable.snaprkitfx_btn_3btn_sticker);
			mSticker2Button.setBackgroundResource(R.drawable.snaprkitfx_btn_3btn_sticker2);
		}
		else if (numVisibleStickerPacks == 1)
		{
			mFilterButton.setBackgroundResource(R.drawable.snaprkitfx_btn_filter);
			mStickerButton.setBackgroundResource(R.drawable.snaprkitfx_btn_sticker);
		}
		
		// hide the divider if only one of two buttons is shown
		mButtonDivider.setVisibility(hasFilters && hasStickers ? View.VISIBLE : View.GONE);
		
		// update the locked message
		if (mAppliedFilter != null && isFilterLocked) setFilterLockMessage(mAppliedFilter.getSettings().getUnlockMessage());
		if (mLastAppliedSticker != null && isStickerLocked) setStickerLockMessage(mLastAppliedSticker.getSettings().getUnlockMessage());
		mMessageTextView.setVisibility(isFilterLocked || isStickerLocked ? View.VISIBLE : View.INVISIBLE);
		
		// update the next button
		boolean isNextButton = hasStickers && hasFilters && isShowingFilters;
		int resourceId = isNextButton ? R.drawable.snaprkitfx_btn_next : R.drawable.snaprkitfx_btn_tick;
		mNextButton.setBackgroundResource(resourceId);
		mNextButton.setEnabled(isNextButton || (!isFilterLocked && !isStickerLocked));
		
		// show or hide the tabletop (prevent showing the tabletop when rendering the final image)
		boolean isImageVisible = mEditedImageView.getVisibility() == View.VISIBLE;
		mTabletop.setVisibility(isShowingFilters || !isImageVisible ? View.GONE : View.VISIBLE);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 *  update view: effects
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void updateViewEffects() {
		if (mFilterPack == null || mFilterPack.getFilters() == null) return;
		for (Filter filter : mFilterPack.getFilters()) {
			View view = mFilterContainer.findViewWithTag(filter);
			if (view == null) return;
			view.setVisibility(filter.getSettings().isVisible() ? View.VISIBLE : View.GONE);
			view.findViewById(R.id.chosen_imageview).setSelected(filter.equals(mAppliedFilter));
			view.findViewById(R.id.locked_overlay_imageview).setVisibility(filter.getSettings().isLocked() ? View.VISIBLE : View.INVISIBLE);
		}
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * update view: edited image view
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void updateViewEditedImageView() {
		boolean isShowingStickers = mInteractionState.equals(InteractionState.SHOWING_STICKERS);
		boolean areStickersUpToDate = mComposedBitmapInteractionCount == mTabletop.getInteractionCount();
		boolean areAllGraphicsPinned = mTabletop.getPinnedGraphicCount() == mTabletop.getGraphicCount();
		boolean preferComposed = !isShowingStickers || (areStickersUpToDate && areAllGraphicsPinned);
		boolean showComposed = preferComposed && mComposedBitmap != null && mComposedBitmap != mBaseBitmap;
		mEditedImageView.setImageBitmap(showComposed ? mComposedBitmap : mBaseBitmap);
		mTabletop.setDrawGraphics(!showComposed);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * update view: progress
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void updateViewProgress() {
		boolean isShowingFilters = mInteractionState.equals(InteractionState.SHOWING_FILTERS);
		boolean areAllGraphicsPinned = mTabletop.getPinnedGraphicCount() == mTabletop.getGraphicCount();
		boolean isComposingBitmap = mComposeBitmapAsyncTask != null;
		
		// show or hide the indeterminate progress (shown when composing the bitmap on the filters or waiting on a composition on the stickers)
		boolean showProgress = isComposingBitmap && (isShowingFilters || (!isShowingFilters && areAllGraphicsPinned));
		if (showProgress) onShowProgressUnblocking();
		else onHideProgressUnblocking();

	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * update lock messages
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void setFilterLockMessage(String message) {
		mMessageTextView.setText(message);
	}
	
	private void setStickerLockMessage(String message) {
		mMessageTextView.setText(message);
		if (mHideStickerLockMessageRunnable != null) mUiThreadHandler.removeCallbacks(mHideStickerLockMessageRunnable);
		mUiThreadHandler.postDelayed(buildHideStickerLockMessageRunnable(), HIDE_LOCK_MESSAGE_DELAY_MS);
	}
	
	private boolean isShowingLockMessage() {
		boolean showingFilterLockMessage = mAppliedFilter != null && mAppliedFilter.getSettings().isLocked();
		boolean showingStickerLockMessage = mLastAppliedSticker != null && mLastAppliedSticker.getSettings().isLocked() && mHideStickerLockMessageRunnable != null;
		return showingFilterLockMessage || showingStickerLockMessage;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * lock messages runnables / animation
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void hideFilterLockMessage(final boolean changeEffect) {
		Animation fadeOutAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
		fadeOutAnim.setAnimationListener(new Animation.AnimationListener() {
			@Override public void onAnimationStart(Animation animation) { }
			@Override public void onAnimationRepeat(Animation animation) { }
			@Override public void onAnimationEnd(Animation animation) {
				// TODO: Revisit default
				// reset applied filter to first / default and update view to reflect the change
				if (changeEffect) onEffectChange(mFilterPack.getFilters().get(0));
				updateView();
			}
		});
		mMessageTextView.startAnimation(fadeOutAnim);
	}
	
	private Runnable buildHideStickerLockMessageRunnable() {
		mHideStickerLockMessageRunnable = new Runnable() {
			@Override public void run() {
				Animation fadeOutAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
				fadeOutAnim.setAnimationListener(new Animation.AnimationListener() {
					@Override public void onAnimationStart(Animation animation) { }
					@Override public void onAnimationRepeat(Animation animation) { }
					@Override public void onAnimationEnd(Animation animation) {
						mHideStickerLockMessageRunnable = null;
						// reset last applied sticker and update view to reflect the change
						mLastAppliedSticker = null;
						updateView();
					}
				});
				mMessageTextView.startAnimation(fadeOutAnim);
			}
		};
		return mHideStickerLockMessageRunnable;
	}
	
	private void cancelHideStickerLockMessageRunnable(boolean runImmediate) {
		if (mHideStickerLockMessageRunnable == null) return;
		mUiThreadHandler.removeCallbacks(mHideStickerLockMessageRunnable);
		if (runImmediate) mHideStickerLockMessageRunnable.run();
		else mHideStickerLockMessageRunnable = null;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on filter button click
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void onFilterButtonClick() {
		// cancel any pending runnables and run them right away
		if (isShowingLockMessage()) cancelHideStickerLockMessageRunnable(true);
		mInteractionState = InteractionState.SHOWING_FILTERS;
		mTabletop.setInteractionEnabled(false);
		mTabletop.pinAllGraphics();
		updateViewEditedImageView();
		updateViewProgress();
		updateView();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on sticker button click
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void onStickerButtonClick(int stickerPackIndex) {
		// if a lock message is showing, are currently selected filter is not available
		// since we're switching to the stickers here, make sure we reset the active filter to the 'default'
		if (isShowingLockMessage()) hideFilterLockMessage(true);
		
		mInteractionState = InteractionState.SHOWING_STICKERS;
		mCurrentStickerPack = stickerPackIndex;
		mTabletop.setInteractionEnabled(true);
		updateViewEditedImageView();
		updateViewProgress();
		updateView();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on next button click
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private void onNextButtonClick() {
		boolean isShowingFilters = mInteractionState.equals(InteractionState.SHOWING_FILTERS);
		boolean hasStickers = hasStickers();
		boolean hasFilters = hasFilters();
		
		// hide filter lock message and change back to default filter 
		if (isShowingFilters && isShowingLockMessage()) hideFilterLockMessage(true);
		
		if (hasStickers && hasFilters && isShowingFilters) mInteractionState = InteractionState.SHOWING_STICKERS; // show the filters
		else {
			// save bitmap and change file
			new SaveEditedBitmapToFileAsyncTask().execute(); 
			// track event: graphics pinned
			mFragmentListener.onAddAnalytic(SnaprImageEditFragmentActivity.ANALYTIC_STICKERS_PINNED_EVENT);
		}
		updateViewEditedImageView();
		updateViewProgress();
		updateView();
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on effect click
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private void onEffectClick(View v) {
		Filter filter = (Filter) v.getTag();
		boolean filterChanged = !filter.equals(mAppliedFilter);
		// we're about to change filters already, so just get rid of the current lock message
		if (isShowingLockMessage() && filterChanged) hideFilterLockMessage(false);
		onEffectChange(filter);
	}
	
	private void onEffectChange(Filter filter) {
		if (filter.equals(mAppliedFilter)) return;
		mAppliedFilter = filter;
		composeBitmap(true);
		updateViewEffects();

		// track event
		mFragmentListener.onAddAnalytic(SnaprImageEditFragmentActivity.ANALYTIC_FILTER_ADDED_EVENT, filter.getSlug());
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on sticker click
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void onStickerClick(View v) {
		if (!isAdded()) return;
		Sticker sticker = (Sticker) v.getTag();
		if (sticker == null) return;
		
		// toast the user if the sticker assets are not yet available
		if (sticker.getImage() == null) {
			Toast toast = Toast.makeText(getActivity(), R.string.snaprkitfx_loading_sticker_please_wait, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
			toast.show();
			return;
		}

		// don't add stickers if the currently selected filter is locked
		if (mAppliedFilter != null && mAppliedFilter.getSettings().isLocked()) return;
		
		// if the sticker changed and the new sticker isn't locked, run fade out animation immediately
		boolean stickerChanged = !JSAObjectUtil.equals(mLastAppliedSticker, sticker);
		if (stickerChanged && !sticker.getSettings().isLocked()) cancelHideStickerLockMessageRunnable(true);
		
		// keep track of last selected sticker for unlock message
		mLastAppliedSticker = sticker;
		updateView();
		// don't add sticker if it's locked
		if (sticker.getSettings().isLocked()) return;
		
		// stickers are made for a 800x800px canvas - scale the sticker based on the actual size our canvas 
		int minImageLength = Math.min(mEditedImageView.getWidth(), mEditedImageView.getHeight());
		float minImageScale = minImageLength / (float) TARGET_CANVAS_SIZE_PX;
		mTabletop.addGraphic(sticker, (int) (sticker.getImage().getWidth() * minImageScale));
		
		// track event
		mFragmentListener.onAddAnalytic(SnaprImageEditFragmentActivity.ANALYTIC_STICKER_ADDED_EVENT, sticker.getSlug());
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * on progress unblocking
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void onShowProgressUnblocking() {
		if (mTabletop != null) mTabletop.setShowSpinner(true);
		if (mTabletop != null) mTabletop.setForceGraphicsBoundsBoxDraw(true);
		if (mFragmentListener != null) mFragmentListener.onShowProgressUnblocking();
	}
	
	private void onHideProgressUnblocking() {
		if (mTabletop != null) mTabletop.setShowSpinner(false);
		if (mTabletop != null) mTabletop.setForceGraphicsBoundsBoxDraw(false);
		if (mFragmentListener != null) mFragmentListener.onHideProgressUnblocking();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * tabletop listener
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	@Override public void onInteraction(int interactionCount) {
		updateViewEditedImageView();
		updateViewProgress();
		updateLastAppliedSticker();
	}
	
	@Override public void onNonInteraction(int interactionCount) {
		composeBitmap(false);
		updateViewProgress();
		updateLastAppliedSticker();
	}
	
	@Override public void onGraphicPinned() {
		composeBitmap(false);
		updateViewProgress();
		updateLastAppliedSticker();
		
		// track event: graphics pinned
		mFragmentListener.onAddAnalytic(SnaprImageEditFragmentActivity.ANALYTIC_STICKERS_PINNED_EVENT);
	}
	
	@Override public void onGraphicRemoved() {
		// track event: graphic removed
		mFragmentListener.onAddAnalytic(SnaprImageEditFragmentActivity.ANALYTIC_STICKER_REMOVED_EVENT);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * compose bitmap
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void composeBitmap(boolean showProgress) {
		if (mComposeBitmapAsyncTask != null) mComposeBitmapAsyncTask.cancel(true);
		mComposeBitmapAsyncTask = new ComposeBitmapAsyncTask(getActivity().getApplicationContext(), showProgress);
		mComposeBitmapAsyncTask.execute();
	}
	                              
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * compose bitmap async task
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private class ComposeBitmapAsyncTask extends AsyncTask<Void, Void, Bitmap> {
		private final Context mContext;
		private final boolean mShowProgress;
		
		public ComposeBitmapAsyncTask(Context context, boolean showProgress) {
			mContext = context;
			mShowProgress = showProgress;
		}

		@Override protected void onPreExecute() {
			if (mShowProgress) mFragmentListener.onShowProgressBlocking(getString(R.string.snaprkitfx_applying));
			super.onPreExecute();
		}
		
		@Override protected Bitmap doInBackground(Void... params) {
			Thread.currentThread().setName(Thread.currentThread().getName() + " [" + getClass().getSimpleName() + "]");
			try {
				
				// load the bitmap from the temporarily saved, resized bitmap
				Bitmap bitmap = SnaprImageEditFragmentUtil.loadTempImage();
				if (isCancelled()) return null;
				
				// generate the base bitmap by applying the effect (if required)
				if (!JSAObjectUtil.equals(mAppliedFilter, mBaseBitmapFilter)) {
					mBaseBitmap = bitmap;
					mBaseBitmapFilter = mAppliedFilter;
					if (mAppliedFilter != null) mAppliedFilter.apply(mContext, mBaseBitmap);
					if (mTabletop.getGraphicCount() == 0) return mBaseBitmap;
					bitmap = SnaprImageEditFragmentUtil.loadTempImage();
					if (isCancelled()) return null;
				}
				
				// draw the stickers and apply the effect
				bitmap = mTabletop.drawOnBitmap(bitmap, true);
				mComposedBitmapInteractionCount = mTabletop.getInteractionCount();
				
				if (isCancelled()) return null;
				if (mAppliedFilter != null) mAppliedFilter.apply(mContext, bitmap);
				if (isCancelled()) return null;
				return bitmap;
			} catch (IOException exception) {
				if (DEBUG) Log.e(SnaprImageEditFragment.class.getSimpleName(), "error applying effect", exception);
				return null;
			}
		}

		@Override protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if (mShowProgress) mFragmentListener.onHideProgressBlocking();
			if (result == null && !isCancelled()) Toast.makeText(mContext, R.string.snaprkitfx_problem_applying_effect, Toast.LENGTH_LONG).show();
			if (isCancelled() || result == null) return;
			mComposedBitmap = result;
			mComposeBitmapAsyncTask = null;
			updateViewEditedImageView();
			updateViewProgress();
			updateView();
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * save edited bitmap to file async task
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private class SaveEditedBitmapToFileAsyncTask extends SnaprImageEditFragmentUtil.SaveEditedBitmapToFileAsyncTask {

		public SaveEditedBitmapToFileAsyncTask() {
			super(getActivity(), mOriginalFile.getAbsolutePath(), mSaveFile.getAbsolutePath(), mImageAspectRatio, mAppliedFilter, mTabletop);
		}

		@Override protected void onPreExecute() {
			super.onPreExecute();
			mTabletop.setDrawGraphics(false);
			mFragmentListener.onShowProgressBlocking(getString(R.string.snaprkitfx_saving_));
			mBaseBitmap = null; // null bitmaps to release unused memory
			mComposedBitmap = null;
			mBaseBitmapFilter = null; // null effects to release unused memory
			mAppliedFilter = null;
			mFilterPack = null; // remove all references to effects and stickers
			mStickerPacks = null;
			updateViewEditedImageView();
			mEditedImageView.setVisibility(View.GONE);
			mTabletop.setVisibility(View.GONE);
			System.gc();
		}

		@Override protected void onPostExecute(JSATuple<File, Boolean> result) {
			super.onPostExecute(result);
			mFragmentListener.onHideProgressBlocking();
			if (result != null && result.getB()) mFragmentListener.onEditComplete(result.getA().getAbsolutePath());
			else Toast.makeText(getActivity(), R.string.snaprkitfx_problem_saving_check_storage, Toast.LENGTH_SHORT).show();
		}
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * load stickers filters async task
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private class LoadStickersFiltersAsyncTask extends AsyncTask<Void, Void, SnaprAssets> {
		
		@Override protected void onPreExecute() {
			super.onPreExecute();
			mFragmentListener.onShowProgressBlocking(getString(R.string.snaprkitfx_loading));
		}

		@Override protected SnaprAssets doInBackground(Void... params) {
			Thread.currentThread().setName(Thread.currentThread().getName() + " [" + getClass().getSimpleName() + "]");
			
			if (DEBUG) JSATimeUtil.logTime();
			
			FilterPack filterPack = inflateEffects(mFilterPackLocation);
			if (DEBUG) JSATimeUtil.logTime("filter packs inflated");
			
			ArrayList<StickerPack> stickerPacks = new ArrayList<StickerPack>();
			for (String stickerPackLocation:mStickerPackLocations)
			{
				StickerPack stickerPack = inflateStickers(stickerPackLocation);
				stickerPacks.add(stickerPack);
			}
			if (DEBUG) JSATimeUtil.logTime("sticker packs inflated");
			
			return new SnaprAssets(filterPack, stickerPacks);
		}
		
		@Override protected void onPostExecute(SnaprAssets result) {
			super.onPostExecute(result);
			mFragmentListener.onHideProgressBlocking();
			FilterPack filterPack = result.getFilterPack();
			ArrayList<StickerPack> stickerPacks = (ArrayList<StickerPack>) result.getStickerPacks();
			if (!isAdded()) return;

			// settings extra
			Bundle extras = getActivity().getIntent().getExtras();
			String configKey = SnaprImageEditFragmentActivity.EXTRA_EFFECT_SETTINGS;
			@SuppressWarnings("unchecked") Map<String, SnaprSetting> suppliedSettings = (Map<String, SnaprSetting>) extras.getSerializable(configKey);
			
			if (filterPack != null) {
				if (suppliedSettings != null) {
					// filter settings
					for (Filter filter : filterPack.getFilters()) {
						SnaprSetting settings = suppliedSettings.get(filter.getSlug());
						if (settings != null) filter.setSettings(settings);
					}
				}
				
				mFilterPack = filterPack;
				if (mFilterPack.getFilters().size() != 0) mAppliedFilter = mFilterPack.getFilters().get(0);
				initialiseEffectViews();
				updateViewEffects();
			}
			
			if (stickerPacks != null) {
				if (suppliedSettings != null) {
					
					for (StickerPack stickerPack : stickerPacks)
					{
						// sticker pack settings
						SnaprSetting settings = suppliedSettings.get(stickerPack.getSlug());
						if (settings != null) stickerPack.setSettings(settings);
						
						// sticker settings
						for (Sticker sticker : stickerPack.getStickers()) {
							settings = suppliedSettings.get(sticker.getSlug());
							if (settings != null) sticker.setSettings(settings);
						}
					}
				}
				
				mStickerPacks = stickerPacks;
				mCurrentStickerPack = findStickerPackIndexBySlug(mStickerPacks, mLaunchStickerPackSlug, 0);
				initialiseStickerPackViews(STICKER_PACK_1, mStickerPacks.get(STICKER_PACK_1));
				initialiseStickerPackViews(STICKER_PACK_2, mStickerPacks.get(STICKER_PACK_2));
			}

			new LoadStickerFilterImagesAsyncTask(getActivity()).execute();
			
			/*
			 * Don't touch interaction state here, because the desired state gets supplied as extra by the parent app. It is up
			 * to the parent app to ensure that the selected mode makes sense. In other words: if SHOWING_STICKERS is supplied,
			 * then it's the parent app's responsibility to make sure that stickers are available.
			 * 
			 * mInteractionState = filterPack != null ? InteractionState.SHOWING_FILTERS : InteractionState.SHOWING_STICKERS;
			 */
			
			updateViewEditedImageView();
			updateView();
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * load sticker filter images async task
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private class LoadStickerFilterImagesAsyncTask extends AsyncTask<Void, Void, Boolean> {
		private final Context mContext;
		
		public LoadStickerFilterImagesAsyncTask(Context context) {
			mContext = context.getApplicationContext();
		}
		
		@Override protected Boolean doInBackground(Void... params) {
			Thread.currentThread().setName(Thread.currentThread().getName() + " [" + getClass().getSimpleName() + "]");
			
			if (DEBUG) JSATimeUtil.logTime();
			
			for (Filter filter : mFilterPack.getFilters())
				filter.loadImagesNoException(mContext, mFilterPackLocation, new SimpleOnImageLoadListener());
			if (DEBUG) JSATimeUtil.logTime("filter pack images loaded");
			
			for (int i=0; i<mStickerPacks.size(); i++)
			{
				for (Sticker sticker : mStickerPacks.get(i).getStickers())
					sticker.loadImagesNoException(mContext, mStickerPackLocations.get(i), new SimpleOnImageLoadListener(i));
			}
			if (DEBUG) JSATimeUtil.logTime("sticker pack images loaded");
			
			return true;
		}
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * simple on image load listener
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private final class SimpleOnImageLoadListener implements OnImageLoadListener {
		private int mStickerPack;
		
		public SimpleOnImageLoadListener()
		{
			mStickerPack = 0;
		}
		
		public SimpleOnImageLoadListener(int stickerPack)
		{
			mStickerPack = stickerPack;
		}
		
		@Override public void onImageLoad(final Object parent, final Bitmap image) {
			if (parent instanceof Sticker) {
				mUiThreadHandler.post(new Runnable() {
					@Override public void run() {
						updateStickerView(mStickerPack, (Sticker) parent);
					}
				});
			} else if (parent instanceof Filter) {
				mUiThreadHandler.post(new Runnable() {
					@Override public void run() {
						updateEffectView((Filter) parent);
					}
				});
			}
		}
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * fragment listener
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public void setFragmentListener(FragmentListener listener) {
		mFragmentListener = listener;
	}

	public static interface FragmentListener {
		void onEditComplete(String filePath);
		void onAddAnalytic(String value);
		void onAddAnalytic(String value, Object... formatArgs);
		void onShowProgressBlocking(String text);
		void onShowProgressBlocking(String title, String text);
		void onHideProgressBlocking();
		void onShowProgressUnblocking();
		void onHideProgressUnblocking();
		void onCancel();
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * JNI library loader
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	static {
		System.loadLibrary("snapr-jni");
	}

	
	
}
