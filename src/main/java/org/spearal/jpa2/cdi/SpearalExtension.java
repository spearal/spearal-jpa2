/**
 * == @Spearal ==>
 * 
 * Copyright (C) 2014 Franck WOLFF & William DRAI (http://www.spearal.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spearal.jpa2.cdi;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;
import javax.persistence.EntityManager;

import org.spearal.jpa2.EntityManagerWrapper;

/**
 * @author Franck WOLFF
 * @author William DRAI
 */
public class SpearalExtension implements Extension {
	
	public void wrapEntityManager(@Observes ProcessProducer<?, EntityManager> event) {
		event.setProducer(new EntityManagerProducerWrapper(event.getProducer()));
	}
	
	private static final class EntityManagerProducerWrapper implements Producer<EntityManager> {
		
		private final Producer<EntityManager> wrappedProducer;
		
		public EntityManagerProducerWrapper(Producer<EntityManager> wrappedProducer) {
			this.wrappedProducer = wrappedProducer;
		}
		
		@Override
		public EntityManager produce(CreationalContext<EntityManager> ctx) {
			EntityManager entityManager = wrappedProducer.produce(ctx);
			return new EntityManagerWrapper(entityManager);
		}

		@Override
		public void dispose(EntityManager entityManager) {
			if (entityManager instanceof EntityManagerWrapper)
				entityManager = ((EntityManagerWrapper)entityManager).getWrappedEntityManager();
			
			wrappedProducer.dispose(entityManager);
		}

		@Override
		public Set<InjectionPoint> getInjectionPoints() {
			return wrappedProducer.getInjectionPoints();
		}
		
	}
	
}