package com.barbonperformance.analysis.strength.infrastructure.document;

import com.barbonperformance.analysis.strength.domain.ProgramDocument;
import com.barbonperformance.analysis.strength.domain.ProgramDocumentRenderer;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Component
class HtmlProgramDocumentRenderer implements ProgramDocumentRenderer {

    private static final String TEMPLATE = "training-program";
    private static final DateTimeFormatter ISSUED_ON = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final TemplateEngine templates;

    HtmlProgramDocumentRenderer() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("pdf/");
        resolver.setSuffix(".html");
        // OpenHTMLtoPDF parses strict XHTML; XML mode makes Thymeleaf emit exactly that.
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setCharacterEncoding("UTF-8");
        this.templates = new TemplateEngine();
        this.templates.setTemplateResolver(resolver);
    }

    @Override
    public byte[] render(ProgramDocument document) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("doc", document);
        context.setVariable("issuedOn", ISSUED_ON.format(document.issuedOn()));
        String html = templates.process(TEMPLATE, context);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            font(builder, "DMSans-Regular.ttf", "DM Sans", 400);
            font(builder, "DMSans-Bold.ttf", "DM Sans", 700);
            font(builder, "BebasNeue-Regular.ttf", "Bebas Neue", 400);
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not render the program PDF", e);
        }
    }

    private static void font(PdfRendererBuilder builder, String file, String family, int weight) {
        builder.useFont(() -> open("pdf/fonts/" + file), family, weight, FontStyle.NORMAL, true);
    }

    private static InputStream open(String path) {
        try {
            return new ClassPathResource(path).getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException("Missing bundled font " + path, e);
        }
    }
}
