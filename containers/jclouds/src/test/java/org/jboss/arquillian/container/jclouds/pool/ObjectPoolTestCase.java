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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.jboss.arquillian.container.jclouds.pool.ObjectPool.UsedObjectStrategy;
import org.junit.Test;


/**
 * ObjectPoolTestCase
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ObjectPoolTestCase
{
   @Test
   public void shouldCreateInitialSize() throws Exception 
   {
      final CountDownLatch objectCreation = new CountDownLatch(2);
      final CountDownLatch objectDesctruction = new CountDownLatch(2);
      
      Creator<HeavyObject> creator = new Creator<HeavyObject>()
      {
         public void destory(HeavyObject object)
         {
            objectDesctruction.countDown();
         }
         
         public HeavyObject create()
         {
            return new HeavyObject();
         }
      };
      ObjectPool<HeavyObject> pool = new ObjectPool<HeavyObject>(
            creator,
            2,
            UsedObjectStrategy.REUSE, 
            new ObjectPool.PoolListener<HeavyObject>()
            {
               public void added(HeavyObject object)
               {
                  objectCreation.countDown();
               }
            });
      
      try
      {
         if(!objectCreation.await(2, TimeUnit.SECONDS))
         {
            Assert.fail("ObjectCreation did not happen");
         }
         
         Assert.assertEquals(2, pool.currentSize());
         PooledObject<HeavyObject> object = pool.get();

         Assert.assertEquals(1, pool.currentSize());
   
         PooledObject<HeavyObject> object2 = pool.get();
         Assert.assertEquals(0, pool.currentSize());
         
         object.close();
         Assert.assertEquals(1, pool.currentSize());
         
         object2.close();
         Assert.assertEquals(2, pool.currentSize());
      }
      catch (Exception e) 
      {
         e.printStackTrace();
      }
      finally
      {
         pool.shutdown();
         Assert.assertEquals(0, pool.currentSize());
         
         if(!objectDesctruction.await(1, TimeUnit.SECONDS))
         {
            Assert.fail("ObjectDescruction did not happen");
         }
      }
   }
   
   public static class HeavyObject 
   {
      
   }
}
