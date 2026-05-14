package edu.ohsu.cmp.ecareplan.model;

import java.util.HashMap;
import java.util.Map;

public enum EndpointProviderType {
    GENERIC(null),
    EPIC("epic"),
    ORACLE("oracle");

    private static final Map<String, EndpointProviderType> TAG_MAP = new HashMap<>();
    static {
        for (EndpointProviderType ept : values()) {
            TAG_MAP.put(ept.tag, ept);
        }
    }

    public static EndpointProviderType fromTag(String tag) {
        return TAG_MAP.get(tag);
    }

    private final String tag;

    EndpointProviderType(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }
}
