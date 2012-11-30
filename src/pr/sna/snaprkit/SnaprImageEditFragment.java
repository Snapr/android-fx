package pr.sna.snaprkit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nz.co.juliusspencer.android.JSAObjectUtil;
import nz.co.juliusspencer.android.JSATimeUtil;
import nz.co.juliusspencer.android.JSATuple;
import pr.sna.snaprkit.SnaprFilterUtil.Filter;
import pr.sna.snaprkit.SnaprFilterUtil.FilterPack;
import pr.sna.snaprkit.SnaprFilterUtil.OnImageLoadListener;
import pr.sna.snaprkit.SnaprStickerUtil.Sticker;
import pr.sna.snaprkit.SnaprStickerUtil.StickerPack;
import pr.sna.snaprkit.tabletop.TabletopSurfaceView;
import pr.sna.snaprkit.tabletop.TabletopSurfaceView.TabletopListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SnaprImageEditFragment extends Fragment implements TabletopListener {
	private FragmentListener mFragmentListener;

	private final List<SnaprEffect> mEffects = new ArrayList<SnaprEffect>();
	private final List<Sticker> mStickers = new ArrayList<Sticker>();
	
	private File mOriginalFile;					// the location of the original, unscaled image
	private File mSaveFile;						// the location to save the modified image to
	
	private Bitmap mBaseBitmap;								// the original, scaled bitmap (possibly with an effect applied)
	private Bitmap mComposedBitmap;							// the original, scaled bitmap (possibly with stickers and an effect applied)
	private ComposeBitmapAsyncTask mComposeBitmapAsyncTask;	// the currently running compose bitmap task
	
	private SnaprEffect mBaseBitmapEffect;					// the effect currently applied to the base bitmap
	private int mComposedBitmapInteractionCount;			// the interaction count of the tabletop when the composed bitmap was created
	
	private SnaprEffect mAppliedEffect;
	private InteractionState mInteractionState = InteractionState.SHOWING_STICKERS;	// the current state of the fragment interaction
	
	private String mFilterPackLocation = FILTER_PACK_PATH_DEFAULT;		// the location (under assets) where the filter packs will be loaded from
	private String mStickerPackLocation = STICKER_PACK_PATH_DEFAULT;	// the location (under assets) where the sticker packs will be loaded from
	
	private Handler mUiThreadHandler;			// handler to run actions on the ui thread
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * views
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private boolean mViewsInitialised;
	
	private ImageView mEditedImageView;
	private TabletopSurfaceView mTabletop;
	private TextView mMessageTextView;
	private View mMessageLayout;

	private View mFilterContainerRoot;
	private ViewGroup mFilterContainer;
	private View mFilterButton;
	
	private ViewGroup mStickerContainer;
	private View mStickerContainerRoot;
	private View mStickerButton;
	
	private View mCancelButton;
	private View mNextButton;
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constants
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private static final boolean DEBUG = false;
	
	private static final String FILTER_PACK_PATH_DEFAULT = "filter_packs/defaults";
	private static final String STICKER_PACK_PATH_DEFAULT = "sticker_packs/defaults";
	
	private static final float MAX_NEW_GRAPHIC_FACTOR = 0.65f; // no newly added graphic will have a width or height greater than image factor

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
		mStickerPackLocation = extras.getString(SnaprImageEditFragmentActivity.EXTRA_STICKER_PACK_PATH);
		
		if (mFilterPackLocation == null) mFilterPackLocation = FILTER_PACK_PATH_DEFAULT;
		if (mStickerPackLocation == null) mStickerPackLocation = STICKER_PACK_PATH_DEFAULT;
		
		mEditedImageView = (ImageView) getView().findViewById(R.id.edited_image);
		mTabletop = (TabletopSurfaceView) getView().findViewById(R.id.tabletop);
		mMessageTextView = (TextView) getView().findViewById(R.id.message_textview);
		mMessageLayout = getView().findViewById(R.id.message_layout);

		mFilterContainer = (ViewGroup) getView().findViewById(R.id.filter_container);
		mStickerContainer = (ViewGroup) getView().findViewById(R.id.sticker_container);
		
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
		
		// handle filter button clicks by showing the filters
		mFilterButton = getView().findViewById(R.id.filter_button);
		mFilterButton.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				mInteractionState = InteractionState.SHOWING_FILTERS;
				mTabletop.setInteractionEnabled(false);
				mTabletop.pinAllGraphics();
				updateViewEditedImageView();
				updateView();
			}
		});
		
		// handle sticker button clicks by showing the stickers
		mStickerButton = getView().findViewById(R.id.sticker_button);
		mStickerButton.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				mInteractionState = InteractionState.SHOWING_STICKERS;
				mTabletop.setInteractionEnabled(true);
				updateViewEditedImageView();
				updateView();
			}
		});
		
		// initialise the tabletop
		mTabletop.setOnNonInteractionListener(this);
		mTabletop.setAutoPinGraphics(true);
		
		final String saveFile = extras.getString(SnaprImageEditFragmentActivity.EXTRA_OUTPUT);
		final String originalFile = getActivity().getIntent().getStringExtra(SnaprImageEditFragmentActivity.EXTRA_FILEPATH);
		final boolean isPhotoTaken = getActivity().getIntent().getBooleanExtra(SnaprImageEditFragmentActivity.EXTRA_TOOK_PHOTO, true);
		final long photoTimestamp = getActivity().getIntent().getLongExtra(SnaprImageEditFragmentActivity.EXTRA_TOOK_PHOTO_TIMESTAMP, -1);
		
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
		if (isPhotoTaken) MediaScannerConnection.scanFile(SnaprKitApplication.getInstance(), new String[] { originalFile }, null,
				new MediaScannerConnection.OnScanCompletedListener() {
						public void onScanCompleted(String path, Uri uri) { onOriginalBitmapAvailable(originalFile, photoTimestamp); } });
		else onOriginalBitmapAvailable(originalFile, photoTimestamp);

		// initialise the effects and stickers (on a background thread)
		new LoadStickersFiltersAsyncTask().execute();
		
		mViewsInitialised = true;
		updateView();
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on original available bitmap
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void onOriginalBitmapAvailable(String originalFilepath, long photoTimestamp) {
		mBaseBitmap = SnaprImageEditFragmentUtil.saveOriginalTempImage(getActivity(), originalFilepath, photoTimestamp);
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
	 * initialise effects
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private FilterPack inflateEffects() {
		try { // load the filter packs from file
			return FilterPack.parse(getActivity().getApplicationContext(), mFilterPackLocation, false);
		} catch (Exception exception) {
			Log.e(SnaprImageEditFragment.class.getSimpleName(), "error inflating effects", exception);
			return null;
		}
	}
	
	private void initialiseEffectViews() {
		if (!isAdded() || !mViewsInitialised) return;
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		// create the thumbnail views for each effect
		for (SnaprEffect effect : mEffects) {
			View view = inflater.inflate(R.layout.snaprkitfx_effect_item, null);
			view.setTag(effect);
			
			ImageView image = (ImageView) view.findViewById(R.id.effect_imageview);
			if (effect.getFilter().getThumbnail() != null) image.setImageBitmap(effect.getFilter().getThumbnail());
			else image.setImageResource(R.drawable.snaprkitfx_ic_effect_placeholder);
			image.setTag(effect);

			TextView tv = ((TextView) view.findViewById(R.id.effect_textview));
			tv.setText(effect.getFilter().getName());

			image.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View view) {
					onEffectClick(view);
				}
			});

			mFilterContainer.addView(view);
		}
	}
	
	private void updateEffectView(SnaprEffect effect) {
		View view = mFilterContainer.findViewWithTag(effect);
		if (view == null) return;
		ImageView image = (ImageView) view.findViewById(R.id.effect_imageview);
		if (effect.getFilter().getThumbnail() != null) image.setImageBitmap(effect.getFilter().getThumbnail());
		else image.setImageResource(R.drawable.snaprkitfx_ic_effect_placeholder);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * initialise stickers
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private StickerPack inflateStickers() {
		try { // load the sticker packs from file
			return StickerPack.parse(getActivity().getApplicationContext(), mStickerPackLocation, false);
		} catch (Exception exception) {
			Log.e(SnaprImageEditFragment.class.getSimpleName(), "error inflating stickers", exception);
			return null;
		}
	}
	
	private void initialiseStickerViews() {
		LayoutInflater inflater = getActivity().getLayoutInflater();

		// create the thumbnail views for each sticker		
		for (Sticker sticker : mStickers) {
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
			
			mStickerContainer.addView(view);
		}
	}

	private void updateStickerView(Sticker sticker) {
		View view = mStickerContainer.findViewWithTag(sticker);
		if (view == null) return;
		ImageView image = (ImageView) view.findViewById(R.id.effect_imageview);
		if (sticker.getThumbnail() != null) image.setImageBitmap(sticker.getThumbnail());
		else image.setImageResource(R.drawable.snaprkitfx_ic_sticker_placeholder);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 *  update view
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void updateView() {
		if (!isAdded() || !mViewsInitialised) return;
		boolean isLocked = mAppliedEffect != null ? mAppliedEffect.isLocked() : false;
		boolean isShowingFilters = mInteractionState.equals(InteractionState.SHOWING_FILTERS);
		boolean hasStickers = mStickers.size() != 0;
		boolean hasFilters = mEffects.size() != 0;
		
		// update the filter or sticker containers
		mFilterContainerRoot.setVisibility(hasFilters && isShowingFilters ? View.VISIBLE : View.GONE);
		mStickerContainerRoot.setVisibility(hasStickers && !isShowingFilters ? View.VISIBLE : View.GONE);

		// update the filter and sticker buttons
		mFilterButton.setVisibility(hasFilters ? View.VISIBLE : View.GONE);
		mFilterButton.setSelected(isShowingFilters);
		mStickerButton.setVisibility(hasStickers ? View.VISIBLE : View.GONE);
		mStickerButton.setSelected(!isShowingFilters);
		
		// update the locked message
		if (mAppliedEffect != null) mMessageTextView.setText(mAppliedEffect.getUnlockMessage());
		mMessageLayout.setVisibility(isLocked ? View.VISIBLE : View.INVISIBLE);
		
		// update the next button
		boolean isNextButton = hasStickers && hasFilters && !isShowingFilters;
		int resourceId = isNextButton ? R.drawable.snaprkitfx_btn_next_normal : R.drawable.snaprkitfx_btn_tick;
		mNextButton.setBackgroundResource(resourceId);
		mNextButton.setEnabled(isNextButton || !isLocked);
		
		// show or hide the tabletop
		mTabletop.setVisibility(isShowingFilters ? View.GONE : View.VISIBLE);
		
		// show or hide the indeterminate progress
		if (mComposeBitmapAsyncTask != null && isShowingFilters) mFragmentListener.onShowProgressUnblocking();
		else mFragmentListener.onHideProgressUnblocking();
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 *  update view: effects
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void updateViewEffects() {
		for (SnaprEffect effect : mEffects) {
			View view = mFilterContainer.findViewWithTag(effect);
			if (view == null) return;
			view.findViewById(R.id.chosen_imageview).setSelected(effect.equals(mAppliedEffect));
			view.findViewById(R.id.locked_overlay_imageview).setVisibility(effect.isLocked() ? View.VISIBLE : View.INVISIBLE);
		}
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * update view: edited image view
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void updateViewEditedImageView() {
		boolean isShowingStickers = mInteractionState.equals(InteractionState.SHOWING_STICKERS);
		boolean isStickerUpToDate = mComposedBitmapInteractionCount == mTabletop.getInteractionCount();
//		boolean isBaseBitmapEffectApplied = mBaseBitmapEffect != null && !mEffects.get(0).equals(mBaseBitmapEffect);
		boolean preferComposed = !isShowingStickers || (/* isBaseBitmapEffectApplied && */ isStickerUpToDate); // show composed even with no effect applied
		boolean showComposed = preferComposed && mComposedBitmap != null && mComposedBitmap != mBaseBitmap;
		mEditedImageView.setImageBitmap(showComposed ? mComposedBitmap : mBaseBitmap);
		mTabletop.setDrawGraphics(!showComposed);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on next button click
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private void onNextButtonClick() {
		boolean isShowingStickers = mInteractionState.equals(InteractionState.SHOWING_STICKERS);
		boolean hasStickers = mStickers.size() != 0;
		boolean hasFilters = mEffects.size() != 0;
		
		if (hasStickers && hasFilters && isShowingStickers) mInteractionState = InteractionState.SHOWING_FILTERS; // show the filters
		else new SaveEditedBitmapToFileAsyncTask().execute(); // save bitmap and change file		
		updateViewEditedImageView();
		updateView();
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on effect click
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private void onEffectClick(View v) {
		SnaprEffect effect = (SnaprEffect) v.getTag();
		if (effect.equals(mAppliedEffect)) return;
		mAppliedEffect = effect;
		composeBitmap(true);
		updateViewEffects();
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
		
		// scale the bitmap such that neither width nor height exceed a given ratio of the image
		int minImageLength = Math.min(mEditedImageView.getWidth(), mEditedImageView.getHeight());
		int maxBitmapLength = Math.max(sticker.getImage().getWidth(), sticker.getImage().getHeight()); 
		float bitmapFactor = maxBitmapLength / (float) minImageLength;
		float bitmapScale = Math.min(MAX_NEW_GRAPHIC_FACTOR / bitmapFactor, 1);
		mTabletop.addGraphic(sticker, (int) (sticker.getImage().getWidth() * bitmapScale));
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * tabletop listener
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	@Override public void onInteraction(int interactionCount) {
		updateViewEditedImageView();
	}
	
	@Override public void onNonInteraction(int interactionCount) {
		composeBitmap(false);
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
			synchronized (SnaprImageEditFragment.this) { // synchronise to prevent two tasks running concurrently
				try {
					
					// load the bitmap from the temporarily saved, resized bitmap
					Bitmap bitmap = SnaprImageEditFragmentUtil.loadTempImage();
					if (isCancelled()) return null;
					
					// generate the base bitmap by applying the effect (if required)
					if (!JSAObjectUtil.equals(mAppliedEffect, mBaseBitmapEffect)) {
						mBaseBitmap = bitmap;
						mBaseBitmapEffect = mAppliedEffect;
						if (mAppliedEffect != null) mAppliedEffect.getFilter().apply(mContext, mBaseBitmap);
						if (mTabletop.getGraphicCount() == 0) return mBaseBitmap;
						bitmap = SnaprImageEditFragmentUtil.loadTempImage();
						if (isCancelled()) return null;
					}
					
					// draw the stickers and apply the effect
					bitmap = mTabletop.drawOnBitmap(bitmap, true);
					mComposedBitmapInteractionCount = mTabletop.getInteractionCount();
					if (isCancelled()) return null;
					if (mAppliedEffect != null) mAppliedEffect.getFilter().apply(mContext, bitmap);
					if (isCancelled()) return null;
					return bitmap;
				} catch (IOException exception) {
					if (DEBUG) Log.e(SnaprImageEditFragment.class.getSimpleName(), "error applying effect", exception);
					return null;
				}
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
			updateView();
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * save edited bitmap to file async task
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private class SaveEditedBitmapToFileAsyncTask extends SnaprImageEditFragmentUtil.SaveEditedBitmapToFileAsyncTask {

		public SaveEditedBitmapToFileAsyncTask() {
			super(getActivity(), mOriginalFile.getAbsolutePath(), mSaveFile.getAbsolutePath(), mAppliedEffect, mTabletop);
		}

		@Override protected void onPreExecute() {
			super.onPreExecute();
			mFragmentListener.onShowProgressBlocking(getString(R.string.snaprkitfx_saving_));
			mBaseBitmap = null; // null bitmaps to release unused memory
			mComposedBitmap = null;
			updateViewEditedImageView();
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
	
	private class LoadStickersFiltersAsyncTask extends AsyncTask<Void, Void, JSATuple<FilterPack, StickerPack>> {
		
		@Override protected void onPreExecute() {
			super.onPreExecute();
			mFragmentListener.onShowProgressBlocking(getString(R.string.snaprkitfx_loading));
		}

		@Override protected JSATuple<FilterPack, StickerPack> doInBackground(Void... params) {
			if (DEBUG) JSATimeUtil.logTime();
			FilterPack filterPack = inflateEffects();
			if (DEBUG) JSATimeUtil.logTime("filter packs inflated");
			StickerPack stickerPack = inflateStickers();
			if (DEBUG) JSATimeUtil.logTime("sticker packs inflated");
			return new JSATuple<FilterPack, StickerPack>(filterPack, stickerPack);
		}
		
		@Override protected void onPostExecute(JSATuple<FilterPack, StickerPack> result) {
			super.onPostExecute(result);
			mFragmentListener.onHideProgressBlocking();
			FilterPack filterPack = result.getA();
			StickerPack stickerPack = result.getB();
			
			if (filterPack != null) {
				for (Filter filter : filterPack.getFilters()) mEffects.add(new SnaprEffect(filter));
				if (mEffects.size() != 0) mAppliedEffect = mEffects.get(0);
				initialiseEffectViews();
				updateViewEffects();
			}
			
			if (stickerPack != null) {
				mStickers.addAll(stickerPack.getStickers());
				initialiseStickerViews();
			}

			if (isAdded()) new LoadStickerFilterImagesAsyncTask(getActivity()).execute();
			
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
			if (DEBUG) JSATimeUtil.logTime();
			for (SnaprEffect effect : mEffects)
				effect.getFilter().loadImagesNoException(mContext, mFilterPackLocation, new SimpleOnImageLoadListener());
			if (DEBUG) JSATimeUtil.logTime("filter pack images loaded");
			for (Sticker sticker : mStickers)
				sticker.loadImagesNoException(mContext, mStickerPackLocation, new SimpleOnImageLoadListener());
			if (DEBUG) JSATimeUtil.logTime("sticker pack images loaded");
			return true;
		}
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * simple on image load listener
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private final class SimpleOnImageLoadListener implements OnImageLoadListener {
		@Override public void onImageLoad(final Object parent, final Bitmap image) {
			if (parent instanceof Sticker) {
				mUiThreadHandler.post(new Runnable() {
					@Override public void run() {
						updateStickerView((Sticker) parent);
					}
				});
			} else if (parent instanceof Filter) {
				mUiThreadHandler.post(new Runnable() {
					@Override public void run() {
						updateEffectView(new SnaprEffect((Filter) parent));
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
