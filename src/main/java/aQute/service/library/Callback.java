package aQute.service.library;

public interface Callback<T> {
	boolean callback(T o) throws Exception;
}
