/**
 * Copyright 2016 The Johns Hopkins University Applied Physics Laboratory LLC
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.jhuapl.dorset.routing;

import java.util.Arrays;

import edu.jhuapl.dorset.Request;
import edu.jhuapl.dorset.agent.Agent;
import edu.jhuapl.dorset.agent.AgentRegistry;

public class ChainRouter implements Router {
    private Router[] routers;

    public ChainRouter(Router... routers) {
        this.routers = Arrays.copyOf(routers, routers.length);
    }

    @Override
    public void initialize(AgentRegistry registry) {
        for (Router router : routers) {
            router.initialize(registry);
        }
    }

    @Override
    public Agent[] getAgents(Request request) {
        Agent[] agents = {};
        for (Router router : routers) {
            agents = router.getAgents(request);
            if (agents.length != 0) {
                break;
            }
        }
        return agents;
    }

}
