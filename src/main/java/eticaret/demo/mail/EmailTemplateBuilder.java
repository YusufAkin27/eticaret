package eticaret.demo.mail;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

public final class EmailTemplateBuilder {

    private EmailTemplateBuilder() {
    }

    private static String getLogoBase64() {
        try {
            ClassPathResource logoResource = new ClassPathResource("logo.png");
            if (logoResource.exists()) {
                byte[] logoBytes = logoResource.getInputStream().readAllBytes();
                return Base64.getEncoder().encodeToString(logoBytes);
            }
        } catch (Exception e) {
            // Logo yüklenemezse boş döndür
        }
        return "";
    }

    public static String build(EmailTemplateModel model) {
        if (model == null) {
            throw new IllegalArgumentException("EmailTemplateModel cannot be null");
        }

        String title = escape(model.getTitle());
        String greeting = wrapParagraph(model.getGreeting());
        String paragraphs = buildParagraphs(model);
        String details = buildDetails(model);
        String highlight = buildHighlight(model);
        String primaryAction = buildPrimaryAction(model);
        String secondaryAction = buildSecondaryAction(model);
        String actions = primaryAction + secondaryAction;
        String customSection = buildCustomSection(model);
        String footerNote = wrapFooter(model.getFooterNote());
        String preheader = escape(model.getPreheader());
        String logoBase64 = getLogoBase64();
        String logoHtml = logoBase64.isEmpty() ? "" : 
            "<div style=\"text-align: center; margin-bottom: 24px;\">" +
            "<img src=\"data:image/png;base64," + logoBase64 + "\" alt=\"Logo\" style=\"max-width: 120px; height: auto;\">" +
            "</div>";

        return """
                <!DOCTYPE html>
                <html lang="tr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                        body {
                            margin: 0;
                            padding: 0;
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
                            background-color: #f5f5f5;
                            color: #333333;
                            line-height: 1.6;
                        }
                        .preheader {
                            display: none !important;
                            visibility: hidden;
                            opacity: 0;
                            color: transparent;
                            height: 0;
                            width: 0;
                            overflow: hidden;
                        }
                        .wrapper {
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .card {
                            background: #ffffff;
                            border-radius: 8px;
                            padding: 32px 24px;
                            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                        }
                        h1 {
                            font-size: 24px;
                            margin: 0 0 20px 0;
                            color: #333333;
                            font-weight: 600;
                        }
                        p {
                            margin: 0 0 16px 0;
                            color: #555555;
                            font-size: 15px;
                            line-height: 1.6;
                        }
                        .details {
                            background: #f9f9f9;
                            border-radius: 6px;
                            padding: 16px;
                            margin: 20px 0;
                        }
                        .details-row {
                            display: flex;
                            justify-content: space-between;
                            margin-bottom: 10px;
                        }
                        .details-row:last-child {
                            margin-bottom: 0;
                        }
                        .details-label {
                            font-weight: 600;
                            color: #333333;
                        }
                        .details-value {
                            color: #555555;
                        }
                        .cta {
                            margin: 24px 0;
                            text-align: center;
                        }
                        .button {
                            display: inline-block;
                            padding: 12px 24px;
                            border-radius: 6px;
                            text-decoration: none;
                            font-weight: 600;
                            font-size: 15px;
                        }
                        .button-primary {
                            background: #333333;
                            color: #ffffff !important;
                        }
                        .button-secondary {
                            background: transparent;
                            color: #333333;
                            border: 1px solid #333333;
                            margin-left: 12px;
                        }
                        .highlight {
                            padding: 16px;
                            background: #f9f9f9;
                            border-radius: 6px;
                            margin: 20px 0;
                            font-weight: 600;
                            color: #333333;
                            font-size: 16px;
                            text-align: center;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 32px;
                            padding-top: 20px;
                            border-top: 1px solid #e0e0e0;
                            color: #888888;
                            font-size: 12px;
                        }
                        @media (max-width: 600px) {
                            .card {
                                padding: 24px 16px;
                            }
                            .details-row {
                                flex-direction: column;
                                gap: 4px;
                            }
                            .button-secondary {
                                margin-left: 0;
                                margin-top: 12px;
                                display: block;
                            }
                        }
                    </style>
                </head>
                <body>
                    <span class="preheader">%s</span>
                    <div class="wrapper">
                        <div class="card">
                            %s
                            <h1>%s</h1>
                            %s
                            %s
                            %s
                            %s
                            %s
                            %s
                            %s
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                title,
                preheader,
                logoHtml,
                title,
                greeting,
                paragraphs,
                details,
                highlight,
                actions,
                customSection,
                footerNote
        );
    }

    private static String wrapParagraph(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return "<p style=\"color:#000000;font-weight:600;font-size:16px;\">" + escape(text) + "</p>";
    }

    private static String buildParagraphs(EmailTemplateModel model) {
        if (model.getParagraphs() == null || model.getParagraphs().isEmpty()) {
            return "";
        }

        return model.getParagraphs().stream()
                .filter(StringUtils::hasText)
                .map(text -> "<p>" + escape(text).replace("\n", "<br/>") + "</p>")
                .collect(Collectors.joining());
    }

    private static String buildDetails(EmailTemplateModel model) {
        Map<String, String> details = model.getDetails();
        if (details == null || details.isEmpty()) {
            return "";
        }

        String rows = details.entrySet().stream()
                .map(entry -> """
                        <div class="details-row">
                            <span class="details-label">%s</span>
                            <span class="details-value">%s</span>
                        </div>
                        """.formatted(escape(entry.getKey()), escape(entry.getValue())))
                .collect(Collectors.joining());

        return "<div class=\"details\">" + rows + "</div>";
    }

    private static String buildHighlight(EmailTemplateModel model) {
        if (!StringUtils.hasText(model.getHighlight())) {
            return "";
        }
        return "<div class=\"highlight\">" + escape(model.getHighlight()) + "</div>";
    }

    private static String buildPrimaryAction(EmailTemplateModel model) {
        if (!StringUtils.hasText(model.getActionText()) || !StringUtils.hasText(model.getActionUrl())) {
            return "";
        }

        String note = StringUtils.hasText(model.getActionNote())
                ? "<p style=\"margin:8px 0 0 0;font-size:13px;color:#000000;opacity:0.8;\">" + escape(model.getActionNote()) + "</p>"
                : "";

        return """
                <div class="cta">
                    <a href="%s" class="button button-primary" target="_blank" rel="noopener noreferrer">%s</a>
                    %s
                </div>
                """.formatted(escapeUrl(model.getActionUrl()), escape(model.getActionText()), note);
    }

    private static String buildSecondaryAction(EmailTemplateModel model) {
        if (!StringUtils.hasText(model.getSecondaryActionText()) || !StringUtils.hasText(model.getSecondaryActionUrl())) {
            return "";
        }

        return """
                <div class="cta">
                    <a href="%s" class="button button-secondary" target="_blank" rel="noopener noreferrer">%s</a>
                </div>
                """.formatted(escapeUrl(model.getSecondaryActionUrl()), escape(model.getSecondaryActionText()));
    }

    private static String buildCustomSection(EmailTemplateModel model) {
        return StringUtils.hasText(model.getCustomSection()) ? model.getCustomSection() : "";
    }

    private static String wrapFooter(String footerNote) {
        if (!StringUtils.hasText(footerNote)) {
            return "";
        }
        return "<p class=\"footer\">" + escape(footerNote) + "</p>";
    }

    private static String escape(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(input.length());
        input.codePoints().forEach(codePoint -> {
            switch (codePoint) {
                case '<' -> builder.append("&lt;");
                case '>' -> builder.append("&gt;");
                case '&' -> builder.append("&amp;");
                case '"' -> builder.append("&quot;");
                case '\'' -> builder.append("&#39;");
                default -> builder.appendCodePoint(codePoint);
            }
        });
        return builder.toString();
    }

    private static String escapeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return "#";
        }
        return new String(url.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}

