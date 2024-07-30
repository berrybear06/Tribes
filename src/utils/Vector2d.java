package utils;

import java.util.*;

/**
 * This class represents a vector, or a position, in the map.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 19/12/11
 */
public class Vector2d
{
    /**
     * X-coordinate of the vector.
     */
    public final int x;

    /**
     * Y-coordinate of the vector.
     */
    public final int y;

    /**
     * Default constructor.
     */
    public Vector2d() {
        this(0, 0);
    }

    /**
     * Checks if a vector and this are the same.
     * @param o the other vector to check
     * @return true if their coordinates are the same.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Vector2d) {
            Vector2d v = (Vector2d) o;
            return x == v.x && y == v.y;
        } else {
            return false;
        }
    }

    public boolean equalsPlusError(Object o, double error) {
        if (o instanceof Vector2d) {
            Vector2d v = (Vector2d) o;
            return x >= v.x - error && x <= v.x + error &&
                    y >= v.y - error && y <= v.y + error;
        } else {
            return false;
        }
    }

    /**
     * Builds a vector from its coordinates.
     * @param x x coordinate
     * @param y y coordinate
     */
    public Vector2d(int x, int y) {
        this.x = x;
        this.y = y;
    }


    /**
     * Builds a vector from another vector.
     * @param v Vector to copy from.
     */
    public Vector2d(Vector2d v) {
        this.x = v.x;
        this.y = v.y;
    }

    /**
     * Creates a copy of this vector
     * @return a copy of this vector
     */
    public Vector2d copy() {
        return new Vector2d(x,y);
    }


    /**
     * Returns a representative String of this vector.
     * @return a representative String of this vector.
     */
    @Override
    public String toString() {
        return x + " : " + y;
    }

    /**
     * Adds two vectors
     * @param a first vector
     * @param b second vector
     * @return returns the vector a+b.
     */
    public static Vector2d add(Vector2d a, Vector2d b) {
        return new Vector2d(a.x + b.x, a.y + b.y);
    }

    public Vector2d add(Vector2d b) {
        return add(this, b);
    }

    /**
     * Subtracts one vector from another
     * @param a first vector
     * @param b second vector
     * @return returns the vector a-b.
     */
    public static Vector2d subtract(Vector2d a, Vector2d b) {
        return new Vector2d(a.x - b.x, a.y - b.y);
    }

    public Vector2d subtract(Vector2d b) {
        return subtract(this, b);
    }

    /**
     * Multiplies a vector by a factor.
     * @param v vector
     * @param fac factor to multiply v by.
     * @return the vector v*fac.
     */
    public static Vector2d mul(Vector2d v, int fac) {
        return new Vector2d(v.x * fac, v.y * fac);
    }

    public Vector2d mul(int fac) {
        return mul(this, fac);
    }

    /**
     * Rotates a vector by an angle in radians.
     * @param v vector
     * @param theta angle in radians
     * @return the rotated vector
     */
    public static Vector2d rotate(Vector2d v, double theta) {
        // rotate this vector by the angle made to the horizontal by this line
        // theta is in radians
        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);

        int nx = (int)(v.x * cosTheta - v.y * sinTheta);
        int ny = (int)(v.x * sinTheta + v.y * cosTheta);

        return new Vector2d(nx, ny);
    }

    public Vector2d rotate(double theta) {
        return rotate(this, theta);
    }

    /**
     * Calculates the scalar product of this vector and the one passed by parameter
     * @param v vector to do the scalar product with.
     * @return the value of the scalar product.
     */
    public int scalarProduct(Vector2d v) {
        return x * v.x + y * v.y;
    }

    /**
     * Gets the square value of the parameter passed.
     * @param x parameter
     * @return x * x
     */
    public static int sqr(int x) {
        return x * x;
    }

    /**
     * Returns the square distance from this vector to the one in the arguments.
     * @param v the other vector, to calculate the distance to.
     * @return the square distance, in pixels, between this vector and v.
     */
    public int sqDist(Vector2d v) {
        return sqr(x - v.x) + sqr(y - v.y);
    }

    /**
     * Gets the magnitude of the vector.
     * @return the magnitude of the vector (Math.sqrt(sqr(x) + sqr(y)))
     */
    public double mag() {
        return Math.sqrt(sqr(x) + sqr(y));
    }

    /**
     * Returns the distance from this vector to the one in the arguments.
     * @param v the other vector, to calculate the distance to.
     * @return the distance, in pixels, between this vector and v.
     */
    public double dist(Vector2d v) {
        return Math.sqrt(sqDist(v));
    }

    /**
     * Returns the distance from this vector to a pair of coordinates.
     * @param xx x coordinate
     * @param yy y coordinate
     * @return the distance, in pixels, between this vector and the pair of coordinates.
     */
    public double dist(int xx, int yy) {
        return Math.sqrt(sqr(x - xx) + sqr(y - yy));
    }

    public double custom_dist(int xx, int yy) {
        return Math.max(Math.abs(x - xx), Math.abs(y - yy));
    }

    public double custom_dist(Vector2d other) {
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }


    /**
     * Returns the atan2 of this vector.
     * @return the atan2 of this vector.
     */
    public double theta() {
        return Math.atan2(y, x);
    }

    /**
     * Normalises a vector.
     */
    public static Vector2d normalise(Vector2d v) {
        double mag = v.mag();
        if(mag == 0)
        {
            return new Vector2d(0, 0);
        }else{
            return new Vector2d((int) (v.x / mag), (int) (v.y / mag));
        }
    }

    public Vector2d normalise() {
        return normalise(this);
    }

    /**
     * Calculates the dot product between this vector and the one passed by parameter.
     * @param v the other vector.
     * @return the dot product between these two vectors.
     */
    public int dot(Vector2d v) {
        return this.x * v.x + this.y * v.y;
    }

    public Vector2d unitVector()
    {
        double l = this.mag();
        if(l > 0)
        {
            return new Vector2d((int)(this.x/l),(int)(this.y/l));
        }
        else return new Vector2d(1,0);
    }

    public boolean adjacentTo(Vector2d v) {
        return Math.abs(v.x-x) <= 1 && Math.abs(v.y-y) <= 1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    private record CacheKey(int x, int y, int radius, int min, int max) {}

    private static Map<CacheKey, List<Vector2d>> neighborhoodCache = new HashMap<>();

    public List<Vector2d> neighborhood(int radius, int min, int max) {
        CacheKey key = new CacheKey(x, y, radius, min, max);
        return neighborhoodCache.computeIfAbsent(key, k -> calculateNeighborhood(radius, min, max));
    }

    /**
     * Returns a list a neighbouring vectors from target for a given radius. This vector's x,y is
     * excluded from the neighbours.
     * @param radius the size of the neighborhood (radius = 1, gives a 3x3 neighborhood ).
     * @param min the minimum value to keep it in bounds (inclusive).
     * @param max the maximum value to keep it in bounds (exclusive).
     * @return A list of neighbors.
     */
    private List<Vector2d> calculateNeighborhood(int radius, int min, int max) {
        List<Vector2d> vectors = new ArrayList<>();

        for(int i = x - radius; i <= x + radius; i++) {
            for(int j = y - radius; j <= y + radius; j++) {

                //Not x,y and within established bounds
                if((i != x || j != y) && (i >= min && j >= min && i < max && j < max))
                {
                    vectors.add(new Vector2d(i, j));
                }
            }
        }
        return Collections.unmodifiableList(vectors);
    }

    public static double manhattanDistance(Vector2d p1, Vector2d p2)
    {
        return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
    }

    public static double chebychevDistance(Vector2d p1, Vector2d p2)
    {
        return Math.max(Math.abs(p1.x-p2.x), Math.abs(p1.y-p2.y));
    }

    public static double euclideanDistance(Vector2d p1, Vector2d p2)
    {
        return Math.sqrt(Math.pow(Math.abs(p1.x - p2.x), 2) + Math.pow(Math.abs(p1.y - p2.y), 2));
    }
}

