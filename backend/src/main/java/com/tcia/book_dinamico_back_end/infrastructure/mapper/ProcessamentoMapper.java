package com.tcia.book_dinamico_back_end.infrastructure.mapper;

import com.tcia.book_dinamico_back_end.api.response.ProcessamentoResponse;
import com.tcia.book_dinamico_back_end.core.enums.ProcessamentoTipo;
import com.tcia.book_dinamico_back_end.domain.model.Processamento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ProcessamentoMapper {

    @Mapping(target = "usuario", source = "usuario.email")
    @Mapping(target = "documentoId", source = "documento.id")
    @Mapping(target = "tipoProcessamento", source = "tipoProcessamento", qualifiedByName = "tipoProcessamento")
    ProcessamentoResponse toResponse(Processamento processamento);

    @Named("tipoProcessamento")
    default String tipoProcessamento(Integer codigo) {
        ProcessamentoTipo tipo = ProcessamentoTipo.peloCodigo(codigo);
        return tipo != null ? tipo.name() : String.valueOf(codigo);
    }
}
