package com.midiwars.util;

/**
 * 'C++ Pair'
 *
 * @param <F> Type of first element.
 * @param <S> Type of second element.
 */
public class Pair<F, S> {

    /** First element. */
    public F first;

    /** Second element. */
    public S second;

    /**
     * Returns a new Pair object.
     *
     * @param first First element of the pair.
     * @param second Second element of the pair.
     */
    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }
}
