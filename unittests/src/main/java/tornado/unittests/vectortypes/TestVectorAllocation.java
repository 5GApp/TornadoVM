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
 */
package tornado.unittests.vectortypes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tornado.api.Parallel;
import tornado.collections.types.Float2;
import tornado.collections.types.Float3;
import tornado.collections.types.Float4;
import tornado.collections.types.VectorFloat3;
import tornado.collections.types.VectorFloat4;
import tornado.runtime.api.TaskSchedule;
import tornado.unittests.common.TornadoTestBase;

public class TestVectorAllocation extends TornadoTestBase {

    /**
     * Test to check the kernel can create a float2 type
     * 
     * @param a
     * @param result
     */
    private static void testVectorAlloc(float[] a, float[] result) {
        for (@Parallel int i = 0; i < a.length; i++) {
            Float2 x = new Float2(1, 10);
            result[i] = a[i] + (x.getX() * x.getY());
        }
    }

    @Test
    public void testAllocation1() {

        int size = 8;

        float[] a = new float[size];
        float[] output = new float[size];

        for (int i = 0; i < size; i++) {
            a[i] = (float) i;
        }

        //@formatter:off
        new TaskSchedule("s0")
            .task("t0", TestVectorAllocation::testVectorAlloc, a, output)
            .streamOut(output)
            .execute();
        //@formatter:on

        for (int i = 0; i < size; i++) {
            assertEquals(a[i] + (10), output[i], 0.001);
        }
    }

    /**
     * Test to check the kernel can create a float2 type
     * 
     * @param a
     * @param result
     */
    private static void testVectorAlloc2(float[] a, VectorFloat4 result) {
        for (@Parallel int i = 0; i < a.length; i++) {
            Float4 x = new Float4(a.length, 10, a[i], a[i] * 10);
            result.set(i, x);
        }
    }

    @Test
    public void testAllocation2() {

        int size = 8;

        float[] a = new float[size];
        VectorFloat4 output = new VectorFloat4(size);

        for (int i = 0; i < size; i++) {
            a[i] = (float) i;
        }

        //@formatter:off
        new TaskSchedule("s0")
                .task("t0", TestVectorAllocation::testVectorAlloc2, a, output)
                .streamOut(output)
                .execute();
        //@formatter:on

        for (int i = 0; i < size; i++) {
            Float4 sequential = new Float4(a.length, 10, a[i], a[i] * 10);
            assertEquals(sequential.getX(), output.get(i).getX(), 0.001);
            assertEquals(sequential.getY(), output.get(i).getY(), 0.001);
            assertEquals(sequential.getZ(), output.get(i).getZ(), 0.001);
            assertEquals(sequential.getW(), output.get(i).getW(), 0.001);
        }
    }

    /**
     * Test to check the kernel can create a float2 type
     * 
     * @param a
     * @param result
     */
    private static void testVectorAlloc3(float[] a, VectorFloat3 result) {
        for (@Parallel int i = 0; i < a.length; i++) {
            Float3 x = new Float3(a.length, 10, a[i]);
            result.set(i, x);
        }
    }

    @Test
    public void testAllocation3() {

        int size = 8;

        float[] a = new float[size];
        VectorFloat3 output = new VectorFloat3(size);

        for (int i = 0; i < size; i++) {
            a[i] = (float) i;
        }

        //@formatter:off
        new TaskSchedule("s0")
                .task("t0", TestVectorAllocation::testVectorAlloc3, a, output)
                .streamOut(output)
                .execute();
        //@formatter:on

        for (int i = 0; i < size; i++) {
            Float3 sequential = new Float3(a.length, 10, a[i]);
            assertEquals(sequential.getX(), output.get(i).getX(), 0.001);
            assertEquals(sequential.getY(), output.get(i).getY(), 0.001);
            assertEquals(sequential.getZ(), output.get(i).getZ(), 0.001);
        }
    }
}
