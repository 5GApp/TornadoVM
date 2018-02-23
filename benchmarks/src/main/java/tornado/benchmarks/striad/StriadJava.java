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
package tornado.benchmarks.striad;

import java.util.Arrays;
import tornado.benchmarks.BenchmarkDriver;

import static tornado.benchmarks.LinearAlgebraArrays.striad;

public class StriadJava extends BenchmarkDriver {

    private final int numElements;

    private float[] x, y, z;
    private final float alpha = 2f;

    public StriadJava(int iterations, int numElements) {
        super(iterations);
        this.numElements = numElements;
    }

    @Override
    public void setUp() {
        x = new float[numElements];
        y = new float[numElements];
        z = new float[numElements];

        Arrays.fill(x, 1);
        Arrays.fill(y, 2);

    }

    @Override
    public void tearDown() {
        x = null;
        y = null;
        z = null;
        super.tearDown();
    }

    @Override
    public void code() {
        striad(alpha, x, y, z);
    }

    @Override
    public void barrier() {

    }

    @Override
    public boolean validate() {

        return true;
    }

    public void printSummary() {
        System.out.printf("id=java-serial, elapsed=%f, per iteration=%f\n", getElapsed(), getElapsedPerIteration());
    }

}
