package run.halo.feed;

import org.springframework.util.Assert;

public record CacheClearRule(Type type, String value) {
    public CacheClearRule {
        Assert.notNull(type, "Type cannot be null");
        Assert.notNull(value, "Value cannot be null");
        if (type == Type.EXACT && !value.startsWith("/")) {
            throw new IllegalArgumentException("Exact value must start with /");
        }
    }

    public static CacheClearRule forPrefix(String prefix) {
        return new CacheClearRule(Type.PREFIX, prefix);
    }

    public static CacheClearRule forExact(String exact) {
        return new CacheClearRule(Type.EXACT, exact);
    }

    public static CacheClearRule forContains(String contains) {
        return new CacheClearRule(Type.CONTAINS, contains);
    }

    @Override
    public String toString() {
        return "CacheClearRule{" +
            "type=" + type +
            ", value='" + value + '\'' +
            '}';
    }

    public enum Type {
        PREFIX,
        EXACT,
        CONTAINS
    }
}

