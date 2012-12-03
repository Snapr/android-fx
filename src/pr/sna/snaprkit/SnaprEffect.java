package pr.sna.snaprkit;

import java.io.Serializable;

import pr.sna.snaprkit.SnaprFilterUtil.Filter;

public class SnaprEffect {
	
	private final Filter mFilter;
	private final boolean mIsLocked;
	private final String mUnlockMessage;
	private final boolean mIsVisible;

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * constructors
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public SnaprEffect(Filter filter) {
		this(filter, false, null, true);
	}

	public SnaprEffect(Filter filter, EffectConfig config) {
		this(filter, config.mIsLocked, config.mUnlockMessage, config.mIsLocked);
		if (!filter.mSlug.equals(config.mSlug)) throw new IllegalArgumentException("config slug doesn't match filter: " + filter.mSlug + " " + config.mSlug);
	}
	
	public SnaprEffect(Filter filter, boolean locked, String unlockMessage, boolean visible) {
		if (locked && unlockMessage == null) throw new IllegalArgumentException("A locked effect must provide an unlock message");
		if (filter == null) throw new IllegalArgumentException();
		mFilter = filter;
		mIsLocked = locked;
		mUnlockMessage = unlockMessage;
		mIsVisible = visible;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * getters
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public Filter getFilter() {
		return mFilter;
	}

	public boolean isLocked() {
		return mIsLocked;
	}

	public String getUnlockMessage() {
		return mUnlockMessage;
	}

	public boolean isVisible() {
		return mIsVisible;
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
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * effect configuration
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/**
	 * A simple POJO class that bundles data into a configuration for effects. A configuration requires the slug of the effect, in order
	 * to match it against a specific {@link SnaprEffect}. An effect configuration defaults to being unlocked. This behaviour can be 
	 * changed by supplying an explicit value indicating whether the effect is (un)locked. If an effect is locked, an appropriate message 
	 * MUST be given for displaying to the user.
	 */
	
	public static final class EffectConfig implements Serializable {
		private static final long serialVersionUID = -7415383060387922436L;
		
		public final String mSlug;
		public boolean mIsLocked;
		public boolean mIsVisible;
		public String mUnlockMessage;
		
		public EffectConfig(String slug) {
			this(slug, true); /* unlocked by default */
		}
		
		public EffectConfig(String slug, boolean isVisible) {
			this(slug, false, null, isVisible); /* unlocked by default */
		}
		
		public EffectConfig(String slug, boolean isLocked, String unlockMessage) {
			this(slug, isLocked, unlockMessage, true);
		}
		
		/**
		 * Creates a new EffectConfig instance based on the given id, isLocked value and unlockMessage.
		 * Note that if isLocked == TRUE, you should also supply an unlockMessage.
		 * @param slug The slug of the effect. 
		 * @param isLocked Whether the effect is (un)locked.
		 * @param unlockMessage The message that should be displayed in case the effect is locked.
		 * @param isVisible Whether the effect is visible to the user or not.
		 */
		public EffectConfig(String slug, boolean isLocked, String unlockMessage, boolean isVisible) {
			if (isLocked && unlockMessage == null) throw new IllegalArgumentException("A locked effect should provide an unlock message!");
			mSlug = slug;
			mIsLocked = isLocked;
			mIsVisible = isVisible;
			mUnlockMessage = unlockMessage; 
		}
	}
}
