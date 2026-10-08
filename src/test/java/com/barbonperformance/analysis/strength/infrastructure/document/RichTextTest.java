package com.barbonperformance.analysis.strength.infrastructure.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RichTextTest {

    private final RichText richText = new RichText();

    @Test
    void doubleAsterisksMarkBold() {
        assertThat(richText.html("Targets **tennis longevity** and **core**."))
                .isEqualTo("Targets <strong>tennis longevity</strong> and <strong>core</strong>.");
    }

    @Test
    void markupInTheTextIsEscapedNotRendered() {
        assertThat(richText.html("<b>A & B</b> \"quoted\" **<i>x</i>**"))
                .isEqualTo("&lt;b&gt;A &amp; B&lt;/b&gt; &quot;quoted&quot; <strong>&lt;i&gt;x&lt;/i&gt;</strong>");
    }

    @Test
    void asterisksThatDontWrapAWordStayAsTheyAre() {
        assertThat(richText.html("3 ** 2, a lone ** and ** spaced **")).isEqualTo("3 ** 2, a lone ** and ** spaced **");
        assertThat(richText.html("**")).isEqualTo("**");
    }

    @Test
    void boldDoesNotSpanLines() {
        assertThat(richText.html("**first\nsecond**")).isEqualTo("**first\nsecond**");
    }
}
