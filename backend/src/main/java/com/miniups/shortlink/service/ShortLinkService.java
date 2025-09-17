package com.miniups.shortlink.service;

import com.miniups.shortlink.dto.ShortLinkCreateRequest;
import com.miniups.shortlink.dto.ShortLinkPageResponse;
import com.miniups.shortlink.dto.ShortLinkResponse;
import com.miniups.shortlink.dto.ShortLinkUpdateRequest;

import jakarta.servlet.http.HttpServletRequest;

public interface ShortLinkService {

    ShortLinkResponse createShortLink(Long userId, ShortLinkCreateRequest request);

    ShortLinkResponse updateShortLink(Long userId, String shortCode, ShortLinkUpdateRequest request);

    ShortLinkResponse getShortLinkDetails(Long userId, String shortCode);

    ShortLinkPageResponse listShortLinks(int page, int size);

    String resolveRedirect(String shortCode, HttpServletRequest request);
}
