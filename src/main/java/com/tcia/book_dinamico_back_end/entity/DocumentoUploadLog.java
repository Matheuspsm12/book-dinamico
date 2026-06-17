package com.tcia.book_dinamico_back_end.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Log de auditoria de uploads (RN32). Persiste tanto upload inicial quanto substituição (RN25).
 *
 * @author TCIA
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "documento_upload_log")
public class DocumentoUploadLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Identificador único do log.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "documento_upload_log_id_seq")
    @SequenceGenerator(name = "documento_upload_log_id_seq",
            sequenceName = "documento_upload_log_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    /**
     * Documento alvo do upload.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    @NotNull(message = "{upload-log.documento.not-null}")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Documento documento;

    /**
     * Usuário (admin) que realizou o upload.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @NotNull(message = "{upload-log.usuario.not-null}")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    /**
     * Nome do arquivo enviado.
     */
    @Column(name = "nome_arquivo", nullable = false, length = 500)
    @NotBlank(message = "{upload-log.nome-arquivo.not-blank}")
    private String nomeArquivo;

    /**
     * Momento do upload.
     */
    @Column(name = "datetime", nullable = false)
    @NotNull(message = "{upload-log.datetime.not-null}")
    private LocalDateTime datetime;

    @PrePersist
    private void prePersist() {
        if (datetime == null) datetime = LocalDateTime.now();
    }
}
