package io.pivotal.security.controller.v1.health;

import static com.google.common.collect.ImmutableMap.of;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class HealthController {

  @RequestMapping(value = "/health", method = RequestMethod.GET)
  public ResponseEntity<Map> getHealth() {
    try {
      return new ResponseEntity<>(of("status", "UP"), HttpStatus.OK);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
