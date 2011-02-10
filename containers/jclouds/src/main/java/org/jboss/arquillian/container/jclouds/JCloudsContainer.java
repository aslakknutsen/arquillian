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

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.jboss.arquillian.container.jclouds.jboss.JBossASCloudDeployer;
import org.jboss.arquillian.container.jclouds.pool.ConnectedNode;
import org.jboss.arquillian.container.jclouds.pool.ConnectedNodePool;
import org.jboss.arquillian.container.jclouds.pool.Creator;
import org.jboss.arquillian.container.jclouds.pool.ObjectPool;
import org.jboss.arquillian.container.jclouds.pool.ObjectPool.UsedObjectStrategy;
import org.jboss.arquillian.container.jclouds.pool.PooledObject;
import org.jboss.arquillian.container.jclouds.pool.strategy.CreateNodesOnDemandConnectedNodeCreator;
import org.jboss.arquillian.container.jclouds.pool.strategy.ExistingPoolConnectedNodeCreator;
import org.jboss.arquillian.container.jclouds.pool.strategy.SingletonExistingNodeCreator;
import org.jboss.arquillian.container.jclouds.spi.CloudDeployer;
import org.jboss.arquillian.container.jclouds.spi.TemplateCreator;
import org.jboss.arquillian.protocol.servlet.ServletMethodExecutor;
import org.jboss.arquillian.spi.ServiceLoader;
import org.jboss.arquillian.spi.client.container.DeployableContainer;
import org.jboss.arquillian.spi.client.container.DeploymentException;
import org.jboss.arquillian.spi.client.container.LifecycleException;
import org.jboss.arquillian.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.spi.core.Instance;
import org.jboss.arquillian.spi.core.InstanceProducer;
import org.jboss.arquillian.spi.core.annotation.ContainerScoped;
import org.jboss.arquillian.spi.core.annotation.DeploymentScoped;
import org.jboss.arquillian.spi.core.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.domain.Credentials;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.ssh.jsch.config.JschSshClientModule;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

/**
 * JCloudsContainer
 * 
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class JCloudsContainer implements DeployableContainer<JCloudsConfiguration> 
{
   private JCloudsConfiguration configuration;
   
   private Random random = new Random();
   
   @Inject 
   private Instance<ServiceLoader> serviceLoader;

   @Inject @ContainerScoped
   private InstanceProducer<CloudDeployer> cloudDeployer;
   
   @Inject @ContainerScoped
   private InstanceProducer<ComputeServiceContext> computeContext;
   
   @Inject @ContainerScoped
   private InstanceProducer<Template> template;

   @Inject @ContainerScoped
   private InstanceProducer<ConnectedNodePool> connectedNodePool;

   @Inject @DeploymentScoped
   private InstanceProducer<PooledObject<ConnectedNode>> pooledObject;
   
   public Class<JCloudsConfiguration> getConfigurationClass()
   {
      return JCloudsConfiguration.class;
   }
   
   public ProtocolDescription getDefaultProtocol()
   {
      return new ProtocolDescription("Servlet 3.0");
   }
   
   public void setup(JCloudsConfiguration configuration) 
   {
      long start = System.currentTimeMillis();
      this.configuration = configuration;
      configuration.validate();

      cloudDeployer.set(serviceLoader.get().onlyOne(CloudDeployer.class, JBossASCloudDeployer.class));

      ComputeServiceContext computeContext = new ComputeServiceContextFactory().createContext(
            configuration.getProvider(),
            configuration.getIdentity(), 
            configuration.getCredential(),
            ImmutableSet.of(
                  new Log4JLoggingModule(), 
                  new JschSshClientModule()), 
            configuration.getOverrides());

      // Bind the ComputeServiceContext to the Arquillian Context
      this.computeContext.set(computeContext);

      // Don't create a template if we're in single running instance mode
      switch(configuration.getMode())
      {
         case BUILD_NODE:
         case CONFIGURED_IMAGE:
         {
            TemplateCreator templateCreator = serviceLoader.get().onlyOne(TemplateCreator.class,
                  DefaultTemplateCreator.class);
            ComputeService computeService = computeContext.getComputeService();
            Template template = templateCreator.createTemplate(configuration, computeService);
            this.template.set(template);
            break;
         }
      }
      System.out.println("setup: " + (System.currentTimeMillis() - start));
   }

   public void start() throws LifecycleException 
   {
      long start = System.currentTimeMillis();

      ComputeServiceContext computeContext = this.computeContext.get();

      try {
         Creator<ConnectedNode> creator;
         UsedObjectStrategy usageStrategy = configuration.getUsedObjectStrategy();
         int poolSize = configuration.getNodeCount();

         switch(configuration.getMode())
         {
            case ACTIVE_NODE:
            {
               creator = new SingletonExistingNodeCreator(computeContext, configuration.getNodeId())
                              .setLoginCredentials(credentialsFromConfig(configuration));
               usageStrategy = UsedObjectStrategy.REUSE; // force reuse, we should not destroy running nodes
               poolSize = 1; // force pool size of 1, we're using a specific node id
               break;
            }
            case ACTIVE_NODE_POOL:
            {
               creator = new ExistingPoolConnectedNodeCreator(computeContext, configuration.getGroup())
                              .setLoginCredentials(credentialsFromConfig(configuration));

               usageStrategy = UsedObjectStrategy.REUSE; // force reuse, we should not destroy running nodes
               break;
            }
            default:
            {
               creator = new CreateNodesOnDemandConnectedNodeCreator(
                     computeContext, 
                     template.get(),
                     configuration.getGroup());
            }
         }
         ObjectPool<ConnectedNode> pool = new ObjectPool<ConnectedNode>(creator, poolSize, usageStrategy);

         connectedNodePool.set(new ConnectedNodePool(pool));
      } 
      catch (Exception e) 
      {
         throw new LifecycleException("Could not start nodes", e);
      }
      System.out.println("start: " + (System.currentTimeMillis() - start));
   }

   protected Credentials credentialsFromConfig(JCloudsConfiguration config) throws IOException {
      return new Credentials(config.getNodeIdentity(),
            Files.toString(new File(config.getCertificate()), Charsets.UTF_8));
   }

   public void deploy(Descriptor descriptor) throws DeploymentException
   {
      throw new UnsupportedOperationException("Deploy of Descriptor not supported");
   }
   
   public void undeploy(Descriptor descriptor) throws DeploymentException
   {
      throw new UnsupportedOperationException("UnDeploy of Descriptor not supported");
   }
   
   public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException 
   {
      long start = System.currentTimeMillis();
      ConnectedNodePool nodeOverview = this.connectedNodePool.get();

      // grab a instance from the pool and add it to the Context so undeploy can get the same
      // instance.
      PooledObject<ConnectedNode> pooledMetadata = nodeOverview.getNode();
      this.pooledObject.set(pooledMetadata);

      ConnectedNode connectedNodeMetadata = pooledMetadata.get();
      NodeMetadata nodeMetadata = connectedNodeMetadata.getNodeMetadata();

      try 
      {
         Thread.currentThread().sleep(random.nextInt(1000));
         this.cloudDeployer.get().deploy(connectedNodeMetadata.getSshClient(), archive);
      } 
      catch (Exception e) 
      {
         throw new DeploymentException("Could not deploy to node", e);
      }

      System.out.println("deploy: " + (System.currentTimeMillis() - start) + " " + Thread.currentThread().getName());
      try 
      {
         String publicAddress = nodeMetadata.getPublicAddresses().iterator().next();

         // TODO: we can't hardcode this..
         return new ProtocolMetaData()
            .addContext(
                  new HTTPContext(publicAddress, configuration.getRemoteServerHttpPort())
                     .add(new Servlet(ServletMethodExecutor.ARQUILLIAN_SERVLET_NAME, "/test")));
      } 
      catch (Exception e) 
      {
         throw new RuntimeException("Could not create ContianerMethodExecutor", e);
      }
   }

   public void undeploy(final Archive<?> archive) throws DeploymentException 
   {
      long start = System.currentTimeMillis();
      PooledObject<ConnectedNode> pooledMetadata = this.pooledObject.get();
      ConnectedNode connectedNodeMetadata = pooledMetadata.get();
      try 
      {
         this.cloudDeployer.get().undeploy(connectedNodeMetadata.getSshClient(), archive);
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

   public void stop() throws LifecycleException 
   {
      long start = System.currentTimeMillis();
      ComputeServiceContext computeContext = this.computeContext.get();
      ConnectedNodePool nodeOverview = this.connectedNodePool.get();
      nodeOverview.shutdownAll();

      computeContext.close();
      System.out.println("stop: " + (System.currentTimeMillis() - start));
   }
}
