package net.moulberry.utils;

public abstract class VectorUtils3d<V3f> {

    private static final float DEG_TO_RAD = (float) Math.PI / 180;

    protected abstract V3f vec3f(float x, float y, float z);
    protected abstract float x(V3f vec);
    protected abstract float y(V3f vec);
    protected abstract float z(V3f vec);

    protected abstract V3f normalize(V3f in);
    protected abstract float dot(V3f one, V3f two);
    protected abstract V3f add(V3f one, V3f two);
    protected abstract V3f mul(V3f one, float mult);

    public V3f getLookVector(float yaw, float pitch, boolean degrees) {
        pitch = pitch * (degrees ? DEG_TO_RAD : 1);
        yaw = -yaw * (degrees ? DEG_TO_RAD : 1);
        float cosYaw = (float) Math.cos(yaw);
        float sinYaw = (float) Math.sin(yaw);
        float cosPitch = (float) Math.cos(pitch);
        float sinPitch = (float) Math.sin(pitch);
        return vec3f(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }

    /**
     * Calculates the closest point on `along` to the line `to`
     * If the lines intersect, this will be the intersection point
     */
    public V3f getClosestPointAlong(V3f alongPoint, V3f alongDir, V3f toPoint, V3f toDir) {
        alongDir = normalize(alongDir);
        toDir = normalize(toDir);

        final float cos = dot(alongDir, toDir);
        final float n = 1 - cos * cos;
        if (n < 1E-10) {
            // the lines are parallel
            return null;
        }

        float x1F = -dot(alongPoint, alongDir);
        alongPoint = vec3f(x(alongPoint) + -x1F*x(alongDir), y(alongPoint) + -x1F*y(alongDir), z(alongPoint) + -x1F*z(alongDir));
        float x2F = -dot(toPoint, toDir);
        toPoint = vec3f(x(toPoint) + x2F*x(toDir), y(toPoint) + x2F*y(toDir), z(toPoint) + x2F*z(toDir));

        final V3f delta0 = vec3f(x(toPoint)-x(alongPoint), y(toPoint)-y(alongPoint), z(toPoint)-z(alongPoint)); // todo: zero?
        final float a = dot(delta0, alongDir);
        final float b = dot(delta0, toDir);

        final float f = (a - b * cos) / n;
        return vec3f(x(alongPoint) + f*x(alongDir), y(alongPoint) + f*y(alongDir), z(alongPoint) + f*z(alongDir));
    }

    /**
     * @param boxMin The coordinate of the minimum point of the box
     *               Must be strictly <= boxMax
     * @param boxMax The coordinate of the maximum point of the box
     *               Must be strictly >= boxMin
     * @param rayOrigin The point where the ray originates
     * @param rayDir The direction of the ray
     * @return The point at which `ray` intersects with `box`, or null if it does not intersect
     */
    public V3f getBoxIntersectionWithRay(V3f boxMin, V3f boxMax, V3f rayOrigin, V3f rayDir) {
        float minX = x(boxMin);
        float minY = y(boxMin);
        float minZ = z(boxMin);

        float maxX = x(boxMax);
        float maxY = y(boxMax);
        float maxZ = z(boxMax);

        float originX = x(rayOrigin);
        float originY = y(rayOrigin);
        float originZ = z(rayOrigin);

        if (minX <= originX && originX <= maxX &&
            minY <= originY && originY <= maxY &&
            minZ <= originZ && originZ <= maxZ) {
            // origin is inside box
            return rayOrigin;
        }

        rayDir = normalize(rayDir);

        float scalar = getBoxIntersectionWithRayAxis(minX, maxX, originX, x(rayDir));
        if (scalar != 0) {
            V3f hitX = add(rayOrigin, mul(rayDir, scalar));
            float y = y(hitX);
            float z = z(hitX);
            if (y(boxMin) <= y && y <= y(boxMax) &&
                z(boxMin) <= z && z <= z(boxMax)) {
                return hitX;
            }
        }

        scalar = getBoxIntersectionWithRayAxis(minY, maxY, originY, y(rayDir));
        if (scalar != 0) {
            V3f hitY = add(rayOrigin, mul(rayDir, scalar));
            float x = x(hitY);
            float z = z(hitY);
            if (x(boxMin) <= x && x <= x(boxMax) &&
                z(boxMin) <= z && z <= z(boxMax)) {
                return hitY;
            }
        }

        scalar = getBoxIntersectionWithRayAxis(minZ, maxZ, originZ, z(rayDir));
        if (scalar != 0) {
            V3f hitZ = add(rayOrigin, mul(rayDir, scalar));
            float x = x(hitZ);
            float y = y(hitZ);
            if (x(boxMin) <= x && x <= x(boxMax) &&
                y(boxMin) <= y && y <= y(boxMax)) {
                return hitZ;
            }
        }

        return null;
    }

    private float getBoxIntersectionWithRayAxis(float min, float max, float origin, float ray) {
        if (max < origin) {
            // find intersection scalar with max
            if (ray < 0) return (max - origin) / ray;
        } else if (min > origin) {
            // find intersection scalar with min
            if (ray > 0) return (min - origin) / ray;
        }
        return 0;
    }

}
