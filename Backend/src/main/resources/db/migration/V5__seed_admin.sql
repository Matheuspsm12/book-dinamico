-- Seed do usuário administrador (N3).
-- E-mail: admin@claro.com.br
-- Senha:  admin
--
-- ⚠️ Senha fraca e permanente (A9 não prevê change-password).
-- Padrão TCIA: hash gerado via pgcrypto in-line ($2a$ é compatível com BCryptPasswordEncoder do Spring).

INSERT INTO usuario (
    id,
    nome,
    empresa,
    email,
    senha_hash,
    justificativa,
    status,
    role,
    criado_em,
    atualizado_em
) VALUES (
    nextval('usuario_seq'),
    'Administrador',
    'Claro',
    'admin@claro.com.br',
    crypt('admin', gen_salt('bf')),
    'Usuário administrador padrão (seed inicial).',
    'APROVADO',
    'ADMIN',
    NOW(),
    NOW()
);
