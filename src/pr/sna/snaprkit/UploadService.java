package pr.sna.snaprkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pr.sna.snaprkit.utils.SnaprJsonUtils;
import pr.sna.snaprkit.Uploader.UploadListener;
import pr.sna.snaprkit.utils.CredentialsUtils;
import pr.sna.snaprkit.utils.FileUtils;
import pr.sna.snaprkit.utils.NetworkUtils;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;

public class UploadService extends Service
{
	// ------------------------------------------------------------------------
	// Members
	// ------------------------------------------------------------------------
	Thread mUploadThread = null;
	List<UploadInfo> mQueue = null;
	Uploader mUploader = null;
	boolean mQueueUploadModeWifiOnly = false;
	boolean mQueueUploadModeOn = true;
	boolean mUploadThreadRunning = false;

	private UploadInfo getUploadInfoById(String id)
	{
		// Find it in the queue
		if (mQueue != null)
		{
			synchronized (mQueue)
			{
				Iterator<UploadInfo> i = mQueue.iterator();
				while (i.hasNext())
				{
					UploadInfo uploadInfo = i.next();
					if (uploadInfo.getId().equals(id))
					{
						return uploadInfo;
					}
				}
			}
		}

		return null;
	}

	private boolean getSignupNeeded(JSONObject jsonResponse, String service)
	{
		boolean serviceSuccess = SnaprJsonUtils.getOperationResult(
				jsonResponse, service);
		if (!serviceSuccess)
		{
			int errorCode = SnaprJsonUtils.getOperationErrorCode(jsonResponse,
					service);

			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod() + ": For service "
						+ service + " got error " + errorCode);

			if (service.equals(Global.SHARE_SERVICE_TWITTER))
			{
				return (errorCode == Global.ERROR_TWITTER_SIGNUP_NEEDED);
			} else if (service.equals(Global.SHARE_SERVICE_FACEBOOK))
			{
				return (errorCode == Global.ERROR_FACEBOOK_SIGNUP_NEEDED);
			} else if (service.equals(Global.SHARE_SERVICE_TUMBLR))
			{
				return (errorCode == Global.ERROR_TUMBLR_SIGNUP_NEEDED);
			} else if (service.equals(Global.SHARE_SERVICE_FOURSQUARE))
			{
				return (errorCode == Global.ERROR_FOURSQUARE_SIGNUP_NEEDED);
			}
		}

		return false;
	}

	private String getSnaprIdFromJSON(JSONObject jsonResponse)
	{
		// Declare
		String snaprId = null;

		if (Global.LOG_MODE)
			Global.log(Global.getCurrentMethod() + ": Got JSON " + jsonResponse);

		// Parse the JSON
		try
		{
			// Get the response object
			JSONObject response = jsonResponse.getJSONObject("response");

			// Get the photo object
			JSONObject photo = response.getJSONObject("photo");

			// Get the id
			snaprId = photo.getString("id");
		} catch (JSONException e)
		{
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod() + ": Failed with error "
						+ e);
		}

		// Return
		if (Global.LOG_MODE)
			Global.log(Global.getCurrentMethod() + ": Returning SNAPR id "
					+ snaprId);

		return snaprId;
	}

	private String getSignupsNeeded(UploadInfo uploadInfo,
			JSONObject jsonResponse)
	{
		// Initialize
		String signupsNeeded = "";

		try
		{
			// Parse the Twitter linked service response
			if (uploadInfo.getTweet())
			{
				if (getSignupNeeded(jsonResponse, Global.SHARE_SERVICE_TWITTER))
				{
					signupsNeeded = signupsNeeded
							+ Global.SHARE_SERVICE_TWITTER + ",";
				}
			}

			// Parse the Facebook linked service response
			if (uploadInfo.getFacebookAlbum() || uploadInfo.getFacebookFeed())
			{
				if (getSignupNeeded(jsonResponse, Global.SHARE_SERVICE_FACEBOOK))
				{
					signupsNeeded = signupsNeeded
							+ Global.SHARE_SERVICE_FACEBOOK + ",";
				}
			}

			// Parse the Tumblr linked service response
			if (uploadInfo.getTumblr())
			{
				if (getSignupNeeded(jsonResponse, Global.SHARE_SERVICE_TUMBLR))
				{
					signupsNeeded = signupsNeeded + Global.SHARE_SERVICE_TUMBLR
							+ ",";
				}
			}

			// Parse the FourSquare linked service response
			if (uploadInfo.getFoursquareCheckin())
			{
				if (getSignupNeeded(jsonResponse,
						Global.SHARE_SERVICE_FOURSQUARE))
				{
					signupsNeeded = signupsNeeded
							+ Global.SHARE_SERVICE_FOURSQUARE + ",";
				}
			}

			// Fix termination
			if (signupsNeeded.length() > 0)
			{
				signupsNeeded = signupsNeeded.substring(0,
						signupsNeeded.length() - 1);
			}
		} catch (Exception e)
		{
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod() + ": Failed with error "
						+ e.toString());
		}

		return signupsNeeded;
	}

	private String getQueueJSON(String activeId, int activePercent)
	{
		JSONObject json = new JSONObject();
		JSONArray jsonUploads = new JSONArray();
		try
		{
			synchronized (mQueue)
			{
				for (int i = 0; i < mQueue.size(); i++)
				{
					// Declare
					JSONObject jsonUpload;
					UploadInfo uploadInfo = mQueue.get(i);

					// Create JSON upload
					String uploadId = uploadInfo.getId();
					if (activeId != null && uploadId != null
							&& uploadId.equals(activeId))
					{
						jsonUpload = uploadInfo.getUploadProgressJSON("active",
								activePercent);
					} else
					{
						jsonUpload = uploadInfo.getUploadProgressJSON(
								"waiting", 0);
					}

					// Add to uploads object
					jsonUploads.put(jsonUpload);
				}
			}
			json.put("uploads", jsonUploads);
		} catch (Exception e)
		{
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod()
						+ ": Failed to generate JSON with error " + e);
		}

		// Return
		return json.toString();
	}

	// UploadListener to support uploading
	private UploadListener mUploadListener = new UploadListener()
	{
		@Override
		public void onUploadStarted(String id)
		{
			if (Global.LOG_MODE)
				Global.log(" -> " + Global.getCurrentMethod());

			// Send an intent to broadcast to the main app
			Intent intent = new Intent();
			intent.setAction(Global.INTENT_BROADCAST_UPLOAD);
			intent.addCategory(Intent.CATEGORY_DEFAULT);
			intent.putExtra(Global.PARAM_ACTION,
					Global.BROADCAST_UPLOAD_STARTED);
			synchronized (mQueue)
			{
				intent.putExtra(Global.PARAM_NUM_UPLOADS, mQueue.size());
			}
			sendBroadcast(intent);

			// Log
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod()
						+ "Sent upload_started with " + mQueue.size()
						+ " uploads");
			if (Global.LOG_MODE)
				Global.log(" <- " + Global.getCurrentMethod());
		}

		@Override
		public void onUploadProgress(String id, int percent)
		{
			if (Global.LOG_MODE)
				Global.log(" -> " + Global.getCurrentMethod() + ": percent "
						+ percent);

			// Find our upload based on id
			UploadInfo uploadInfo = getUploadInfoById(id);

			if (uploadInfo != null)
			{
				// Create JSON data
				String jsonData = getQueueJSON(uploadInfo.getId(), percent);

				// Send an intent to broadcast to the main app
				Intent intent = new Intent();
				intent.setAction(Global.INTENT_BROADCAST_UPLOAD);
				intent.addCategory(Intent.CATEGORY_DEFAULT);
				intent.putExtra(Global.PARAM_ACTION,
						Global.BROADCAST_UPLOAD_PROGRESS);
				intent.putExtra(Global.PARAM_JSON_DATA, jsonData);
				sendBroadcast(intent);

				// Log
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod()
							+ ": Send broadcast for upload with id " + id);
			} else
			{
				// Log
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod()
							+ ": Could not find upload with id " + id + "!");
			}

			if (Global.LOG_MODE)
				Global.log(" <- " + Global.getCurrentMethod());
		}

		// The exception for this function is either a HttpClient exception,
		// an IOException or a JSON error wrapped in a RuntimeException
		@Override
		public void onUploadFailed(String id, Exception e)
		{
			try
			{
				// Special exception checking to only broadcast JSON exceptions
				// which we have previously wrapped in RuntimeException
				// Also remove upload from queue when encountering JSON
				// exceptions

				if (e instanceof RuntimeException)
				{
					// Broadcast error
					Intent intent = new Intent();
					intent.setAction(Global.INTENT_BROADCAST_UPLOAD);
					intent.addCategory(Intent.CATEGORY_DEFAULT);
					intent.putExtra(Global.PARAM_ACTION,
							Global.BROADCAST_UPLOAD_ERROR);
					intent.putExtra(Global.PARAM_ERROR_MESSAGE, e.getMessage());
					UploadService.this.sendBroadcast(intent);

					// Find upload and kill it
					UploadInfo info = getUploadInfoById(id);
					if (mQueue != null)
					{
						synchronized (mQueue)
						{
							mQueue.remove(info);
						}
					}
				}

				// Wait 3 seconds after failed uploads
				Thread.sleep(3000);
			} catch (Exception ex)
			{
			}
		}

		@Override
		public void onUploadComplete(String id, JSONObject jsonResponse)
		{
			if (Global.LOG_MODE)
				Global.log(" -> " + Global.getCurrentMethod());

			// Get the upload information
			UploadInfo uploadInfo = getUploadInfoById(id);

			// Check if we need sign ups
			String signupsNeeded = getSignupsNeeded(uploadInfo, jsonResponse);
			String snaprId = getSnaprIdFromJSON(jsonResponse);

			// Send an intent to broadcast to the main app
			Intent intent = new Intent();
			intent.setAction(Global.INTENT_BROADCAST_UPLOAD);
			intent.addCategory(Intent.CATEGORY_DEFAULT);
			intent.putExtra(Global.PARAM_ACTION,
					Global.BROADCAST_UPLOAD_COMPLETED);
			intent.putExtra(Global.PARAM_ID, id);
			intent.putExtra(Global.PARAM_SNAPR_ID, snaprId);
			intent.putExtra(Global.PARAM_SIGNUPS_NEEDED, signupsNeeded);
			synchronized (mQueue)
			{
				intent.putExtra(Global.PARAM_NUM_UPLOADS, mQueue.size() - 1); // subtract
																				// 1
																				// because
																				// we
																				// have
																				// not
																				// removed
																				// the
																				// completed
																				// upload
																				// from
																				// queue
			}
			UploadService.this.sendBroadcast(intent);

			if (Global.LOG_MODE)
				Global.log(" <- " + Global.getCurrentMethod());
		}

		@Override
		public void onUploadCanceled(String id)
		{
			if (Global.LOG_MODE)
				Global.log(" -> " + Global.getCurrentMethod());

			// Broadcast the cancel
			sendQueueItemCanceled(id);

			if (Global.LOG_MODE)
				Global.log(" <- " + Global.getCurrentMethod());
		}

		@Override
		public void onUploadStopped(String id)
		{
			// Update the Queue JSON
			sendQueueStatus();
		}
	};

	private int getQueueSize()
	{
		// Get the size
		int size = 0;
		if (mQueue != null)
		{
			synchronized (mQueue)
			{
				if (mQueue != null)
					size = mQueue.size();
			}
		}

		return size;
	}

	private void restoreQueue()
	{
		if (Global.LOG_MODE)
			Global.log(" -> " + Global.getCurrentMethod());

		// Get queue file contents
		String queueSettingsFileName = FileUtils
				.getSnaprCacheDirectory(getApplicationContext())
				+ "/queue.json";
		String jsonString = FileUtils.getStringFromFile(queueSettingsFileName);

		// Load the credentials from the shared preferences
		String credentials[] = new String[2];
		CredentialsUtils.loadCredentials(UploadService.this, credentials);
		// String userName = credentials[0];
		String accessToken = credentials[1];

		if (Global.LOG_MODE)
			Global.log(Global.getCurrentMethod()
					+ ": Loaded JSON queue string "
					+ ((jsonString != null) ? jsonString : ""));

		// Check if we have contents
		if (jsonString != null && jsonString.length() > 0)
		{
			try
			{
				// Convert string to JSON array
				JSONArray jsonArray = new JSONArray(jsonString);

				// Reinstantiate each object
				for (int i = 0; i < jsonArray.length(); i++)
				{
					// Create the UploadInfo
					JSONObject upload = jsonArray.getJSONObject(i);
					UploadInfo uploadInfo = UploadInfo.fromJSON(upload);
					uploadInfo.setAccessToken(accessToken);

					// See if the file is still in queue -- useful to remove old
					// data and maintain consistency
					if ((Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)
							|| (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED_READ_ONLY))
					{
						if (FileUtils.isFilePresent(uploadInfo.getFileName()))
						{
							synchronized (mQueue)
							{
								mQueue.add(uploadInfo);
								if (Global.LOG_MODE)
									Global.log(Global.getCurrentMethod()
											+ ": Added queue item "
											+ uploadInfo.getId()
											+ " with filename "
											+ uploadInfo.getFileName());
							}
						} else
						{
							if (Global.LOG_MODE)
								Global.log(Global.getCurrentMethod()
										+ ": Did not add queue item "
										+ uploadInfo.getId()
										+ " with filename "
										+ uploadInfo.getFileName()
										+ " because data file is missing");
						}
					} else
					{
						synchronized (mQueue)
						{
							mQueue.add(uploadInfo);
							if (Global.LOG_MODE)
								Global.log(Global.getCurrentMethod()
										+ ": Added queue item "
										+ uploadInfo.getId()
										+ " with filename "
										+ uploadInfo.getFileName());
						}
					}
				}
			} catch (Exception e)
			{
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod()
							+ ": Failed with error" + e.toString());
			}
		}

		if (Global.LOG_MODE)
			Global.log(" <- " + Global.getCurrentMethod());
	}

	private void saveQueue()
	{
		if (Global.LOG_MODE)
			Global.log(" -> " + Global.getCurrentMethod());

		JSONArray jsonArray = new JSONArray();

		// Process each object in the queue
		synchronized (mQueue)
		{
			// Process queue
			for (int i = 0; i < mQueue.size(); i++)
			{
				UploadInfo uploadInfo = mQueue.get(i);
				JSONObject upload = uploadInfo.toJSON();
				jsonArray.put(upload);
			}
		}

		// Transform the JSON array to string
		String jsonString = jsonArray.toString();

		if (Global.LOG_MODE)
			Global.log(Global.getCurrentMethod()
					+ ": Saving JSON queue string "
					+ ((jsonString != null) ? jsonString : ""));

		// Save the string to disk
		String queueSettingsFileName = FileUtils
				.getSnaprCacheDirectory(getApplicationContext())
				+ "/queue.json";
		FileUtils.saveStringToFile(jsonString, queueSettingsFileName);

		if (Global.LOG_MODE)
			Global.log(" <- " + Global.getCurrentMethod());
	}

	/**
	 * Checks whether the queue should upload based on connection settings
	 * 
	 * @return Boolean value indicating whether we should upload
	 */
	public boolean shouldUpload()
	{
		// Check conditions for uploading
		// Condition #1: We must have more than 0 files in queue
		if (getQueueSize() == 0)
		{
			// Log
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod()
						+ ": Exiting upload service because queue size is 0");

			// Return
			return false;
		}

		// Condition #2: User specified queue state must be on
		if (mQueueUploadModeOn == false)
		{
			// Log
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod()
						+ ": Exiting upload service because queue upload mode on is false");

			// Return
			return false;
		}

		// Condition #3: Thread state must be set to run
		if (mUploadThreadRunning == false)
		{
			// Log
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod()
						+ ": Exiting upload service because we received a stop request");

			// Return
			return false;
		}

		// Condition #4: If in Wifi-only mode, Wifi must be available
		if (mQueueUploadModeWifiOnly
				&& (NetworkUtils.isWifiConnected(UploadService.this) == false))
		{
			// Log
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod()
						+ ": Exiting upload service because we are in Wifi only mode and Wifi is off");

			// Return
			return false;
		}

		// Condition #5: Some network connection must be available
		if (NetworkUtils.isAnyConnected(UploadService.this) == false)
		{
			// Log
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod()
						+ ": Exiting upload service because there's no data connectivity");

			// Return
			return false;
		}

		// All conditions satisfied
		if (Global.LOG_MODE)
			Global.log(Global.getCurrentMethod()
					+ ": Continuing to process queue because all continuation conditions satisfied");
		return true;
	}

	@Override
	public void onCreate()
	{
		// Log
		if (Global.LOG_MODE)
			Global.log(" -> " + Global.getCurrentMethod());

		// Initialize the queue
		mQueue = Collections.synchronizedList(new ArrayList<UploadInfo>());

		// Restore the previous queue from disk
		restoreQueue();

		// Create the upload thread
		mUploadThread = new Thread()
		{
			public void run()
			{
				// Loop while files are left to be processed
				while (shouldUpload())
				{
					// Get the first file in the queue
					// Use synchronized block for thread safety
					UploadInfo uploadInfo = null;
					synchronized (mQueue)
					{
						try
						{
							uploadInfo = mQueue.get(0);
						}
						catch (IndexOutOfBoundsException e)
						{
						}
					}
					
					// Exit if we have no upload
					if (uploadInfo == null) continue;

					// Upload it synchronously
					// Send notifications through registered UploadListener
					mUploader = new Uploader(uploadInfo, mUploadListener);
					boolean success = mUploader.startUpload();

					// Log
					if (Global.LOG_MODE)
						Global.log(Global.getCurrentMethod()
								+ ": Upload success was " + success);

					// Remove it from array list and disk
					if (success)
					{
						if (Global.LOG_MODE)
							Global.log(Global.getCurrentMethod()
									+ ": Removing file "
									+ uploadInfo.getFileName()
									+ " from queue and disk");

						// Use synchronized block for thread safety to remove
						// from array
						synchronized (mQueue)
						{
							mQueue.remove(uploadInfo);
						}
					}
				}

				// We're done, so terminate service
				UploadService.this.stopSelf();

				// Log
				if (Global.LOG_MODE)
					Global.log(" <- " + Global.getCurrentMethod()
							+ ": Closing service...");
			}
		};

		super.onCreate();

		// Log
		if (Global.LOG_MODE)
			Global.log(" <- " + Global.getCurrentMethod());
	}

	@Override
	public void onDestroy()
	{
		// Log
		if (Global.LOG_MODE)
			Global.log(" -> " + Global.getCurrentMethod());

		// Stop the thread
		stopQueue();

		// Save the queue to disk
		saveQueue();

		// Perform other cleanup
		super.onDestroy();

		// Log
		if (Global.LOG_MODE)
			Global.log(" <- " + Global.getCurrentMethod());
	}

	public void sendQueueStatus()
	{
		// Broadcast queue JSON back to
		String jsonData = null;
		String currentUploadId = null;
		if (mUploader != null && mUploader.getCurrentUpload() != null)
			currentUploadId = mUploader.getCurrentUpload().getId();
		jsonData = getQueueJSON(currentUploadId, 0);

		// Send an intent to broadcast to the main app
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(Global.INTENT_BROADCAST_UPLOAD);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent.putExtra(Global.PARAM_ACTION,
				Global.BROADCAST_UPLOAD_STATUS);
		broadcastIntent.putExtra(Global.PARAM_JSON_DATA, jsonData);
		UploadService.this.sendBroadcast(broadcastIntent);
	}

	public void sendQueueItemCanceled(String id)
	{
		// Send an intent to broadcast to the main app
		Intent intent = new Intent();
		intent.setAction(Global.INTENT_BROADCAST_UPLOAD);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtra(Global.PARAM_ACTION, Global.BROADCAST_UPLOAD_CANCELED);
		intent.putExtra(Global.PARAM_ID, id);
		synchronized (mQueue)
		{
			intent.putExtra(Global.PARAM_NUM_UPLOADS, mQueue.size());
		}
		UploadService.this.sendBroadcast(intent);
	}

	public void startQueue()
	{
		if (Global.LOG_MODE)
			Global.log(" -> " + Global.getCurrentMethod());

		// Start the queue upload thread
		if (mUploadThreadRunning == false)
		{
			// Flip the flag for thread quit
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod()
						+ ": Starting upload thread");
			mUploadThreadRunning = true;

			// Must check thread state too for when start and stop are close
			// together, else we'll crash
			if (mUploadThread.getState() != Thread.State.RUNNABLE
					&& mUploadThread.getState() != Thread.State.WAITING
					&& mUploadThread.getState() != Thread.State.TIMED_WAITING
					&& mUploadThread.getState() != Thread.State.BLOCKED)
			{
				mUploadThread.start();
			}
		}

		if (Global.LOG_MODE)
			Global.log(" <- " + Global.getCurrentMethod());
	}

	public void stopQueue()
	{
		if (Global.LOG_MODE)
			Global.log(" -> " + Global.getCurrentMethod());
		mUploadThreadRunning = false; // Set flag to cause thread loop to quit
		if (mUploader != null)
		{
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod()
						+ ": Stopping upload thread");
			mUploader.stopUpload(); // Stop the upload so it hits flag sooner
		} else
		{
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod()
						+ ": Could not find upload manager!");
		}
		if (Global.LOG_MODE)
			Global.log(" <- " + Global.getCurrentMethod());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// Log
		if (Global.LOG_MODE)
			Global.log(" -> " + Global.getCurrentMethod());

		// Check parameters
		if (intent == null)
			return super.onStartCommand(intent, flags, startId);

		// Check the intent message type
		int action = intent.getIntExtra("action", -1);
		if (action == Global.ACTION_QUEUE_START)
		{
			// Set the queue upload mode
			mQueueUploadModeOn = intent.getBooleanExtra(
					Global.PARAM_QUEUE_UPLOAD_MODE_ON, false);
			mQueueUploadModeWifiOnly = intent.getBooleanExtra(
					Global.PARAM_QUEUE_UPLOAD_MODE_WIFI_ONLY, false);

			// Start the queue
			startQueue();
		} else if (action == Global.ACTION_QUEUE_STOP)
		{
			// Set queue upload mode
			mQueueUploadModeOn = intent.getBooleanExtra(
					Global.PARAM_QUEUE_UPLOAD_MODE_ON, false);
			mQueueUploadModeWifiOnly = intent.getBooleanExtra(
					Global.PARAM_QUEUE_UPLOAD_MODE_WIFI_ONLY, false);

			// Stop the queue
			stopQueue();
		} else if (action == Global.ACTION_QUEUE_CLEAR)
		{
			// Pause queue upload thread and then clear all data
			// Use synchronized block for thread safety
			stopQueue();
			synchronized (mQueue)
			{
				mQueue.clear();
			}
		} else if (action == Global.ACTION_QUEUE_ADD)
		{
			// Retrieve all upload parameters
			String accessToken = intent
					.getStringExtra(Global.PARAM_ACCESS_TOKEN);
			String id = intent.getStringExtra(Global.PARAM_ID);
			String photo = intent.getStringExtra(Global.PARAM_PHOTO);
			String description = intent
					.getStringExtra(Global.PARAM_DESCRIPTION);
			String latitude = intent.getStringExtra(Global.PARAM_LATITUDE);
			String longitude = intent.getStringExtra(Global.PARAM_LONGITUDE);
			String location = intent.getStringExtra(Global.PARAM_LOCATION);
			String date = intent.getStringExtra(Global.PARAM_DATE);
			String privacy = intent.getStringExtra(Global.PARAM_PRIVACY);
			boolean tweet = intent.getBooleanExtra(Global.PARAM_TWEET, false);
			boolean facebookFeed = intent.getBooleanExtra(
					Global.PARAM_FACEBOOK_FEED, false);
			boolean facebookAlbum = intent.getBooleanExtra(
					Global.PARAM_FACEBOOK_ALBUM, false);
			String facebookAlbumName = intent
					.getStringExtra(Global.PARAM_FACEBOOK_ALBUM_NAME);
			boolean tumblr = intent.getBooleanExtra(Global.PARAM_TUMBLR, false);
			boolean foursquareCheckin = intent.getBooleanExtra(
					Global.PARAM_FOURSQUARE_CHECKIN, false);
			String foursquareVenueId = intent
					.getStringExtra(Global.PARAM_FOURSQUARE_VENUE);
			String appGroup = intent.getStringExtra(Global.PARAM_APP_GROUP);
			String publicGroup = intent
					.getStringExtra(Global.PARAM_PUBLIC_GROUP);
			String uploadParams = intent.getStringExtra(Global.PARAM_UPLOAD_PARAMS);

			// Check if a file with the same id is already in the queue
			// Can happen when we press the Share button in quick succession
			UploadInfo existingUploadInfo = getUploadInfoById(id);
			if (existingUploadInfo == null)
			{
				if (Global.LOG_MODE)
					Global.log("File with id " + id + " does not exist ");

				// Create upload object
				UploadInfo uploadInfo = new UploadInfo();
				uploadInfo.setUploadUrl(Global.URL_UPLOAD_LOCATION);
				uploadInfo.setAccessToken(accessToken);
				uploadInfo.setId(id);
				uploadInfo.setFileName(photo);
				uploadInfo.setDescription(description);
				uploadInfo.setPrivacy(privacy);
				uploadInfo.setLatitude(latitude);
				uploadInfo.setLongitude(longitude);
				uploadInfo.setLocationName(location);
				uploadInfo.setPictureDateTime(date);
				uploadInfo.setTweet(tweet);
				uploadInfo.setFacebookFeed(facebookFeed);
				uploadInfo.setFacebookAlbum(facebookAlbum);
				uploadInfo.setFacebookAlbumName(facebookAlbumName);
				uploadInfo.setTumblr(tumblr);
				uploadInfo.setFoursquareCheckin(foursquareCheckin);
				uploadInfo.setFoursquareVenue(foursquareVenueId);
				uploadInfo.setAppGroup(appGroup);
				uploadInfo.setPublicGroup(publicGroup);
				uploadInfo.setUploadParams(uploadParams);

				// Add the object to queue
				// Use synchronized block for thread safety
				synchronized (mQueue)
				{
					mQueue.add(uploadInfo);
				}

				// Log
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod()
							+ "ACTION_QUEUE_ADD received parameters:");
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "accessToken = "
							+ accessToken);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "id = " + id);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "photo = " + photo);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "description = "
							+ description);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "latitude = "
							+ latitude);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "longitude = "
							+ longitude);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "location = "
							+ location);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "date = " + date);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "privacy = "
							+ privacy);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "tweet = " + tweet);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "facebookFeed = "
							+ facebookFeed);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "facebookAlbum = "
							+ facebookAlbum);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod()
							+ "facebookAlbumName = " + facebookAlbumName);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "tumblr = " + tumblr);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod()
							+ "foursquareCheckin = " + foursquareCheckin);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "foursquareVenue = "
							+ foursquareVenueId);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "appGroup = "
							+ appGroup);
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "publicGroup = "
							+ publicGroup);
				
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod() + "uploadParams = "
							+ uploadParams);

				// Start queue processing
				if (mQueueUploadModeOn && !mUploadThreadRunning)
				{
					startQueue();
				}
			} else
			{
				if (Global.LOG_MODE)
					Global.log("File with id " + id + " already EXISTS!!! ");
			}
		} else if (action == Global.ACTION_QUEUE_REMOVE)
		{
			// Retrieve file id parameter
			String id = intent.getStringExtra(Global.PARAM_ID);

			// Log
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod()
						+ "ACTION_QUEUE_REMOVE received parameters:");
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod() + "id = " + id);

			// Remove from list
			UploadInfo uploadInfo = getUploadInfoById(id);
			if (uploadInfo != null)
			{
				if (mUploader != null && mUploader.getCurrentUpload() != null
						&& mUploader.getCurrentUpload().getId().equals(id))
				{
					if (Global.LOG_MODE)
						Global.log(Global.getCurrentMethod()
								+ ": Cancelling ongoing upload");
					mUploader.cancelUpload();

					if (Global.LOG_MODE)
						Global.log(Global.getCurrentMethod()
								+ ": Removing upload from queue");
					synchronized (mQueue)
					{
						mQueue.remove(uploadInfo);
					}
				} else
				{
					if (Global.LOG_MODE)
						Global.log(Global.getCurrentMethod()
								+ ": Removing upload from queue");
					synchronized (mQueue)
					{
						mQueue.remove(uploadInfo);
					}

					// Brodcast the cancel (otherwise done from cancelUpload)
					if (Global.LOG_MODE)
						Global.log(Global.getCurrentMethod()
								+ ": Broadcasting cancel");
					sendQueueItemCanceled(id);
				}

				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod()
							+ ": Removing upload from disk");
				FileUtils.removeDiskFile(uploadInfo.getFileName());
			} else
			{
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod()
							+ ": Failed to get upload info");
				
				// Brodcast the cancel (for failed uploads)
				if (Global.LOG_MODE)
					Global.log(Global.getCurrentMethod()
							+ ": Broadcasting cancel");
				sendQueueItemCanceled(id);
				
			}
		} else if (action == Global.ACTION_QUEUE_UPLOAD_MODE)
		{
			// Change queue upload mode
			mQueueUploadModeOn = intent.getBooleanExtra(
					Global.PARAM_QUEUE_UPLOAD_MODE_ON, false);
			mQueueUploadModeWifiOnly = intent.getBooleanExtra(
					Global.PARAM_QUEUE_UPLOAD_MODE_WIFI_ONLY, false);

			// Log
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod()
						+ "ACTION_QUEUE_UPLOAD_MODE received parameters:");
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod() + "queueUploadModeOn = "
						+ mQueueUploadModeOn);
			if (Global.LOG_MODE)
				Global.log(Global.getCurrentMethod()
						+ "queueUploadModeWifiOnly = "
						+ mQueueUploadModeWifiOnly);
		} else if (action == Global.ACTION_QUEUE_UPDATE_STATUS)
		{
			sendQueueStatus();
		} else if (action == -1)
		{
			// Do nothing
		}

		// Log
		if (Global.LOG_MODE)
			Global.log(" <- " + Global.getCurrentMethod());
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
}