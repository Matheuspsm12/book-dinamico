package com.tcia.book_dinamico_back_end.domain.model;

import com.tcia.book_dinamico_back_end.core.enums.ExtensaoDocumento;
import com.tcia.book_dinamico_back_end.core.enums.TipoDocumento;
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
    @NotBlank(message = "Nome do documento não pode estar vazio!")
    @Size(max = 255, message = "Nome do documento deve ter no máximo {max} caracteres!")
    private String nome;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Descrição não pode estar vazia!")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    @NotNull(message = "Tipo do documento não pode ser nulo!")
    private TipoDocumento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "extensao", nullable = false, length = 10)
    @NotNull(message = "Extensão do documento não pode ser nula!")
    private ExtensaoDocumento extensao;

    @Column(name = "caminho_armazenamento", nullable = false, length = 500)
    @NotBlank(message = "Caminho de armazenamento não pode estar vazio!")
    @Size(max = 500, message = "Caminho de armazenamento deve ter no máximo {max} caracteres!")
    private String caminhoArmazenamento;

    @Column(name = "tamanho_bytes", nullable = false)
    @NotNull(message = "Tamanho do arquivo não pode ser nulo!")
    private Long tamanhoBytes;

    @Column(name = "data_atualizacao", nullable = false)
    @NotNull(message = "Data de atualização não pode ser nula!")
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
