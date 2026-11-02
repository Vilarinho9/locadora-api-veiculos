package br.com.locadora.domain;

public enum CategoriaVeiculo {
    ECONOMICO,
    INTERMEDIARIO,
    SUV,
    LUXO,
    ESPORTIVO;

    /** Categorias premium exigem idade mínima maior do condutor. */
    public boolean isPremium() {
        return this == LUXO || this == ESPORTIVO;
    }
}
