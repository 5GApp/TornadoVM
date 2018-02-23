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
package tornado.unittests.tasks;

import static org.junit.Assert.assertEquals;

import java.util.stream.IntStream;

import org.junit.Test;

import tornado.api.Parallel;
import tornado.runtime.TornadoDriver;
import tornado.runtime.TornadoRuntime;
import tornado.runtime.api.TaskSchedule;

/**
 * Testing Tornado with one task in the same device. The {@link TaskSchedule}
 * contains a single task. This task is executed on either on the default device
 * of the one selected.
 *
 */
public class TestSingleTaskSingleDevice {

    public static void simpleTask(double[] a, double[] b, double[] c) {
        for (@Parallel int i = 0; i < c.length; i++) {
            c[i] = a[i] + b[i];
        }
    }

    @Test
    public void testSimpleTask() {
        final int numElements = 4096;
        double[] a = new double[numElements];
        double[] b = new double[numElements];
        double[] c = new double[numElements];

        IntStream.range(0, numElements).sequential().forEach(i -> {
            a[i] = (float) Math.random();
            b[i] = (float) Math.random();
        });

        //@formatter:off
        new TaskSchedule("s0")
            .streamIn(a, b)
            .task("t0", TestSingleTaskSingleDevice::simpleTask, a, b, c)
            .streamOut(c)
            .execute();
        //@formatter:on

        for (int i = 0; i < c.length; i++) {
            assertEquals(a[i] + b[i], c[i], 0.001);
        }
    }

    @Test
    public void testSimpleTaskOnDevice0() {
        final int numElements = 4096;
        double[] a = new double[numElements];
        double[] b = new double[numElements];
        double[] c = new double[numElements];

        IntStream.range(0, numElements).sequential().forEach(i -> {
            a[i] = (float) Math.random();
            b[i] = (float) Math.random();
        });

        TaskSchedule s0 = new TaskSchedule("s0");
        TornadoDriver driver = TornadoRuntime.getTornadoRuntime().getDriver(0);

        int deviceNumber = 0;
        s0.setDevice(driver.getDevice(deviceNumber));

        //@formatter:off
        s0.streamIn(a, b)
            .task("t0", TestSingleTaskSingleDevice::simpleTask, a, b, c)
            .streamOut(c)
            .execute();
        //@formatter:on

        for (int i = 0; i < c.length; i++) {
            assertEquals(a[i] + b[i], c[i], 0.001);
        }
    }

    @Test
    public void testSimpleTaskOnDevice1() {
        final int numElements = 4096;
        double[] a = new double[numElements];
        double[] b = new double[numElements];
        double[] c = new double[numElements];

        IntStream.range(0, numElements).sequential().forEach(i -> {
            a[i] = (float) Math.random();
            b[i] = (float) Math.random();
        });

        TaskSchedule s0 = new TaskSchedule("s0");
        TornadoDriver driver = TornadoRuntime.getTornadoRuntime().getDriver(0);

        // select device 1 it is available
        int deviceNumber = 0;
        if (driver.getDeviceCount() > 1) {
            deviceNumber = 1;
        }

        s0.setDevice(driver.getDevice(deviceNumber));

        //@formatter:off
		s0.streamIn(a, b)
		  .task("t0", TestSingleTaskSingleDevice::simpleTask, a, b, c)
		  .streamOut(c)
		  .execute();
	    //@formatter:on

        for (int i = 0; i < c.length; i++) {
            assertEquals(a[i] + b[i], c[i], 0.001);
        }
    }

}
