package run.halo.feed;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.halo.app.plugin.SharedEvent;

/**
 * Represents an event to request the clearing of RSS cache with flexible rules.
 * This event allows specifying multiple strategies for cache invalidation, including
 * prefix matching, exact route matching, and keyword containment.
 *
 * <p>Attributes:</p>
 * <ul>
 *   <li>rules (required, List):
 *     A list of rules defining the cache clearing strategy. Each rule includes:
 *     <ul>
 *       <li>type (required, String): The type of matching rule. Supported values are:
 *         <ul>
 *           <li>prefix: Matches cache entries with keys that start with the specified prefix.</li>
 *           <li>exact: Matches cache entries with keys that exactly match the specified value.</li>
 *           <li>contains: Matches cache entries with keys that contain the specified substring
 *           .</li>
 *         </ul>
 *       </li>
 *       <li>value (required, String): The matching value for the rule.
 *         <ul>
 *           <li>For type "prefix", the value is the prefix path (e.g., "/feed/").</li>
 *           <li>For type "exact", the value is the exact route (e.g., "/feed/moments/rss.xml")
 *           .</li>
 *           <li>For type "contains", the value is a substring to search for (e.g., "moments").</li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </li>
 *   <li>applyToAll (optional, boolean, default: false):
 *     Indicates whether to clear all cache entries. If true, all rules in the "rules" list are
 *     ignored,
 *     and the entire cache is cleared.</li>
 * </ul>
 */
@SharedEvent
@Getter
public class RssCacheClearRequested extends ApplicationEvent {
    private final List<CacheClearRule> rules;
    private final boolean applyToAll;

    public RssCacheClearRequested(Object source, List<CacheClearRule> rules, boolean applyToAll) {
        super(source);
        this.rules = (rules == null ? List.of() : new ArrayList<>(rules));
        this.applyToAll = applyToAll;
    }

    public static RssCacheClearRequested forAll(Object source) {
        return new RssCacheClearRequested(source, null, true);
    }

    public static RssCacheClearRequested forRules(Object source, List<CacheClearRule> rules) {
        return new RssCacheClearRequested(source, rules, false);
    }

    public static RssCacheClearRequested forRule(Object source, CacheClearRule rule) {
        List<CacheClearRule> rules = new ArrayList<>();
        rules.add(rule);
        return new RssCacheClearRequested(source, rules, false);
    }
}
