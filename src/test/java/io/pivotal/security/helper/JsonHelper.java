package io.pivotal.security.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.util.Set;

import static com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE;

public class JsonHelper {
  private static final ObjectMapper objectMapper = new ObjectMapper()
      .setPropertyNamingStrategy(SNAKE_CASE);
  private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  public static byte[] serialize(Object object) {
    try {
      return objectMapper.writeValueAsBytes(object);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T deserialize(byte[] json, Class<T> klass) {
    try {
      return objectMapper.readValue(json, klass);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T deserialize(String json, Class<T> klass) {
    try {
      return objectMapper.readValue(json, klass);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> Set<ConstraintViolation<T>> validate(T original) {
    return validator.validate(original);
  }

  public static <T> Set<ConstraintViolation<T>> deserializeAndValidate(String json, Class<T> klass) {
    try {
      T object = objectMapper.readValue(json, klass);
      return validator.validate(object);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> Set<ConstraintViolation<T>> deserializeAndValidate(byte[] json, Class<T> klass) {
    try {
      T object = objectMapper.readValue(json, klass);
      return validator.validate(object);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Matcher<ConstraintViolation> hasViolationWithMessage(String expectedMessage) {
    return new BaseMatcher<ConstraintViolation>() {
      @Override
      public boolean matches(final Object item) {
        final ConstraintViolation violation = (ConstraintViolation) item;
        return violation.getMessage().equals(expectedMessage);
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText("getMessage() should equal").appendValue(expectedMessage);
      }
    };
  }
}
