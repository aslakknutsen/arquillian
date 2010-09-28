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
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;

/**
 * CloudNodeCreator
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class TemplateCloudNodeCreator implements Creator<NodeMetadata>
{
   private ComputeServiceContext context;
   private Template template;
   private String tag;
   
   public TemplateCloudNodeCreator(ComputeServiceContext context, Template template, String tag)
   {
      this.context = context;
      this.template = template;
      this.tag = tag;
   }
   
   /* (non-Javadoc)
    * @see org.jboss.arquillian.container.jclouds.pool.Creator#create()
    */
   public NodeMetadata create()
   {
      try
      {
         return context.getComputeService().runNodesWithTag(
               tag,
               1, 
               template).iterator().next();
      }
      catch (RunNodesException e)
      {
         throw new RuntimeException(e);
      }
   }
   
   /* (non-Javadoc)
    * @see org.jboss.arquillian.container.jclouds.pool.Destroyer#destory(java.lang.Object)
    */
   public void destory(NodeMetadata object)
   {
      context.getComputeService().destroyNode(object.getId());
   }
}
