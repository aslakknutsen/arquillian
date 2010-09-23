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

import java.util.UUID;

/**
 * JCloudsConfiguration
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class JCloudsConfiguration // extends ExtensionConfiguration  ARQ-215
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
    * The name of the nodes to start
    */
   private String tag = UUID.randomUUID().toString().replaceAll("-", "");
   
   /**
    * The number of nodes to start
    */
   private Integer nodeCount = 1;
   
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
   public String getIdentity()
   {
      return identity;
   }
   
   /**
    * @param account the account to set
    */
   public JCloudsConfiguration setIdentity(String account)
   {
      this.identity = account;
      return this;
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
   public JCloudsConfiguration setCredential(String credential)
   {
      this.credential = credential;
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
}
