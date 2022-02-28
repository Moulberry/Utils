package net.moulberry.utils;

import com.google.common.collect.testing.*;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import net.moulberry.utils.probability.WeightedRandomSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.truth.Truth.assertThat;

public class WeightedRandomSetTest {

    @Test
    public void guavaSetTest() {
        TestSuite test = SetTestSuiteBuilder.using(new TestStringSetGenerator() {
            @Override
            protected Set<String> create(String[] elements) {
                WeightedRandomSet<String> set = new WeightedRandomSet<>(true);
                set.addAll(Arrays.asList(elements));
                return set;
            }
        }).named("randomSet")
          .withFeatures(
                  CollectionFeature.SUPPORTS_ADD,
                  CollectionFeature.SUPPORTS_REMOVE,
                  CollectionSize.ANY
          ).createTestSuite();

        TestRunner testRunner = new TestRunner(System.err);
        TestResult result = testRunner.doRun(test);

        assert result.wasSuccessful();
    }

    @Test
    public void cloneTest() {
        WeightedRandomSet<Integer> set = new WeightedRandomSet<>();
        set.add(1, 1);
        set.add(2, 7);
        set.add(3, 3);

        // noinspection unchecked
        WeightedRandomSet<Integer> cloned = set.deepClone();

        assertThat(cloned).isEqualTo(set);
        assertThat(cloned.hashCode()).isEqualTo(set.hashCode());
        assertThat(cloned.size()).isEqualTo(set.size());
        assertThat(cloned.totalWeight()).isEqualTo(set.totalWeight());
    }

    @Test
    public void emptyTree() {
        WeightedRandomSet<Integer> set = new WeightedRandomSet<>();
        assertThat(set.sample()).isNull();
    }

    @Test
    public void negativeWeight() {
        WeightedRandomSet<Integer> set = new WeightedRandomSet<>();
        Assertions.assertThrows(AssertionError.class, () -> {
            set.add(1337, -3);
        });
    }

    @Test
    public void singleTree() {
        WeightedRandomSet<Integer> set = new WeightedRandomSet<>();
        set.add(1337, 3);
        assertThat(set.sample()).isEqualTo(1337);
    }

    @Test
    public void containsTest() {
        WeightedRandomSet<Integer> set = new WeightedRandomSet<>();

        assertThat(set.add(1337, 2)).isTrue();
        assertThat(set.add(1337, 2)).isFalse();
        assertThat(set.contains(1337)).isTrue();
        assertThat(set.contains(69)).isFalse();
        assertThat(set.pop()).isEqualTo(1337);
        assertThat(set.contains(1337)).isFalse();
        assertThat(set.contains(69)).isFalse();
    }

    @Test
    public void simultaneousIterationTest() {
        WeightedRandomSet<Integer> set = new WeightedRandomSet<>();

        set.add(1, 1);
        set.add(2, 1);
        set.add(3, 1);

        Iterator<Integer> iterator1 = set.iterator();
        Iterator<Integer> iterator2 = set.iterator();

        Assertions.assertDoesNotThrow(iterator2::hasNext);
        Assertions.assertDoesNotThrow(iterator2::next);
        Assertions.assertThrows(IllegalStateException.class, iterator1::hasNext);
        Assertions.assertThrows(IllegalStateException.class, iterator1::next);
    }

    @Test
    public void invalidIterationRemoveTest() {
        WeightedRandomSet<Integer> set = new WeightedRandomSet<>();

        set.add(1, 1);
        set.add(2, 1);
        set.add(3, 1);

        Iterator<Integer> iterator = set.iterator();

        set.remove(4); // Element not in set shouldn't fail iteration

        Assertions.assertDoesNotThrow(iterator::hasNext);
        Assertions.assertDoesNotThrow(iterator::next);

        set.remove(1); // Element in set should now fail iteration

        Assertions.assertThrows(IllegalStateException.class, iterator::hasNext);
        Assertions.assertThrows(IllegalStateException.class, iterator::next);
    }

    @Test
    public void invalidIterationAddTest() {
        WeightedRandomSet<Integer> set = new WeightedRandomSet<>();

        set.add(1, 1);
        set.add(2, 1);
        set.add(3, 1);

        Iterator<Integer> iterator = set.iterator();

        Assertions.assertDoesNotThrow(iterator::hasNext);
        Assertions.assertDoesNotThrow(iterator::next);

        set.add(4, 1);

        Assertions.assertThrows(IllegalStateException.class, iterator::hasNext);
        Assertions.assertThrows(IllegalStateException.class, iterator::next);
    }

    @Test
    public void iterationTest() {
        WeightedRandomSet<Integer> set = new WeightedRandomSet<>();

        Set<Integer> values = new HashSet<>();
        for (int i=0; i<100; i++) {
            set.add(i, 1);
            values.add(i);
        }

        for (int sample : set) {
            assertThat(sample).isIn(values);
            values.remove(sample);
        }

        assertThat(values).isEmpty();
        assertThat(set.size()).isEqualTo(100);
    }

    @Test
    public void popTest() {
        WeightedRandomSet<Integer> set = new WeightedRandomSet<>();

        Set<Integer> values = new HashSet<>();
        for (int i=0; i<100; i++) {
            values.add(i);
            set.add(i, 1);
        }

        while (!values.isEmpty()) {
            Integer popped = set.pop();

            assertThat(popped).isNotNull();
            assertThat(popped).isIn(values);
            assertThat(set.contains(popped)).isFalse();

            values.remove(popped);

            assertThat(set.size()).isEqualTo(values.size());
            assertThat(set.totalWeight()).isEqualTo(values.size());
        }

        assertThat(values).isEmpty();
        assertThat(set.pop()).isNull();
    }

    public int[][] calculateTheoreticalFairness(int trials) {
        int[][] samples = new int[3][3];

        Random rand = ThreadLocalRandom.current();
        for (int i=0; i<trials; i++) {
            float f = rand.nextFloat()*100;
            if (f <= 17) {
                samples[0][0]++;
                if (rand.nextFloat()*(32+51) < 32) {
                    samples[1][1]++;
                    samples[2][2]++;
                } else {
                    samples[1][2]++;
                    samples[2][1]++;
                }
            } else if (f <= 17+32) {
                samples[0][1]++;
                if (rand.nextFloat()*(17+51) < 17) {
                    samples[1][0]++;
                    samples[2][2]++;
                } else {
                    samples[1][2]++;
                    samples[2][0]++;
                }
            } else {
                samples[0][2]++;
                if (rand.nextFloat()*(17+32) < 17) {
                    samples[1][0]++;
                    samples[2][1]++;
                } else {
                    samples[1][1]++;
                    samples[2][0]++;
                }
            }
        }

        return samples;
    }

    @Test
    public void iterationFairnessTest() {
        WeightedRandomSet<Integer> set = new WeightedRandomSet<>();

        final float totalWeight = 100;
        set.add(0, 17);
        set.add(1, 32);
        set.add(2, 51);

        assertThat(set.totalWeight()).isEqualTo(totalWeight);
        assertThat(set.size()).isEqualTo(3);

        int[][] actualSamples = new int[3][3];

        final int trials = 1_000_000;
        for (int i=0; i<trials; i++) {
            int index = 0;
            for (int sample : set) {
                actualSamples[index++][sample]++;
            }
        }

        int[][] theoreticalSamples = calculateTheoreticalFairness(trials);

        for (int i=0; i<3; i++) {
            for (int j=0; j<3; j++) {
                assertThat(actualSamples[i][j]/(float)trials)
                        .isWithin(0.01f).of(theoreticalSamples[i][j]/(float)trials);
            }
        }
    }

    @Test
    public void fairnessTest() {
        WeightedRandomSet<Integer> set = new WeightedRandomSet<>();

        final float totalWeight = 100;
        set.add(1, 17);
        set.add(1337, 32);
        set.add(64, 51);

        assertThat(set.totalWeight()).isEqualTo(totalWeight);
        assertThat(set.size()).isEqualTo(3);

        int samples1 = 0;
        int samples2 = 0;
        int samples3 = 0;

        final int trials = 1_000_000;
        for (int i=0; i<trials; i++) {
            Integer sample = set.sample();

            assertThat(sample).isNotNull();

            if (sample == 1) {
                samples1++;
            } else if (sample == 1337) {
                samples2++;
            } else if (sample == 64) {
                samples3++;
            } else {
                throw new IllegalStateException("Returned unknown value");
            }
        }

        float ratioSamples1 = (float)samples1/trials;
        float ratioSamples2 = (float)samples2/trials;
        float ratioSamples3 = (float)samples3/trials;

        assertThat(ratioSamples1).isWithin(0.01f).of(17/totalWeight);
        assertThat(ratioSamples2).isWithin(0.01f).of(32/totalWeight);
        assertThat(ratioSamples3).isWithin(0.01f).of(51/totalWeight);
    }

}
