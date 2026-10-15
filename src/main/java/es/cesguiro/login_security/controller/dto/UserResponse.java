package es.cesguiro.login_security.controller.dto;

import java.util.Set;

public record UserResponse(Long id, String email, String displayName, Set<String> roles) {
}
