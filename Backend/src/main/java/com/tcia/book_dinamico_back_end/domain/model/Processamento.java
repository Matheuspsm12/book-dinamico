package com.tcia.book_dinamico_back_end.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tcia.book_dinamico_back_end.core.enums.ProcessamentoResultado;
import com.tcia.book_dinamico_back_end.core.enums.ProcessamentoTipo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "processamento")
@EntityListeners(AuditingEntityListener.class)
public class Processamento implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "processamento_id_seq")
    @SequenceGenerator(name = "processamento_id_seq", sequenceName = "processamento_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "nome_arquivo", nullable = false, length = 500)
    private String nomeArquivo;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @CreatedDate
    @Column(name = "data_start", nullable = false, updatable = false)
    private LocalDateTime dataStart;

    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    @Column(name = "tipo_processamento", nullable = false)
    private Integer tipoProcessamento;

    @Column(name = "executado", nullable = false)
    private Boolean executado;

    @Column(name = "reprocessar", nullable = false)
    private Boolean reprocessar;

    @Column(name = "qtd_reprocessar", nullable = false)
    private Integer qtdReprocessar;

    @Column(name = "qtd_reprocessado", nullable = false)
    private Integer qtdReprocessado;

    @Column(name = "resultado", columnDefinition = "TEXT")
    private String resultado;

    @Column(name = "resultado_amigavel", length = 200)
    private String resultadoAmigavel;

    @Column(name = "parametro", length = 500)
    private String parametro;

    @Column(name = "arquivo_a_processar", length = 500)
    private String arquivoAProcessar;

    @Column(name = "arquivo_processado", length = 500)
    private String arquivoProcessado;

    @Column(name = "tamanho", length = 200)
    private String tamanho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Documento documento;

    public String getNomeProcessamento() {
        ProcessamentoTipo tipo = ProcessamentoTipo.peloCodigo(tipoProcessamento);
        return tipo != null ? tipo.getDescricao() : null;
    }

    @PrePersist
    private void prePersist() {
        if (dataStart == null) dataStart = LocalDateTime.now();
        if (executado == null) executado = false;
        if (reprocessar == null) reprocessar = false;
        if (qtdReprocessar == null) qtdReprocessar = 0;
        if (qtdReprocessado == null) qtdReprocessado = 0;
        if (resultadoAmigavel == null) resultadoAmigavel = ProcessamentoResultado.AGENDADO.getDescricao();
    }
}
