/*
 * This file is part of Tornado: A heterogeneous programming framework: 
 * https://github.com/beehive-lab/tornado
 *
 * Copyright (c) 2013-2018 APT Group, School of Computer Science, 
 * The University of Manchester
 *
 * This work is partially supported by EPSRC grants:
 * Anyscale EP/L000725/1 and PAMELA EP/K008730/1.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors: James Clarkson
 *
 */
package tornado.collections.types;

import java.nio.DoubleBuffer;
import tornado.api.Payload;
import tornado.api.Vector;
import tornado.collections.math.TornadoMath;

import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.MIN_VALUE;
import static java.lang.String.format;
import static java.nio.DoubleBuffer.wrap;
import static tornado.collections.types.DoubleOps.fmt8;

/**
 * Class that represents a vector of 3x doubles e.g. <double,double,double>
 *
 * @author jamesclarkson
 *
 */
@Vector
public final class Double8 implements PrimitiveStorage<DoubleBuffer> {

    public static final Class<Double8> TYPE = Double8.class;

    /**
     * backing array
     */
    @Payload
    final protected double[] storage;

    /**
     * number of elements in the storage
     */
    final private static int numElements = 8;

    protected Double8(double[] storage) {
        this.storage = storage;
    }

    public Double8() {
        this(new double[numElements]);
    }

    public Double8(double s0, double s1, double s2, double s3, double s4, double s5, double s6, double s7) {
        this();
        setS0(s0);
        setS1(s1);
        setS2(s2);
        setS3(s3);

        setS4(s4);
        setS5(s5);
        setS6(s6);
        setS7(s7);
    }

    public double get(int index) {
        return storage[index];
    }

    public void set(int index, double value) {
        storage[index] = value;
    }

    public void set(Double8 value) {
        for (int i = 0; i < 8; i++) {
            set(i, value.get(i));
        }
    }

    public double getS0() {
        return get(0);
    }

    public double getS1() {
        return get(1);
    }

    public double getS2() {
        return get(2);
    }

    public double getS3() {
        return get(3);
    }

    public double getS4() {
        return get(4);
    }

    public double getS5() {
        return get(5);
    }

    public double getS6() {
        return get(6);
    }

    public double getS7() {
        return get(7);
    }

    public void setS0(double value) {
        set(0, value);
    }

    public void setS1(double value) {
        set(1, value);
    }

    public void setS2(double value) {
        set(2, value);
    }

    public void setS3(double value) {
        set(3, value);
    }

    public void setS4(double value) {
        set(4, value);
    }

    public void setS5(double value) {
        set(5, value);
    }

    public void setS6(double value) {
        set(6, value);
    }

    public void setS7(double value) {
        set(7, value);
    }

    public Double4 getHi() {
        return new Double4(getS4(), getS5(), getS6(), getS7());
    }

    public Double4 getLo() {
        return new Double4(getS0(), getS1(), getS2(), getS3());
    }

    /**
     * Duplicates this vector
     *
     * @return
     */
    public Double8 duplicate() {
        Double8 vector = new Double8();
        vector.set(this);
        return vector;
    }

    public String toString(String fmt) {
        return format(fmt, getS0(), getS1(), getS2(), getS3(), getS4(), getS5(), getS6(), getS7());
    }

    @Override
    public String toString() {
        return toString(fmt8);
    }

    protected static final Double8 loadFromArray(final double[] array, int index) {
        final Double8 result = new Double8();
        for (int i = 0; i < numElements; i++) {
            result.set(i, array[index + i]);
        }
        return result;
    }

    protected final void storeToArray(final double[] array, int index) {
        for (int i = 0; i < numElements; i++) {
            array[index + i] = get(i);
        }
    }

    @Override
    public void loadFromBuffer(DoubleBuffer buffer) {
        asBuffer().put(buffer);
    }

    @Override
    public DoubleBuffer asBuffer() {
        return wrap(storage);
    }

    @Override
    public int size() {
        return numElements;
    }

    /**
     * *
     * Operations on Double8 vectors
     */
    public static Double8 add(Double8 a, Double8 b) {
        final Double8 result = new Double8();
        for (int i = 0; i < numElements; i++) {
            result.set(i, a.get(i) + b.get(i));
        }
        return result;
    }

    public static Double8 add(Double8 a, double b) {
        final Double8 result = new Double8();
        for (int i = 0; i < numElements; i++) {
            result.set(i, a.get(i) + b);
        }
        return result;
    }

    public static Double8 sub(Double8 a, Double8 b) {
        final Double8 result = new Double8();
        for (int i = 0; i < numElements; i++) {
            result.set(i, a.get(i) - b.get(i));
        }
        return result;
    }

    public static Double8 sub(Double8 a, double b) {
        final Double8 result = new Double8();
        for (int i = 0; i < numElements; i++) {
            result.set(i, a.get(i) - b);
        }
        return result;
    }

    public static Double8 div(Double8 a, Double8 b) {
        final Double8 result = new Double8();
        for (int i = 0; i < numElements; i++) {
            result.set(i, a.get(i) / b.get(i));
        }
        return result;
    }

    public static Double8 div(Double8 a, double value) {
        final Double8 result = new Double8();
        for (int i = 0; i < numElements; i++) {
            result.set(i, a.get(i) / value);
        }
        return result;
    }

    public static Double8 mult(Double8 a, Double8 b) {
        final Double8 result = new Double8();
        for (int i = 0; i < numElements; i++) {
            result.set(i, a.get(i) * b.get(i));
        }
        return result;
    }

    public static Double8 mult(Double8 a, double value) {
        final Double8 result = new Double8();
        for (int i = 0; i < numElements; i++) {
            result.set(i, a.get(i) * value);
        }
        return result;
    }

    public static Double8 min(Double8 a, Double8 b) {
        final Double8 result = new Double8();
        for (int i = 0; i < numElements; i++) {
            result.set(i, Math.min(a.get(i), b.get(i)));
        }
        return result;
    }

    public static double min(Double8 value) {
        double result = MAX_VALUE;
        for (int i = 0; i < numElements; i++) {
            result = Math.min(result, value.get(i));
        }
        return result;
    }

    public static Double8 max(Double8 a, Double8 b) {
        final Double8 result = new Double8();
        for (int i = 0; i < numElements; i++) {
            result.set(i, Math.max(a.get(i), b.get(i)));
        }
        return result;
    }

    public static double max(Double8 value) {
        double result = MIN_VALUE;
        for (int i = 0; i < numElements; i++) {
            result = Math.max(result, value.get(i));
        }
        return result;
    }

    public static Double8 sqrt(Double8 a) {
        final Double8 result = new Double8();
        for (int i = 0; i < numElements; i++) {
            a.set(i, TornadoMath.sqrt(a.get(i)));
        }
        return result;
    }

    public static boolean isEqual(Double8 a, Double8 b) {
        return TornadoMath.isEqual(a.asBuffer().array(), b.asBuffer().array());
    }

    public static double findULPDistance(Double8 value, Double8 expected) {
        return TornadoMath.findULPDistance(value.asBuffer().array(), expected.asBuffer().array());
    }

}
