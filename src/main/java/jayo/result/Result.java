/*
 * Copyright (c) 2026-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 *
 * Java rewrite from the Kotlin stdlib implementation
 * (https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/src/kotlin/util/Result.kt), original copyright is
 * below
 *
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package jayo.result;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A discriminated union that encapsulates a successful outcome with a value of type {@link T} or a failure with an
 * arbitrary {@link Throwable} exception.
 */
public final class Result<T> implements Serializable {
    private final @Nullable Object value;

    private Result(final @Nullable Object value) {
        this.value = value;
    }

    /**
     * @return {@code true} if this instance represents a successful outcome.
     * In this case {@link #isFailure} returns {@code false}.
     */
    public boolean isSuccess() {
        return !(value instanceof Failure);
    }

    /**
     * @return {@code true} if this instance represents a failed outcome.
     * In this case {@link #isSuccess} returns {@code false}.
     */
    public boolean isFailure() {
        return value instanceof Failure;
    }

    /**
     * @return the encapsulated value if this instance represents a {@linkplain #isSuccess successful outcome} or
     * {@code null} if it is a {@linkplain #isFailure failed outcome}.
     * <p>
     * This function is shorthand for {@code getOrElse(() -> null);} (see {@link #getOrElse}) or
     * {@code fold(Function::identity, ex -> null);} (see {@link #fold}).
     */
    @SuppressWarnings("unchecked")
    public @Nullable T getOrNull() {
        if (isSuccess()) {
            return (T) value;
        }
        return null;
    }

    /**
     * @return the encapsulated {@link Throwable} exception if this instance represents a
     * {@linkplain #isFailure failed outcome} or {@code null} if it is a {@linkplain #isSuccess successful outcome}.
     * <p>
     * This function is shorthand for {@code fold(result -> null, Function::identity);} (see {@link #fold}).
     */
    public @Nullable Throwable exceptionOrNull() {
        if (isFailure()) {
            return ((Failure) value).exception;
        }
        return null;
    }

    /**
     * @return a string {@code Success(v)} if this instance represents {@linkplain #isSuccess success} where {@code v}
     * is a string representation of the value or a string {@code Failure(x)} if it is {@linkplain #isFailure failure}
     * where {@code x} is a string representation of the exception.
     */
    @Override
    public @NonNull String toString() {
        if (isFailure()) {
            return value.toString(); // "Failure( " + exception + ")"
        }
        return "Success(" + value + ")";
    }

    /**
     * @return an instance that encapsulates the given {@code value} as successful value.
     */
    public static <T> @NonNull Result<T> success(final T value) {
        return new Result<>(value);
    }

    /**
     * @return an instance that encapsulates the given {@linkplain Throwable exception} as failure.
     */
    public static <T> @NonNull Result<T> failure(final @NonNull Throwable exception) {
        Objects.requireNonNull(exception);
        return new Result<>(new Failure(exception));
    }

    /**
     * Execute the specified {@code callable} and returns its encapsulated result if invocation was successful,
     * catching any {@link Throwable} exception that was thrown from the {@code callable} execution and encapsulating it
     * as a failure.
     */
    public static <R> @NonNull Result<R> runCatching(final @NonNull Callable<R> callable) {
        Objects.requireNonNull(callable);

        try {
            return success(callable.call());
        } catch (Throwable e) {
            return failure(e);
        }
    }

    private void throwOnFailure() {
        if (isSuccess()) {
            return;
        }

        final var ex = ((Failure) value).exception;
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }

        throw new RuntimeException(ex);
    }

    /**
     * @return the encapsulated value if this instance represents {@linkplain #isSuccess success} or throws the
     * encapsulated {@link Throwable} exception if it is {@linkplain #isFailure failure}.
     * <p>
     * This function is shorthand for {@code getOrElse(ex -> throw ex);} (see {@link #getOrElse}).
     */
    @SuppressWarnings("unchecked")
    public T getOrThrow() {
        throwOnFailure();
        return (T) value;
    }

    /**
     * @return the encapsulated value if this instance represents {@linkplain #isSuccess success} or the result of
     * {@code onFailure} function for the encapsulated {@link Throwable} exception if it is
     * {@linkplain #isFailure failure}.
     * <p>
     * Note: this function rethrows any {@link Throwable} exception thrown by {@code onFailure} function.
     * <p>
     * This function is shorthand for {@code fold(Function::identity, onFailure);} (see {@link #fold}).
     */
    @SuppressWarnings("unchecked")
    public T getOrElse(final @NonNull Function<@NonNull Throwable, ? extends T> onFailure) {
        Objects.requireNonNull(onFailure);

        final var ex = exceptionOrNull();
        if (ex == null) {
            return (T) value;
        }
        return onFailure.apply(ex);
    }

    /**
     * @return the encapsulated value if this instance represents {@linkplain #isSuccess success} or the
     * {@code defaultValue} if it is {@linkplain #isFailure failure}.
     * <p>
     * This function is shorthand for {@code getOrElse(ex -> defaultValue);} (see {@link #getOrElse}).
     */
    @SuppressWarnings("unchecked")
    public T getOrDefault(final T defaultValue) {
        if (isFailure()) {
            return defaultValue;
        }
        return (T) value;
    }

    /**
     * @return the result of {@code onSuccess} for the encapsulated value if this instance represents
     * {@linkplain #isSuccess success} or the result of {@code onFailure} function for the encapsulated
     * {@link Throwable} exception if it is {@linkplain #isFailure failure}.
     * <p>
     * Note: this function rethrows any {@link Throwable} exception thrown by {@code onSuccess} or by {@code onFailure}
     * function.
     */
    @SuppressWarnings("unchecked")
    public <R> R fold(final @NonNull Function<T, R> onSuccess,
                      final @NonNull Function<@NonNull Throwable, R> onFailure) {
        final var ex = exceptionOrNull();
        if (ex == null) {
            return onSuccess.apply((T) value);
        } else {
            return onFailure.apply(ex);
        }
    }

    /**
     * @return the encapsulated result of the given {@code transform} function applied to the encapsulated value if this
     * instance represents {@linkplain #isSuccess success} or the original encapsulated {@link Throwable} exception if
     * it is {@linkplain #isFailure failure}.
     * <p>
     * Note: this function rethrows any {@link Throwable} exception thrown by {@code transform} function.
     * See {@link #mapCatching} for an alternative that encapsulates exceptions.
     */
    @SuppressWarnings("unchecked")
    public <R> @NonNull Result<R> map(final @NonNull Function<T, R> transform) {
        Objects.requireNonNull(transform);

        if (isSuccess()) {
            return success(transform.apply((T) value));
        }
        return new Result<>(value);
    }

    /**
     * @return the encapsulated result of the given {@code transform} function applied to the encapsulated value if this
     * instance represents {@linkplain #isSuccess success} or the original encapsulated {@link Throwable} exception if
     * it is {@linkplain #isFailure failure}.
     * <p>
     * This function catches any {@link Throwable} exception thrown by {@code transform} function and encapsulates it as
     * a failure.
     * See {@link #map} for an alternative that rethrows exceptions from {@code transform} function.
     */
    @SuppressWarnings("unchecked")
    public <R> @NonNull Result<R> mapCatching(final @NonNull Function<T, R> transform) {
        Objects.requireNonNull(transform);

        if (isSuccess()) {
            try {
                return success(transform.apply((T) value));
            } catch (Throwable e) {
                return failure(e);
            }
        }
        return new Result<>(value);
    }

    /**
     * @return the encapsulated result of the given {@code transform} function applied to the encapsulated
     * {@link Throwable} exception if this instance represents {@linkplain #isFailure failure} or the
     * original encapsulated value if it is {@linkplain #isSuccess success}.
     * <p>
     * Note: this function rethrows any {@link Throwable} exception thrown by {@code transform} function.
     * See {@link #recoverCatching} for an alternative that encapsulates exceptions.
     */
    public @NonNull Result<T> recover(final @NonNull Function<@NonNull Throwable, ? extends T> transform) {
        Objects.requireNonNull(transform);

        if (isFailure()) {
            return success(transform.apply(((Failure) value).exception));
        }
        return this;
    }

    /**
     * @return the encapsulated result of the given {@code transform} function applied to the encapsulated
     * {@link Throwable} exception if this instance represents {@linkplain #isFailure failure} or the
     * original encapsulated value if it is {@linkplain #isSuccess success}.
     * <p>
     * This function catches any {@link Throwable} exception thrown by {@code transform} function and encapsulates it as
     * a failure. See {@link #recover} for an alternative that rethrows exceptions.
     */
    public @NonNull Result<T> recoverCatching(final @NonNull Function<@NonNull Throwable, ? extends T> transform) {
        Objects.requireNonNull(transform);

        if (isFailure()) {
            try {
                return success(transform.apply(((Failure) value).exception));
            } catch (Throwable e) {
                return failure(e);
            }
        }
        return this;
    }

    /**
     * Performs the given {@code action} on the encapsulated value if this instance represents
     * {@linkplain #isSuccess success}.
     *
     * @return the original {@code Result} unchanged.
     */
    @SuppressWarnings("unchecked")
    public @NonNull Result<T> onSuccess(final @NonNull Consumer<T> action) {
        if (isSuccess()) {
            action.accept((T) value);
        }
        return this;
    }

    /**
     * Performs the given {@code action} on the encapsulated {@link Throwable} exception if this instance represents
     * {@linkplain #isFailure failure}.
     *
     * @return the original {@code Result} unchanged.
     */
    public Result<T> onFailure(final @NonNull Consumer<@NonNull Throwable> action) {
        Throwable ex = exceptionOrNull();
        if (ex != null) {
            action.accept(ex);
        }
        return this;
    }

    private static final class Failure implements Serializable {
        private final @NonNull Throwable exception;

        private Failure(final @NonNull Throwable exception) {
            this.exception = Objects.requireNonNull(exception);
        }

        @Override
        public boolean equals(final @Nullable Object other) {
            return other instanceof Failure && exception.equals(((Failure) other).exception);
        }

        @Override
        public int hashCode() {
            return exception.hashCode();
        }

        @Override
        public @NonNull String toString() {
            return "Failure(" + exception + ")";
        }
    }
}
