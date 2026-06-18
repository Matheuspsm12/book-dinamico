package com.tcia.book_dinamico_back_end.repository.specification;

import com.tcia.book_dinamico_back_end.controller.request.UsuarioFiltroRequest;
import com.tcia.book_dinamico_back_end.entity.Usuario;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Component
public class UsuarioSpecifications {

    private static final String UNACCENT_FUNCTION = "unaccent";

    public Specification<Usuario> comFiltros(UsuarioFiltroRequest filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filtro == null) {
                return cb.conjunction();
            }

            if (filtro.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filtro.getStatus()));
            }

            if (filtro.getEmpresa() != null && !filtro.getEmpresa().isBlank()) {
                String empresa = normalizar(filtro.getEmpresa());
                Expression<String> empresaUnaccent = cb.function(
                        UNACCENT_FUNCTION, String.class, cb.lower(root.get("empresa")));
                predicates.add(cb.like(empresaUnaccent, "%" + empresa + "%"));
            }

            if (filtro.getNome() != null && !filtro.getNome().isBlank()) {
                String nome = normalizar(filtro.getNome());
                Expression<String> nomeUnaccent = cb.function(
                        UNACCENT_FUNCTION, String.class, cb.lower(root.get("nome")));
                predicates.add(cb.like(nomeUnaccent, "%" + nome + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }
}
