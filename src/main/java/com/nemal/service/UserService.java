package com.nemal.service;

import com.nemal.dto.ProfileUpdateDto;
import com.nemal.dto.UserDto;
import com.nemal.dto.UserRegistrationDto;
import com.nemal.entity.Department;
import com.nemal.entity.Designation;
import com.nemal.entity.User;
import com.nemal.enums.Role;
import com.nemal.repository.DepartmentRepository;
import com.nemal.repository.DesignationRepository;
import com.nemal.repository.UserRepository;
import com.nemal.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository, DepartmentRepository departmentRepository, DesignationRepository designationRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public String register(UserRegistrationDto dto) {
        User user = User.builder()
                .email(dto.email())
                .passwordHash(passwordEncoder.encode(dto.password()))
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .role(dto.role())
                .build();
        userRepository.save(user);
        return jwtService.generateToken((UserDetails) user);
    }

    public String authenticate(UserRegistrationDto dto) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));
        User user = userRepository.findByEmail(dto.email()).orElseThrow();
        return jwtService.generateToken((UserDetails) user);
    }

    public UserDto createUser(UserDto dto, Role role) {
        User user = User.builder()
                .email(dto.email())
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .role(role)
                .build();
        userRepository.save(user);
        return UserDto.from(user);
    }

    public void deactivateUser(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setIsActive(false);
        userRepository.save(user);
    }

    //update profile




    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return (UserDetails) userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
