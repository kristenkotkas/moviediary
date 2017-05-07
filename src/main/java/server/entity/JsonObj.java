package server.entity;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

/**
 * Implementation of JsonObject that avoids casting exceptions and attempts to parse values.
 * Invalid values will be returned as null.
 *
 * @author <a href="https://bitbucket.org/kristjanhk/">Kristjan Hendrik Küngas</a>
 */
public class JsonObj extends JsonObject {

    public JsonObj(String json) {
        super(json);
    }

    public JsonObj(Map<String, Object> map) {
        super(map);
    }

    public JsonObj() {
    }

    public static JsonObj fromParent(Object parent) {
        if (parent == null || !(parent instanceof JsonObject)) {
            return null;
        }
        return new JsonObj(((JsonObject) parent).getMap());
    }

    private Object get(String key) {
        Objects.requireNonNull(key);
        return getMap().get(key);
    }

    /**
     * Gets value as String.
     */
    @Override
    public String getString(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence) {
            return value.toString();
        }
        return String.valueOf(value);
    }

    /**
     * Gets value as Integer.
     * Parses Integer, Number, String and Boolean types.
     */
    @Override
    public Integer getInteger(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return ((Integer) value);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value));
            } catch (NumberFormatException ignored) {
            }
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? 1 : 0;
        }
        return null;
    }

    /**
     * Gets value as Integer.
     * Parses Integer, Number, String and Boolean types.
     */
    @Override
    public Long getLong(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return ((Long) value);
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value));
            } catch (NumberFormatException ignored) {
            }
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? 1L : 0L;
        }
        return null;
    }

    /**
     * Gets value as Double.
     * Parses Integer, Number, String and Boolean types.
     */
    @Override
    public Double getDouble(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return ((Double) value);
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value));
            } catch (NumberFormatException ignored) {
            }
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? 1d : 0d;
        }
        return null;
    }

    /**
     * Gets value as Float.
     * Parses Integer, Number, String and Boolean types.
     */
    @Override
    public Float getFloat(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Float) {
            return ((Float) value);
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        if (value instanceof String) {
            try {
                return Float.parseFloat(((String) value));
            } catch (NumberFormatException ignored) {
            }
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? 1f : 0f;
        }
        return null;
    }

    /**
     * Gets value as Boolean.
     * True if value is true, "true" or number >= 1.
     */
    @Override
    public Boolean getBoolean(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() >= 1;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    /**
     * Gets value as JsonObject.
     * JsonObjects are converted to JsonObjs.
     * Parses JsonObject and Map types.
     */
    @SuppressWarnings("unchecked")
    @Override
    public JsonObject getJsonObject(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof JsonObject) {
            if (value instanceof JsonObj) {
                return ((JsonObj) value);
            }
            return new JsonObj(((JsonObject) value).getMap());
        }
        if (value instanceof Map) {
            return new JsonObj(((Map<String, Object>) value));
        }
        return null;
    }

    /**
     * Gets value as JsonArray.
     * Parses JsonArray and List types.
     */
    @Override
    public JsonArray getJsonArray(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof JsonArray) {
            return ((JsonArray) value);
        }
        if (value instanceof List) {
            return new JsonArray(((List) value));
        }
        return null;
    }

    /**
     * Gets value as byte[].
     */
    @Override
    public byte[] getBinary(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(String.valueOf(value));
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    @Override
    public String getString(String key, String def) {
        return get(key) != null ? getString(key) : def;
    }

    @Override
    public Integer getInteger(String key, Integer def) {
        return get(key) != null ? getInteger(key) : def;
    }

    @Override
    public Long getLong(String key, Long def) {
        return get(key) != null ? getLong(key) : def;
    }

    @Override
    public Double getDouble(String key, Double def) {
        return get(key) != null ? getDouble(key) : def;
    }

    @Override
    public Float getFloat(String key, Float def) {
        return get(key) != null ? getFloat(key) : def;
    }

    @Override
    public Boolean getBoolean(String key, Boolean def) {
        return get(key) != null ? getBoolean(key) : def;
    }

    @Override
    public JsonObject getJsonObject(String key, JsonObject def) {
        return get(key) != null ? getJsonObject(key) : def;
    }

    @Override
    public JsonArray getJsonArray(String key, JsonArray def) {
        return get(key) != null ? getJsonArray(key) : def;
    }

    @Override
    public byte[] getBinary(String key, byte[] def) {
        return get(key) != null ? getBinary(key) : def;
    }

    public JsonObj putIfAbsent(String key, Object value) {
        getMap().putIfAbsent(key, value);
        return this;
    }

    /**
     * Copies the JsonObj.
     */
    @Override
    public JsonObject copy() {
        return new JsonObj(getMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> checkAndCopy(e.getValue()))));
    }

    /**
     * Checks and copies the value.
     */
    @SuppressWarnings("unchecked")
    private Object checkAndCopy(Object val) {
        if (val == null ||
                (val instanceof Number && !(val instanceof BigDecimal)) ||
                val instanceof Boolean ||
                val instanceof String ||
                val instanceof Character) {
            return val;
        } else if (val instanceof CharSequence) {
            val = val.toString();
        } else if (val instanceof JsonObject) {
            val = ((JsonObject) val).copy();
        } else if (val instanceof JsonArray) {
            val = ((JsonArray) val).copy();
        } else if (val instanceof Map) {
            val = (new JsonObject((Map) val)).copy();
        } else if (val instanceof List) {
            val = (new JsonArray((List) val)).copy();
        } else if (val instanceof byte[]) {
            val = Base64.getEncoder().encodeToString((byte[]) val);
        } else if (val instanceof Instant) {
            val = ISO_INSTANT.format((Instant) val);
        } else {
            throw new IllegalStateException("Illegal type in JsonObject: " + val.getClass());
        }
        return val;
    }
}