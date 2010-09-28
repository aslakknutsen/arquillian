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

import java.util.UUID;

import org.jboss.arquillian.container.jclouds.pool.ObjectPool.UsedObjectStrategy;
import org.jboss.arquillian.spi.ContainerConfiguration;
import org.jboss.arquillian.spi.ContainerProfile;

/**
 * JCloudsConfiguration
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class JCloudsConfiguration implements ContainerConfiguration
{
   
   /**
    * The JClouds Provider name to use
    */
   private String provider;
   
   /**
    * The Account name to use for this provider. 
    */
   private String identity;
   
   /**
    * The Accounts credential.
    */
   private String credential;

   /**
    * Path to certificate used for SSH.
    */
   private String certificate;
   
   /**
    * The name of the nodes to start
    */
   private String tag = UUID.randomUUID().toString().replaceAll("-", "");
   
   /**
    * The number of nodes to start
    */
   private Integer nodeCount = 1;

   /**
    * 
    */
   private UsedObjectStrategy usedObjectStrategy = UsedObjectStrategy.REUSE;

   /**
    * The ID of the image to start. 
    */
   private String imageId;
   
   /**
    * Run against a specific node. 
    */
   private String nodeId = null;
   
   /**
    * Port opened up to the server and used by the Servlet Protocol.
    */
   private int remoteServerHttpPort = 8080;

   /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.ContainerConfiguration#getContainerProfile()
    */
   public ContainerProfile getContainerProfile()
   {
      return ContainerProfile.CLIENT;
   }
   
   /**
    * @return the provider
    */
   public String getProvider()
   {
      return provider;
   }
   
   /**
    * @param provider the provider to set
    */
   public void setProvider(String provider)
   {
      this.provider = provider;
   }
   
   /**
    * @return the identity
    */
   public String getIdentity()
   {
      return identity;
   }
   
   /**
    * @param identity the identity to set
    */
   public void setIdentity(String identity)
   {
      this.identity = identity;
   }
   
   /**
    * @return the credential
    */
   public String getCredential()
   {
      return credential;
   }
   
   /**
    * @param credential the credential to set
    */
   public void setCredential(String credential)
   {
      this.credential = credential;
   }
   
   
   /**
    * @return the tag
    */
   public String getTag()
   {
      return tag;
   }
   
   /**
    * @param tag the tag to set
    */
   public void setTag(String tag)
   {
      this.tag = tag;
   }
   
   /**
    * @return the nodeCount
    */
   public Integer getNodeCount()
   {
      return nodeCount;
   }
   
   /**
    * @param nodeCount the nodeCount to set
    */
   public void setNodeCount(Integer nodeCount)
   {
      this.nodeCount = nodeCount;
   }
   
   /**
    * @return the remoteServerHttpPort
    */
   public int getRemoteServerHttpPort()
   {
      return remoteServerHttpPort;
   }
   
   /**
    * @param remoteServerHttpPort the remoteServerHttpPort to set
    */
   public void setRemoteServerHttpPort(int remoteServerHttpPort)
   {
      this.remoteServerHttpPort = remoteServerHttpPort;
   }
   
   /**
    * @return the certificate
    */
   public String getCertificate()
   {
      return certificate;
   }
   
   /**
    * @param certificate the certificate to set
    */
   public void setCertificate(String certificate)
   {
      this.certificate = certificate;
   }

   /**
    * @return the usedObjectStrategy
    */
   public UsedObjectStrategy getUsedObjectStrategy()
   {
      return usedObjectStrategy;
   }
   
   /**
    * @param usedObjectStrategy 
    */
   public void setUsedObjectStrategy(String usedObjectStrategy)
   {
      this.usedObjectStrategy = UsedObjectStrategy.valueOf(usedObjectStrategy);
   }

   /**
    * @return the imageId
    */
   public String getImageId()
   {
      return imageId;
   }
   
   /**
    * @param imageId the imageId to set
    */
   public void setImageId(String imageId)
   {
      this.imageId = imageId;
   }
   
   /**
    * @return the nodeId
    */
   public String getNodeId()
   {
      return nodeId;
   }
   
   /**
    * @param nodeId the nodeId to set
    */
   public void setNodeId(String nodeId)
   {
      this.nodeId = nodeId;
   }
   
   public boolean useRunningNode()
   {
      return nodeId != null;
   }
   
}
