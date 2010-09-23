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
   private String account;
   
   /**
    * The Accounts key.
    */
   private String key;

   /**
    * The name of the nodes to start
    */
   private String tag = UUID.randomUUID().toString().replaceAll("-", "");
   
   /**
    * The number of nodes to start
    */
   private Integer nodeCount = 1;

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
   public JCloudsConfiguration setProvider(String provider)
   {
      this.provider = provider;
      return this;
   }
   
   /**
    * @return the account
    */
   public String getAccount()
   {
      return account;
   }
   
   /**
    * @param account the account to set
    */
   public JCloudsConfiguration setAccount(String account)
   {
      this.account = account;
      return this;
   }
   
   /**
    * @return the key
    */
   public String getKey()
   {
      return key;
   }
   
   /**
    * @param key the key to set
    */
   public JCloudsConfiguration setKey(String key)
   {
      this.key = key;
      return this;
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
   public JCloudsConfiguration setTag(String tag)
   {
      this.tag = tag;
      return this;
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
   public JCloudsConfiguration setNodeCount(Integer nodeCount)
   {
      this.nodeCount = nodeCount;
      return this;
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
   public JCloudsConfiguration setRemoteServerHttpPort(int remoteServerHttpPort)
   {
      this.remoteServerHttpPort = remoteServerHttpPort;
      return this;
   }
}
