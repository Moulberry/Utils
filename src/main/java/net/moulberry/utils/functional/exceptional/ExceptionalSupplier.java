package net.moulberry.utils.functional.exceptional;

@FunctionalInterface
public interface ExceptionalSupplier<T, E extends Throwable> {

    T get() throws E;

}
