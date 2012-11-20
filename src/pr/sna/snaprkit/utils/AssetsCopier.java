package pr.sna.snaprkit.utils;

import android.app.Activity;
import android.os.AsyncTask;

public class AssetsCopier extends AsyncTask<Void, Void, Void>
{
	// Members
	private Activity mParent = null;
	private TransitionDialog mTransitionDialog;
	private boolean mShowDialogs = false;
	private AssetCopierListener mListener = null;
	
	public AssetsCopier(Activity parentActivity, AssetCopierListener listener)
	{
		mParent = parentActivity;
		mListener = listener;
		mShowDialogs = true;
		mTransitionDialog = new TransitionDialog(mParent);
	}
	
	public AssetsCopier(Activity parentActivity, AssetCopierListener listener, boolean showDialogs)
	{
		mParent = parentActivity;
		mListener = listener;
		mShowDialogs = showDialogs;
		mTransitionDialog = new TransitionDialog(mParent);
	}
	
	@Override
	protected void onPostExecute(Void result)
	{
		super.onPostExecute(result);
		
		// Cancel the progress dialog
		if (mShowDialogs) mTransitionDialog.cancelTransitionDialog();
		
		// Indicate completion
		if (mListener != null) mListener.onComplete();
	}

	@Override
	protected void onPreExecute()
	{
		super.onPreExecute();
		if (mShowDialogs) mTransitionDialog.showTransitionDialog("Loading assets...", null);
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		AssetUtils.copyAssetsToCache(mParent);
		return null;
	}
	
	public interface AssetCopierListener
	{
		public void onComplete();
	}
}