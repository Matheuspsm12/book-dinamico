package com.tcia.book_dinamico_back_end.infrastructure.mapper;

import com.tcia.book_dinamico_back_end.api.response.PerfilResponse;
import com.tcia.book_dinamico_back_end.api.response.PermissaoResponse;
import com.tcia.book_dinamico_back_end.domain.model.Perfil;
import com.tcia.book_dinamico_back_end.domain.model.PerfilPermissao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PerfilMapper {

    @Mapping(target = "permissoes", source = "perfilPermissao")
    PerfilResponse toResponse(Perfil perfil);

    List<PerfilResponse> toResponseList(List<Perfil> perfis);

    @Mapping(target = "id", source = "permissao.id")
    @Mapping(target = "nomePermissao", source = "permissao.nomePermissao")
    @Mapping(target = "descricao", source = "permissao.descricao")
    PermissaoResponse toPermissaoResponse(PerfilPermissao perfilPermissao);
}
