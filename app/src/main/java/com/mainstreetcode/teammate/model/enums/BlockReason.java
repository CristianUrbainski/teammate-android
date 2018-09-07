package com.mainstreetcode.teammate.model.enums;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

public class BlockReason extends MetaData {

    BlockReason(String code, String name) {
        super(code, name);
    }

    public static BlockReason empty() {
        return new BlockReason("", "");
    }

    public static class GsonAdapter extends MetaData.GsonAdapter<BlockReason> {
        @Override
        BlockReason fromJson(String code, String name, JsonObject body, JsonDeserializationContext context) {
            return new BlockReason(code, name);
        }
    }
}
