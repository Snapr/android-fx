package pr.sna.snaprkit.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import pr.sna.snaprkit.Global;
import pr.sna.snaprkit.utils.AssetsCopier.AssetCopierListener;

import android.app.Activity;
import android.content.res.AssetManager;

public class AssetUtils
{
	public void prepareSnaprAssets(Activity activity, AssetCopierListener listener, boolean showDialogs)
	{
		if (android.os.Build.VERSION.SDK_INT >= Global.SDK_HONEYCOMB && !AssetUtils.areSnaprAssetsPresent(activity))
    	{
    		// Copy assets and load			
    		AssetsCopier assetsCopier = new AssetsCopier(activity, listener, showDialogs); 
    		assetsCopier.execute();
    	}
	}
	
	public static void copyAssetsToCache(Activity activity)
	{
		copyFileOrDir(activity, "snaprkit_html");
	}
	
	public static boolean areSnaprAssetsPresent(Activity activity)
	{
		 
		return pr.sna.snaprkit.utils.FileUtils.isDirectoryPresent(getSnaprAssetsDirectory(activity));
	}
	
	public static String getSnaprDataDirectory(Activity activity)
	{
		return "/data/data/" + activity.getPackageName() + "/"; // must end in slash
	}
	
	public static String getSnaprAssetsDirectory(Activity activity)
	{
		return getSnaprDataDirectory(activity) + "snaprkit_html/"; // must end in slash
	}
	
	private static void copyFileOrDir(Activity activity, String path)
	{
		AssetManager assetManager = activity.getAssets();
		String assets[] = null;
		try
		{
			assets = assetManager.list(path);
			if (assets.length == 0)
			{
				copyFile(activity, path);
			}
			else
			{
				String fullPath = getSnaprDataDirectory(activity) + path;
				File dir = new File(fullPath);
				if (!dir.exists())
				{
					dir.mkdir();
				}
				for (int i = 0; i < assets.length; ++i)
				{
					copyFileOrDir(activity, path + "/" + assets[i]);
				}
            }
        }
		catch (IOException e)
		{
			if (Global.LOG_MODE) Global.log("I/O Exception" + e.getMessage());
		}
	}

	private static void copyFile(Activity activity, String filename)
	{
		AssetManager assetManager = activity.getAssets();

		InputStream in = null;
		OutputStream out = null;
		try
		{
			in = assetManager.open(filename);
			String newFileName = "/data/data/" + activity.getPackageName() + "/" + filename;
			out = new FileOutputStream(newFileName);

			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1)
			{
				out.write(buffer, 0, read);
			}
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log( e.getMessage());
		}
	}
}