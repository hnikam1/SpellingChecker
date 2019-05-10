package symspell;

import org.eclipse.jdt.annotation.NonNull;


 public class Match<T> implements Comparable<Match<T>> {
    private final @NonNull T value;
    private final int distance;

    public Match(T value, int distance) {
        this.value = value;
        this.distance = distance;
    }

    @Override
    public int compareTo(Match<T> o) {
        return Integer.compare(distance, o.distance);
    }

    @NonNull
    public T getValue() {
        return value;
    }

    public int getDistance() {
        return distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Match<?> match = (Match<?>) o;

        return distance == match.distance && value.equals(match.value);
    }

    @Override
    public int hashCode() {
        int result = value.hashCode();
        result = 31 * result + distance;
        return result;
    }

    @Override
    public String toString() {
        return "Match{" +
                "value=" + value +
                ", distance=" + distance +
                '}';
    }
}