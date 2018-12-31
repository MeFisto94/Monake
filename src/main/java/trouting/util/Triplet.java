/*
 * Wobblytrout
 *
 * Copyright (c) 2018 Wobblytrout
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package trouting.util;

import com.google.common.base.Objects;

/**
 * Container to ease passing around a tuple of three objects. This object provides
 * a sensible implementation of equals(), returning true if equals() is true on
 * each of the contained objects.
 */
public final class Triplet<F, S, T> {

    public final F first;
    public final S second;
    public final T third;

    /**
     * Constructor for a Pair.
     *
     * @param first the first object in the triplet
     * @param second the second object in the triplet
     * @param third the third object in the triplet
     *
     */
    public Triplet(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    /**
     * Checks the three objects for equality by delegating to their respective
     * {@link Object#equals(Object)} methods.
     *
     * @param o the {@link Triplet} to which this one is to be checked for
     * equality
     * @return true if the underlying objects of the Triplet are both considered
     * equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Triplet)) {
            return false;
        }
        Triplet<?, ?, ?> p = (Triplet<?, ?, ?>) o;
        return Objects.equal(p.first, first) && Objects.equal(p.second, second) && Objects.equal(p.third, third);
    }

    /**
     * Compute a hash code using the hash codes of the underlying objects
     *
     * @return a hashcode of the Triplet
     */
    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode()) ^ (third == null ? 0 : third.hashCode());
    }

    /**
     * Convenience method for creating an appropriately typed triplet.
     *
     * @param a the first object in the triplet
     * @param b the second object in the triplet
     * @param c the third object in the triplet
     * @return a triplet that is templatized with the types of a, b and c
     */
    public static <A, B, C> Triplet<A, B, C> create(A a, B b, C c) {
        return new Triplet<A, B, C>(a, b, c);
    }

    @Override
    public String toString() {
        return "Triplet[" + first + "," + second + "," + third + "]";
    }
}
