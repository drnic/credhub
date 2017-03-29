package io.pivotal.security.util;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.test.context.support.DefaultActiveProfilesResolver;

public class DatabaseProfileResolver extends DefaultActiveProfilesResolver {

  @Override
  public String[] resolve(Class<?> testClass) {
    return (String[]) ArrayUtils.addAll(new String[]{System.getProperty("spring.profiles.active")},
        super.resolve(testClass));
  }
}
