package com.tcia.book_dinamico_back_end.email;

import com.tcia.book_dinamico_back_end.entity.Usuario;
import com.tcia.book_dinamico_back_end.exception.EmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Envio de e-mails do portal. @Async — falha não bloqueia o request HTTP.
 * Gate {@code app.email.habilitado} permite desligar SMTP em DEV/teste sem alterar código.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class EmailAdapter {

    private final JavaMailSender mailSender;

    @Value("${app.email.habilitado:false}")
    private boolean habilitado;

    @Value("${app.email.remetente}")
    private String remetente;

    @Async
    public void enviarAprovacao(Usuario usuario) {
        String assunto = "Seu cadastro no Portal Book Dinâmico foi aprovado";
        String corpo = """
                <p>Olá %s,</p>
                <p>Boa notícia! Seu cadastro no <strong>Portal Book Dinâmico</strong> foi
                <strong>aprovado</strong> pelo administrador.</p>
                <p>Você já pode entrar com o e-mail e a senha que cadastrou.</p>
                <p>Atenciosamente,<br/>Equipe Book Dinâmico</p>
                """.formatted(escape(usuario.getNome()));
        enviar(usuario.getEmail(), assunto, corpo);
    }

    @Async
    public void enviarRejeicao(Usuario usuario) {
        String assunto = "Seu cadastro no Portal Book Dinâmico foi rejeitado";
        String corpo = """
                <p>Olá %s,</p>
                <p>Infelizmente seu cadastro no <strong>Portal Book Dinâmico</strong> foi
                <strong>rejeitado</strong>. Se acredita que isto é um engano, entre em contato
                com o administrador.</p>
                <p>Atenciosamente,<br/>Equipe Book Dinâmico</p>
                """.formatted(escape(usuario.getNome()));
        enviar(usuario.getEmail(), assunto, corpo);
    }

    private void enviar(String para, String assunto, String corpoHtml) {
        if (!habilitado) {
            log.info("[email desabilitado] destinatário={} assunto={}", para, assunto);
            return;
        }
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(remetente);
            helper.setTo(para);
            helper.setSubject(assunto);
            helper.setText(corpoHtml, true);
            mailSender.send(mime);
            log.info("E-mail enviado para {}", para);
        } catch (MessagingException | RuntimeException e) {
            log.error("Falha ao enviar e-mail para {}: {}", para, e.getMessage(), e);
            throw new EmailException("Falha ao enviar e-mail para " + para, e);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
