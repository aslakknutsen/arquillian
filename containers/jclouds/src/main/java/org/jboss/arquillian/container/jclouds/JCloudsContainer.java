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

import static org.jclouds.compute.options.TemplateOptions.Builder.blockOnComplete;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.jboss.arquillian.protocol.servlet_3.ServletMethodExecutor;
import org.jboss.arquillian.spi.Configuration;
import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.DeployableContainer;
import org.jboss.arquillian.spi.DeploymentException;
import org.jboss.arquillian.spi.LifecycleException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.predicates.NodePredicates;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.net.IPSocket;
import org.jclouds.ssh.SshClient;
import org.jclouds.ssh.jsch.config.JschSshClientModule;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

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
      JCloudsConfiguration config = configuration.getContainerConfig(JCloudsConfiguration.class);

      ComputeServiceContext computeContext = new ComputeServiceContextFactory().createContext(
            config.getProvider(),
            config.getIdentity(), 
            config.getCredential(),
            ImmutableSet.of(new Log4JLoggingModule(), new JschSshClientModule()));

      // Bind the ComputeServiceContext to the Arquillian Context so other Handlers can interact
      // with it
      context.add(ComputeServiceContext.class, computeContext);

      if(!config.useRunningNode())
      {
         ComputeService computeService = computeContext.getComputeService();
         Template template = createTemplate(config, computeService);
         context.add(Template.class, template);
      }
   }

   private Template createTemplate(JCloudsConfiguration config, ComputeService computeService)
   {
      Template template = computeService.templateBuilder()
            .options(
                  blockOnComplete(false).blockOnPort(config.getRemoteServerHttpPort(), 300)
                  .inboundPorts(22, config.getRemoteServerHttpPort()))
                  .build();

      try
      {
         // note this is a dependency on the template resolution
         template.getOptions().runScript(
               RunScriptData.createScriptInstallAndStartJBoss(
                     Files.toString(new File(config.getCertificate() + ".pub"), Charsets.UTF_8),
                     template.getImage().getOperatingSystem()));

      }
      catch (IOException e)
      {
         Throwables.propagate(e);
      }
      return template;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#start(org.jboss.arquillian.spi.Context)
    */
   public void start(Context context) throws LifecycleException
   {
      JCloudsConfiguration config = context.get(Configuration.class).getContainerConfig(JCloudsConfiguration.class);
      ComputeServiceContext computeContext = context.get(ComputeServiceContext.class);

      try
      {
         Set<NodeMetadata> startedNodes = new HashSet<NodeMetadata>();
         if(config.useRunningNode())
         {
            startedNodes.add(
                  computeContext.getComputeService().getNodeMetadata(
                        config.getNodeId()));
         }
         else
         {
            // start the nodes
            Template template = context.get(Template.class);
            startedNodes.addAll(
                  computeContext.getComputeService().runNodesWithTag(
                        config.getTag(),
                        config.getNodeCount(), 
                        template));
               
         }
         context.add(NodeOverview.class, new NodeOverview(startedNodes));
      }
      catch (Exception e)
      {
         throw new LifecycleException("Could not start nodes", e);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#deploy(org.jboss.arquillian.spi.Context,
    * org.jboss.shrinkwrap.api.Archive)
    */
   public ContainerMethodExecutor deploy(Context context, Archive<?> archive) throws DeploymentException
   {
      JCloudsConfiguration config = context.get(Configuration.class).getContainerConfig(JCloudsConfiguration.class);
      ComputeServiceContext computeContext = context.get(ComputeServiceContext.class);
      NodeOverview nodeOverview = context.get(NodeOverview.class);
      NodeMetadata nodeMetadata = nodeOverview.getStartedNodes().iterator().next();
      String publicAddress = nodeMetadata.getPublicAddresses().iterator().next();

      try
      {
         IPSocket socket = new IPSocket(publicAddress, 22);
         SshClient ssh = computeContext.utils().sshFactory()
               .create(
                     socket, 
                     nodeMetadata.getCredentials().identity, 
                     Files.toByteArray(new File(config.getCertificate())));
         try 
         {
            ssh.connect();
            Payload toSend = Payloads.newInputStreamPayload(archive.as(ZipExporter.class).exportZip());
            ssh.put(archive.getName(), toSend);
            ssh.exec("mv " + archive.getName() + " /usr/local/jboss/server/default/deploy/");
            
         } 
         finally 
         {
            if (ssh != null)
            {
               ssh.disconnect();
            }
         }
      }
      catch (Exception e) 
      {
         throw new DeploymentException("Could not deploy to node", e);
      }

      try
      {
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
   public void undeploy(Context context, Archive<?> archive) throws DeploymentException
   {
      JCloudsConfiguration config = context.get(Configuration.class).getContainerConfig(JCloudsConfiguration.class);
      ComputeServiceContext computeContext = context.get(ComputeServiceContext.class);
      NodeOverview nodeOverview = context.get(NodeOverview.class);
      NodeMetadata nodeMetadata = nodeOverview.getStartedNodes().iterator().next();
      String publicAddress = nodeMetadata.getPublicAddresses().iterator().next();

      try
      {
         IPSocket socket = new IPSocket(publicAddress, 22);
         SshClient ssh = computeContext.utils().sshFactory()
               .create(
                     socket, 
                     nodeMetadata.getCredentials().identity, 
                     Files.toByteArray(new File(config.getCertificate())));
         try 
         {
            ssh.connect();
            ssh.exec("rm -rf /usr/local/jboss/server/default/deploy/" + archive.getName());
         } 
         finally 
         {
            if (ssh != null)
            {
               ssh.disconnect();
            }
         }
      }
      catch (Exception e) 
      {
         throw new DeploymentException("Could not deploy to node", e);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#stop(org.jboss.arquillian.spi.Context)
    */
   public void stop(Context context) throws LifecycleException
   {
      JCloudsConfiguration config = context.get(Configuration.class).getContainerConfig(JCloudsConfiguration.class);
      ComputeServiceContext computeContext = context.get(ComputeServiceContext.class);

      if(!config.useRunningNode())
      {
         computeContext.getComputeService().destroyNodesMatching(NodePredicates.withTag(config.getTag()));
      }
   }

}