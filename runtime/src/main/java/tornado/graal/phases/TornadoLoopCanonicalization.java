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
package tornado.graal.phases;

import java.util.Collections;
import java.util.List;
import org.graalvm.compiler.loop.LoopEx;
import org.graalvm.compiler.loop.LoopsData;
import org.graalvm.compiler.nodes.LoopBeginNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.phases.Phase;

import static tornado.graal.loop.LoopCanonicalizer.canonicalizeLoop;

public class TornadoLoopCanonicalization extends Phase {

    @Override
    protected void run(StructuredGraph graph) {

        if (graph.hasLoops()) {
            final LoopsData data = new LoopsData(graph);

            final List<LoopEx> loops = data.outerFirst();
            Collections.reverse(loops);
            for (LoopEx loop : loops) {
                int numBackedges = loop.loopBegin().loopEnds().count();
                if (numBackedges > 1) {
                    final LoopBeginNode loopBegin = loop.loopBegin();
                    canonicalizeLoop(graph, loopBegin);
                }
            }

        }

    }

}
