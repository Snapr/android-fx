package pr.sna.snaprkit.test;

import pr.sna.snaprkit.R;
import pr.sna.snaprkit.SnaprKitFragment;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class SnaprKitTestActivity extends FragmentActivity {

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_activity_layout);

		if (savedInstanceState == null) {
			Fragment fragment = SnaprKitTestFragment.newInstance();
			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, "content").commit();
		}
	}

	public static class SnaprKitTestFragment extends SnaprKitFragment {
		
		public static SnaprKitTestFragment newInstance() {
			SnaprKitTestFragment fragment = new SnaprKitTestFragment();
			/* no args to set */
			return fragment;
		}
		
		@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View parent = super.onCreateView(inflater, container, savedInstanceState);
			parent.setVisibility(View.GONE); // hide parent view
			LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.test_fragment_layout, container, false);
			layout.addView(parent); // add parent view for compatibility
			return layout;
		}
		
		@Override public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			setFilterPackPath("filter_packs/zombies");
			setStickerPackPath("sticker_packs/zombies");
			
			getView().findViewById(android.R.id.button1).setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					runCameraAction();
				}
			});
			getView().findViewById(android.R.id.button2).setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					runGalleryAction();
				}
			});
			
			getView().findViewById(android.R.id.button3).setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					runStaticAction("/storage/sdcard0/DCIM/Camera/SNAPR_20121104_112613.jpg");
				}
			});
		}
		
	}
	
}
