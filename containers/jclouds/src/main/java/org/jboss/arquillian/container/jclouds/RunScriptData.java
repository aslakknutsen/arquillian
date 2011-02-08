package org.jboss.arquillian.container.jclouds;

<<<<<<< HEAD
import static org.jclouds.compute.util.ComputeServiceUtils.extractTargzIntoDirectory;
=======
import static org.jclouds.compute.util.ComputeServiceUtils.extractZipIntoDirectory;
>>>>>>> updated to show an example of how to start jboss
import static org.jclouds.scriptbuilder.domain.Statements.exec;
import static org.jclouds.scriptbuilder.domain.Statements.interpret;

import java.net.URI;
import java.util.Map;

import org.jclouds.compute.domain.OperatingSystem;
import org.jclouds.compute.predicates.OperatingSystemPredicates;
import org.jclouds.scriptbuilder.InitBuilder;
import org.jclouds.scriptbuilder.domain.AuthorizeRSAPublicKey;
import org.jclouds.scriptbuilder.domain.Statement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * 
 * @author Adrian Cole
 */
public class RunScriptData {
   private static String jbossHome = "/usr/local/jboss";

   public static String createScriptInstallBase(OperatingSystem os) {
      if (os == null || OperatingSystemPredicates.supportsApt().apply(os))
         return APT_RUN_SCRIPT;
      else if (OperatingSystemPredicates.supportsYum().apply(os))
         return YUM_RUN_SCRIPT;
      else if (OperatingSystemPredicates.supportsZypper().apply(os))
         return ZYPPER_RUN_SCRIPT;
      else
         throw new IllegalArgumentException("don't know how to handle" + os.toString());
   }

   public static Statement createScriptInstallAndStartJBoss(String publicKey, OperatingSystem os) {
      Map<String, String> envVariables = ImmutableMap.of("jbossHome", jbossHome);
      Statement toReturn = new InitBuilder(
            "jboss",
            jbossHome,
            jbossHome,
            envVariables,
            ImmutableList.<Statement> of(
                  new AuthorizeRSAPublicKey(publicKey),
                  exec(createScriptInstallBase(os)),
                  exec("rm -rf /var/cache/apt /usr/lib/vmware-tools"),// jeos hasn't enough room!
                  extractTargzIntoDirectory(
                        URI.create("http://commondatastorage.googleapis.com/jclouds-repo/jboss-as-distribution-6.0.0.20100911-M5.tar.gz"),
                        "/usr/local"), exec("{md} " + jbossHome), exec("mv /usr/local/jboss-*/* " + jbossHome),
                  exec("chmod -R oug+r+w " + jbossHome)),
            ImmutableList
                  .<Statement> of(interpret("java -Xms128m -Xmx512m -XX:MaxPermSize=256m -Dorg.jboss.resolver.warning=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 -Djava.endorsed.dirs=lib/endorsed -classpath bin/run.jar org.jboss.Main -b 0.0.0.0")));
      return toReturn;
   }

   public static final String APT_RUN_SCRIPT = new StringBuilder()//
         .append("echo nameserver 208.67.222.222 >> /etc/resolv.conf\n")//
         .append("cp /etc/apt/sources.list /etc/apt/sources.list.old\n")//
         .append(
               "sed 's~us.archive.ubuntu.com~mirror.anl.gov/pub~g' /etc/apt/sources.list.old >/etc/apt/sources.list\n")//
         .append("which curl || apt-get update -y -qq && apt-get install -f -y -qq --force-yes curl\n")//
         .append(
               "(which java && java -fullversion 2>&1|egrep -q 1.6 ) || apt-get install -f -y -qq --force-yes openjdk-6-jre\n")//
         .append("rm -rf /var/cache/apt /usr/lib/vmware-tools\n")// jeos hasn't enough room!
         .toString();

   public static final String YUM_RUN_SCRIPT = new StringBuilder()
         .append("echo nameserver 208.67.222.222 >> /etc/resolv.conf\n")
         //
         .append("echo \"[jdkrepo]\" >> /etc/yum.repos.d/CentOS-Base.repo\n")
         //
         .append("echo \"name=jdkrepository\" >> /etc/yum.repos.d/CentOS-Base.repo\n")
         //
         .append(
               "echo \"baseurl=http://ec2-us-east-mirror.rightscale.com/epel/5/i386/\" >> /etc/yum.repos.d/CentOS-Base.repo\n")//
         .append("echo \"enabled=1\" >> /etc/yum.repos.d/CentOS-Base.repo\n")//
         .append("which curl ||yum --nogpgcheck -y install curl\n")//
         .append(
               "(which java && java -fullversion 2>&1|egrep -q 1.6 ) || yum --nogpgcheck -y install java-1.6.0-openjdk&&")//
         .append("echo \"export PATH=\\\"/usr/lib/jvm/jre-1.6.0-openjdk/bin/:\\$PATH\\\"\" >> /root/.bashrc\n")//
         .toString();

   public static final String ZYPPER_RUN_SCRIPT = new StringBuilder()//
         .append("echo nameserver 208.67.222.222 >> /etc/resolv.conf\n")//
         .append("which curl || zypper install curl\n")//
         .append("(which java && java -fullversion 2>&1|egrep -q 1.6 ) || zypper install java-1.6.0-openjdk\n")//
         .toString();
}
