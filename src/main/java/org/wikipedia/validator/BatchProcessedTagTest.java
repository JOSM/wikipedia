// License: GPL. For details, see LICENSE file.
package org.wikipedia.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.wikipedia.tools.ListUtil;

public abstract class BatchProcessedTagTest<T extends BatchProcessedTagTest.TestCompanion> extends Test.TagTest {

    Notification finalNotification = null;

    BatchProcessedTagTest(String name, String description) {
        super(name, description);
    }

    private final List<T> primitivesForBatches = new ArrayList<>();

    /**
     * Creates a companion object for the given primitive, on which the test can later continue to operate.
     * E.g. if you want to perform some check on tag xyz=*, you could return here the String value of tag xyz=*. When processing a batch later on you can
     * @param primitive a primitive for which a companion object should be created
     * @return the companion object if the primitive should be checked in a batch
     *     or {@code null} if the given primitive should be excluded from the check
     */
    protected abstract T prepareTestCompanion(final OsmPrimitive primitive);

    /**
     * This can be used as last argument for {@link ListUtil#processInBatches(List, int, Consumer, BiConsumer)}
     * @param batchIndex the index of the currently processed batch (starting at 0)
     * @param numBatches the total number of batches that are processed
     */
    final void updateBatchProgress(int batchIndex, int numBatches) {
        progressMonitor.setExtraText(I18n.tr("({0} items, processing batch {1} of {2})", primitivesForBatches.size(), batchIndex + 1, numBatches));
    }

    @Override
    public final void startTest(ProgressMonitor progressMonitor) {
        primitivesForBatches.clear();
        finalNotification = null;
        super.startTest(progressMonitor);
    }

    @Override
    public final void check(final OsmPrimitive primitive) {
        final T testCompanion = primitive == null ? null : prepareTestCompanion(primitive);
        if (testCompanion != null) {
            primitivesForBatches.add(testCompanion);
        }
    }

    protected abstract void check(final List<T> allPrimitives);

    @Override
    public final void endTest() {
        try {
            check(primitivesForBatches);
            if (finalNotification != null) {
                finalNotification.show();
            }
            super.endTest();
        } catch (Exception e) {
            Logging.error(e);
        }
    }

    abstract static class TestCompanion {
        private final OsmPrimitive primitive;

        TestCompanion(final OsmPrimitive primitive) {
            this.primitive = Objects.requireNonNull(primitive);
        }

        final OsmPrimitive getPrimitive() {
            return primitive;
        }
    }
}
