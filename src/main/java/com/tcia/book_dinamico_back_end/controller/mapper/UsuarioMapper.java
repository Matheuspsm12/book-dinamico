package com.tcia.book_dinamico_back_end.controller.mapper;

import com.tcia.book_dinamico_back_end.controller.request.UsuarioCadastroRequest;
import com.tcia.book_dinamico_back_end.controller.request.UsuarioEdicaoRequest;
import com.tcia.book_dinamico_back_end.controller.response.UsuarioResponse;
import com.tcia.book_dinamico_back_end.entity.Usuario;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

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
