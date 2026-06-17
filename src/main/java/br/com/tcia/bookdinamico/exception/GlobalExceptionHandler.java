package br.com.tcia.bookdinamico.exception;

import br.com.tcia.bookdinamico.controller.response.ApiErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Log4j2
@ControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ApiErroResponse> buildErrorResponse(
            Exception ex, HttpStatus status, String cabecalhoLog, String message,
            HttpServletRequest request, boolean logar) {

        if (logar) {
            log.error("{} em [{}]: {}", ex.getClass().getSimpleName(),
                    request.getRequestURI(), cabecalhoLog.concat(message), ex);
        }

        ApiErroResponse error = ApiErroResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ApiErroResponse> handleNegocioException(NegocioException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, "Erro de negócio: ", ex.getChave(), request, true);
    }

    @ExceptionHandler(SistemaException.class)
    public ResponseEntity<ApiErroResponse> handleSistemaException(SistemaException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Sistema Exception: ", ex.getMessage(), request, true);
    }

    @ExceptionHandler(ErroAutenticacaoException.class)
    public ResponseEntity<ApiErroResponse> handleAutenticacao(ErroAutenticacaoException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, "Erro de autenticação: ", ex.getChave(), request, false);
    }

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<ApiErroResponse> handleEmailException(EmailException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, "Erro ao enviar e-mail: ", ex.getMessage(), request, true);
    }

    @ExceptionHandler(ArquivoException.class)
    public ResponseEntity<ApiErroResponse> handleArquivoException(ArquivoException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, "Erro com arquivo: ", ex.getChave(), request, true);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErroResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, "Recurso não encontrado: ", ex.getMessage(), request, true);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErroResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensagens = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> String.format("%s: %s", fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.joining(" | "));
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, "Validação falhou: ", mensagens, request, true);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErroResponse> handleHibernateConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, "Violação de constraint: ",
                "Algum campo não está respeitando as regras do banco.", request, true);
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiErroResponse> handleTransactionSystem(TransactionSystemException ex, HttpServletRequest request) {
        Throwable causa = ex.getCause();
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violationEx) {
                String mensagens = violationEx.getConstraintViolations().stream()
                        .map(v -> String.format("%s: %s", v.getPropertyPath(), v.getMessage()))
                        .reduce("", (msg1, msg2) -> msg1.isEmpty() ? msg2 : msg1 + " | " + msg2);
                return buildErrorResponse(violationEx, HttpStatus.BAD_REQUEST, "Violação de constraint: ", mensagens, request, true);
            }
            causa = causa.getCause();
        }
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, "Erro de transação: ",
                "Dados inválidos ou incompletos. Verifique os campos obrigatórios.", request, true);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErroResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, "Violação de integridade: ",
                "Dados inválidos ou duplicados.", request, true);
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<ApiErroResponse> handleSqlIntegrityConstraint(SQLIntegrityConstraintViolationException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, "Restrição de integridade do banco: ",
                "Dados referenciados não existem ou são inválidos.", request, true);
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ApiErroResponse> handleDataAccessResourceFailure(DataAccessResourceFailureException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, "Erro de acesso ao recurso: ", ex.getMessage(), request, true);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErroResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, "Acesso negado: ", ex.getMessage(), request, true);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErroResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, "Requisição inválida (JSON malformado): ", ex.getMessage(), request, true);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErroResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String msg = String.format("Parâmetro '%s' inválido. Valor recebido: '%s'", ex.getName(), ex.getValue());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, "Parâmetro inválido: ", msg, request, true);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiErroResponse> handleIOException(IOException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Erro de I/O: ", ex.getMessage(), request, true);
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ApiErroResponse> handleNumberFormatException(NumberFormatException ex, HttpServletRequest request) {
        String msg = "Formato numérico inválido: " + ex.getMessage();
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, "Erro de formato numérico: ", msg, request, true);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErroResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, "Endpoint não encontrado: ",
                "Recurso não encontrado para " + request.getRequestURI(), request, false);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErroResponse> handleException(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado: ", ex.getMessage(), request, true);
    }
}
