package cam.baqr.baqrcam.ui;

import com.baqr.baqr.R;
import com.baqr.baqrcam.api.CustomHttpServer;
import com.baqr.baqrcam.api.CustomRtspServer;
import com.baqr.http.TinyHttpServer;
import com.baqr.streaming.SessionBuilder;
import com.baqr.streaming.rtsp.RtspServer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PreviewFragment extends Fragment {

	public final static String TAG = "PreviewFragment";

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private TextView mTextView;
    private CustomHttpServer mHttpServer;
    private RtspServer mRtspServer;
    private Context context;
    
    public PreviewFragment(Context ctx) {
    	context = ctx;
    }
	
	@SuppressWarnings("deprecation")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.baqrcam_preview,container,false);

		mTextView = (TextView)rootView.findViewById(R.id.tooltip);
		
		if (((BaqrcamActivity)getActivity()).device == ((BaqrcamActivity)getActivity()).TABLET) {

			mSurfaceView = (SurfaceView)rootView.findViewById(R.id.tablet_camera_view);
			mSurfaceHolder = mSurfaceView.getHolder();

			// We still need this line for backward compatibility reasons with android 2
			mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

			SessionBuilder.getInstance().setSurfaceHolder(mSurfaceHolder);

		} 
		
		return rootView;
	}
	
	@Override
	public void onPause() {
		super.onPause();
    	context.unbindService(mHttpServiceConnection);
    	context.unbindService(mRtspServiceConnection);
	}
	
	@Override
    public void onResume() {
    	super.onResume();
		context.bindService(new Intent(context,CustomHttpServer.class), mHttpServiceConnection, Context.BIND_AUTO_CREATE);
		context.bindService(new Intent(context,CustomRtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
    }
	
	public void update() {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mTextView != null) {
					if ((mRtspServer != null && mRtspServer.isStreaming()) || (mHttpServer != null && mHttpServer.isStreaming()))
						mTextView.setVisibility(View.INVISIBLE);
					else 
						mTextView.setVisibility(View.VISIBLE);
				}
			}
		});
	}
	
    private final ServiceConnection mRtspServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mRtspServer = (RtspServer) ((RtspServer.LocalBinder)service).getService();
			update();
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {}
	};
    
    private final ServiceConnection mHttpServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mHttpServer = (CustomHttpServer) ((TinyHttpServer.LocalBinder)service).getService();
			update();
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {}
	};
	
}
