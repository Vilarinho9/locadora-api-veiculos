package br.com.locadora.exception;

/** Conflito com o estado atual (duplicidade, vínculo existente...) → HTTP 409. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
