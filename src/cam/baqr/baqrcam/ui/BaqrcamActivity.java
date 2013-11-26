package cam.baqr.baqrcam.ui;

import com.baqr.baqr.R;
import com.baqr.baqrcam.api.CustomHttpServer;
import com.baqr.baqrcam.api.CustomRtspServer;
import com.baqr.http.TinyHttpServer;
import com.baqr.streaming.SessionBuilder;
import com.baqr.streaming.rtsp.RtspServer;
import com.baqr.streaming.video.VideoQuality;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

/** 
 * Baqrcam basically launches an RTSP server and an HTTP server, 
 * clients can then connect to them and start/stop audio/video streams on the phone.
 */
public class BaqrcamActivity extends FragmentActivity {
	
	/** Default quality of video streams. */
	public static VideoQuality videoQuality = new VideoQuality(640,480,15,500000);

	/** By default AMR is the audio encoder. */
	public static int audioEncoder = SessionBuilder.AUDIO_AMRNB;

	/** By default H.263 is the video encoder. */
	public static int videoEncoder = SessionBuilder.VIDEO_H263;

	/** If the notification is enabled in the status bar of the phone. */
	public boolean notificationEnabled = true;

	/** The HttpServer will use those variables to send reports about the state of the app to the web interface. */
	public boolean applicationForeground = true;
	public Exception lastCaughtException = null;

	/** Contains an approximation of the battery level. */
	public int batteryLevel = 0;

	public final int HANDSET = 0x01;
	public final int TABLET = 0x02;
	
	private static BaqrcamActivity sDroidActivity;

	// We assume that the device is a phone
	public int device = HANDSET;

	private ViewPager mViewPager;
	private PowerManager.WakeLock mWakeLock;
	private SectionsPagerAdapter mAdapter;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private CustomHttpServer mHttpServer;
	private RtspServer mRtspServer;

	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Get context of activity to be passed to other classes
		sDroidActivity = this;
		
		// Set back button as home
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Get preferences
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		notificationEnabled = settings.getBoolean("notification_enabled", true);
		
		// On android 3.* AAC ADTS is not supported so we set the default encoder to AMR-NB, on android 4.* AAC is the default encoder
		audioEncoder = (Integer.parseInt(android.os.Build.VERSION.SDK)<14) ? SessionBuilder.AUDIO_AMRNB : SessionBuilder.AUDIO_AAC;
		audioEncoder = Integer.parseInt(settings.getString("audio_encoder", String.valueOf(audioEncoder)));
		videoEncoder = Integer.parseInt(settings.getString("video_encoder", String.valueOf(videoEncoder)));

		// Read video quality settings from the preferences 
		videoQuality = VideoQuality.merge(
				new VideoQuality(
						settings.getInt("video_resX", 0),
						settings.getInt("video_resY", 0), 
						Integer.parseInt(settings.getString("video_framerate", "0")), 
						Integer.parseInt(settings.getString("video_bitrate", "0"))*1000),
						videoQuality);

		SessionBuilder.getInstance() 
		.setContext(getApplicationContext())
		.setAudioEncoder(!settings.getBoolean("stream_audio", true)?0:audioEncoder)
		.setVideoEncoder(!settings.getBoolean("stream_video", false)?0:videoEncoder)
		.setVideoQuality(videoQuality);

		// Listens to changes of preferences
		settings.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);

		registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		setContentView(R.layout.baqrcam_baqrcam);

		mAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.handset_pager);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		mSurfaceView = (SurfaceView)findViewById(R.id.handset_camera_view);
		mSurfaceHolder = mSurfaceView.getHolder();
		// We still need this line for backward compatibility reasons with android 2
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		SessionBuilder.getInstance().setSurfaceHolder(mSurfaceHolder);

		mViewPager.setAdapter(mAdapter);

		// Prevents the phone from going to sleep mode
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "com.baqr.Baqrcam.wakelock");

		// Starts the service of the HTTP server
		this.startService(new Intent(this,CustomHttpServer.class));

		// Starts the service of the RTSP server
		this.startService(new Intent(this,CustomRtspServer.class));

	}
	
	/**
	 * Get instance of Baqrcam to be passed to other classes
	 * @return
	 */
	public static BaqrcamActivity getInstance() {
		return sDroidActivity;
	}

	public void onStart() {
		super.onStart();

		// Lock screen
		mWakeLock.acquire();

		// Did the user disabled the notification ?
		if (this.notificationEnabled) {
			Intent notificationIntent = new Intent(this, BaqrcamActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

			NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
			Notification notification = builder.setContentIntent(pendingIntent)
					.setWhen(System.currentTimeMillis())
					.setTicker(getText(R.string.notification_title))
					.setSmallIcon(R.drawable.icon)
					.setContentTitle(getText(R.string.notification_title))
					.setContentText(getText(R.string.notification_content)).build();
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(0,notification);
		} else {
			removeNotification();
		}

		bindService(new Intent(this,CustomHttpServer.class), mHttpServiceConnection, Context.BIND_AUTO_CREATE);
		bindService(new Intent(this,CustomRtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);

	}

	@Override
	public void onStop() {
		super.onStop();
		// A WakeLock should only be released when isHeld() is true !
		if (mWakeLock.isHeld()) mWakeLock.release();
		if (mHttpServer != null) mHttpServer.removeCallbackListener(mHttpCallbackListener);
		unbindService(mHttpServiceConnection);
		if (mRtspServer != null) mRtspServer.removeCallbackListener(mRtspCallbackListener);
		unbindService(mRtspServiceConnection);
	}

	@Override
	public void onResume() {
		super.onResume();
		this.applicationForeground = true;
	}

	@Override
	public void onPause() {
		super.onPause();
		this.applicationForeground = false;
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mBatteryInfoReceiver);
		super.onDestroy();
	}

	@Override    
	public void onBackPressed() {
		Intent setIntent = new Intent(Intent.ACTION_MAIN);
		setIntent.addCategory(Intent.CATEGORY_HOME);
		setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(setIntent);
	}

	@Override    
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.baqrcam_menu, menu);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.quit), 1);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.options), 1);
		return true;
	}

	@Override    
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;

		switch (item.getItemId()) {
		case R.id.options:
			// Starts QualityListActivity where user can change the streaming quality
			intent = new Intent(BaqrcamActivity.this, OptionsActivity.class);
			startActivityForResult(intent, 0);
			return true;
		case R.id.quit:
			quitBaqrcam();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void quitBaqrcam() {
		// Removes notification
		if (this.notificationEnabled) removeNotification();       
		// Kills HTTP server
		this.stopService(new Intent(this,CustomHttpServer.class));
		// Kills RTSP server
		this.stopService(new Intent(this,CustomRtspServer.class));
		// Returns to home menu
		finish();
	}
	
	private ServiceConnection mRtspServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mRtspServer = (CustomRtspServer) ((RtspServer.LocalBinder)service).getService();
			mRtspServer.addCallbackListener(mRtspCallbackListener);
			mRtspServer.start();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {}

	};

	private RtspServer.CallbackListener mRtspCallbackListener = new RtspServer.CallbackListener() {

		@Override
		public void onError(RtspServer server, Exception e, int error) {
			// We alert the user that the port is already used by another app.
			if (error == RtspServer.ERROR_BIND_FAILED) {
				new AlertDialog.Builder(BaqrcamActivity.this)
				.setTitle(R.string.port_used)
				.setMessage(getString(R.string.bind_failed, "RTSP"))
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						startActivityForResult(new Intent(BaqrcamActivity.this, OptionsActivity.class),0);
					}
				})
				.show();
			}
		}

		@Override
		public void onMessage(RtspServer server, int message) {
			if (message==RtspServer.MESSAGE_STREAMING_STARTED) {
				if (mAdapter != null && mAdapter.getHandsetFragment() != null) 
					mAdapter.getHandsetFragment().update();
			} else if (message==RtspServer.MESSAGE_STREAMING_STOPPED) {
				if (mAdapter != null && mAdapter.getHandsetFragment() != null) 
					mAdapter.getHandsetFragment().update();
			}
		}

	};	

	private ServiceConnection mHttpServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mHttpServer = (CustomHttpServer) ((TinyHttpServer.LocalBinder)service).getService();
			mHttpServer.addCallbackListener(mHttpCallbackListener);
			mHttpServer.start();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {}

	};

	private TinyHttpServer.CallbackListener mHttpCallbackListener = new TinyHttpServer.CallbackListener() {

		@Override
		public void onError(TinyHttpServer server, Exception e, int error) {
			// We alert the user that the port is already used by another app.
			if (error == TinyHttpServer.ERROR_HTTP_BIND_FAILED ||
					error == TinyHttpServer.ERROR_HTTPS_BIND_FAILED) {
				String str = error==TinyHttpServer.ERROR_HTTP_BIND_FAILED?"HTTP":"HTTPS";
				new AlertDialog.Builder(BaqrcamActivity.this)
				.setTitle(R.string.port_used)
				.setMessage(getString(R.string.bind_failed, str))
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						startActivityForResult(new Intent(BaqrcamActivity.this, OptionsActivity.class),0);
					}
				})
				.show();
			}
		}

		@Override
		public void onMessage(TinyHttpServer server, int message) {
			if (message==CustomHttpServer.MESSAGE_STREAMING_STARTED) {
				if (mAdapter != null && mAdapter.getHandsetFragment() != null) 
					mAdapter.getHandsetFragment().update();
				if (mAdapter != null && mAdapter.getPreviewFragment() != null)	
					mAdapter.getPreviewFragment().update();
			} else if (message==CustomHttpServer.MESSAGE_STREAMING_STOPPED) {
				if (mAdapter != null && mAdapter.getHandsetFragment() != null) 
					mAdapter.getHandsetFragment().update();
				if (mAdapter != null && mAdapter.getPreviewFragment() != null)	
					mAdapter.getPreviewFragment().update();
			}
		}

	};

	private void removeNotification() {
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
	}

	public void log(String s) {
		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
	}

	class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			if (device == HANDSET) {
				switch (i) {
				case 0: return new HandsetFragment(BaqrcamActivity.this);
				case 1: return new PreviewFragment(BaqrcamActivity.this);
				}
			} else {
				switch (i) {
				case 0: return new TabletFragment(BaqrcamActivity.this);
				}        		
			}
			return null;
		}

		@Override
		public int getCount() {
			return device==HANDSET ? 2 : 2;
		}

		public HandsetFragment getHandsetFragment() {
			return (HandsetFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.handset_pager+":0");
		}

		public PreviewFragment getPreviewFragment() {
			if (device == HANDSET) {
				return (PreviewFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.handset_pager+":1");
			} else {
				return (PreviewFragment) getSupportFragmentManager().findFragmentById(R.id.preview);
			}
		}

		@Override
		public CharSequence getPageTitle(int position) {
			if (device == HANDSET) {
				switch (position) {
				case 0: return getString(R.string.page0);
				case 1: return getString(R.string.page1);
				}        		
			} else {
				switch (position) {
				case 0: return getString(R.string.page0);
				}
			}
			return null;
		}

	}
	
	private OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals("video_resX") || key.equals("video_resY")) {
				videoQuality.resX = sharedPreferences.getInt("video_resX", 0);
				videoQuality.resY = sharedPreferences.getInt("video_resY", 0);
			}

			else if (key.equals("video_framerate")) {
				videoQuality.framerate = Integer.parseInt(sharedPreferences.getString("video_framerate", "0"));
			}

			else if (key.equals("video_bitrate")) {
				videoQuality.bitrate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "0"))*1000;
			}

			else if (key.equals("audio_encoder") || key.equals("stream_audio")) { 
				audioEncoder = Integer.parseInt(sharedPreferences.getString("audio_encoder", String.valueOf(audioEncoder)));
				SessionBuilder.getInstance().setAudioEncoder( audioEncoder );
				if (!sharedPreferences.getBoolean("stream_audio", false)) 
					SessionBuilder.getInstance().setAudioEncoder(0);
			}

			else if (key.equals("stream_video") || key.equals("video_encoder")) {
				videoEncoder = Integer.parseInt(sharedPreferences.getString("video_encoder", String.valueOf(videoEncoder)));
				SessionBuilder.getInstance().setVideoEncoder( videoEncoder );
				if (!sharedPreferences.getBoolean("stream_video", true)) 
					SessionBuilder.getInstance().setVideoEncoder(0);
			}

			else if (key.equals("notification_enabled")) {
				notificationEnabled  = sharedPreferences.getBoolean("notification_enabled", true);
			}

		}  
	};

	private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			batteryLevel = intent.getIntExtra("level", 0);
		}
	};
}