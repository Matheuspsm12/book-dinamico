package br.com.tcia.bookdinamico.entity;

import br.com.tcia.bookdinamico.enums.ExtensaoDocumento;
import br.com.tcia.bookdinamico.enums.TipoDocumento;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
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
@Table(name = "documento")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE documento SET ativo = false WHERE id = ?")
public class Documento implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Identificador único do documento.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "documento_id_seq")
    @SequenceGenerator(name = "documento_id_seq", sequenceName = "documento_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    /**
     * Nome de exibição do documento.
     */
    @Column(name = "nome", nullable = false, length = 255)
    @NotBlank(message = "{documento.nome.not-blank}")
    @Size(max = 255, message = "{documento.nome.size}")
    private String nome;

    /**
     * Descrição do documento.
     */
    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "{documento.descricao.not-blank}")
    private String descricao;

    /**
     * Tipo/categoria do documento.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    @NotNull(message = "{documento.tipo.not-null}")
    private TipoDocumento tipo;

    /**
     * Extensão do arquivo armazenado.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "extensao", nullable = false, length = 10)
    @NotNull(message = "{documento.extensao.not-null}")
    private ExtensaoDocumento extensao;

    /**
     * Caminho do binário no filesystem ({@code ${BOOK_DIRETORIO}}).
     */
    @Column(name = "caminho_armazenamento", nullable = false, length = 500)
    @NotBlank(message = "{documento.caminho.not-blank}")
    @Size(max = 500, message = "{documento.caminho.size}")
    private String caminhoArmazenamento;

    /**
     * Tamanho do binário em bytes.
     */
    @Column(name = "tamanho_bytes", nullable = false)
    @NotNull(message = "{documento.tamanho.not-null}")
    private Long tamanhoBytes;

    /**
     * Data de atualização controlada manualmente pelo admin (A6).
     */
    @Column(name = "data_atualizacao", nullable = false)
    @NotNull(message = "{documento.data-atualizacao.not-null}")
    private LocalDate dataAtualizacao;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por", nullable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario criadoPor;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario atualizadoPor;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @PrePersist
    private void prePersist() {
        if (ativo == null) ativo = true;
    }
}
