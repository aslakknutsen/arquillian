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

import org.jboss.arquillian.container.jclouds.pool.ObjectPool;
import org.jboss.arquillian.container.jclouds.pool.ObjectPool.UsedObjectStrategy;
import org.jboss.arquillian.container.jclouds.pool.PooledObject;
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
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.net.IPSocket;
import org.jclouds.scriptbuilder.domain.AuthorizeRSAPublicKey;
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

      // Bind the ComputeServiceContext to the Arquillian Context
      context.add(ComputeServiceContext.class, computeContext);

      // Don't create a template if we're in single running instance mode
      if(!config.useRunningNode())
      {
         ComputeService computeService = computeContext.getComputeService();
         Template template = createTemplate(config, computeService);
         context.add(Template.class, template);
      }
   }

   private Template createTemplate(JCloudsConfiguration config, ComputeService computeService)
   {
      TemplateBuilder templateBuilder = computeService.templateBuilder()
            .options(
                  blockOnComplete(false).blockOnPort(config.getRemoteServerHttpPort(), 300)
                  .inboundPorts(22, config.getRemoteServerHttpPort()));

      String authorizeKey = null;
      try
      {
         authorizeKey = Files.toString(new File(config.getCertificate() + ".pub"), Charsets.UTF_8);
      }
      catch (IOException e)
      {
         Throwables.propagate(e);
      }

      // use a user defined image if specified
      if(config.getImageId() != null)
      {
         templateBuilder.imageId(config.getImageId())
            .options(TemplateOptions.Builder.runScript(new AuthorizeRSAPublicKey(authorizeKey)));
         
      }
      Template template = templateBuilder.build();
      
      // if no image is defined, we run the install routine
      if(config.getImageId() == null)
      {
         // note this is a dependency on the template resolution
         template.getOptions().runScript(
               RunScriptData.createScriptInstallAndStartJBoss(
                     authorizeKey,
                     template.getImage().getOperatingSystem()));
   
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
         ObjectPool<NodeMetadata> pool;
         if(config.useRunningNode())
         {
            pool = new ObjectPool<NodeMetadata>(
                  new RunningCloudNodeCreator(computeContext, config.getNodeId()),
                  1,
                  UsedObjectStrategy.REUSE);
         }
         else
         {
            // start the nodes
            Template template = context.get(Template.class);
            pool = new ObjectPool<NodeMetadata>(
                  new TemplateCloudNodeCreator(
                        computeContext, 
                        template, 
                        config.getTag()),
                  config.getNodeCount(),
                  config.getUsedObjectStrategy());
         }
         context.add(NodeOverview.class, new NodeOverview(pool));
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
   public ContainerMethodExecutor deploy(final Context context, final Archive<?> archive) throws DeploymentException
   {
      JCloudsConfiguration config = context.get(Configuration.class).getContainerConfig(JCloudsConfiguration.class);
      NodeOverview nodeOverview = context.get(NodeOverview.class);

      // grab a instance from the pool and add it to the Context so undeploy can get the same instance.
      PooledObject<NodeMetadata> nodeMetadata = nodeOverview.getNode();
      context.add(PooledObject.class, nodeMetadata);
      
      String publicAddress = nodeMetadata.get().getPublicAddresses().iterator().next();
      try
      {
         executeCommands(
               nodeMetadata.get(), 
               context, 
               new CommandExecuter()
               {
                  public void execute(SshClient client)
                  {
                     Payload toSend = Payloads.newInputStreamPayload(archive.as(ZipExporter.class).exportZip());
                     client.put(archive.getName(), toSend);
                     client.exec("/usr/local/jboss/bin/twiddle.sh invoke 'jboss.system:service=MainDeployer' deploy file://`pwd`/" + archive.getName());
                     //client.exec("mv " + archive.getName() + " /usr/local/jboss/server/default/deploy/");
                  }
               });
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
   public void undeploy(final Context context, final Archive<?> archive) throws DeploymentException
   {
      @SuppressWarnings("unchecked")
      PooledObject<NodeMetadata> nodeMetadata = (PooledObject<NodeMetadata>)context.get(PooledObject.class);
      try
      {
         executeCommands(
               nodeMetadata.get(), 
               context, 
               new CommandExecuter()
               {
                  public void execute(SshClient client)
                  {
                     client.exec("/usr/local/jboss/bin/twiddle.sh invoke 'jboss.system:service=MainDeployer' undeploy file://`pwd`/" + archive.getName());
                     //client.exec("rm -rf /usr/local/jboss/server/default/deploy/" + archive.getName());
                  }
               });
      }
      catch (Exception e) 
      {
         throw new DeploymentException("Could not deploy to node", e);
      }
      finally
      {
         nodeMetadata.close();
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.jboss.arquillian.spi.DeployableContainer#stop(org.jboss.arquillian.spi.Context)
    */
   public void stop(Context context) throws LifecycleException
   {
      ComputeServiceContext computeContext = context.get(ComputeServiceContext.class);
      NodeOverview nodeOverview = context.get(NodeOverview.class);
      nodeOverview.shutdownAll();

      computeContext.close();
   }
 
   private void executeCommands(NodeMetadata metadata, Context context, CommandExecuter executer)
      throws Exception
   {
      JCloudsConfiguration config = context.get(Configuration.class).getContainerConfig(JCloudsConfiguration.class);
      ComputeServiceContext computeContext = context.get(ComputeServiceContext.class);
      
      String publicAddress = metadata.getPublicAddresses().iterator().next();
      IPSocket socket = new IPSocket(publicAddress, 22);
      SshClient ssh = computeContext.getUtils().sshFactory()
            .create(
                  socket, 
                  metadata.getCredentials().identity, 
                  Files.toByteArray(new File(config.getCertificate())));
 
      try 
      {
         ssh.connect();
         executer.execute(ssh);
      } 
      finally 
      {
         if (ssh != null)
         {
            ssh.disconnect();
         }
      }
   }
   
   private interface CommandExecuter 
   {
      void execute(SshClient client);
   }
}