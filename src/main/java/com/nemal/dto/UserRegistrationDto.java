package com.nemal.dto;

import com.nemal.enums.Role;

public record UserRegistrationDto(String email, String password, String firstName, String lastName, Role role) {}