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
package org.jboss.arquillian.container.jclouds;

import org.jboss.arquillian.container.jclouds.pool.Creator;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;

/**
 * A base creator that connects to the node using SSH on create and disconnects on destroy. <br/>
 * When the created {@link ConnectedNodeMetadata} is available in the pool it contains a open ssh connection. 
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public abstract class ConnectedNodeCreator implements Creator<ConnectedNodeMetadata>
{
   private ComputeServiceContext computeContext;
   
   private String nodeIdentity;
   private String certificate;
   
   public ConnectedNodeCreator(ComputeServiceContext computeContext)
   {
      this.computeContext = computeContext;
   }
   
   /**
    * @param certificate the certificate to set
    */
   public ConnectedNodeCreator setCertificate(String certificate)
   {
      this.certificate = certificate;
      return this;
   }
   
   /**
    * @param nodeIdentity the nodeIdentity to set
    */
   public ConnectedNodeCreator setNodeIdentity(String nodeIdentity)
   {
      this.nodeIdentity = nodeIdentity;
      return this;
   }
   
   /**
    * @return the computeContext
    */
   public ComputeServiceContext getComputeContext()
   {
      return computeContext;
   }
   
   /* (non-Javadoc)
    * @see org.jboss.arquillian.container.jclouds.pool.Creator#create()
    */
   public final ConnectedNodeMetadata create()
   {
      ConnectedNodeMetadata metadata = new ConnectedNodeMetadata(
            computeContext.utils().sshFactory(), 
            createNodeMetadata(), 
            nodeIdentity, 
            certificate);
      metadata.connect();
      return metadata;
   }

   /* (non-Javadoc)
    * @see org.jboss.arquillian.container.jclouds.pool.Destroyer#destory(java.lang.Object)
    */
   public final void destory(ConnectedNodeMetadata metadata)
   {
      metadata.disconnect();
      destroyNodeMetadata(metadata.getNodeMetadata());
   }

   public abstract NodeMetadata createNodeMetadata();
   
   public abstract void destroyNodeMetadata(NodeMetadata nodeMetadata);
}