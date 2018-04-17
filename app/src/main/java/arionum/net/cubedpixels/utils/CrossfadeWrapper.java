package arionum.net.cubedpixels.utils;

import com.mikepenz.crossfader.Crossfader;
import com.mikepenz.materialdrawer.interfaces.ICrossfader;

public class CrossfadeWrapper implements ICrossfader {
	private Crossfader mCrossfader;

	public CrossfadeWrapper(Crossfader crossfader) {
		this.mCrossfader = crossfader;
	}

    //WRAPPER FOR ICONS
    @Override
	public void crossfade() {
		mCrossfader.crossFade();
	}

	@Override
	public boolean isCrossfaded() {
		return mCrossfader.isCrossFaded();
	}
}