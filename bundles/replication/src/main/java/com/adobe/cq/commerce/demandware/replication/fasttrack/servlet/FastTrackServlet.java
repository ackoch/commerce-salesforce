/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package com.adobe.cq.commerce.demandware.replication.fasttrack.servlet;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// TODO instanceId should not be provided as a parameter, we should call all fast track agents
// /on/fasttrackreplicate?path=/content/us-sitegenesis/content/categories/womens&instanceId=us

@Component(label = "Demandware Fast Track Replication", immediate = true)
@SlingServlet(paths = {"/on/fasttrackreplicate"}, methods = "GET", generateComponent = false)
public class FastTrackServlet extends SlingSafeMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(FastTrackServlet.class);
    public static final String DEMANDWARE_FT_AGENT_ID = "demandware-ft-%s-sitegenesis";
    @Reference
    protected ResourceResolverFactory rrf;
    @Reference
    private Replicator replicator;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String path = request.getParameter("path");
        String instanceId = request.getParameter("instanceId");
        if (path == null || instanceId == null) {
            response.getWriter().write("Failed: no path/instanceId parameter");
            return;
        }
        replicate(path, instanceId);
        response.getWriter().write("Successfully replicated " + path);
    }

    private void replicate(final String path, final String instanceId) {
        try {
            ReplicationOptions options = new ReplicationOptions();
            options.setSuppressVersions(true);
            options.setSynchronous(true);
            options.setSuppressStatusUpdate(false);

            String agentId = String.format(DEMANDWARE_FT_AGENT_ID, instanceId);
            options.setFilter(agent -> agent.getId().equalsIgnoreCase(agentId));

            replicator.replicate(getSession(), ReplicationActionType.ACTIVATE, path, options);
        }
        catch(Exception e) {
            LOG.error("Failed to replicate", e);
        }
    }

    private Session getSession() throws LoginException {
        Map<String, Object> param = new HashMap<>();
        param.put(ResourceResolverFactory.SUBSERVICE, "replication");
        ResourceResolver resolver = rrf.getServiceResourceResolver(param);
        return resolver.adaptTo(Session.class);
    }
}
