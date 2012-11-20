package pr.sna.snaprkit;

import pr.sna.snaprkit.R;
import pr.sna.snaprkit.utils.UrlUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class WebViewExternalActivity extends Activity {

	// Constants
	public static final int DISPLAY_EXTERNAL_URL = 4;
	
	// Members
	private WebView mWebView = null;
	private String  mStartupUrl = null;
	private String  mBareExitUrl = null;
	private String  mCurrentUrl = null;
	private Handler mHandler = new Handler();
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// Do not reload on screen orientation changes
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		// Call base implementation
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		// Call base implementation
		super.onSaveInstanceState(outState);
		
		// Save our custom data fields
		if (Global.LOG_MODE) Global.log(" Saving instance state ");
		if (Global.LOG_MODE) Global.log(" -> mStartupUrl is: " + mStartupUrl);
		outState.putString("mStartupUrl", mStartupUrl);
		if (Global.LOG_MODE) Global.log(" -> mBareExitUrl is: " + mBareExitUrl);
		outState.putString("mBareExitUrl", mBareExitUrl);
		if (Global.LOG_MODE) Global.log(" -> mCurrentUrl is: " + mCurrentUrl);
		outState.putString("mCurrentUrl", mCurrentUrl);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
        super.onCreate(savedInstanceState);
        init(savedInstanceState);
        if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
	}
	
	@Override
	public void onBackPressed()
	{	
		// Get the webview
    	WebView webView = (WebView) findViewById(R.id.snaprkit_webViewExternal);
    	
    	// When the user presses the back key and there is history activity, go back
		if (webView.canGoBack()) {
			webView.goBack();
		}
		else
		{
			super.onBackPressed();
		}
	}
	
	/*
	 * This runnable automatically completes the page load within 5 seconds
	 * Workaround for the fact that progress and page completion events are
	 * sometimes disjointed, causing the yellow topbar to remain at the top
	 */
	private Runnable mFinishPageLoad = new Runnable()
	{
		public void run()
		{
			// Set progress to 100% so progress bar disappears
			WebViewExternalActivity.this.setTitle(getActivityTitle(100));
			WebViewExternalActivity.this.setProgress(10000);
			
			// Set the current URL
			mCurrentUrl = mWebView.getUrl();
			
			// Cancel pending requests
			mHandler.removeCallbacks(mFinishPageLoad);
			
			// Log
			if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + " with " + mCurrentUrl);
		}
	};
	
	public void initProgressBar()
	{
		// Add progress bar support
        this.getWindow().requestFeature(Window.FEATURE_PROGRESS);
        
        // Make progress bar visible
        getWindow().setFeatureInt( Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
	}
	
	public void initExitButton()
	{
		// Get the button 
		Button exitButton = (Button)findViewById(R.id.buttonExit);
		
		// Set click handler
		exitButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				// Terminate the activity
				WebViewExternalActivity.this.finish();
			}
		});
	}
	
	public String getActivityTitle(Integer progress)
	{
		// Set the title
		if(progress == 100)
		{
			return mWebView.getTitle();
		}
		else
		{
			return "Loading " + progress.toString() + "%...";
		}
	}

    // Set webview settings and display startup page
    public void initWebView(Bundle savedInstanceState)
    {    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Get the control
        mWebView = (WebView) findViewById(R.id.snaprkit_webViewExternal);
        
        // Override clicks
        mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, "external - shouldOverrideUrlLoading() " + url);
				
				String bareUrl = UrlUtils.getBareUrl(url);
				
				// Check if we are at the exit url
				if (mBareExitUrl != null && bareUrl != null && bareUrl.equals(mBareExitUrl))
				{
					// Close the activity and return redirect url
					Intent resultIntent = new Intent();
					resultIntent.putExtra(Global.PARAM_URL, url);
					setResult(Activity.RESULT_OK, resultIntent);
					finish();
				}
				else
				{
					// Load the Url 
					mWebView.loadUrl(url);					
				}
				
				return true;
			}

			@Override
			public void onPageFinished(WebView view, String url)
			{
				// Log
				if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + " with " + view.getUrl());
				
				// Update the UI to indicate page finished loading
				mHandler.post(mFinishPageLoad);
				
				// Proceed as usual
				super.onPageFinished(view, url);
				
				// Log
				if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
			}

			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				
				// Set progress to 100% so progress bar disappears
				WebViewExternalActivity.this.setTitle(getActivityTitle(100));
				WebViewExternalActivity.this.setProgress(10000);
				
				// Proceed as usual
				super.onReceivedError(view, errorCode, description, failingUrl);
			}
    	});
        
        // Add support for JavaScript alert dialogs - necessary for unlink dialogs
        mWebView.setWebChromeClient(new WebChromeClient() {
        	
            @Override
			public void onProgressChanged(WebView view, int newProgress) {
				super.onProgressChanged(view, newProgress);
				
				// Log
				if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + " with " + view.getUrl() + "," +  newProgress + "%");
				
				// Activities and WebViews measure progress with different scales.
			    // The progress meter will automatically disappear when we reach 100%
			    WebViewExternalActivity.this.setProgress(newProgress * 100);
			    WebViewExternalActivity.this.setTitle(getActivityTitle(newProgress));
			    
			    // Set timer which will automatically stop the page load indicator after 5 seconds
			    mHandler.removeCallbacks(mFinishPageLoad);
			    mHandler.postDelayed(mFinishPageLoad, 5000);
			    
			    // Log
				if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
			}
        	
        	@Override  
            public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result)  
            {
        		if (!isFinishing()) // Need check to avoid random crashes when we are in the backgroound
        		{
	                new AlertDialog.Builder(WebViewExternalActivity.this)  
	                    .setTitle(R.string.snaprkit_name)  
	                    .setMessage(message)  
	                    .setPositiveButton(android.R.string.ok,  
	                            new AlertDialog.OnClickListener()  
	                            {  
	                                public void onClick(DialogInterface dialog, int which)  
	                                {  
	                                    result.confirm();  
	                                }  
	                            })  
	                    .setCancelable(false)  
	                    .create()  
	                    .show();  
        		}
        		
                return true;  
            };
            
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) 
            {
            	if (!isFinishing()) // Need check to avoid random crashes when we are in the backgroound
            	{
	                new AlertDialog.Builder(WebViewExternalActivity.this)
	                    .setTitle(R.string.snaprkit_name)
	                    .setMessage(message)
	                    .setPositiveButton(android.R.string.ok, 
	                            new DialogInterface.OnClickListener() 
	                            {
	                                public void onClick(DialogInterface dialog, int which) 
	                                {
	                                    result.confirm();
	                                }
	                            })
	                    .setNegativeButton(android.R.string.cancel, 
	                            new DialogInterface.OnClickListener() 
	                            {
	                                public void onClick(DialogInterface dialog, int which) 
	                                {
	                                    result.cancel();
	                                }
	                            })
	                .create()
	                .show();
            	}
            
                return true;
            };
            
            @Override
            public void onReceivedTitle(WebView view, String title)
            {
                setTitle(title);
                super.onReceivedTitle(view, title);
            } 
        });
        
        // Enable JavaScript
        mWebView.getSettings().setJavaScriptEnabled(true);
        
        // Enable HTML5 local storage (but don't make it persistent)
        mWebView.getSettings().setDomStorageEnabled(true);
        
        // Clear spurious cache data
        mWebView.clearHistory();
        mWebView.clearFormData();
        mWebView.clearCache(true);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        
        // Accept cookies
        CookieSyncManager.createInstance(this); 
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        
        // Make sure that the webview does not allocate blank space on the side for the scrollbars
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        
        // Restore webview state
        if (savedInstanceState != null && savedInstanceState.isEmpty() == false)
        {
        	// Saved instance
        	// Restore the webview history
            // Must be done before using the webview, otherwise the restore fails
        	
        	// Log
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found webview state");
        	
        	// Restore the state
        	mWebView.restoreState(savedInstanceState);
        }
        
        // Load the URL
        if (mCurrentUrl != null)
        {
        	mWebView.loadUrl(mCurrentUrl);
        }
        else
        {
        	mWebView.loadUrl(mStartupUrl);
        }
        
        // Bugfix for http://code.google.com/p/android/issues/detail?id=7189
        // If we want the soft keyboard to show for form fields in the webview
        // we must obtain focus after load. We also need to add an event handler 
        // to obtain focus after every field touch
        mWebView.requestFocus(View.FOCUS_DOWN);
        mWebView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        if (!v.hasFocus()) {
                            v.requestFocus();
                        }
                        break;
                }
                return false;
            }
        });
        
        // Log
        if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    }

	public String getStartupUrl()
    {
    	return getIntent().getStringExtra("url");
    }
    
    public String getRedirectUrl()
    {
    	// Get the startup url
    	String startupUrl = getStartupUrl();
    	
    	// Check the result
    	if (startupUrl != null && startupUrl.length() > 0)
    	{
	    	// Parse it for redirect
	    	Uri uri = Uri.parse(startupUrl);

	    	if (uri != null)
	    	{
	    		// Get the redirect_url parameter
	    		String exitUrl = uri.getQueryParameter(Global.PARAM_REDIRECT);
	    		
	    		// Return
	    		if (exitUrl!= null && exitUrl.length() > 0)
	    		{
	    			return exitUrl;
	    		}
	    		else
	    		{
	    			return null;
	    		}
	    	}
    	}
    	
    	// Return
    	return null;
    }
	
    public void init(Bundle savedInstanceState)
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Init progress bar support - must come before layout
    	initProgressBar();
    	
    	// Set layout
    	setContentView(R.layout.snaprkit_webview_external);
    	
    	// Prepare the startup URL
    	mStartupUrl = getStartupUrl();
    	if (savedInstanceState != null && !savedInstanceState.isEmpty())
    	{
    		String savedStartupUrl = savedInstanceState.getString("mStartupUrl");
    		if (savedStartupUrl != null && savedStartupUrl.length()>0)
    		{
    			mStartupUrl = savedStartupUrl;
    		}
    	}
    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Startup Url for external webview is " + mStartupUrl);
    	
    	// Prepare the exit URL
    	String exitUrl = getRedirectUrl();
    	mBareExitUrl = UrlUtils.getBareUrl(exitUrl);
    	if (savedInstanceState != null && !savedInstanceState.isEmpty())
    	{
    		String savedBareExitUrl = savedInstanceState.getString("mBareExitUrl");
    		if (savedBareExitUrl != null && savedBareExitUrl.length()>0)
    		{
    			mBareExitUrl = savedBareExitUrl;
    		}
    	}
    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Exit Url for external webview is " + mBareExitUrl);
    	
    	// Prepare the current URL
    	mCurrentUrl = null;
    	if (savedInstanceState != null && !savedInstanceState.isEmpty())
    	{
    		String savedCurrentUrl = savedInstanceState.getString("mCurrentUrl");
    		if (savedCurrentUrl != null && savedCurrentUrl.length()>0)
    		{
    			mCurrentUrl = savedCurrentUrl;
    		}
    	}
    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Current Url for external webview is " + mCurrentUrl);
    	
    	// Prepare the exit button
    	initExitButton();
    	
        // Prepare the webview
    	initWebView(savedInstanceState);
        
        // Log
        if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    }
}
