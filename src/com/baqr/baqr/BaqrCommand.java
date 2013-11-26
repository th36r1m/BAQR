package com.baqr.baqr;

import cam.baqr.baqrcam.ui.BaqrcamActivity;

import com.baqr.locate.BaqrLocateMain;
import com.bluetooth.DeviceSelectActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class BaqrCommand extends Fragment {

	public static final String ARG_SECTION_NUMBER = "section_number";
	
	private Context context;
	private Button videoBtn;
	private Button chatBtn;
	private Button bluetoothBtn;
	private Button vpnBtn;
	private Button locationBtn;
	private Button mapBtn;
	
	public BaqrCommand(Context ctx) {
		context = ctx;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.baqr_command, container, false);
		
		videoBtn = (Button) rootView.findViewById(R.id.videoBtn);
		chatBtn = (Button) rootView.findViewById(R.id.chatBtn);
		bluetoothBtn = (Button) rootView.findViewById(R.id.bluetoothBtn);
		vpnBtn = (Button) rootView.findViewById(R.id.vpnBtn);
		locationBtn = (Button) rootView.findViewById(R.id.locationBtn);
		mapBtn = (Button) rootView.findViewById(R.id.mapBtn);
		
		videoBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {

				try {
					Intent videoIntent = new Intent(getActivity(), BaqrcamActivity.class);
					startActivity(videoIntent);	
				} catch (Exception e) {
					Log.e("Error loading video intent: ", "" + e);
				}
			}
		});
		
		chatBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
		});
		
		bluetoothBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {

				try {
					Intent blueIntent = new Intent(context, DeviceSelectActivity.class);
					startActivity(blueIntent);	
				} catch (Exception e) {
					Log.e("Error loading video intent: ", "" + e);
				}
			}
		});
		
		vpnBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
		});
		
		locationBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					Intent locationIntent = new Intent(context, BaqrLocateMain.class);
					startActivity(locationIntent);	
				} catch (Exception e) {
					Log.e("Error loading location intent: ", "" + e);
				}
			}
		});
		
		mapBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
		});
	
		return rootView;
	}
}
