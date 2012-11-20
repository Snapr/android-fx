package pr.sna.snaprkit;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import pr.sna.snaprkit.utils.ExceptionUtils;
import pr.sna.snaprkit.utils.SnaprJsonUtils;
import pr.sna.snaprkit.utils.CountingMultipartEntity;
import pr.sna.snaprkit.utils.CountingMultipartEntity.ProgressListener;

public class Uploader
{	
	// ------------------------------------------------------------------------
	// Members
	// ------------------------------------------------------------------------
	private UploadInfo mUploadInfo;
	private HttpPost mRequest;
	private UploadListener mUploadListener;
	private File mFile;
	private boolean mCanceled = false;
	private boolean mStopped = false;

	// ------------------------------------------------------------------------
	// Event listeners
	// ------------------------------------------------------------------------
	
    private ProgressListener mProgressListener = new ProgressListener()
    {

    	int last_percent = 100;
    	
		@Override
		public void transferred(long num)
		{
			// Call the upload listener
			if (mUploadListener != null)
			{
				double percentage = (double)num /  mFile.length();
				int percent = (int)(percentage * 100);
				
				// Ugly bug fix - for some narrow cropped images, the 
				// CountingMultipartEntity class will receive more bytes 
				// thru the write() methods than the actual file size
				// leading to percentages greater than 100
				if (percent > 100) percent = 100;
				
				if (last_percent != percent)
				{
					mUploadListener.onUploadProgress(mUploadInfo.getId(), percent);
					last_percent = percent;
				}
			}
		}
    
    };
    
	// ------------------------------------------------------------------------
	// Constructors and Destructor
	// ------------------------------------------------------------------------
    
    public Uploader()
    {
    }

    public Uploader(UploadInfo uploadInfo, UploadListener uploadListener)
    {
    	mUploadInfo = uploadInfo;
    	mUploadListener = uploadListener;
    }
    
    @Override
	protected void finalize() throws Throwable {

    	// Try and abort the upload
    	mRequest.abort();

		super.finalize();
	}
	
	// ------------------------------------------------------------------------
	// Getters / Setters
	// ------------------------------------------------------------------------
    	
	public UploadInfo getCurrentUpload()
	{
		return mUploadInfo;
	}

	public void setCurrentUpload(UploadInfo uploadInfo)
	{
		mUploadInfo = uploadInfo;
	}
	
	public UploadListener getUploadListener() {
		return mUploadListener;
	}
	
	public void setUploadListener(UploadListener uploadListener) {
		mUploadListener = uploadListener;
	}
	
	// ------------------------------------------------------------------------
	// Worker functions
	// ------------------------------------------------------------------------

	/**
	 * Starts the upload and a flag to indicate whether it was successful
	 * @return Boolean value that is true when the upload completed successfully
	 *         and is false when upload fails or is aborted / cancelled
	 */
	public boolean startUpload()
    {	
		// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());

    	// Clear internals
    	mCanceled = false;
    	mStopped = false;
    	
    	// Check parameters
    	if (mUploadInfo == null ||
    		mUploadInfo.getFileName() == null || mUploadInfo.getFileName().length() == 0 || 
    		mUploadInfo.getUploadUrl() == null || mUploadInfo.getUploadUrl().length() == 0 ||
    		mUploadListener == null)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to perform upload because one or more parameters are missing!");
    		return false;
    	}
    	
    	// Create the counting multipart entity
    	CountingMultipartEntity entity = new CountingMultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, mProgressListener);
    	
    	// Add the file parameter
    	mFile = new File(mUploadInfo.getFileName());
		FileBody encFile = new FileBody(mFile);
		entity.addPart("photo", encFile);
		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Added photo parameter (encoded file) to web request");
		
		// Add the string parameters
		try
		{
			// Parse the upload parameters and send them
			URI uri = URI.create("snapr://upload?" + mUploadInfo.getUploadParams());
			List<NameValuePair> params = URLEncodedUtils.parse(uri, "UTF-8");
			
			// Loop the list and add each parameter to the POST
			for (int i=0; i<params.size(); i++)
			{
				// Extract the GET param
				NameValuePair param = params.get(i);
				
				// Extract paramName and paramValue
				String paramName = param.getName();
				String paramValue = param.getValue();
				if (paramValue == null) paramValue = "";
				
				// Add the new POST param
				if (!paramName.equals("photo") && !paramName.equals("redirect_url"))
				{
					entity.addPart(paramName, new StringBody(paramValue, "text/plain", Charset.forName( "UTF-8" )));
					if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Added param " + paramName + " to web request as " + paramValue);
				}
			}
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed string parameter encoding with error " + ExceptionUtils.getExceptionStackString(e));
			return false;
		}
		
		// Declare
		boolean uploadSuccess = false;
		Exception exception = null;
		JSONObject jsonResponse = null;
		
		// Create the request and add the multipart contents
		mRequest = new HttpPost(mUploadInfo.getUploadUrl());
		mRequest.setEntity(entity);
		
		// Create the client
		DefaultHttpClient client = new DefaultHttpClient();
		
		try
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": File " + mUploadInfo.getFileName() + " upload started");
			if (mUploadListener != null) mUploadListener.onUploadStarted(mUploadInfo.getId());
			
			// Perform request
			// Request is performed synchronously
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = client.execute(mRequest, responseHandler);

            String errorMessage;
            if (responseBody != null && responseBody.length() > 0)
            {
            	// Log
                if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Response from upload is: " + responseBody);
                
                // Parse the response
                jsonResponse = new JSONObject(responseBody);
                uploadSuccess = SnaprJsonUtils.getOperationResult(jsonResponse, null);
                if (!uploadSuccess)
                {
                	errorMessage = SnaprJsonUtils.getOperationErrorMessage(jsonResponse, null);  
                	throw new RuntimeException(errorMessage);
                }
            }
        }
		catch (Exception e)
        {
			exception = e;
        }
		
		// Check upload status and call events
		if (mCanceled)
		{
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": File " + mUploadInfo.getFileName() + " upload cancelled");
    		if (mUploadListener != null) mUploadListener.onUploadCanceled(mUploadInfo.getId());			
		}
		else if (mStopped)
		{
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": File " + mUploadInfo.getFileName() + " upload stopped");
        	if (mUploadListener != null) mUploadListener.onUploadStopped(mUploadInfo.getId());
        	uploadSuccess = false;			
		}
		else if (exception != null)
		{
			// Notify user
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to perform upload with error: " +  ExceptionUtils.getExceptionStackString(exception));
            if (mUploadListener != null) mUploadListener.onUploadFailed(mUploadInfo.getId(), exception);
            uploadSuccess = false;
		}
        else
        {
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": File " + mUploadInfo.getFileName() + " upload finished");
        	if (mUploadListener != null) mUploadListener.onUploadComplete(mUploadInfo.getId(), jsonResponse);
        }
        
        // Clear internals
        mFile = null;
        mRequest = null;
    	mCanceled = false;
    	mStopped = false;
        
        if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
        
        return uploadSuccess;
    }
    
    /**
     * Used when the user cancels an upload
     */
    public void cancelUpload()
    {
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Stopping current upload");
    	
    	if (mRequest != null)
    	{
        	mCanceled = true;
    		mRequest.abort();
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Cancelling current upload (sent Httpclient abort)");
    	}
    	else
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Cancel request ignored because no upload in progress");
    	}
    }

    /**
     * Used when we need to stop an upload because of queue setting changes, etc. 
     */
    public void stopUpload()
    {
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	if (mRequest != null)
    	{
    		mStopped = true;
    		mRequest.abort();
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Stopping current upload (sent Httpclient abort)");
    	}
    	else
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Stop request ignored because no upload in progress");
    	}
    	
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
	// ------------------------------------------------------------------------
	// Listener definitions
	// ------------------------------------------------------------------------
    
    public interface UploadListener
    {
    	public void onUploadStarted(String id);
    	public void onUploadProgress(String id, int percent);
    	public void onUploadCanceled(String id);
    	public void onUploadStopped(String id);
    	public void onUploadFailed(String id, Exception e);
    	public void onUploadComplete(String id, JSONObject jsonResponse);
    }
}