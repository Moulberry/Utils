package net.moulberry.utils.functional.exceptional;

@FunctionalInterface
public interface ExceptionalConsumer<T, E extends Throwable> {

    void accept(T t) throws E;

}
