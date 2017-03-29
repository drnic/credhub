package io.pivotal.security.view;

import io.pivotal.security.util.CheckedFunction;
import java.util.Objects;

public enum SecretKind implements SecretKindFromString {
  VALUE {
    @Override
    public <T, E extends Throwable> CheckedFunction<T, E> lift(CheckedMapping<T, E> mapping) {
      Objects.requireNonNull(mapping);
      return mapping::value;
    }
  },
  JSON {
    @Override
    public <T, E extends Throwable> CheckedFunction<T, E> lift(CheckedMapping<T, E> mapping) {
      Objects.requireNonNull(mapping);
      return mapping::json;
    }
  },
  PASSWORD {
    @Override
    public <T, E extends Throwable> CheckedFunction<T, E> lift(CheckedMapping<T, E> mapping) {
      Objects.requireNonNull(mapping);
      return mapping::password;
    }
  },
  CERTIFICATE {
    @Override
    public <T, E extends Throwable> CheckedFunction<T, E> lift(CheckedMapping<T, E> mapping) {
      Objects.requireNonNull(mapping);
      return mapping::certificate;
    }
  },
  SSH {
    @Override
    public <T, E extends Throwable> CheckedFunction<T, E> lift(CheckedMapping<T, E> mapping) {
      Objects.requireNonNull(mapping);
      return mapping::ssh;
    }
  },
  RSA {
    @Override
    public <T, E extends Throwable> CheckedFunction<T, E> lift(CheckedMapping<T, E> mapping) {
      Objects.requireNonNull(mapping);
      return mapping::rsa;
    }
  };

  public abstract <T, E extends Throwable> CheckedFunction<T, E> lift(CheckedMapping<T, E> mapping);

  public interface CheckedMapping<T, E extends Throwable> {

    T value(T t) throws E;

    T json(T t) throws E;

    T password(T t) throws E;

    T certificate(T t) throws E;

    T ssh(T t) throws E;

    T rsa(T t) throws E;
  }
}
