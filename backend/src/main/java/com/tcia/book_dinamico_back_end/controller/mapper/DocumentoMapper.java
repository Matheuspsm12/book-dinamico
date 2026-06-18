package com.tcia.book_dinamico_back_end.controller.mapper;

import com.tcia.book_dinamico_back_end.controller.response.DocumentoResponse;
import com.tcia.book_dinamico_back_end.entity.Documento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentoMapper {

    @Mapping(target = "criadoPorId",     source = "criadoPor.id")
    @Mapping(target = "atualizadoPorId", source = "atualizadoPor.id")
    DocumentoResponse toResponse(Documento documento);

    List<DocumentoResponse> toResponseList(List<Documento> documentos);
}
