package com.tcia.book_dinamico_back_end.api.assembler;

import com.tcia.book_dinamico_back_end.api.controller.ProcessamentoController;
import com.tcia.book_dinamico_back_end.api.response.ProcessamentoResponse;
import com.tcia.book_dinamico_back_end.domain.model.Processamento;
import com.tcia.book_dinamico_back_end.infrastructure.mapper.ProcessamentoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
@RequiredArgsConstructor
public class ProcessamentoModelAssembler
        implements RepresentationModelAssembler<Processamento, EntityModel<ProcessamentoResponse>> {

    private final ProcessamentoMapper mapper;

    @Override
    public EntityModel<ProcessamentoResponse> toModel(Processamento processamento) {
        ProcessamentoResponse response = mapper.toResponse(processamento);
        return EntityModel.of(response,
                linkTo(methodOn(ProcessamentoController.class).buscar(response.getId())).withSelfRel());
    }
}
