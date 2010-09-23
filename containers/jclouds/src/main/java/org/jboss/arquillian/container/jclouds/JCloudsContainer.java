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

import static org.jclouds.compute.domain.OsFamily.UBUNTU;
import static org.jclouds.compute.options.TemplateOptions.Builder.inboundPorts;

import java.io.File;
import java.net.URL;
import java.util.Set;

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
import org.jclouds.compute.predicates.NodePredicates;
import org.jclouds.io.Payloads;
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
   /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.DeployableContainer#setup(org.jboss.arquillian.spi.Context, org.jboss.arquillian.spi.Configuration)
    */
   public void setup(Context context, Configuration configuration)
   {
      JCloudsConfiguration config = configuration.getContainerConfig(JCloudsConfiguration.class);

      ComputeServiceContext computeContext = new ComputeServiceContextFactory().createContext(
            config.getProvider(), 
            config.getIdentity(), 
            config.getCredential(),
            ImmutableSet.of(
                  new Log4JLoggingModule(), 
                  new JschSshClientModule()));

      // Bind the ComputeServiceContext to the Arquillian Context so other Handlers can interact with it
      context.add(ComputeServiceContext.class, computeContext);
      
      ComputeService computeService = computeContext.getComputeService();

      // TOOD: should be extracted out into some sort of configuration..
      Template template = computeService.templateBuilder()
                                 //.smallest()
                                 .osFamily(UBUNTU)
                                 //.minRam(1000)
                                 .options(
                                       inboundPorts(1099, config.getRemoteServerHttpPort())
                                       .authorizePublicKey(
                                             Payloads.newFilePayload(
                                                   new File(System.getProperty("user.home") + "/.ssh/id_rsa.pub")))
                                       .runScript(Payloads.newStringPayload(createScriptBoot())))
                                 .build();
      
      context.add(Template.class, template);
   }

   /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.DeployableContainer#start(org.jboss.arquillian.spi.Context)
    */
   public void start(Context context) throws LifecycleException
   {
      JCloudsConfiguration config = context.get(Configuration.class).getContainerConfig(JCloudsConfiguration.class);
      ComputeServiceContext computeContext = context.get(ComputeServiceContext.class);
      Template template = context.get(Template.class);
      
      try
      {
         // start the nodes
         Set<? extends NodeMetadata> startedNodes = computeContext.getComputeService().runNodesWithTag(
               config.getTag(), 
               config.getNodeCount(), 
               template);
         
         context.add(NodeOverview.class, new NodeOverview(startedNodes));
      }
      catch (Exception e) 
      {
         throw new LifecycleException("Could not start nodes", e);
      }
   }

   /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.DeployableContainer#deploy(org.jboss.arquillian.spi.Context, org.jboss.shrinkwrap.api.Archive)
    */
   public ContainerMethodExecutor deploy(Context context, Archive<?> archive) throws DeploymentException
   {
      JCloudsConfiguration config = context.get(Configuration.class).getContainerConfig(JCloudsConfiguration.class);
      NodeOverview nodeOverview = context.get(NodeOverview.class);
      String publicAddress = nodeOverview.getStartedNodes().iterator().next().getPublicAddresses().iterator().next();

      // TODO: push Archive to BlobStore 
      
      try 
      {
         return new ServletMethodExecutor(
               new URL(
                     "http",
                     publicAddress,
                     config.getRemoteServerHttpPort(),
                     "/")
               );
      } 
      catch (Exception e) 
      {
         throw new RuntimeException("Could not create ContianerMethodExecutor", e);
      }
   }
   
   /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.DeployableContainer#undeploy(org.jboss.arquillian.spi.Context, org.jboss.shrinkwrap.api.Archive)
    */
   public void undeploy(Context context, Archive<?> archive) throws DeploymentException
   {
      /*
      NodeOverview nodeOverview = context.get(NodeOverview.class);
      String publicAddress = nodeOverview.getStartedNodes().iterator().next().getPublicAddresses().iterator().next();
      */
      // TODO: remove Archive from BlobStore 
      
   }

   /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.DeployableContainer#stop(org.jboss.arquillian.spi.Context)
    */
   public void stop(Context context) throws LifecycleException
   {
      JCloudsConfiguration config = context.get(Configuration.class).getContainerConfig(JCloudsConfiguration.class);
      ComputeServiceContext computeContext = context.get(ComputeServiceContext.class);

      computeContext.getComputeService().destroyNodesMatching(
            NodePredicates.withTag(config.getTag()));

   }

   // TODO: extract out to a SPI for each container etc, on how to install it self
   protected String createScriptBoot()
   {
      return createScriptInstallJava() + "\n" + createScriptInstallAndStartJBoss();
   }
   
   protected String createScriptInstallJava()
   {
      return "";
   }

   protected String createScriptInstallAndStartJBoss()
   {
      return "";
   }
}