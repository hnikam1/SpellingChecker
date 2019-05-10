package symspell;


public interface Metric<T> {

    /**
     * Computes the distance between two objects.
     * Like any distance metric, this should be non-negative, symmetric, satisfy the triangle inequality
     * and satisfy distance(x,y) == 0  iff x == y
     */
    int distance(T x, T y);

}
