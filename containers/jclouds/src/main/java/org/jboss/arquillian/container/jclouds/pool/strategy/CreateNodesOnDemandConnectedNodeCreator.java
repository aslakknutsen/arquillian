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
package org.jboss.arquillian.container.jclouds.pool.strategy;

import org.jboss.arquillian.container.jclouds.pool.ConnectedNodeCreator;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;

/**
 * 
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 */
public class CreateNodesOnDemandConnectedNodeCreator extends ConnectedNodeCreator {
   private Template template;
   private String group;

   public CreateNodesOnDemandConnectedNodeCreator(ComputeServiceContext context, Template template, String group) {
      super(context);
      this.template = template;
      this.group = group;
   }

   @Override
   public NodeMetadata createNode() {
      try {
         return getComputeContext().getComputeService().createNodesInGroup(group, 1, template).iterator().next();
      } catch (RunNodesException e) {
         for (NodeMetadata node : e.getSuccessfulNodes()){
            getComputeContext().getComputeService().destroyNode(node.getId());
         }
         throw new RuntimeException(e);
      }
   }

   @Override
   public void destroyNode(NodeMetadata nodeMetadata) {
      getComputeContext().getComputeService().destroyNode(nodeMetadata.getId());
   }
}
