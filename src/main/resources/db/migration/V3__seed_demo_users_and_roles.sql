INSERT INTO roles (name)
VALUES ('ROLE_ADMIN'), ('ROLE_USER');

-- Usuarios didácticos: la contraseña de ambos es "password". Cambiarla antes de cualquier uso real.
INSERT INTO users (email, display_name, password_hash)
VALUES
    ('demo.admin@example.com', 'Usuario administrador de ejemplo', '$2b$10$cP0LTHNNH2vByI4MwnJvUOS5cmDjfnUSH8xHpf8EDwOPGaBPhk4.2'),
    ('demo.user@example.com', 'Usuario de ejemplo', '$2b$10$/0rUDi5FCMd2czVG.xPm0.5ivKfB16lOYIQTVyhEqjmPfY5JPxOIS');

INSERT INTO user_roles (user_id, role_id)
SELECT users.id, roles.id
FROM users
CROSS JOIN roles
WHERE users.email = 'demo.admin@example.com'
  AND roles.name IN ('ROLE_ADMIN', 'ROLE_USER');

INSERT INTO user_roles (user_id, role_id)
SELECT users.id, roles.id
FROM users
JOIN roles ON roles.name = 'ROLE_USER'
WHERE users.email = 'demo.user@example.com';
