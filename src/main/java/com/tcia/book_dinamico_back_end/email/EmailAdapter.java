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

    /** Indica se o envio de e-mails está ativo (configurado em {@code app.email.habilitado}). */
    public boolean isHabilitado() {
        return habilitado;
    }

    @Async
    public void enviarAprovacao(Usuario usuario) {
        String assunto = "Seu cadastro no Portal Book foi aprovado";
        String corpo = """
                <p>Olá %s,</p>
                <p>Boa notícia! Seu cadastro no <strong>Portal Book</strong> foi
                <strong>aprovado</strong> pelo administrador.</p>
                <p>Você já pode entrar com o e-mail e a senha que cadastrou.</p>
                <p>Atenciosamente,<br/>Equipe Book</p>
                """.formatted(escape(usuario.getNome()));
        enviar(usuario.getEmail(), assunto, corpo);
    }

    @Async
    public void enviarSenhaTemporaria(Usuario usuario, String senhaTemporaria) {
        String assunto = "Sua nova senha de acesso — Portal Book";
        String corpo = """
                <p>Olá %s,</p>
                <p>Você solicitou a alteração de senha no <strong>Portal Book</strong>.</p>
                <p>Sua nova senha temporária é:</p>
                <p style="font-size:18px;font-weight:bold;letter-spacing:1px;background:#f3f3f3;padding:8px 14px;display:inline-block;border-radius:4px;">%s</p>
                <p>Recomendamos que, após o login, você acesse o portal e troque por uma senha de sua preferência.</p>
                <p>Se você não solicitou esta alteração, entre em contato imediatamente com o administrador.</p>
                <p>Atenciosamente,<br/>Equipe Book</p>
                """.formatted(escape(usuario.getNome()), escape(senhaTemporaria));
        enviar(usuario.getEmail(), assunto, corpo);
    }

    @Async
    public void enviarRejeicao(Usuario usuario) {
        String assunto = "Seu cadastro no Portal Book foi rejeitado";
        String corpo = """
                <p>Olá %s,</p>
                <p>Infelizmente seu cadastro no <strong>Portal Book</strong> foi
                <strong>rejeitado</strong>. Se acredita que isto é um engano, entre em contato
                com o administrador.</p>
                <p>Atenciosamente,<br/>Equipe Book</p>
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
