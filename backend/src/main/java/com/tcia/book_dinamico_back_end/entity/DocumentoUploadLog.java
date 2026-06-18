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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "documento_upload_log")
public class DocumentoUploadLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "documento_upload_log_id_seq")
    @SequenceGenerator(name = "documento_upload_log_id_seq",
            sequenceName = "documento_upload_log_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    @NotNull(message = "{upload-log.documento.not-null}")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Documento documento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @NotNull(message = "{upload-log.usuario.not-null}")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @Column(name = "nome_arquivo", nullable = false, length = 500)
    @NotBlank(message = "{upload-log.nome-arquivo.not-blank}")
    private String nomeArquivo;

    @Column(name = "datetime", nullable = false)
    @NotNull(message = "{upload-log.datetime.not-null}")
    private LocalDateTime datetime;

    @PrePersist
    private void prePersist() {
        if (datetime == null) datetime = LocalDateTime.now();
    }
}
