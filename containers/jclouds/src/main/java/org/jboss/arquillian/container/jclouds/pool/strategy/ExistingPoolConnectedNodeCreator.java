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


import java.util.Iterator;

import org.jboss.arquillian.container.jclouds.pool.ConnectedNodeCreator;
import org.jboss.arquillian.container.jclouds.pool.Creator;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.predicates.NodePredicates;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

/**
 * A {@link Creator} that can match up against
 * 
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 */
public class ExistingPoolConnectedNodeCreator extends ConnectedNodeCreator {
   private String group;

   private Iterator<? extends NodeMetadata> foundNodes;

   public ExistingPoolConnectedNodeCreator(ComputeServiceContext context, String group) {
      super(context);
      this.group = group;
   }

   @Override
   public NodeMetadata createNode() {
      synchronized (this) {
         if (foundNodes == null) {
            foundNodes = (Iterator<? extends NodeMetadata>)Iterables.filter(
                              getComputeContext().getComputeService().listNodesDetailsMatching(NodePredicates.all()),
                              Predicates.and(NodePredicates.inGroup(group), Predicates.not(NodePredicates.TERMINATED)));
         }
         if (foundNodes.hasNext()) {
            return (NodeMetadata) foundNodes.next();
         } else {
            throw new RuntimeException("Requested more nodes in pool then found in cloud");
         }
      }
   }

   @Override
   public void destroyNode(NodeMetadata nodeMetadata) {
   }
}
