package io.proleap.benchmark;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.*;

final class TreeMetrics {
    record Metrics(long nodes, int depth, String sample) {}
    private static final Set<String> INTERESTING = Set.of("moveStatement", "callStatement", "ifStatement",
            "evaluateStatement", "performStatement", "goToStatement", "computeStatement", "setStatement",
            "stringStatement", "unstringStatement", "inspectStatement", "dataDescriptionEntryFormat1");

    static Metrics measure(ParseTree tree, Parser parser) {
        long[] nodes = {0}; int[] depth = {0};
        LinkedHashMap<String, String> samples = new LinkedHashMap<>();
        walk(tree, parser, 0, nodes, depth, samples);
        StringJoiner joiner = new StringJoiner("\n");
        samples.forEach((k, v) -> joiner.add(k + ": " + v));
        return new Metrics(nodes[0], depth[0], joiner.toString());
    }

    private static void walk(ParseTree tree, Parser parser, int level, long[] nodes, int[] depth,
                             Map<String, String> samples) {
        nodes[0]++; depth[0] = Math.max(depth[0], level);
        if (tree instanceof ParserRuleContext c) {
            String rule = parser.getRuleNames()[c.getRuleIndex()];
            if (INTERESTING.contains(rule) && !samples.containsKey(rule)) {
                String value = c.toStringTree(parser).replaceAll("\\s+", " ");
                samples.put(rule, value.substring(0, Math.min(500, value.length())));
            }
        }
        for (int i = 0; i < tree.getChildCount(); i++) walk(tree.getChild(i), parser, level + 1, nodes, depth, samples);
    }
}
