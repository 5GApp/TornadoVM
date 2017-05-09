/*
 * Copyright 2012 James Clarkson.
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
 */
package tornado.benchmarks.convolvearray;

import tornado.benchmarks.BenchmarkDriver;
import tornado.benchmarks.GraphicsKernels;
import tornado.collections.types.FloatOps;
import tornado.runtime.api.TaskSchedule;

import static tornado.benchmarks.BenchmarkUtils.createFilter;
import static tornado.benchmarks.BenchmarkUtils.createImage;
import static tornado.common.Tornado.getProperty;
import static tornado.benchmarks.BenchmarkUtils.createFilter;
import static tornado.benchmarks.BenchmarkUtils.createImage;
import static tornado.common.Tornado.getProperty;

public class ConvolveImageArrayTornado extends BenchmarkDriver {

    private final int imageSizeX, imageSizeY, filterSize;

    private float[] input, output, filter;

    private TaskSchedule graph;

    public ConvolveImageArrayTornado(int iterations, int imageSizeX,
            int imageSizeY, int filterSize) {
        super(iterations);
        this.imageSizeX = imageSizeX;
        this.imageSizeY = imageSizeY;
        this.filterSize = filterSize;
    }

    @Override
    public void setUp() {
        input = new float[imageSizeX * imageSizeY];
        output = new float[imageSizeX * imageSizeY];
        filter = new float[filterSize * filterSize];

        createImage(input, imageSizeX, imageSizeY);
        createFilter(filter, filterSize, filterSize);

        graph = new TaskSchedule("benchmark")
                .task("convolveImageArray", GraphicsKernels::convolveImageArray,
                        input, filter, output, imageSizeX, imageSizeY, filterSize,
                        filterSize)
                .streamOut(output);

        graph.warmup();

    }

    @Override
    public void tearDown() {
        graph.dumpProfiles();

        input = null;
        output = null;
        filter = null;

        graph.getDevice().reset();
        super.tearDown();
    }

    @Override
    public void code() {
        graph.execute();
    }

    @Override
    public boolean validate() {

        final float[] result = new float[imageSizeX * imageSizeY];

        code();
        graph.clearProfiles();

        GraphicsKernels.convolveImageArray(input, filter, result, imageSizeX,
                imageSizeY, filterSize, filterSize);

        float maxULP = 0f;
        for (int i = 0; i < output.length; i++) {
            final float ulp = FloatOps.findMaxULP(result[i], output[i]);

            if (ulp > maxULP) {
                maxULP = ulp;
            }
        }
        return maxULP < MAX_ULP;
    }

    public void printSummary() {
        if (isValid()) {
            System.out.printf(
                    "id=%s, elapsed=%f, per iteration=%f\n",
                    getProperty("s0.device"), getElapsed(),
                    getElapsedPerIteration());
        } else {
            System.out.printf("id=%s produced invalid result\n",
                    getProperty("s0.device"));
        }
    }
}
