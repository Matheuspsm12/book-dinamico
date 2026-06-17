package br.com.tcia.bookdinamico.controller.mapper;

import br.com.tcia.bookdinamico.controller.response.DocumentoResponse;
import br.com.tcia.bookdinamico.entity.Documento;
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
