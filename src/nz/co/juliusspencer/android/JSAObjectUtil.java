package nz.co.juliusspencer.android;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JSAObjectUtil {

	/** Return whether or not the two objects are equal (taking into consideration nulls). */
	public static boolean equals(Object a, Object b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		return a.equals(b);
	}

	/** Return a list of all the declared fields on the given class (including classes from which the given class extends). */
	public static List<Field> getDeclaredAndInheritedFields(Class<?> cls) {
		List<Field> fields = new ArrayList<Field>();
		Class<?> superClass = cls.getSuperclass();
		if (superClass != null) fields.addAll(getDeclaredAndInheritedFields(superClass));
		fields.addAll(Arrays.asList(cls.getDeclaredFields()));
		return fields;
	}
	
	/** Return a list of all declared methods on the given class (including classes from which the given class extends). */
	public static List<Method> getDeclaredAndInheritedMethods(Class<?> cls) {
		List<Method> methods = new ArrayList<Method>();
		Class<?> superclass = cls.getSuperclass();
		if (superclass != null) methods.addAll(getDeclaredAndInheritedMethods(superclass));
		methods.addAll(Arrays.asList(cls.getDeclaredMethods()));
		return methods;
	}
	
	/** Return the a list of all the declared fields on the given class (including classes from which the given class extends). */
	public static Field getDeclaredOrInheritedField(Class<?> cls, String name, boolean accessible) throws NoSuchFieldException {
		try {
			Field field = cls.getDeclaredField(name);
			field.setAccessible(accessible);
			return field;
		} catch (NoSuchFieldException exception) {
			Class<?> superclass = cls.getSuperclass();
			if (superclass == null) throw exception;
			return getDeclaredOrInheritedField(superclass, name, accessible);
		}
	}
	
	/** Return the method declared on the class (or a class from which the given class extends). */
	public static Method getDeclaredOrInheritedMethod(Class<?> cls, String name, Class<?> ... parameterTypes) throws SecurityException, NoSuchMethodException {
		try {
			return cls.getDeclaredMethod(name, parameterTypes);
		} catch (NoSuchMethodException exception) {
			Class<?> superclass = cls.getSuperclass();
			if (superclass == null) throw exception;
			return getDeclaredOrInheritedMethod(superclass, name, parameterTypes);
		}
	}
	
}
