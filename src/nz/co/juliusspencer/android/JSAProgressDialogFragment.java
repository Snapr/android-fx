package nz.co.juliusspencer.android;

import java.io.Serializable;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

/**
 * The {@link JSAProgressDialogFragment} class is an extension of the {@link DialogFragment} class that wraps the default 
 * {@link ProgressDialog} component in a {@link Fragment}. Only the indeterminate version of the progress bar is shown.
 */

public class JSAProgressDialogFragment extends DialogFragment {
	public static final String DEFAULT_FRAGMENT_TAG = "dialog";
	
	protected static final String DIALOG_ID = "dialog_id";
	protected static final String DIALOG_DATA = "dialog_data";
	
	private static final String TITLE = "title";
	private static final String TEXT = "text";
	private static final String CANCELABLE = "cancelable";
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * create
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static JSAProgressDialogFragment create(String title) {
		return create(title, (String) null);
	}
	
	public static JSAProgressDialogFragment create(Context context, int title, int text) {
		return create(context.getString(title), context.getString(text));
	}
	
	public static JSAProgressDialogFragment create(String title, String text) {
		return create(title, text, false);
	}
	
	public static JSAProgressDialogFragment create(Context context, int title, int text, boolean cancelable) {
		return create(context.getString(title), context.getString(text), cancelable);
	}
	
	public static JSAProgressDialogFragment create(String title, String text, boolean cancelable) {
		JSAProgressDialogFragment fragment = new JSAProgressDialogFragment();
		
		Bundle arguments = new Bundle();
		arguments.putString(TITLE, title);
		arguments.putString(TEXT, text);
		arguments.putBoolean(CANCELABLE, cancelable);
		
		fragment.setArguments(arguments);
        return fragment;
    }
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * set id
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public JSAProgressDialogFragment setId(int id) {
		Bundle arguments = getArguments() != null ? getArguments() : new Bundle();
		arguments.putInt(DIALOG_ID, id);
		return this;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * set data
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public JSAProgressDialogFragment setData(Serializable data) {
		Bundle arguments = getArguments() != null ? getArguments() : new Bundle();
		arguments.putSerializable(DIALOG_DATA, data);
		return this;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * show
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public void show(FragmentActivity activity) {
		show(activity, DEFAULT_FRAGMENT_TAG);
	}
	
	public void show(FragmentActivity activity, String tag) {
		if (tag == null) throw new IllegalArgumentException();
		FragmentManager fm = activity.getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
	    Fragment previousFragment = fm.findFragmentByTag(tag);
	    if (previousFragment != null) ft.remove(previousFragment);
	    ft.addToBackStack(null);
	    show(ft, tag);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * on create dialog
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	@Override public final Dialog onCreateDialog(Bundle savedInstanceState) {
		return onCreateDialogInternal(savedInstanceState, null);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * on create dialog internal
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
    private Dialog onCreateDialogInternal(Bundle savedInstanceState, DialogInterface.OnCancelListener listener) {
    	Bundle arguments = getArguments();
    	String title = arguments.getString(TITLE);
    	String text = arguments.getString(TEXT);
    	boolean cancelable = arguments.getBoolean(CANCELABLE);

    	ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setTitle(title);
        dialog.setMessage(text);
        dialog.setIndeterminate(false);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(listener);
        
        setCancelable(cancelable);
        return dialog;
    }
	
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * get dialog
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
    
	@Override public ProgressDialog getDialog() {
		return (ProgressDialog) super.getDialog();
	}
	
}
