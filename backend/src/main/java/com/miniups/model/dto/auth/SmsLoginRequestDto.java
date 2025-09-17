package com.miniups.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SmsLoginRequestDto {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{6,20}$", message = "Phone number format is invalid")
    private String phone;

    @NotBlank(message = "Verification code is required")
    private String code;

    public SmsLoginRequestDto() {
    }

    public SmsLoginRequestDto(String phone, String code) {
        this.phone = phone;
        this.code = code;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

