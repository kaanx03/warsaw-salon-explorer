package com.kaandev.salonexplorer.ingestion.normalizer;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PhoneNormalizer {

    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    public String normalize(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) return null;
        try {
            var number = phoneUtil.parse(rawPhone, "PL");
            if (!phoneUtil.isValidNumber(number)) {
                log.debug("Invalid phone number: {}", rawPhone);
                return null;
            }
            return phoneUtil.format(number, PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            log.debug("Phone parse failed: {} → {}", rawPhone, e.getMessage());
            return null;
        }
    }
}
