package pr.sna.snaprkit.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class AlertUtils
{
	public static void showAlert(Context context, String message, String title)
	{
		//if (!isFinishing()) // Need check to avoid random crashes when we are in the background
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setMessage(message);
			builder.setCancelable(true);
			builder.setTitle(title);
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}
}