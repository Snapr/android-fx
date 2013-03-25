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
import android.view.View;
import android.view.Window;
import android.widget.TextView;

public class SnaprImageEditFragmentActivity extends FragmentActivity implements FragmentListener {
	private ArrayList<String> mAnalytics = new ArrayList<String>();
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * constants
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
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
	public static final String EXTRA_STICKER_PACK_PATH = "EXTRA_STICKER_PACK_PATH";
	
	public static final String EXTRA_EFFECT_SETTINGS = "EXTRA_EFFECT_SETTINGS";
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * constants: analytics
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static final String ANALYTIC_PAGE_LOADED = "snaprkit-parent://coremetrics/?tag_type=Page View&category_id=ANDROID_VSPINK_APP_PICS_FILTERS_P&page_id=ANDROID_VSPINK_APP_PICS_FILTERS_SELECT_P";
	public static final String ANALYTIC_CANCEL_EVENT = "snaprkit-parent://coremetrics/?tag_type=Manual Link Click&cm_re=spring2012-_-sub-_-cancel_image_upload& link_name=CANCEL IMAGE UPLOAD&page_id=ANDROID_VSPINK_APP_PICS_FILTERS_SELECT_P"; 
	public static final String ANALYTIC_SHARE_EVENT = "snaprkit-parent://coremetrics/?tag_type=Conversion Event& link_name=CANCEL IMAGE UPLOAD& category_id=VSPINK_APP_PICS_FILTERS_SELECTED_P&element_id=ANDROID_APP_FILTERS_SELECTED_(FILTER_NAME)&action_type=2";
	
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
		if (builder.mJustTookPhoto) intent.putExtra(EXTRA_TOOK_PHOTO_TIMESTAMP, builder.mJustTookPhotoTimestamp);
		if (builder.mFilterPackPath != null) intent.putExtra(EXTRA_FILTER_PACK_PATH, builder.mFilterPackPath);
		if (builder.mStickerPackPath != null) intent.putExtra(EXTRA_STICKER_PACK_PATH, builder.mStickerPackPath);
		
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

	@Override public void onCancel() {
		onAddAnalytic(SnaprImageEditFragmentActivity.ANALYTIC_PAGE_LOADED);
		optionallyFinishActivity();
	}

	@Override public void onAddAnalytic(String value) {
		getAnalytics().add(value);
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
		private String mStickerPackPath;
		private Map<String, SnaprSetting> mSettings;
		
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
		
		public Builder setStickerPackPath(String path) {
			mStickerPackPath = path;
			return this;
		}
		
		public Builder setSettings(Map<String, SnaprSetting> settings) {
			mSettings = settings;
			return this;
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * cancel dialog fragment
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	/**
	 * The {@link CancelDialogFragment} is an extension of {@link DialogFragment} and is used to display to the user a message to optionally
	 * allow them to specify where to save a photo taken from the camera.
	 * 
	 * This feature is currently incomplete and unused (but left include as it may be resurrected).
	 */
	
	public static class CancelDialogFragment extends DialogFragment {
		
		public static CancelDialogFragment newInstance() {
			CancelDialogFragment fragment = new CancelDialogFragment();
			Bundle args = new Bundle();
			fragment.setArguments(args);
			return fragment;
		}
		
		public CancelDialogFragment() {
			/* do nothing */
		}
		
		@Override public Dialog onCreateDialog(Bundle savedInstanceState) {
			
			View view = getActivity().getLayoutInflater().inflate(R.layout.snaprkitfx_cancel_dialog, null);
			final TextView text = (TextView) view.findViewById(R.id.textview);
			text.setText(R.string.snaprkitfx_save_photo_questionmark);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.snaprkitfx_save_photo_questionmark);
			builder.setView(view);
			
			builder.setNegativeButton(R.string.snaprkitfx_dont_save, new DialogInterface.OnClickListener() {
				@Override public void onClick(DialogInterface dialog, int which) {
					SnaprImageEditFragmentActivity activity = (SnaprImageEditFragmentActivity) getActivity();
					activity.finishActivity();
					dialog.dismiss();
				}
			});
			builder.setPositiveButton(R.string.snaprkitfx_save, new DialogInterface.OnClickListener() {
				@Override public void onClick(DialogInterface dialog, int which) {
					// TODO: show user text field to input file name (saved in gallery folder)
					SnaprImageEditFragmentActivity activity = (SnaprImageEditFragmentActivity) getActivity();
					activity.finishActivity();
					dialog.dismiss();
				}
			});
			
			final AlertDialog dialog = builder.create();
			return dialog;
		}

	}
	
}
