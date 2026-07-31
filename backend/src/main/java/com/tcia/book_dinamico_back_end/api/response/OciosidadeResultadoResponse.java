package com.tcia.book_dinamico_back_end.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OciosidadeResultadoResponse {

    /** Verificação não rodou porque o envio de e-mail está desabilitado. */
    private boolean emailDesabilitado;

    /** Usuários que receberam o e-mail de notificação de ociosidade nesta execução. */
    private List<String> notificados;

    /** Usuários desativados por não terem se manifestado dentro do prazo. */
    private List<String> desativados;
}
