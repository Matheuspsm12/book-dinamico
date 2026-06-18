package com.tcia.book_dinamico_back_end.annotations;

import com.tcia.book_dinamico_back_end.controller.response.ApiErroResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Requisição bem-sucedida"),
        @ApiResponse(responseCode = "201", description = "Recurso criado com sucesso"),
        @ApiResponse(responseCode = "204", description = "Requisição bem-sucedida, sem conteúdo"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErroResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acesso negado",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErroResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErroResponse.class))),
        @ApiResponse(responseCode = "500", description = "Erro interno no servidor",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErroResponse.class)))
})
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DocumentarAPI {
}
