package br.com.locadora.exception;

import br.com.locadora.dto.ErroResponse;
import br.com.locadora.dto.ErroResponse.CampoErro;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Tradução centralizada de exceções em respostas HTTP com formato único ({@link ErroResponse}).
 *
 * <pre>
 * 400 → validação de campos, JSON malformado, parâmetro inválido
 * 404 → recurso inexistente
 * 409 → conflito (CPF/placa duplicados, vínculos, concorrência)
 * 422 → regra de negócio violada
 * 500 → erro inesperado (detalhes só no log)
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------------------------------------------------------------- exceções do domínio

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Object> naoEncontrado(RecursoNaoEncontradoException ex, WebRequest request) {
        return montar(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<Object> conflito(ConflitoException ex, WebRequest request) {
        return montar(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Object> regraNegocio(RegraNegocioException ex, WebRequest request) {
        return montar(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, null);
    }

    // ---------------------------------------------------------------- persistência

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> integridade(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return montar(HttpStatus.CONFLICT,
                "A operação viola uma restrição de integridade (registro duplicado ou em uso)", request, null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Object> concorrencia(ObjectOptimisticLockingFailureException ex, WebRequest request) {
        return montar(HttpStatus.CONFLICT,
                "O registro foi alterado por outra operação. Atualize os dados e tente novamente", request, null);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Object> ordenacaoInvalida(PropertyReferenceException ex, WebRequest request) {
        return montar(HttpStatus.BAD_REQUEST,
                "Campo de ordenação inválido: '" + ex.getPropertyName() + "'", request, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> violacao(ConstraintViolationException ex, WebRequest request) {
        return montar(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    // ---------------------------------------------------------------- erros de requisição (400)

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new CampoErro(e.getField(), e.getDefaultMessage()))
                .toList();
        return montar(HttpStatus.BAD_REQUEST, "Dados inválidos: verifique os campos informados", request, campos);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        String mensagem = "Corpo da requisição ausente ou mal formatado";
        Throwable causa = ex.getCause();
        if (causa instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
            String campo = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            mensagem = "Valor inválido para o campo '" + campo + "': " + ife.getValue();
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                mensagem += " (valores aceitos: " + Arrays.toString(ife.getTargetType().getEnumConstants()) + ")";
            }
        }
        return montar(HttpStatus.BAD_REQUEST, mensagem, request, null);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        return montar(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro '" + ex.getPropertyName() + "': " + ex.getValue(), request, null);
    }

    /** Qualquer outra exceção padrão do Spring MVC (405, 415, 404 de rota...) sai no mesmo formato. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        String mensagem = body instanceof ProblemDetail pd && pd.getDetail() != null
                ? pd.getDetail()
                : ex.getMessage();
        return montar(statusCode, mensagem, request, null);
    }

    // ---------------------------------------------------------------- erro inesperado (500)

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> inesperado(Exception ex, WebRequest request) {
        log.error("Erro inesperado", ex);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor. Tente novamente mais tarde", request, null);
    }

    // ---------------------------------------------------------------- util

    private ResponseEntity<Object> montar(HttpStatusCode status, String mensagem, WebRequest request,
                                          List<CampoErro> campos) {
        HttpStatus resolvido = HttpStatus.resolve(status.value());
        ErroResponse corpo = new ErroResponse(
                LocalDateTime.now(),
                status.value(),
                resolvido != null ? resolvido.getReasonPhrase() : "Erro",
                mensagem,
                request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : null,
                campos);
        return ResponseEntity.status(status).body(corpo);
    }
}
