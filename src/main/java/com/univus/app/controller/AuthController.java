package com.univus.app.controller;

import com.univus.app.domain.dto.SignupRequestDto;
import com.univus.app.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/signup")
  public ResponseEntity<?> signup(@RequestBody SignupRequestDto request) {
    try {
      authService.signup(request);
      return ResponseEntity.status(HttpStatus.CREATED).build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}
