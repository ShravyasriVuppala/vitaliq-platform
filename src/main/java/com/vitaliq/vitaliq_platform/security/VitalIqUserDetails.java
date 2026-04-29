package com.vitaliq.vitaliq_platform.security;

import com.vitaliq.vitaliq_platform.model.auth.ApiKey;
import com.vitaliq.vitaliq_platform.model.auth.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@RequiredArgsConstructor
public class VitalIqUserDetails implements UserDetails {

    private final User user;
    private final ApiKey apiKey;  // Nullable - null if JWT, present if API key; Used for scope validation

    public VitalIqUserDetails(User user) {
        this.user = user;
        this.apiKey = null;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // We don't use this for scope validation; interface method
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}