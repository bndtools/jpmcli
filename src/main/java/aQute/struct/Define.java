package aQute.struct;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Define the semantics of this field. Can be used by the program to provide
 * information about a field
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
	ElementType.FIELD
})
public @interface Define {
	/**
	 * Human readable description of this field in English. Localization
	 * techniques should use the class.field ide to provide localized
	 * descriptions if needed.
	 * 
	 * @return the description, can be the empty string
	 */
	String description() default "";

	/**
	 * If true, this field is required to be not null (default).
	 * 
	 * @return if this field is required
	 */
	boolean optional() default false;

	/**
	 * If set, checks if this field should be regarded as a field to be
	 * considered in equals.
	 * 
	 * @return
	 */
	boolean id() default false;

	/**
	 * Regular expression patterns that must all match.
	 * 
	 * @return
	 */
	String pattern() default "";

	/**
	 * For numeric fields, check if they are more or equals than min
	 */
	long min() default Long.MIN_VALUE;

	/**
	 * For numeric fields, check if they are less or equals than max
	 */
	long max() default Long.MAX_VALUE;

	String label() default "";

	public static class FieldValidator extends struct {
		public String	label;
		public String	description;
		public boolean	optional	= false;
		public boolean	id			= false;
		public String	pattern;
		public long		min;
		public long		max;
	}

	public static class TypeValidator extends struct {
		public String						fqn;
		public Map<String,FieldValidator>	validators	= map();

		public static TypeValidator getValidator(Class< ? extends struct> type) {
			TypeValidator tv = new TypeValidator();
			tv.fqn = type.getSimpleName();
			for (Field f : struct.getCache(type).fs) {
				if (f.getName().equals("__extra"))
					continue;

				FieldValidator v = new FieldValidator();
				tv.validators.put(f.getName(), v);
				Define define = f.getAnnotation(Define.class);
				if (define != null) {
					v.label = emptyIsNull(define.label());
					v.description = emptyIsNull(define.description());
					v.optional = define.optional();
					v.id = define.id();
					v.pattern = emptyIsNull(define.pattern());
					v.min = define.min();
					v.max = define.max();
				} else {
					v.label = toLabel(f.getName());
					v.id = f.getName().equals("_id");
				}
			}
			return tv;
		}

		private static String toLabel(String name) {
			StringBuilder sb = new StringBuilder(Character.toUpperCase(name.charAt(0)));
			for (int i = 1; i < name.length() - 1; i++) {
				char c = name.charAt(i);
				char f = name.charAt(i + 1);
				if (Character.isUpperCase(c) && Character.isLowerCase(f)) {
					sb.append(' ');
				} else if (c == '_')
					sb.append(' ');
				else
					sb.append(c);
			}
			return sb.toString();
		}

		static String emptyIsNull(String s) {
			s = s.trim();
			if (s.isEmpty())
				return null;
			return s;
		}
	}

}
