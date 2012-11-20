package nz.co.juliusspencer.android;

public class JSATuple<S, T> implements Comparable<JSATuple<S, T>> {
	protected S mA;
	protected T mB;

	public JSATuple(S a, T b) {
		mA = a;
		mB = b;
	}

	public S getA() {
		return mA;
	}

	public T getB() {
		return mB;
	}

	@Override public String toString() {
		return "(" + mA + ", " + mB + ")";
	}

	@Override public boolean equals(Object o) {
		if (o == null) throw new NullPointerException();
		if (!(o instanceof JSATuple<?, ?>)) return false;
		JSATuple<?, ?> tuple = (JSATuple<?, ?>) o;
		return JSAObjectUtil.equals(mA, tuple.mA) && JSAObjectUtil.equals(mB, tuple.mB);
	}
	
	@Override protected Object clone() {
		return new JSATuple<S, T>(mA, mB);
	}
	
	@Override public int hashCode() {
		int code = 0;
		if (mA != null) code += mA.hashCode();
		if (mB != null) code += mB.hashCode();
		return code;
	}

	/**
	 * Compare two tuples for sorting purposes.
	 * 
	 * The implementation compares the values of the two tuple values. If the first tuple values not equal, this comparison value is returned.
	 * If the first tuple values are equals, the comparison value of the second tuple value is returned.
	 * 
	 * If the given tuple is null, a {@link NullPointerException} is thrown.
	 * If the values of the tuple are null, the null values are considered to come before any other value (excluding the case where both are null).
	 */
	@Override public int compareTo(JSATuple<S, T> tuple) {
		if (tuple == null) throw new NullPointerException();
		if (this.equals(tuple)) return 0;
		
		boolean isFirstComparable = mA instanceof Comparable<?> || tuple.mA instanceof Comparable<?>;
		boolean isSecondComparable = mB instanceof Comparable<?> || tuple.mB instanceof Comparable<?>;
		if (!isFirstComparable && !isSecondComparable) throw new RuntimeException("types of tuple are not comparable");
		
		@SuppressWarnings("unchecked") Comparable<S> cA = isFirstComparable ? (Comparable<S>) mA : null;
		@SuppressWarnings("unchecked") Comparable<T> cB = isSecondComparable ? (Comparable<T>) mB : null;
		int firstCompare = JSAObjectUtil.equals(mA, tuple.mA) ? 0 : cA != null && tuple.mA != null ? cA.compareTo(tuple.mA) : mA == null ? -1 : 1;
		int secondCompare = JSAObjectUtil.equals(mB, tuple.mB) ? 0 : cB != null && tuple.mB != null ? cB.compareTo(tuple.mB) : mB == null ? -1 : 1;
		return firstCompare != 0 ? firstCompare : secondCompare;
	}
	
}
