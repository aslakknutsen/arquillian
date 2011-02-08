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
package org.jboss.arquillian.container.jclouds.pool;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.ssh.SshClient;
import org.jclouds.util.CredentialUtils;

import com.google.common.base.Function;

/**
 * Represents a node that has an active ssh connection.
 * 
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @author Adrian Cole
 */
public class ConnectedNode {
   private final SshClient sshClient;
   private final NodeMetadata nodeMetadata;

   public ConnectedNode(Function<NodeMetadata, SshClient> createSshClientOncePortIsListeningOnNode,
         NodeMetadata nodeMetadata, @Nullable Credentials overridingLoginCredentials) {
      this.nodeMetadata = NodeMetadataBuilder
            .fromNodeMetadata(checkNotNull(nodeMetadata, "nodeMetadata"))
            .credentials(CredentialUtils.overrideCredentialsIfSupplied(nodeMetadata.getCredentials(), overridingLoginCredentials))
            .build();
      this.sshClient = createSshClientOncePortIsListeningOnNode.apply(nodeMetadata);
   }

   /**
    * @return the nodeMetadata
    */
   public NodeMetadata getNodeMetadata() {
      return nodeMetadata;
   }

   /**
    * @return the sshClient
    */
   public SshClient getSshClient() {
      return sshClient;
   }

   protected void connect() {
      sshClient.connect();
   }

   protected void disconnect() {
      sshClient.disconnect();
   }
}
