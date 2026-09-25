package com.kfokam48.codegenerator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire (B6) sur une règle réelle : les codes de présence font exactement
 * 6 caractères de l'alphabet autorisé (H6, RG18) et la génération est variée.
 */
class CodeGeneratorTest {

    private final CodeGenerator generator = new CodeGenerator();

    @Test
    void codeDe6CaracteresDansLAlphabet() {
        for (int i = 0; i < 100; i++) {
            String code = generator.nouveauCode();
            assertThat(code).hasSize(6).matches("[A-HJ-NP-Z2-9]{6}");
        }
    }

    @Test
    void deuxCodesSuccessifsSontDifferent() {
        assertThat(generator.nouveauCode()).isNotEqualTo(generator.nouveauCode());
    }
}
