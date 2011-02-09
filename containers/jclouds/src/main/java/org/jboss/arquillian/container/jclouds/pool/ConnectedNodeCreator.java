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
package org.jboss.arquillian.container.jclouds.pool;

import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;

/**
 * A base creator that connects to the node using SSH on create and disconnects on destroy. <br/>
 * When the created {@link ConnectedNode} is available in the pool it contains a open ssh
 * connection.
 * 
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @author Adrian Cole
 */
public abstract class ConnectedNodeCreator implements Creator<ConnectedNode> {
   private ComputeServiceContext computeContext;

   public ConnectedNodeCreator(ComputeServiceContext computeContext) {
      this.computeContext = computeContext;
   }

   /**
    * @return the computeContext
    */
   public ComputeServiceContext getComputeContext() {
      return computeContext;
   }

   public final ConnectedNode create() {
      ConnectedNode node = new ConnectedNode(computeContext.utils().sshForNode(), createNode());
      node.connect();
      return node;
   }

   public final void destroy(ConnectedNode node) {
      node.disconnect();
      destroyNode(node.getNodeMetadata());
   }

   public abstract NodeMetadata createNode();

   public abstract void destroyNode(NodeMetadata nodeMetadata);
}