package com.baqr.baqrcam.ui;

import com.baqr.baqr.R;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TabletFragment extends Fragment {
	
	private Context context;
	
	public TabletFragment(Context ctx) {
		context = ctx;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.baqrcam_tablet,container,false);
		return rootView ;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

}
