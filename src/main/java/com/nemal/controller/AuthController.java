package com.nemal.controller;

import com.nemal.dto.UserRegistrationDto;
import com.nemal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationDto dto) {
        return ResponseEntity.ok(userService.register(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody UserRegistrationDto dto) {
        return ResponseEntity.ok(userService.authenticate(dto));
    }
}