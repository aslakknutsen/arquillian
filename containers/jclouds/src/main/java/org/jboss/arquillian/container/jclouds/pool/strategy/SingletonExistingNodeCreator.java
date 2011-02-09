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

import static com.google.common.base.Preconditions.checkNotNull;

import org.jboss.arquillian.container.jclouds.pool.ConnectedNodeCreator;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.domain.Credentials;

/**
 * 
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 */
public class SingletonExistingNodeCreator extends ConnectedNodeCreator {
   private String nodeId;
   private Credentials credentials;

   public SingletonExistingNodeCreator(ComputeServiceContext context, String nodeId) {
      super(context);
      this.nodeId = nodeId;
   }

   /**
    * @param certificate
    *           the certificate to set
    */
   public ConnectedNodeCreator setLoginCredentials(Credentials credentials) {
      this.credentials = credentials;
      return this;
   }

   @Override
   public NodeMetadata createNode() {
      return NodeMetadataBuilder
      .fromNodeMetadata(checkNotNull(
               getComputeContext().getComputeService().getNodeMetadata(nodeId), "nodeMetadata for %s", nodeId))
      .credentials(credentials)
      .build();
   }

   @Override
   public void destroyNode(NodeMetadata nodeMetadata) {
      // no op, don't destroy something we did not create.
   }
}