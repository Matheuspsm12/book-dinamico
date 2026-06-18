package br.com.tcia.bookdinamico.controller.mapper;

import br.com.tcia.bookdinamico.controller.request.UsuarioCadastroRequest;
import br.com.tcia.bookdinamico.controller.request.UsuarioEdicaoRequest;
import br.com.tcia.bookdinamico.controller.response.UsuarioResponse;
import br.com.tcia.bookdinamico.entity.Usuario;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    /**
     * Cadastro → Entity. Campos sensíveis (id, status, role, senha_hash, auditoria,
     * decisão de aprovação) NÃO vêm do cliente — são preenchidos pelo service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "senhaHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "perfil", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    @Mapping(target = "aprovadoPor", ignore = true)
    @Mapping(target = "decididoEm", ignore = true)
    Usuario toEntity(UsuarioCadastroRequest request);

    @Mapping(target = "aprovadoPorId", source = "aprovadoPor.id")
    @Mapping(target = "role", source = "perfil.nomePerfil")
    UsuarioResponse toResponse(Usuario usuario);

    List<UsuarioResponse> toResponseList(List<Usuario> usuarios);

    /**
     * Atualização parcial (Phase 4 — RN14 / A3): admin edita nome/empresa/email.
     * Demais campos são ignorados independentemente do payload. Campos nulos no DTO
     * NÃO sobrescrevem a entidade (NullValuePropertyMappingStrategy.IGNORE).
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "senhaHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "perfil", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    @Mapping(target = "aprovadoPor", ignore = true)
    @Mapping(target = "decididoEm", ignore = true)
    @Mapping(target = "justificativa", ignore = true)
    void atualizar(@MappingTarget Usuario usuario, UsuarioEdicaoRequest request);
}
