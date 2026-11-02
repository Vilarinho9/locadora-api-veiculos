package br.com.locadora;

import br.com.locadora.validation.CpfUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpfUtilTest {

    @Test
    void aceitaCpfsValidosComEmMascara() {
        assertTrue(CpfUtil.valido("529.982.247-25"));
        assertTrue(CpfUtil.valido("52998224725"));
        assertTrue(CpfUtil.valido("111.444.777-35"));
        assertTrue(CpfUtil.valido("123.456.789-09"));
        assertTrue(CpfUtil.valido("390.533.447-05"));
    }

    @Test
    void rejeitaCpfsInvalidos() {
        assertFalse(CpfUtil.valido("529.982.247-24"));   // dígito verificador errado
        assertFalse(CpfUtil.valido("111.111.111-11"));   // sequência repetida
        assertFalse(CpfUtil.valido("123"));              // tamanho errado
        assertFalse(CpfUtil.valido(""));
        assertFalse(CpfUtil.valido(null));
    }

    @Test
    void somenteDigitosRemoveMascara() {
        assertEquals("52998224725", CpfUtil.somenteDigitos("529.982.247-25"));
    }
}
