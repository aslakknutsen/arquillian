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

import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.net.IPSocket;
import org.jclouds.ssh.SshClient;
import org.jclouds.ssh.SshClient.Factory;

import com.google.common.io.Files;

/**
 * Wraps a  NodeMetadata giving it the exrat
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ConnectedNodeMetadata
{
   private SshClient sshClient;
   private NodeMetadata nodeMetadata;
   
   public ConnectedNodeMetadata(Factory sshFactory, NodeMetadata nodeMetadata, String nodeIdentity, String certificate)
   {
      this.nodeMetadata = nodeMetadata;
      
      try
      {
         createSSHClient(sshFactory, nodeIdentity, certificate);
      }
      catch (Exception e) 
      {
         throw new RuntimeException(e);
      }
   }

   private void createSSHClient(SshClient.Factory factory, String nodeIdentity, String certificate) throws Exception
   {
      String publicAddress = nodeMetadata.getPublicAddresses().iterator().next();
      IPSocket socket = new IPSocket(publicAddress, 22);
      sshClient = factory.create(
                  socket, 
                  nodeIdentity != null ? nodeIdentity:nodeMetadata.getCredentials().identity,
                  Files.toByteArray(new File(certificate)));
   }

   /**
    * @return the nodeMetadata
    */
   public NodeMetadata getNodeMetadata()
   {
      return nodeMetadata;
   }

   /**
    * @return the sshClient
    */
   public SshClient getSSHClient()
   {
      return sshClient;
   }
   
   protected void connect()
   {
      sshClient.connect();
   }
   
   protected void disconnect()
   {
      sshClient.disconnect();
   }
}
