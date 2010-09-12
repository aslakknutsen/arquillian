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

import org.jboss.arquillian.extension.jclouds.event.AfterStop;
import org.jboss.arquillian.extension.jclouds.event.BeforeStop;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.event.Event;
import org.jboss.arquillian.spi.event.suite.EventHandler;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.predicates.NodePredicates;

/**
 * CloudStopper
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class CloudStopper implements EventHandler<Event>
{

   /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.event.suite.EventHandler#callback(org.jboss.arquillian.spi.Context, java.lang.Object)
    */
   public void callback(Context context, Event event) throws Exception
   {
      // Currently unsupported for Extensions to have their own configuration based on arquillian.xml ARQ-215
      JCloudsConfiguration config = null; //context.get(Configuration.class).getExtensionConfig(JCloudsConfiguraiton.class); 
      if(config == null)
      {
         config = context.get(JCloudsConfiguration.class);
      }

      ComputeServiceContext computeContext = context.get(ComputeServiceContext.class);
      if(computeContext == null)
      {
         throw new IllegalStateException(ComputeServiceContext.class.getName() + " could not be found in Context.");
      }
      ComputeService computeService = computeContext.getComputeService();
      
      try
      {
         context.fire(new BeforeStop());
         computeService.destroyNodesMatching(NodePredicates.withTag(config.getTag()));
         context.fire(new AfterStop());
      }
      finally
      {
         computeContext.close();
      }
   }
}
