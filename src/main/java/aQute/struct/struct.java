package aQute.struct;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

/**
 * A struct represents the struct in C, but then more powerful. Any class that
 * extends a struct must only use a set of basic classes in public fields.
 * Though implementation classes may have methods (public or private) it should
 * not have any non-public fields or abstract the access to the fields in
 * get/set methods. The basic idea of the struct is that the public fields are
 * the public API.
 * <p/>
 * The reason for the struct is that classes fall apart when there is a process
 * boundary. A programmer has a reasonable expectation that the program its code
 * runs in is consistent, the same classes are used to access the objects. This
 * is not true once you go to a distributed world, and today most worlds are
 * distributed. Once an object escapes the program, it MUST expose its internal
 * state, turning the internal state into public API. This is one of the main
 * reasons serialization in Java has failed. In an OO environment there is the
 * illusion that an object has privacy, but in virtually all non-toy programs
 * this illusion is too easily shattered. It also explains the success of ruby
 * and Javascript that both not even attempt to have private state.
 * <p/>
 * However, Java has many good parts and also OO has proven to be very useful in
 * lots of applications. To leverage all these good parts (especially the
 * amazing things IDEs do with the type information) this class attempts to find
 * a middle ground between the error-prone permissiveness of dynamic languages
 * and the overly constrained and often illusionary Java world. A struct is like
 * a constrained map (see {@link #asMap()} for more information). However,
 * structs provide type safe access and all the wonders of IDE magic like
 * refactoring and completion.
 * <p/>
 * structs should be used in places where information is shared between
 * different processes. The available methods make it possible to easily
 * serialize (JSON is built in, but other serializations are possible) because
 * its types are strictly limited. Traversing an object is straightforward for
 * this reason.
 * <p/>
 * Since the purpose of structs is to be used anywhere, this package is kept
 * simple and small to minimize the burden. However, it is expected that there
 * are many libraries that leverage structs since the type information in fields
 * makes it possible to decode for example a JSON stream into the type safe
 * structs.
 * <p/>
 * The following types are allowed in fields of structs:
 * 
 * <pre>
 * struct 
 * String 
 * primitives
 * Number subclasses
 * Iterable (lists, sets, etc)
 * {@code Map<String,Object>}
 * arrays
 * </pre>
 * 
 * Custom types are allowed but are translated to their toString()
 * representation and must be constructable with that representation. It is
 * usually MUCH better to deconstruct such objects into a complex struct.
 */
@SuppressWarnings("rawtypes")
public class struct implements Cloneable {

	/*
	 * Used to figure out if we need to escape a key with quotes if we print it
	 * in abbreviated form.
	 */
	final static Pattern	NEED_NO_ESCAPE	= Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");
	static Field[]			EMPTY			= new Field[] {};

	/**
	 * We maintain a cache of type information in the class (and an in a private
	 * instance variable) to speed up the use of reflection.
	 */
	static class Cache {
		/*
		 * Quick access to (reused) fields Notice that all fields are public and
		 * do not require setAccessible calls.
		 */
		Map<String,Field>	fields	= new HashMap<String,Field>();
		Field[]				fs;
		/*
		 * Array of sorted names
		 */
		String[]			names;

		/*
		 * Unmodifiable set of keys (used in keySet)
		 */
		Set<String>			keys	= Collections.unmodifiableSet(fields.keySet());

		/*
		 * The number of fields
		 */
		int					size;

		/*
		 * If Id annotations are specified, used for equals and hashCode
		 */
		Field[]				ids		= EMPTY;

	}

	/**
	 * Allow classes to be gc'ed, so keep the cache information in a
	 * {@link WeakHashMap}.
	 */
	private final static WeakHashMap<Class,Cache>	caches	= new WeakHashMap<Class,struct.Cache>();

	/*
	 * For fast access
	 */
	private final Cache								cache	= getCache(this.getClass());

	/*
	 * Maintains once calculated hash code TODO can we actually has, detect
	 * changes?
	 */
	private int										hash;

	/**
	 * If decoders detect that there is more information in the input, they can
	 * store this information in the __extra field. This field is normally not
	 * set and decoders must be able to create such a map (and then decode the
	 * content further without any type information).
	 */

	@Define(optional = true, description = "Used for extra data found in decoding a stream")
	public Map<String,Object>						__extra;

	/**
	 * Check in our constructor that we really have fields.
	 */

	public struct() {
		if (cache.fs.length == 0)
			throw new IllegalStateException("A struct requires at least one field: " + getClass());
	}

	/**
	 * Calculate the static type information for each class.
	 * 
	 * @param clazz
	 *            the class to calculate for.
	 * @return the cache
	 */
	static synchronized Cache getCache(Class clazz) {
		Cache c = caches.get(clazz);

		if (c == null) {
			Set<Field> ids = new HashSet<Field>();
			Field id = null;

			c = new Cache();
			List<Field> fs = new ArrayList<Field>();
			for (Field f : clazz.getFields()) {
				if (!Modifier.isStatic(f.getModifiers())) {
					fs.add(f);
					if (f.getName().equals("_id"))
						id = f;
					Define define = f.getAnnotation(Define.class);

					if (define != null && define.id())
						ids.add(f);
				}
			}
			c.fs = fs.toArray(new Field[0]);
			c.size = c.fs.length;

			for (Field f : c.fs) {
				if (!f.getName().equals("__extra"))
					c.fields.put(f.getName(), f);
			}
			c.names = c.fields.keySet().toArray(new String[0]);

			if (ids.isEmpty()) {
				if (id != null)
					c.ids = new Field[] {
						id
					};
			} else {
				c.ids = ids.toArray(new Field[ids.size()]);
			}
			Arrays.sort(c.names);
			caches.put(clazz, c);
		}
		return c;
	}

	/**
	 * Print out in a JSON like format (shortened for readability)
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Formatter format = new Formatter(sb);
		toString(this, format, false);
		return sb.toString();
	}

	/**
	 * Printout in strict JSON format
	 * 
	 * @param appendable
	 *            destination
	 */
	public void toJson(Appendable appendable) {
		Formatter format = new Formatter(appendable);
		toString(this, format, true);
	}

	/**
	 * Printout in strict JSON format with UTF-8 encoding
	 * 
	 * @param out
	 *            destination
	 */
	public void toJson(OutputStream out) throws IOException {
		Writer w = new OutputStreamWriter(out, "UTF-8");
		try {
			toJson(w);
		}
		finally {
			w.close();
		}
	}

	/**
	 * Printout an iterable as a JSON array
	 * 
	 * @param i
	 *            the iterable
	 * @param sb
	 *            the formatter
	 * @param json
	 *            if we need strict json or shorten a bit
	 */
	private static void toString(Iterable< ? > i, Formatter sb, boolean json) {
		sb.format("[");
		String del = "";
		for (Object m : i) {
			sb.format(del);
			toString(m, sb, json);
			del = ",";
		}
		sb.format("]");
	}

	/**
	 * Printout an map as a JSON object
	 * 
	 * @param map
	 *            the map
	 * @param sb
	 *            the formatter
	 * @param json
	 *            if we need strict json or shorten a bit
	 */
	private static void toString(Map< ? , ? > map, Formatter sb, boolean json) {
		sb.format("{");
		String del = "";
		for (Entry< ? , ? > e : map.entrySet()) {
			Object k = e.getKey();
			Object v = e.getValue();
			if (v == null)
				continue;

			String s;
			if (k == null)
				s = "null";
			else
				s = k.toString();

			sb.format(del);
			if (json || !NEED_NO_ESCAPE.matcher(s).matches())
				toString(e.getKey().toString(), sb, json);
			else
				sb.format(s);

			sb.format(":");
			toString(e.getValue(), sb, json);

			del = ",";
		}
		sb.format("}");
	}

	/**
	 * Printout a struct as a JSON object
	 * 
	 * @param o
	 *            the object
	 * @param sb
	 *            the formatter
	 * @param json
	 *            if we need strict json or shorten a bit
	 */
	private static void toString(struct o, Formatter sb, boolean json) {
		try {
			sb.format("{");
			String del = "";
			for (Field f : o.fields()) {
				Object object = f.get(o);
				if (object != null) {
					sb.format(del);
					String key = f.getName();
					if (json)
						toString(key, sb, json);
					else
						sb.format(f.getName());
					sb.format(":");
					toString(object, sb, json);
					del = ",";
				}
			}
			sb.format("}");
		}
		catch (IllegalAccessException ie) {
			// Cannot happen because we only use public fields
		}
	}

	/**
	 * Printout any object as a JSON object. Must be a Iterable, Map, struct,
	 * Number, Boolean, null, byte[], String. Other objects are translated with
	 * their toString method and properly quoted.
	 * 
	 * @param o
	 *            the object
	 * @param sb
	 *            the formatter
	 * @param json
	 *            if we need strict json or shorten a bit
	 */
	public static void toString(Object o, Formatter sb, boolean json) {
		if (o == null) {
			sb.format("null");
			return;
		}

		if (o.getClass().isArray()) {
			toStringArray(o, sb, json);
			return;
		}
		if (o instanceof Iterable) {
			toString((Iterable< ? >) o, sb, json);
			return;
		}

		if (o instanceof Map) {
			toString((Map< ? , ? >) o, sb, json);
			return;
		}

		if (o instanceof struct) {
			toString((struct) o, sb, json);
			return;
		}

		if (o instanceof Number || o instanceof Boolean) {
			sb.format("%s", o);
			return;
		}

		toString(o.toString(), sb, json);
	}

	/**
	 * Handle arrays
	 */
	private static void toStringArray(Object a, Formatter sb, boolean json) {
		if (a instanceof byte[]) {
			byte[] array = (byte[]) a;
			int skipBegin = 15;
			int skipEnd = array.length - 15;

			sb.format("\"");
			for (int i = 0; i < array.length; i++) {
				// Shorten long strings
				if (json == false && i >= skipBegin && i < skipEnd) {
					if (i == skipBegin)
						sb.format("...");
					continue;
				}
				sb.format("%02X", array[i]);
			}
			sb.format("\"");
			return;
		}
		sb.format("[");
		String del = "";
		int l = Array.getLength(a);
		for (int i = 0; i < l; i++) {
			sb.format(del);
			Object o = Array.get(a, i);
			toString(o, sb, json);
			del = ",";
		}
		sb.format("]");
	}

	/**
	 * Printout a string as a JSON object. If not a strict JSON format we
	 * shorten strings to 50 characters.
	 * 
	 * @param s
	 *            the string
	 * @param sb
	 *            the formatter
	 * @param json
	 *            if we need strict json or shorten a bit
	 */
	private static void toString(String s, Formatter sb, boolean json) {
		sb.format("\"");
		int skipBegin = 25;
		int skipEnd = s.length() - 25;

		for (int i = 0; i < s.length(); i++) {

			// Shorten long strings
			if (json == false && i >= skipBegin && i < skipEnd) {
				if (i == skipBegin)
					sb.format("...");
				continue;
			}

			char c = s.charAt(i);
			switch (c) {
				case '\t' :
					sb.format("\\t");
					break;

				case '\b' :
					sb.format("\\b");
					break;

				case '\f' :
					sb.format("\\f");
					break;
				case '\n' :
					sb.format("\\n");
					break;
				case '\r' :
					sb.format("\\r");
					break;

				case '\\' :
				case '\'' :
				case '\"' :
					sb.format("\\%c", c);
					break;

				default :
					if (c > 0x7F || c < 0x20) {
						sb.format("\\u%04x", (int) c);
					} else
						sb.format("%c", c);
			}
		}
		sb.format("\"");
	}

	/**
	 * Get the generic type of a field
	 * 
	 * @param key
	 *            the name of the field
	 * @return the type of the field or null if no such field
	 */
	public Type getType(String key) {
		Field f = cache.fields.get(key);
		if (f == null)
			return null;
		return f.getGenericType();
	}

	/**
	 * Provide access to the fields of a class, using the caching structure.
	 * 
	 * @param c
	 *            the class
	 * @return the public fields (-__extra)
	 */
	public static Field[] fields(Class c) {
		return getCache(c).fs;
	}

	/**
	 * Get the field of a class without throwing an exception
	 * 
	 * @param c
	 *            the class
	 * @return the public field or null (-__extra)
	 */
	public static Field getField(Class c, String f) {
		return getCache(c).fields.get(f);
	}

	/**
	 * Get the field of this object without throwing an exception
	 * 
	 * @param c
	 *            the class
	 * @return the public field or null (-__extra)
	 */
	public Field getField(String f) {
		return cache.fields.get(f);
	}

	/**
	 * Return the value of the given field
	 * 
	 * @param name
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public Object $(String name) throws Exception {
		Field f = cache.fields.get(name);
		if (f == null)
			return null;

		return f.get(this);
	}

	public void $(String key, Object value) throws Exception {
		Field f = getField(key);
		f.set(this, value);
	}

	public Field[] fields() {
		return cache.fs;
	}

	/**
	 * Implement equal and hash according to the ids
	 */
	@Override
	public boolean equals(Object other) {

		if (other == null)
			return false;

		if (this == other)
			return true;

		if (other.getClass() != getClass())
			return false;

		if (cache.ids.length != 0)
			try {
				for (Field f : cache.ids) {
					Object a = f.get(this);
					Object b = f.get(other);
					if (a != b) {
						if (a == null)
							return false;
						if (!a.equals(b))
							return false;

						if (a.getClass().isArray()) {
							if (a instanceof byte[]) {
								if (!Arrays.equals((byte[]) a, (byte[]) b))
									return false;
							}
							// TODO other primitives
							Arrays.deepEquals((Object[]) a, (Object[]) b);
						} else
							return false;
					}
				}
				return true;
			}
			catch (Exception e) {}
		return false;
	}

	/**
	 * Implement hash according to first id
	 */

	@Override
	public int hashCode() {
		if (hash != 0)
			return hash;

		if (cache.ids.length == 0)
			return super.hashCode();
		try {
			Object v = cache.ids[0].get(this);
			if (v != null)
				return hash = v.hashCode();
		}
		catch (Exception e) {}
		return hash = -1;
	}

	/**
	 * Convenience methods for fields that want to be initialized. TODO optimize
	 * with list that uses very little space when not used
	 * 
	 * @return
	 */
	public static <T> List<T> list() {
		return new ArrayList<T>();
	}

	/**
	 * Convenience methods for fields that want to be initialized. TODO optimize
	 * with list that uses very little space when not used
	 * 
	 * @return
	 */
	public static <T> Map<String,T> map() {
		return new LinkedHashMap<String,T>();
	}

	/**
	 * Convenience methods for fields that want to be initialized. TODO optimize
	 * with list that uses very little space when not used
	 * 
	 * @return
	 */
	public static <T> Set<T> set() {
		return new LinkedHashSet<T>();
	}

	/**
	 * Turn this struct into a map that is backed by the struct. That is changes
	 * to the map are reflected in the struct and vice versa. Puts to
	 * non-existent fields end up in the __extra field.
	 * 
	 * @return
	 */
	public Map<String,Object> asMap() {
		return new Map<String,Object>() {

			@Override
			public int size() {
				return cache.fs.length;
			}

			@Override
			public boolean isEmpty() {
				return size() == 0;
			}

			@Override
			public boolean containsKey(Object key) {
				return cache.fields.containsKey(key);
			}

			@Override
			public boolean containsValue(Object value) {
				try {
					for (Field f : cache.fs) {
						Object v = f.get(struct.this);
						if (v == null) {
							if (value == null)
								;
							return true;
						} else {
							if (v.equals(value))
								return true;
						}
					}
				}
				catch (IllegalAccessException e) {
					// Only public fields ...
				}
				return false;
			}

			@Override
			public Object get(Object key) {
				try {
					Field f = cache.fields.get(key);
					if (f == null)
						return null;

					return f.get(struct.this);
				}
				catch (IllegalAccessException e) {
					// Only public fields ...
				}
				return null;
			}

			@Override
			public Object put(String key, Object value) {
				try {
					Field f = cache.fields.get(key);
					if (f == null) {
						if (__extra == null) {
							__extra = new HashMap<String,Object>();

						}
						return __extra.put(key, value);
					}

					Object old = f.get(this);
					f.set(struct.this, value);
					return old;
				}
				catch (IllegalAccessException e) {
					// Only public fields ...
				}
				return null;
			}

			@Override
			public Object remove(Object key) {
				throw new UnsupportedOperationException("No such key possible for struct " + getClass() + "." + key);
			}

			@Override
			public void putAll(Map< ? extends String, ? extends Object> m) {
				for (java.util.Map.Entry< ? extends String, ? extends Object> e : m.entrySet()) {
					put(e.getKey(), e.getValue());
				}
			}

			@Override
			public void clear() {
				throw new UnsupportedOperationException("struct " + getClass() + " cannot be cleared");
			}

			@Override
			public Set<String> keySet() {
				return cache.keys;
			}

			@Override
			public Collection<Object> values() {
				List<Object> objects = new ArrayList<Object>();
				try {
					for (Field f : cache.fs) {
						objects.add(f.get(struct.this));
					}
				}
				catch (IllegalAccessException e) {
					// Only public fields ...
				}
				return objects;
			}

			@Override
			public Set<java.util.Map.Entry<String,Object>> entrySet() {
				Set<java.util.Map.Entry<String,Object>> entries = new HashSet<Map.Entry<String,Object>>();
				for (final Field f : cache.fs) {
					entries.add(new Map.Entry<String,Object>() {

						@Override
						public String getKey() {
							return f.getName();
						}

						@Override
						public Object getValue() {
							try {
								return f.get(struct.this);
							}
							catch (IllegalAccessException e) {
								// Only public fields ...
							}
							return null;
						}

						@Override
						public Object setValue(Object value) {
							try {
								Object old = f.get(struct.this);
								f.set(struct.this, value);
								return old;
							}
							catch (IllegalAccessException e) {
								// Only public fields ...
							}
							return null;
						}
					});
				}
				return entries;
			}

		};
	}

	/**
	 * Validate the struct and its children.
	 * 
	 * @param object
	 * @param prefix
	 * @return
	 * @throws Exception
	 */
	public enum ErrorCode {
		MISMATCH, TOO_LARGE, TOO_SMALL, SCRIPT, REQUIRED, NO_FIELDS, OTHER;
	}

	public static class Error extends struct {
		public ErrorCode	code	= ErrorCode.OTHER;
		public String		path;
		public String		description;
		public String		failure;
		public Object		value;
	}

	public List<Error> validate() throws Exception {
		List<Error> result = new ArrayList<struct.Error>();
		validateStruct(null, this, result, "");
		return result;
	}

	private static void validateStruct(Define ourDefine, struct struct, List<Error> result, String path) {
		if (struct.cache.size == 1) {
			// note we always have the __extra field
			result.add(getValidation(null, path, "No public fields are defined", null, ErrorCode.NO_FIELDS));
		}

		for (Field f : struct.cache.fs) {
			Object o;
			try {
				o = f.get(struct);
			}
			catch (IllegalAccessException e) {
				// Just public fields
				return;
			}

			Define define = f.getAnnotation(Define.class);
			if (o == null && (define == null || define.optional() == false)) {
				result.add(getValidation(define, path + "." + f.getName(), "Required but is null", null,
						ErrorCode.REQUIRED));
			}
			if (o == null)
				continue;

			validate(define, result, o, path + "." + f.getName());
		}
	}

	private static void validate(Define define, List<Error> result, Object o, String path) {
		if (o instanceof struct) {
			validateStruct(define, (struct) o, result, path);
			return;
		}
		if (o instanceof Iterable) {
			validateIterable(define, (Iterable) o, result, path);
			return;
		}

		if (o.getClass().isArray() && !(o.getClass() == byte[].class || o.getClass() == char[].class)) {
			validateArray(define, o, result, path);
			return;
		}

		if (o instanceof Map) {
			validateMap(define, (Map) o, result, path);
			return;
		}

		if (define == null)
			return;

		if (o instanceof Number) {
			Number n = (Number) o;
			long l = n.longValue();
			if (l < define.min()) {
				result.add(getValidation(define, path, "Value is too small, must be at least " + define.min(), o,
						ErrorCode.TOO_SMALL));
			}
			if (l > define.max()) {
				result.add(getValidation(define, path, "Value is too large, must be at most " + define.max(), o,
						ErrorCode.TOO_LARGE));
			}
		}

		String s = o.toString();
		String pattern = define.pattern();
		if (!pattern.isEmpty()) {
			if (!s.matches(pattern)) {
				result.add(getValidation(define, path, "Could not match pattern " + pattern, o, ErrorCode.MISMATCH));
			}
		}
	}

	private static Error getValidation(Define define, String path, String string, Object value, ErrorCode code) {
		Error v = new Error();
		v.code = code;
		if (define != null)
			v.description = define.description();
		else
			v.description = "<>";

		v.path = path;
		if (v.path.startsWith("."))
			v.path = v.path.substring(1);

		v.failure = string;
		v.value = value;
		return v;
	}

	private static void validateIterable(Define define, Iterable< ? > it, List<Error> result, String path) {
		int n = 0;
		for (Object o : it) {
			validate(define, result, o, path + "[" + n + "]");
			n++;
		}
	}

	private static void validateMap(Define define, Map< ? , ? > map, List<Error> result, String path) {
		for (Entry< ? , ? > e : map.entrySet()) {
			validate(define, result, e.getValue(), path + "." + e.getKey());
		}
	}

	private static void validateArray(Define define, Object array, List<Error> result, String path) {
		int l = Array.getLength(array);
		for (int i = 0; i < l; i++) {
			validate(define, result, Array.get(array, i), path + "[" + i + "]");
		}
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void check(String why, String... fields) throws Exception {
		List<Error> s = validate();
		if (!s.isEmpty())
			throw new InvalidException(why, s);
	}

	public static void validate(Pattern pattern, String input) throws Exception {
		if (pattern.matcher(input).matches())
			return;

		throw new IllegalArgumentException(input + " does not match pattern: " + pattern.pattern());
	}
}
