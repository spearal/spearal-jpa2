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
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.spearal.SpearalFactory;
import org.spearal.jpa2.EntityManagerWrapper;
import org.spearal.jpa2.SpearalConfigurator;

/**
 * @author Franck WOLFF
 * @author William DRAI
 */
public class SpearalExtension implements Extension {
	
	public void wrapEntityManager(@Observes ProcessProducer<?, EntityManager> event) {
		if (event.getAnnotatedMember().isAnnotationPresent(SpearalEnabled.class))
			event.setProducer(new EntityManagerProducerWrapper(event.getProducer()));
	}
	
	public void wrapEntityManagerFactory(@Observes ProcessProducer<?, EntityManagerFactory> event, BeanManager beanManager) {
		if (event.getAnnotatedMember().isAnnotationPresent(SpearalEnabled.class))
			event.setProducer(new EntityManagerFactoryProducerWrapper(event.getProducer(), beanManager));
	}
	
	public void setupEntityManagerFactories(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
		for (Bean<?> bean : beanManager.getBeans(SpearalFactory.class))
			beanManager.getReference(bean, SpearalFactory.class, beanManager.createCreationalContext(bean)).toString();
		
		for (Bean<?> bean : beanManager.getBeans(EntityManagerFactory.class))
			beanManager.getReference(bean, EntityManagerFactory.class, beanManager.createCreationalContext(bean)).toString();
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
	
	private static final class EntityManagerFactoryProducerWrapper implements Producer<EntityManagerFactory> {
		
		private final Producer<EntityManagerFactory> wrappedProducer;
		private final BeanManager beanManager;
		
		public EntityManagerFactoryProducerWrapper(Producer<EntityManagerFactory> wrappedProducer, BeanManager beanManager) {
			this.wrappedProducer = wrappedProducer;
			this.beanManager = beanManager;
		}
		
		@Override
		public EntityManagerFactory produce(CreationalContext<EntityManagerFactory> ctx) {
			EntityManagerFactory entityManagerFactory = wrappedProducer.produce(ctx);
			
			Set<Bean<?>> beans = beanManager.getBeans(SpearalFactory.class);
			for (Bean<?> bean : beans) {
				SpearalFactory spearalFactory = (SpearalFactory)beanManager.getReference(bean, SpearalFactory.class, beanManager.createCreationalContext(bean));
				SpearalConfigurator.init(spearalFactory, entityManagerFactory);
			}
			
			return entityManagerFactory;
		}

		@Override
		public void dispose(EntityManagerFactory entityManagerFactory) {
			wrappedProducer.dispose(entityManagerFactory);
		}

		@Override
		public Set<InjectionPoint> getInjectionPoints() {
			return wrappedProducer.getInjectionPoints();
		}
	}
}
