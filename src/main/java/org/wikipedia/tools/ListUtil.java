// License: GPL. For details, see LICENSE file.
package org.wikipedia.tools;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class ListUtil {
    private ListUtil() {
        // Private constructor to avoid instantiation
    }

    private static final BiConsumer<Integer, Integer> EMPTY_BICONSUMER = (a, b) -> {};

    /**
     * Splits the given list {@code fullList} into batches of a size of {@code maxBatchSize} or less and each batch is
     * then consumed by the given {@link Consumer} {@code processBatch}.
     * @param fullList the list that should be split into batches
     * @param maxBatchSize the maximum size of one batch
     * @param processBatch the consumer that is run on each batch
     * @param updateProgress progress updater
     * @param <T> the type of the list elements
     */
    public static <T> void processInBatches(final List<T> fullList, int maxBatchSize, final Consumer<List<T>> processBatch, final BiConsumer<Integer, Integer> updateProgress) {
        final int numPrimitives = fullList.size();
        final int numBatches = numPrimitives / maxBatchSize + (numPrimitives % maxBatchSize == 0 ? 0 : 1);
        for (int batchIndex = 0; batchIndex * maxBatchSize < numPrimitives; batchIndex++) {
            updateProgress.accept(batchIndex, numBatches);
            processBatch.accept(fullList.subList(batchIndex * maxBatchSize, Math.min(numPrimitives, (batchIndex + 1) * maxBatchSize)));
        }
    }

    public static <T> void processInBatches(final List<T> fullList, int maxBatchSize, final Consumer<List<T>> processBatch) {
        processInBatches(fullList, maxBatchSize, processBatch, EMPTY_BICONSUMER);
    }


}
