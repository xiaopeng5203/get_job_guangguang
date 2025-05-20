package boss;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BlackItemDeserializer extends JsonDeserializer<List<BossConfig.BlackItem>> {
    @Override
    public List<BossConfig.BlackItem> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        List<BossConfig.BlackItem> result = new ArrayList<>();
        JsonNode node = p.getCodec().readTree(p);
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    result.add(new BossConfig.BlackItem(item.asText(), null));
                } else if (item.isObject()) {
                    String name = item.has("name") ? item.get("name").asText() : null;
                    Integer days = item.has("days") && !item.get("days").isNull() ? item.get("days").asInt() : null;
                    result.add(new BossConfig.BlackItem(name, days));
                }
            }
        }
        return result;
    }
}