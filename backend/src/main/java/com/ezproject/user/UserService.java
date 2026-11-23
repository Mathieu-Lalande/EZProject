package com.ezproject.user;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.common.BusinessRuleException;
import com.ezproject.common.NotFoundException;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDto> findAll() {
        return repository.findAllByOrderByDisplayNameAsc().stream().map(UserDto::from).toList();
    }

    public AppUser getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur", id));
    }

    public AppUser getByEmail(String email) {
        return repository.findByEmailIgnoreCase(email).orElseThrow(() -> new NotFoundException("Utilisateur", email));
    }

    @Transactional
    public UserDto create(CreateUserRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleException("Un utilisateur existe déjà avec cet email");
        }
        AppUser user = new AppUser(request.email(), request.displayName(),
                passwordEncoder.encode(request.password()), request.role(), request.jobTitle());
        return UserDto.from(repository.save(user));
    }
}
