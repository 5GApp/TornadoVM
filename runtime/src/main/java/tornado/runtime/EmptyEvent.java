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
package tornado.runtime;

import tornado.api.Event;
import tornado.api.enums.TornadoExecutionStatus;

public class EmptyEvent implements Event {

    private final String name;

    public EmptyEvent(String name) {
        this.name = name;
    }

    public EmptyEvent() {
        this("Empty Event");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getExecutionTime() {
        return 0;
    }

    @Override
    public double getQueuedTime() {
        return 0;
    }

    @Override
    public TornadoExecutionStatus getStatus() {
        return TornadoExecutionStatus.COMPLETE;
    }

    @Override
    public double getTotalTime() {
        return 0;
    }

    @Override
    public void retain() {

    }

    @Override
    public void waitOn() {

    }

    @Override
    public long getSubmitTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getStartTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getEndTime() {
        // TODO Auto-generated method stub
        return 0;
    }

}
