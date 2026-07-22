package com.tcia.book_dinamico_back_end.infrastructure.mapper;

import com.tcia.book_dinamico_back_end.api.response.PermissaoResponse;
import com.tcia.book_dinamico_back_end.domain.model.Permissao;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PermissaoMapper {

    PermissaoResponse toResponse(Permissao permissao);

    List<PermissaoResponse> toResponseList(List<Permissao> permissoes);
}
