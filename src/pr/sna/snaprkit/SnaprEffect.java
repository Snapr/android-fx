package pr.sna.snaprkit;

import pr.sna.snaprkit.SnaprFilterUtil.Filter;

public class SnaprEffect {
	
	private final Filter mFilter;
	private final boolean mLocked;
	private final String mUnlockMessage;

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constructors
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public SnaprEffect(Filter filter) {
		this(filter, false, null);
	}

	public SnaprEffect(Filter filter, boolean locked, String unlockMessage) {
		if (locked && unlockMessage == null) throw new IllegalArgumentException("A locked effect must provide an unlock message");
		if (filter == null) throw new IllegalArgumentException();
		mFilter = filter;
		mLocked = locked;
		mUnlockMessage = unlockMessage;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * getters
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public Filter getFilter() {
		return mFilter;
	}

	public boolean isLocked() {
		return mLocked;
	}

	public String getUnlockMessage() {
		return mUnlockMessage;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * equals
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	@Override public int hashCode() {
		return mFilter.getSlug().hashCode();
	}
	
	@Override public boolean equals(Object o) {
		if (!(o instanceof SnaprEffect)) return false;
		SnaprEffect effect = (SnaprEffect) o;
		return effect.mFilter.getSlug().equals(mFilter.getSlug());
	}
	
}
