package br.com.locadora.exception;

/** Recurso inexistente → HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Long id) {
        super(recurso + " não encontrado(a) com id " + id);
    }
}
