package com.tcia.book_dinamico_back_end.entity;

import com.tcia.book_dinamico_back_end.enums.ExtensaoDocumento;
import com.tcia.book_dinamico_back_end.enums.TipoDocumento;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Documento do Book Dinâmico. Soft-delete via {@code ativo} + {@code @SQLDelete} (padrão TCIA).
 * {@code data_atualizacao} é campo manual do admin (A6) — distinto de {@code atualizado_em} (auto JPA).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "documento")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE documento SET ativo = false WHERE id = ?")
public class Documento implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "documento_id_seq")
    @SequenceGenerator(name = "documento_id_seq", sequenceName = "documento_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "nome", nullable = false, length = 255)
    @NotBlank
    @Size(max = 255)
    private String nome;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    @NotNull
    private TipoDocumento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "extensao", nullable = false, length = 10)
    @NotNull
    private ExtensaoDocumento extensao;

    @Column(name = "caminho_armazenamento", nullable = false, length = 500)
    @NotBlank
    @Size(max = 500)
    private String caminhoArmazenamento;

    @Column(name = "tamanho_bytes", nullable = false)
    @NotNull
    private Long tamanhoBytes;

    @Column(name = "data_atualizacao", nullable = false)
    @NotNull
    private LocalDate dataAtualizacao;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por", nullable = false, updatable = false)
    private Usuario criadoPor;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por", nullable = false)
    private Usuario atualizadoPor;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @PrePersist
    private void prePersist() {
        if (ativo == null) ativo = true;
    }
}
