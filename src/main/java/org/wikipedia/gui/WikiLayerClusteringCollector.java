// License: GPL. For details, see LICENSE file.
package org.wikipedia.gui;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.openstreetmap.josm.tools.Pair;

public class WikiLayerClusteringCollector implements Collector<Point, Map<Pair<Integer, Integer>, Collection<Point>>, Collection<Collection<Point>>> {

    private final int gridWidth;
    private final int gridHeight;

    WikiLayerClusteringCollector(final double markerWidth, final double markerHeight) {
        this.gridWidth = (int) Math.min(Integer.MAX_VALUE, 3 * markerWidth);
        this.gridHeight = (int) Math.min(Integer.MAX_VALUE, 3 * markerHeight);
    }

    @Override
    public Supplier<Map<Pair<Integer, Integer>, Collection<Point>>> supplier() {
        return ConcurrentHashMap::new;
    }

    @Override
    public BiConsumer<Map<Pair<Integer, Integer>, Collection<Point>>, Point> accumulator() {
        return (result, p) -> {
            final Pair<Integer, Integer> gridPos = Pair.create(p.x / gridWidth, p.y / gridHeight);
            if (!result.containsKey(gridPos)) {
                result.put(gridPos, new ArrayList<>());
            }
            result.get(gridPos).add(p);
        };
    }

    @Override
    public BinaryOperator<Map<Pair<Integer, Integer>, Collection<Point>>> combiner() {
        return (a, b) -> {
            for (final Map.Entry<Pair<Integer, Integer>, Collection<Point>> bEntry : b.entrySet()) {
                if (a.containsKey(bEntry.getKey())) {
                    a.get(bEntry.getKey()).addAll(bEntry.getValue());
                } else {
                    a.put(bEntry.getKey(), bEntry.getValue());
                }
            }
            return a;
        };
    }

    @Override
    public Function<Map<Pair<Integer, Integer>, Collection<Point>>, Collection<Collection<Point>>> finisher() {
        return Map::values;
    }

    @Override
    public Set<Characteristics> characteristics() {
        final HashSet<Characteristics> characteristics = new HashSet<>(2);
        Collections.addAll(characteristics, Characteristics.UNORDERED);
        return characteristics;
    }
}
