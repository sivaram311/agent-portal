package com.vpsfilebridge.controller;

import com.vpsfilebridge.model.LoginRequest;
import com.vpsfilebridge.model.LoginResponse;
import com.vpsfilebridge.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
  private final JwtUtil jwtUtil;
  private final String username;
  private final String password;

  public AuthController(
      JwtUtil jwtUtil,
      @Value("${app.auth.username}") String username,
      @Value("${app.auth.password}") String password) {
    this.jwtUtil = jwtUtil;
    this.username = username;
    this.password = password;
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "ok", "app", "VPS FileBridge");
  }

  @PostMapping("/auth/login")
  public LoginResponse login(@RequestBody LoginRequest req) {
    if (req == null || req.username() == null || req.password() == null
        || !username.equals(req.username()) || !password.equals(req.password())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    return new LoginResponse(jwtUtil.generate(req.username()), req.username());
  }

  @PostMapping("/auth/logout")
  public ResponseEntity<Void> logout() {
    return ResponseEntity.noContent().build();
  }
}
