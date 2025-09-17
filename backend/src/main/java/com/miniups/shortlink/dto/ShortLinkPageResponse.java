package com.miniups.shortlink.dto;

import java.util.List;

public class ShortLinkPageResponse {

    private List<ShortLinkResponse> records;
    private long total;
    private int page;
    private int size;

    public List<ShortLinkResponse> getRecords() {
        return records;
    }

    public void setRecords(List<ShortLinkResponse> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
