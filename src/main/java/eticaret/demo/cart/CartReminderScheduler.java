package eticaret.demo.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import eticaret.demo.audit.AuditLogService;
import eticaret.demo.mail.EmailMessage;
import eticaret.demo.mail.MailService;
import eticaret.demo.mail.EmailTemplateBuilder;
import eticaret.demo.mail.EmailTemplateModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartReminderScheduler {

    private final CartService cartService;
    private final MailService mailService;
    private final AuditLogService auditLogService;

    /**
     * Günde 3 kez çalışır (sabah 10:00, öğlen 14:00, akşam 18:00)
     * 2 saat önce sepete ürün eklenmiş ve hala aktif olan sepetler için hatırlatma maili gönder
     * Bir kere mail gönderildiyse 3 gün boyunca tekrar gönderilmez
     */
    @Scheduled(cron = "0 0 10,14,18 * * ?") // Günde 3 kez: 10:00, 14:00, 18:00
    @Transactional
    public void sendCartReminderEmails() {
        log.info("Sepet hatırlatma maili kontrolü başlatılıyor...");
        
        try {
            List<Cart> cartsToRemind = cartService.getCartsForReminderEmail();
            log.info("Hatırlatma maili gönderilecek sepet sayısı: {}", cartsToRemind.size());
            
            int sentCount = 0;
            for (Cart cart : cartsToRemind) {
                try {
                    // Mail gönderilmiş mi kontrol et (audit log'dan)
                    if (hasReminderEmailSent(cart.getId())) {
                        log.debug("Sepet {} için hatırlatma maili daha önce gönderilmiş, atlanıyor", cart.getId());
                        continue;
                    }
                    
                    sendReminderEmail(cart);
                    sentCount++;
                    
                    // Audit log
                    auditLogService.logSimple("CART_REMINDER_EMAIL", "Cart", cart.getId(),
                            "Sepet hatırlatma maili gönderildi", null);
                    
                } catch (Exception e) {
                    log.error("Sepet {} için hatırlatma maili gönderilirken hata: {}", cart.getId(), e.getMessage(), e);
                    auditLogService.logError("CART_REMINDER_EMAIL", "Cart", cart.getId(),
                            "Hatırlatma maili gönderilirken hata: " + e.getMessage(), e.getMessage(), null);
                }
            }
            
            log.info("Sepet hatırlatma maili işlemi tamamlandı. Gönderilen: {}", sentCount);
        } catch (Exception e) {
            log.error("Sepet hatırlatma maili işlemi sırasında hata: {}", e.getMessage(), e);
        }
    }

    /**
     * Hatırlatma maili gönder
     */
    private void sendReminderEmail(Cart cart) {
        String email = null;
        String userName = "Değerli Müşterimiz";
        
        if (cart.getUser() != null && cart.getUser().getEmail() != null) {
            email = cart.getUser().getEmail();
            userName = cart.getUser().getEmail().split("@")[0]; // Email'den isim çıkar
        } else if (cart.getGuestUserId() != null) {
            // Guest kullanıcı için email yok, atla
            log.debug("Guest sepet {} için email adresi yok, mail gönderilemiyor", cart.getId());
            return;
        }
        
        if (email == null || email.isEmpty()) {
            log.warn("Sepet {} için email adresi bulunamadı", cart.getId());
            return;
        }
        
        String subject = "Sepetinizi Onaylamayı Unutmayın! 🛒";
        String htmlContent = buildReminderEmailContent(cart, userName);
        
        EmailMessage emailMessage = EmailMessage.builder()
                .toEmail(email)
                .subject(subject)
                .body(htmlContent)
                .isHtml(true)
                .build();
        
        mailService.queueEmail(emailMessage);
        log.info("Sepet hatırlatma maili gönderildi: {} -> {}", cart.getId(), email);
    }

    /**
     * Hatırlatma maili içeriğini oluştur
     */
    private String buildReminderEmailContent(Cart cart, String userName) {
        int itemCount = cart.getItems() != null ? cart.getItems().size() : 0;
        String itemsHtml = buildCartItemsHtml(cart);

        LinkedHashMap<String, String> details = new LinkedHashMap<>();
        details.put("Sepet No", cart.getId() != null ? cart.getId().toString() : "-");
        details.put("Ürün Sayısı", String.valueOf(itemCount));
        details.put("Toplam Tutar", formatPrice(cart.getTotalAmount()));

        List<String> paragraphs = new ArrayList<>();
        paragraphs.add("Sepetinizde <strong>" + itemCount + "</strong> ürün bulunuyor ve henüz onaylamadınız.");
        if (!itemsHtml.isEmpty()) {
            paragraphs.add("<div style=\"margin: 20px 0;\">" + itemsHtml + "</div>");
        }
        paragraphs.add("Sepetinizi tamamlamak için ürünlerinizi gözden geçirebilir ve ödemenizi gerçekleştirebilirsiniz.");

        return EmailTemplateBuilder.build(EmailTemplateModel.builder()
                .title("Sepetinizi Onaylamayı Unutmayın!")
                .preheader("Sepetinizde bekleyen ürünler var.")
                .greeting("Merhaba " + sanitize(userName) + ",")
                .paragraphs(paragraphs)
                .highlight("Toplam Tutar: " + formatPrice(cart.getTotalAmount()))
                .details(details)
                .actionText("Sepetimi Görüntüle")
                .actionUrl("yusufakin.online/cart")
                .footerNote("Bu mail, sepetinize ürün ekledikten 2 saat sonra otomatik olarak gönderildi.")
                .build());
    }

    private String buildCartItemsHtml(Cart cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<div style=\"margin: 20px 0;\">");
        
        for (CartItem item : cart.getItems()) {
            if (item.getProduct() == null) {
                continue;
            }
            
            String productImageUrl = item.getProduct().getCoverImageUrl();
            String imageHtml = "";
            if (productImageUrl != null && !productImageUrl.isEmpty()) {
                imageHtml = String.format(
                    "<div style=\"text-align: center; margin-bottom: 12px;\">" +
                    "<img src=\"%s\" alt=\"%s\" style=\"max-width: 200px; width: 100%%; height: auto; border-radius: 8px; object-fit: cover;\">" +
                    "</div>",
                    sanitize(productImageUrl),
                    sanitize(item.getProduct().getName())
                );
            }
            
            builder.append("<div style=\"background:#ffffff;padding:16px;margin:12px 0;border-radius:8px;border:1px solid #e0e0e0;box-shadow:0 2px 4px rgba(0,0,0,0.05);\">");
            
            if (!imageHtml.isEmpty()) {
                builder.append(imageHtml);
            }
            
            builder.append("<div style=\"font-weight:600;color:#333333;font-size:16px;margin-bottom:8px;word-wrap:break-word;\">")
                    .append(sanitize(item.getProduct().getName()))
                    .append("</div>");
            
            builder.append("<div style=\"color:#555555;font-size:14px;line-height:1.6;\">");
            builder.append("<div style=\"margin-bottom:4px;\">Adet: <strong>").append(item.getQuantity()).append("</strong></div>");
            
            if (item.getWidth() != null && item.getHeight() != null) {
                builder.append("<div style=\"margin-bottom:4px;\">Boyut: ").append(item.getWidth()).append(" x ").append(item.getHeight()).append(" cm</div>");
            }
            if (item.getPleatType() != null) {
                builder.append("<div style=\"margin-bottom:4px;\">Pile: ").append(sanitize(item.getPleatType())).append("</div>");
            }
            
            builder.append("<div style=\"margin-top:12px;padding-top:12px;border-top:1px solid #e0e0e0;color:#333333;font-weight:600;font-size:15px;\">")
                    .append("Fiyat: ").append(formatPrice(item.getSubtotal()))
                    .append("</div>");
            
            builder.append("</div>");
            builder.append("</div>");
        }
        
        builder.append("</div>");
        return builder.toString();
    }

    private String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private String formatPrice(BigDecimal amount) {
        if (amount == null) {
            return "0,00 ₺";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString() + " ₺";
    }

    /**
     * Bu sepet için hatırlatma maili daha önce gönderilmiş mi?
     */
    private boolean hasReminderEmailSent(Long cartId) {
        // Audit log'dan kontrol et
        return auditLogService.hasReminderEmailSent(cartId);
    }
}

