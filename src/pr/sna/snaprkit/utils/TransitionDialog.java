package pr.sna.snaprkit.utils;

import pr.sna.snaprkit.Global;
import android.app.ProgressDialog;
import android.content.Context;

public class TransitionDialog
{
	// Members
	private Context mContext;
	private ProgressDialog mTransitionDialog;
	
	// Constructor
	public TransitionDialog(Context context)
	{
		mContext = context;
	}
	
	public void showTransitionDialog(String message, String title)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	if (mTransitionDialog == null || mTransitionDialog.isShowing() == false)
    	{
    		mTransitionDialog = new ProgressDialog(mContext);
    		mTransitionDialog.setMessage(message);
    		mTransitionDialog.setTitle(title);
    		mTransitionDialog.setIndeterminate(true);
    		mTransitionDialog.setCancelable(true);
    		mTransitionDialog.show();
    	}
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
	
	public void cancelTransitionDialog()
	{
		if (mTransitionDialog != null && mTransitionDialog.isShowing())
		{
			mTransitionDialog.dismiss();
			mTransitionDialog = null;
		}
	}
	
}