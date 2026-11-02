package br.com.locadora.exception;

/** Requisição válida, mas viola uma regra de negócio → HTTP 422. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
