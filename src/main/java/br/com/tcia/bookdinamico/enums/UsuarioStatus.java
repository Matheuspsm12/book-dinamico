package br.com.tcia.bookdinamico.enums;

/**
 * Estados de ciclo de vida do usuário (RN07, RN10, RN13, RN14).
 * <ul>
 *     <li>{@link #PENDENTE} — cadastrado, aguardando decisão do admin.</li>
 *     <li>{@link #APROVADO} — pode autenticar.</li>
 *     <li>{@link #REJEITADO} — admin rejeitou; não autentica.</li>
 *     <li>{@link #DESATIVADO} — admin desativou; não autentica. Funciona como soft-delete.</li>
 * </ul>
 */
public enum UsuarioStatus {
    PENDENTE,
    APROVADO,
    REJEITADO,
    DESATIVADO
}
