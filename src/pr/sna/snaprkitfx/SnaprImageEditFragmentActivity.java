package pr.sna.snaprkitfx;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nz.co.juliusspencer.android.JSAProgressDialogFragment;
import pr.sna.snaprkitfx.SnaprImageEditFragment.FragmentListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;

public class SnaprImageEditFragmentActivity extends FragmentActivity implements FragmentListener {
	public static final String TAG = "SNA";
	
	private final ArrayList<String> mAnalytics = new ArrayList<String>();
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * constants
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static final boolean DEBUG = false;
	
	public static final int EDIT_IMAGE = 20;
	public static final int EDIT_IMAGE_REQUEST_CODE = 20; 
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * constants: extras
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/**
	 * The path for the source image.
	 */
	public static final String EXTRA_FILEPATH = "EXTRA_FILEPATH";
	
	/**
	 * The filename to use for the file. All images are saved in the Environment.DIRECTORY_PICTURES folder.
	 */
	public static final String EXTRA_OUTPUT = "EXTRA_OUTPUT";
	
	/**
	 * A boolean to determine whether a photo was just taken to supply the source image.
	 */
	public static final String EXTRA_TOOK_PHOTO = "EXTRA_TOOK_PHOTO";
	public static final String EXTRA_TOOK_PHOTO_TIMESTAMP = "EXTRA_TOOK_PHOTO_TIMESTAMP";
	
	public static final String EXTRA_ANALYTICS = "EXTRA_ANALYTICS";
	
	public static final String EXTRA_FILTER_PACK_PATH = "EXTRA_FILTER_PACK_PATH";
	public static final String EXTRA_STICKER_PACK_PATHS = "EXTRA_STICKER_PACK_PATHS";
	
	public static final String EXTRA_LAUNCH_MODE = "EXTRA_LAUNCH_MODE";
	public static final String EXTRA_LAUNCH_STICKER_PACK = "EXTRA_LAUNCH_STICKER_PACK";
	public static final String EXTRA_EFFECT_SETTINGS = "EXTRA_EFFECT_SETTINGS";
	
	public static final String EXTRA_IMAGE_ASPECT_RATIO = "EXTRA_IMAGE_ASPECT_RATIO";
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * constants: analytics
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static final String ANALYTIC_PAGE_LOADED = "snaprkit-parent://coremetrics/?tag_type=Page View&category_id=ANDROID_VSPINK_APP_PICS_FILTERS_P&page_id=ANDROID_VSPINK_APP_PICS_FILTERS_SELECT_P";
	public static final String ANALYTIC_CANCEL_EVENT = "snaprkit-parent://coremetrics/?tag_type=Manual Link Click&cm_re=spring2012-_-sub-_-cancel_image_upload& link_name=CANCEL IMAGE UPLOAD&page_id=ANDROID_VSPINK_APP_PICS_FILTERS_SELECT_P"; 
	public static final String ANALYTIC_SHARE_EVENT = "snaprkit-parent://coremetrics/?tag_type=Conversion Event& link_name=CANCEL IMAGE UPLOAD& category_id=VSPINK_APP_PICS_FILTERS_SELECTED_P&element_id=ANDROID_APP_FILTERS_SELECTED_(FILTER_NAME)&action_type=2";
	// sticker added
	public static final String ANALYTIC_STICKER_ADDED_EVENT = "snaprkit-parent://coremetrics/?tag_type=Real Estate&tid=8&cm_re=012714-_-springbreak-_-filter_%s";
	public static final String ANALYTIC_STICKER_REMOVED_EVENT = "snaprkit-parent://coremetrics/?tag_type=Conversion Event&category_id=ANDROID_APP_PINKS_GOT_SPIRIT_GAMEDAY_P&link_name=ANDROID_APP_CLICKS_X&action_type=2";
	public static final String ANALYTIC_STICKERS_PINNED_EVENT = "snaprkit-parent://coremetrics/?tag_type=Conversion Event&category_id=ANDROID_APP_PINKS_GOT_SPIRIT_GAMEDAY_P&link_name=ANDROID_APP_CLICKS_CHECK_MARK&action_type=2";
	// filter added
	public static final String ANALYTIC_FILTER_ADDED_EVENT = "snaprkit-parent://coremetrics/?tag_type=Real Estate&tid=8&cm_re=012714-_-springbreak-_-sticker_%s";
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * on create
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    @SuppressLint("NewApi") @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.snaprkitfx_edit_layout);
        
        if (Build.VERSION.SDK_INT >= 11 && getActionBar() != null) getActionBar().hide();
		SnaprImageEditFragment fragment = (SnaprImageEditFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
		fragment.setFragmentListener(this);
    }
    
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * start activity
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public static void startActivity(Activity activity, Builder builder) {
		Intent intent = getIntentForStartActivity(activity, builder);
		activity.startActivity(intent);
	}
	
	public static void startActivityForResult(Activity activity, Builder builder) {
		startActivityForResult(activity, builder, EDIT_IMAGE);
	}
	
	public static void startActivityForResult(Activity activity, Builder builder, int requestCode) {
		Intent intent = getIntentForStartActivity(activity, builder);
		activity.startActivityForResult(intent, requestCode);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * get intent for start activity
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static Intent getIntentForStartActivity(Context context, Builder builder) {
		Intent intent = new Intent(context, SnaprImageEditFragmentActivity.class);
		intent.putExtra(EXTRA_FILEPATH, builder.mFile.getAbsolutePath());
		intent.putExtra(EXTRA_OUTPUT, builder.mOutputFile.getAbsolutePath());
		intent.putExtra(EXTRA_TOOK_PHOTO, builder.mJustTookPhoto);
		if (builder.mImageAspectRatio != 0f) intent.putExtra(EXTRA_IMAGE_ASPECT_RATIO, builder.mImageAspectRatio);
		if (builder.mJustTookPhoto) intent.putExtra(EXTRA_TOOK_PHOTO_TIMESTAMP, builder.mJustTookPhotoTimestamp);
		if (builder.mFilterPackPath != null) intent.putExtra(EXTRA_FILTER_PACK_PATH, builder.mFilterPackPath);
		if (builder.mStickerPackPaths != null) intent.putStringArrayListExtra(EXTRA_STICKER_PACK_PATHS, builder.mStickerPackPaths);
		intent.putExtra(EXTRA_LAUNCH_MODE, builder.mLaunchMode.name());
		intent.putExtra(EXTRA_LAUNCH_STICKER_PACK, builder.mLaunchStickerPack);
		
		Serializable effectSettings = builder.mSettings == null ? null : 
			builder.mSettings instanceof Serializable ? (Serializable) builder.mSettings : 
			new HashMap<String, SnaprSetting>(builder.mSettings);
		if (effectSettings != null) intent.putExtra(EXTRA_EFFECT_SETTINGS, effectSettings);
		
		return intent;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * fragment listener
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	@Override public void onEditComplete(String filePath) {
		Intent i = new Intent();
		i.putExtra(EXTRA_FILEPATH, filePath);
		onAddAnalytic(SnaprImageEditFragmentActivity.ANALYTIC_SHARE_EVENT);
		i.putExtra(EXTRA_ANALYTICS, getAnalytics());
		setResult(RESULT_OK, i);
		finish();
	}

	@Override public void onCancel(boolean requestConfirmation) {
		if (requestConfirmation)
		{
			showCancelConfirmationDialog();
		}
		else
		{
			onCancelConfirmed();
		}
	}

	@Override public void onAddAnalytic(String value) {
		if (DEBUG) Log.i(TAG, "tracking event: " + value);
		getAnalytics().add(value);
	}
	
	@Override public void onAddAnalytic(String value, Object... formatArgs) {
		if (DEBUG) Log.i(TAG, "tracking event: " + String.format(value, formatArgs));
		getAnalytics().add(String.format(value, formatArgs));
	}

	@Override public void onShowProgressBlocking(String title) {
		JSAProgressDialogFragment.create(getString(R.string.snaprkitfx_please_wait), title).show(this);
	}
	
	@Override public void onShowProgressBlocking(String title, String text) {
		JSAProgressDialogFragment.create(title, text).show(this);
	}
	
	@Override public void onHideProgressBlocking() {
		FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentByTag(JSAProgressDialogFragment.DEFAULT_FRAGMENT_TAG);
		if (fragment == null) return;
		if (!(fragment instanceof DialogFragment)) return;
		DialogFragment df = (DialogFragment) fragment;
		try {
			df.dismiss();
		} catch (Exception exception) { /* do nothing */ }
	}
	
	@Override public void onShowProgressUnblocking() {
		setProgressBarIndeterminateVisibility(true);
	}

	@Override public void onHideProgressUnblocking() {
		setProgressBarIndeterminateVisibility(false);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * analytics
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public ArrayList<String> getAnalytics() {
		return mAnalytics;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * on back pressed
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	@Override public void onBackPressed() {
		optionallyFinishActivity();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * optionally finish activity
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void optionallyFinishActivity() {
		
		/*
		// see {@link CancelDialogFragment}
		boolean tookPhoto = getIntent().getExtras().getBoolean(EXTRA_TOOK_PHOTO);
		if (!tookPhoto) { finishActivity(); return; }
		CancelDialogFragment fragment = CancelDialogFragment.newInstance();
		fragment.show(getSupportFragmentManager(), "dialog");
		*/
		
		finishActivity();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * finish activity
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void finishActivity() {
		Intent i = new Intent();
		i.putStringArrayListExtra(EXTRA_ANALYTICS, getAnalytics());
		setResult(RESULT_CANCELED, i);
		finish();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * builder
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static class Builder {
		private final File mFile;
		private final File mOutputFile;
		private final boolean mJustTookPhoto;
		private final long mJustTookPhotoTimestamp;
		private String mFilterPackPath;
		private ArrayList<String> mStickerPackPaths;
		private float mImageAspectRatio;
		private Map<String, SnaprSetting> mSettings;
		// default to filters being selected
		private LaunchMode mLaunchMode = LaunchMode.FILTERS;
		private String mLaunchStickerPack = null;
		
		public Builder(File file, File outputFile) {
			this(file, outputFile, false, -1);
		}
		
		public Builder(File file, File outputFile, boolean justTookPhoto, long justTookPhotoTimestamp) {
			mFile = file;
			mOutputFile = outputFile;
			mJustTookPhoto = justTookPhoto;
			mJustTookPhotoTimestamp = justTookPhotoTimestamp;
		}
		
		public Builder setFilterPackPath(String path) {
			mFilterPackPath = path;
			return this;
		}
		
		public Builder setStickerPackPaths(ArrayList<String> paths) {
			mStickerPackPaths = paths;
			return this;
		}
		
		public Builder setImageAspectRatio(float imageAspectRatio) {
			mImageAspectRatio = imageAspectRatio;
			return this;
		}
		
		public Builder setSettings(Map<String, SnaprSetting> settings) {
			mSettings = settings;
			return this;
		}
		
		/** 
		 * Note that this does not check whether the mode is actually available. In other words: it's up to the caller to ensure
		 * that if {@link LaunchMode#STICKERS} is set, {@link #setStickerPackPaths(String)} is also called with a valid value.
		 */
		public Builder setLaunchMode(LaunchMode mode) {
			if (mode == null) throw new IllegalArgumentException("mode cannot be null; use either LaunchMode.FILTERS or LaunchMode.STICKERS");
			mLaunchMode = mode;
			return this;
		}
		
		public Builder setLaunchStickerPack(String stickerPackSlug)
		{
			mLaunchStickerPack = stickerPackSlug;
			return this;
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * cancel dialog fragment
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/** A simple enum that allows the initially selected tab (stickers vs. filters) to be configured using {@link Builder#setLaunchMode(LaunchMode)}.*/
	public static enum LaunchMode {
		FILTERS ("filters"),
		STICKERS ("stickers");
		
		private final String mName;
		
		LaunchMode(String name) {
			mName = name;
		}
		
		public String getName() {
			return mName;
		}
		
		/** Returns the {@link LaunchMode} matching the given name (cases are ignored). Throws an {@link IllegalArgumentException} if no match is found. */
		public static LaunchMode parse(String name) {
			if (TextUtils.isEmpty(name)) throw new IllegalArgumentException("Unsupported mode value: " + name);
			for (LaunchMode mode : values()) {
				if (!mode.mName.equalsIgnoreCase(name)) continue;
				return mode;
			}
			throw new IllegalArgumentException("No match found for mode value: " + name);
		}
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * cancel dialog fragment
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public void showCancelConfirmationDialog()
	{
		DialogFragment dialog = CancelDialogFragment.newInstance();
		dialog.show(getSupportFragmentManager(), "dialog");
	}
	
	public void onCancelConfirmed()
	{
		onAddAnalytic(SnaprImageEditFragmentActivity.ANALYTIC_PAGE_LOADED);
		optionallyFinishActivity();
	}
	
	/**
	 * The {@link CancelDialogFragment} is an extension of {@link DialogFragment} and is used to display to the user a message to 
	 * display a warning about unsaved changes.
	 */
	
	public static class CancelDialogFragment extends DialogFragment
	{
		public static CancelDialogFragment newInstance()
		{
			CancelDialogFragment frag = new CancelDialogFragment();
			Bundle args = new Bundle();
			frag.setArguments(args);
			return frag;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState)
		{
			return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.snaprkitfx_cancel_title)
				.setMessage(R.string.snaprkitfx_cancel_message)
				.setPositiveButton(R.string.snaprkitfx_cancel_exit,
					new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int whichButton)
						{
							((SnaprImageEditFragmentActivity)getActivity()).onCancelConfirmed();
						}
					}
				)
				.setNegativeButton(android.R.string.cancel,
					new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int whichButton) 
						{
							// Do nothing
						}
					}
				)
				.create();
		}
	}
	
}
