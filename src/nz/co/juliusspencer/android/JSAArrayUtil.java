package nz.co.juliusspencer.android;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JSAArrayUtil {

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * to array
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	@SuppressWarnings("unchecked")
	public static <T> T[] toArray(Collection<? extends T> items, Class<T> cls) {
		T[] array = (T[]) Array.newInstance(cls, 0);
		return items.toArray(array);
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * to array list
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static <T> List<T> toArrayList(T[] items) {
		if (items == null) return null;
		List<T> result = new ArrayList<T>();
		for (T item : items) result.add(item);
		return result;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * to primitive
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static int[] toPrimitive(Integer[] list) {
		int[] result = new int[list.length];
		for (int i = 0; i < list.length; i++) 
			result[i] = list[i];
		return result;
	}
	
	public static int[] toPrimitiveInt(List<Integer> list) {
		int[] result = new int[list.size()];
		for (int i = 0; i < list.size(); i++) 
			result[i] = list.get(i);
		return result;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * from primitive
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static Integer[] fromPrimitive(int[] list) {
		Integer[] result = new Integer[list.length];
		for (int i = 0; i < list.length; i++) 
			result[i] = list[i];
		return result;
	}
	
	public static List<Integer> fromPrimitiveList(int[] list) {
		List<Integer> result = new ArrayList<Integer>(list.length);
		for (int i = 0; i < list.length; i++) 
			result.add(list[i]);
		return result;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * join
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	public static <T> String join(Collection<T> items, String separator) {
		StringBuilder builder = new StringBuilder();
		List<T> list = items instanceof List ? (List<T>) items : new ArrayList<T>(items);
		for (int i = 0; i < list.size(); i++) {
			builder.append(list.get(i));
			if (i != list.size() - 1) builder.append(separator);
		}
		return builder.toString();
	}
	
}
