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

import java.net.URL;

import org.jboss.arquillian.container.jclouds.JCloudsConfiguration.Mode;
import org.jboss.arquillian.container.jclouds.jboss.JBossASCloudDeployer;
import org.jboss.arquillian.container.jclouds.pool.Creator;
import org.jboss.arquillian.container.jclouds.pool.ObjectPool;
import org.jboss.arquillian.container.jclouds.pool.ObjectPool.UsedObjectStrategy;
import org.jboss.arquillian.container.jclouds.pool.PooledObject;
import org.jboss.arquillian.container.jclouds.spi.CloudDeployer;
import org.jboss.arquillian.container.jclouds.spi.TemplateCreator;
import org.jboss.arquillian.protocol.servlet_3.ServletMethodExecutor;
import org.jboss.arquillian.spi.Configuration;
import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.DeployableContainer;
import org.jboss.arquillian.spi.DeploymentException;
import org.jboss.arquillian.spi.LifecycleException;
import org.jboss.shrinkwrap.api.Archive;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.ssh.jsch.config.JschSshClientModule;

import com.google.common.collect.ImmutableSet;

/**
 * JCloudsContainer
 * 
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class JCloudsContainer implements DeployableContainer
{
   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#setup(org.jboss.arquillian.spi.Context,
    * org.jboss.arquillian.spi.Configuration)
    */
   public void setup(Context context, Configuration configuration)
   {
      long start = System.currentTimeMillis(); 
      JCloudsConfiguration config = configuration.getContainerConfig(JCloudsConfiguration.class);
      config.validate();
      
      context.add(
            CloudDeployer.class, 
            context.getServiceLoader().onlyOne(
                  CloudDeployer.class, 
                  JBossASCloudDeployer.class));
      
      ComputeServiceContext computeContext = new ComputeServiceContextFactory().createContext(
            config.getProvider(),
            config.getIdentity(), 
            config.getCredential(),
            ImmutableSet.of(new Log4JLoggingModule(), new JschSshClientModule()),
            config.getOverrides());

      // Bind the ComputeServiceContext to the Arquillian Context
      context.add(ComputeServiceContext.class, computeContext);

      // Don't create a template if we're in single running instance mode
      if(Mode.BUILD_NODE == config.getMode() || Mode.CONFIGURED_IMAGE == config.getMode())
      {
         TemplateCreator templateCreator = context.getServiceLoader().onlyOne(TemplateCreator.class, DefaultTemplateCreator.class);
         ComputeService computeService = computeContext.getComputeService();
         Template template = templateCreator.createTemplate(config, computeService);
         context.add(Template.class, template);
      }
      
      System.out.println("setup: " + (System.currentTimeMillis() - start));
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#start(org.jboss.arquillian.spi.Context)
    */
   public void start(Context context) throws LifecycleException
   {
      long start = System.currentTimeMillis();
      
      JCloudsConfiguration config = context.get(Configuration.class).getContainerConfig(JCloudsConfiguration.class);
      ComputeServiceContext computeContext = context.get(ComputeServiceContext.class);

      try
      {
         Creator<ConnectedNodeMetadata> creator;
         UsedObjectStrategy usageStrategy = config.getUsedObjectStrategy();
         int poolSize = config.getNodeCount();
         
         if(Mode.ACTIVE_NODE == config.getMode())
         {
            creator = new RunningCloudNodeCreator(computeContext, config.getNodeId())
                           .setCertificate(config.getCertificate())
                           .setNodeIdentity(config.getNodeIdentity());

            usageStrategy = UsedObjectStrategy.REUSE; // force reuse, we should not destroy running nodes
            poolSize = 1; // force pool size of 1, we're using a specific node id
         }
         else if(Mode.ACTIVE_NODE_POOL == config.getMode())
         {
            creator = new RunningCloudNodePoolCreator(computeContext, config.getTag())
                           .setCertificate(config.getCertificate())
                           .setNodeIdentity(config.getNodeIdentity());

            usageStrategy = UsedObjectStrategy.REUSE; // force reuse, we should not destroy running nodes
         }
         else
         {
            creator  = new TemplateCloudNodeCreator(computeContext, context.get(Template.class), config.getTag())
                           .setCertificate(config.getCertificate())
                           .setNodeIdentity(config.getNodeIdentity());
         }
         ObjectPool<ConnectedNodeMetadata> pool = new ObjectPool<ConnectedNodeMetadata>(
               creator, 
               poolSize,
               usageStrategy);
         
         context.add(NodeOverview.class, new NodeOverview(pool));
      }
      catch (Exception e)
      {
         throw new LifecycleException("Could not start nodes", e);
      }
      System.out.println("start: " + (System.currentTimeMillis() - start));
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#deploy(org.jboss.arquillian.spi.Context,
    * org.jboss.shrinkwrap.api.Archive)
    */
   public ContainerMethodExecutor deploy(final Context context, final Archive<?> archive) throws DeploymentException
   {
      long start = System.currentTimeMillis();
      JCloudsConfiguration config = context.get(Configuration.class).getContainerConfig(JCloudsConfiguration.class);
      NodeOverview nodeOverview = context.get(NodeOverview.class);

      // grab a instance from the pool and add it to the Context so undeploy can get the same instance.
      PooledObject<ConnectedNodeMetadata> pooledMetadata = nodeOverview.getNode();
      context.add(PooledObject.class, pooledMetadata);
      
      ConnectedNodeMetadata connectedNodeMetadata = pooledMetadata.get();
      NodeMetadata nodeMetadata = connectedNodeMetadata.getNodeMetadata();
      
      try
      {
         context.get(CloudDeployer.class).deploy(
               connectedNodeMetadata.getSSHClient(), 
               archive);
      }
      catch (Exception e) 
      {
         throw new DeploymentException("Could not deploy to node", e);
      }

      System.out.println("deploy: " + (System.currentTimeMillis() - start) + " " + Thread.currentThread().getName());
      try
      {
         String publicAddress = nodeMetadata.getPublicAddresses().iterator().next();

         return new ServletMethodExecutor(new URL("http", publicAddress, config.getRemoteServerHttpPort(), "/"));
      }
      catch (Exception e)
      {
         throw new RuntimeException("Could not create ContianerMethodExecutor", e);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#undeploy(org.jboss.arquillian.spi.Context,
    * org.jboss.shrinkwrap.api.Archive)
    */
   public void undeploy(final Context context, final Archive<?> archive) throws DeploymentException
   {
      long start = System.currentTimeMillis();
      @SuppressWarnings("unchecked")
      PooledObject<ConnectedNodeMetadata> pooledMetadata = (PooledObject<ConnectedNodeMetadata>)context.get(PooledObject.class);
      ConnectedNodeMetadata connectedNodeMetadata = pooledMetadata.get();
      try
      {
         context.get(CloudDeployer.class).undeploy(
               connectedNodeMetadata.getSSHClient(), 
               archive);
      }
      catch (Exception e) 
      {
         throw new DeploymentException("Could not deploy to node", e);
      }
      finally
      {
         // return the node to the pool for reuse or destruction depending on the usage strategy
         pooledMetadata.close();
      }
      System.out.println("undeploy: " + (System.currentTimeMillis() - start) + " " + Thread.currentThread().getName());
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#stop(org.jboss.arquillian.spi.Context)
    */
   public void stop(Context context) throws LifecycleException
   {
      long start = System.currentTimeMillis();
      ComputeServiceContext computeContext = context.get(ComputeServiceContext.class);
      NodeOverview nodeOverview = context.get(NodeOverview.class);
      nodeOverview.shutdownAll();

      computeContext.close();
      System.out.println("stop: " + (System.currentTimeMillis() - start));
   }
}
