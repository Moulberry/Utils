package net.moulberry.utils;

import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.assertThat;

public class TestVectorUtils3d {

    @Test
    public void testTopY() {
        Vec3f result = VECTOR_UTILS.getBoxIntersectionWithRay(
                new Vec3f(-100, -10, -100),
                new Vec3f(100, -5, 100),
                new Vec3f(0, 0, 0),
                new Vec3f(1, -1, 1)
        );

        assertVec3f(result, new Vec3f(5, -5, 5));
    }

    @Test
    public void testBotY() {
        Vec3f result = VECTOR_UTILS.getBoxIntersectionWithRay(
                new Vec3f(-100, 5, -100),
                new Vec3f(100, 10, 100),
                new Vec3f(0, 0, 0),
                new Vec3f(0.3f, 1, 0.7f)
        );

        assertVec3f(result, new Vec3f(1.5f, 5, 3.5f));
    }

    @Test
    public void testMiss() {
        Vec3f result = VECTOR_UTILS.getBoxIntersectionWithRay(
                new Vec3f(100, -10, 100),
                new Vec3f(200, -5, 200),
                new Vec3f(0, 0, 0),
                new Vec3f(1, -1, 1)
        );

        assertThat(result).isNull();
    }

    @Test
    public void testInside() {
        Vec3f origin = new Vec3f(150, -7, 130);

        Vec3f result = VECTOR_UTILS.getBoxIntersectionWithRay(
                new Vec3f(100, -10, 100),
                new Vec3f(200, -5, 200),
                origin,
                new Vec3f(1, 1, 1)
        );

        assertVec3f(result, origin);
    }

    private static void assertVec3f(Vec3f result, Vec3f expected) {
        try {
            assertThat(result.x()).isWithin(0.0001f).of(expected.x());
            assertThat(result.y()).isWithin(0.0001f).of(expected.y());
            assertThat(result.z()).isWithin(0.0001f).of(expected.z());
        } catch (AssertionError e) {
            assertThat(result).isEqualTo(expected);
        }
    }

    public record Vec3f(float x, float y, float z) {}

    private static final VectorUtils3d<Vec3f> VECTOR_UTILS = new VectorUtils3d<>() {
        @Override
        protected Vec3f vec3f(float x, float y, float z) {
            return new Vec3f(x, y, z);
        }

        @Override
        protected float x(Vec3f vec) {
            return vec.x();
        }

        @Override
        protected float y(Vec3f vec) {
            return vec.y();
        }

        @Override
        protected float z(Vec3f vec) {
            return vec.z();
        }

        @Override
        protected Vec3f normalize(Vec3f in) {
            float divLength = 1 / (float)Math.sqrt(in.x()*in.x() + in.y()*in.y() + in.z()*in.z());
            return new Vec3f(in.x() * divLength, in.y() * divLength, in.z() * divLength);
        }

        @Override
        protected float dot(Vec3f one, Vec3f two) {
            return one.x()*two.x() + one.y()*two.y() + one.z()*two.z();
        }

        @Override
        protected Vec3f add(Vec3f one, Vec3f two) {
            return new Vec3f(one.x() + two.x(), one.y() + two.y(), one.z() + two.z());
        }

        @Override
        protected Vec3f mul(Vec3f one, float mult) {
            return new Vec3f(one.x() * mult, one.y() * mult, one.z() * mult);
        }
    };

}
