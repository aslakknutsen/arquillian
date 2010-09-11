/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.extension.jclouds;

import static org.jclouds.compute.domain.OsFamily.RHEL;
import static org.jclouds.compute.options.TemplateOptions.Builder.inboundPorts;

import java.util.Set;

import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.event.Event;
import org.jboss.arquillian.spi.event.suite.EventHandler;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;

/**
 * CloudStarter
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class CloudStarter implements EventHandler<Event>
{

   /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.event.suite.EventHandler#callback(org.jboss.arquillian.spi.Context, java.lang.Object)
    */
   public void callback(Context context, Event event) throws Exception
   {
      // Currently unsupported for Extensions to have their own configuration based on arquillian.xml ARQ-215
      JCloudsConfiguration config = null; //context.get(Configuration.class).getExtensionConfig(JCloudsConfiguraiton.class); 
      
      ComputeServiceContext computeContext = new ComputeServiceContextFactory().createContext(
            config.getProvider(), 
            config.getAccount(), 
            config.getKey());

      // Bind the ComputeServiceContext to the Arquillian Context so other Handlers can interact with it
      context.add(ComputeServiceContext.class, computeContext);
      
      ComputeService computeService = computeContext.getComputeService();

      // Bind the ComputeService to the Arquillian Context so other Handlers can interact with it
      context.add(ComputeService.class, computeService);
      
      // TOOD: should be extracted out into some sort of configuration..
      Template template = computeService.templateBuilder()
                                 .smallest()
                                 .osFamily(RHEL)
                                 .minRam(1000)
                                 .options(inboundPorts(1099, 8080))
                                 .build();
      
      // start the nodes
      Set<? extends NodeMetadata> startedNodes = computeService.runNodesWithTag(
            config.getTag(), 
            config.getNodeCount(), 
            template);
      
      //ARQ-124, we need to manipulate the Container configuration with the started node data, set JNDI ips etc
      // startedNodes.iterator().next().getPublicAddresses();
   }
}
