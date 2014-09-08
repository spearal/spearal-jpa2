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
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.spearal.jpa2.EntityManagerFactoryWrapper;
import org.spearal.jpa2.EntityManagerWrapper;

/**
 * @author Franck WOLFF
 * @author William DRAI
 */
public class SpearalExtension implements Extension {
	
	public void wrapEntityManager(@Observes ProcessProducer<?, EntityManager> event) {
		event.setProducer(new EntityManagerProducerWrapper(event.getProducer()));
	}
	
	public void wrapEntityManagerFactory(@Observes ProcessProducer<?, EntityManagerFactory> event) {
		event.setProducer(new EntityManagerFactoryProducerWrapper(event.getProducer()));
	}
	
	
	public void prepareSetup(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {		
		AnnotatedType<SpearalSetup> setupType = beanManager.createAnnotatedType(SpearalSetup.class);		
		event.addAnnotatedType(setupType, SpearalSetup.class.getName());
	}
	
	public void setupEntityManagerFactories(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
		Set<Bean<?>> beans = beanManager.getBeans(SpearalSetup.class);
		for (Bean<?> bean : beans)	// Force creation of setup class at application statup
			beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)).toString();
    }
	
	private static final class EntityManagerProducerWrapper implements Producer<EntityManager> {
		
		private final Producer<EntityManager> wrappedProducer;
		
		public EntityManagerProducerWrapper(Producer<EntityManager> wrappedProducer) {
			this.wrappedProducer = wrappedProducer;
		}
		
		@Override
		public EntityManager produce(CreationalContext<EntityManager> ctx) {
			EntityManager entityManager = wrappedProducer.produce(ctx);
			if (entityManager instanceof EntityManagerWrapper)
				return entityManager;
			
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
		
		public EntityManagerFactoryProducerWrapper(Producer<EntityManagerFactory> wrappedProducer) {
			this.wrappedProducer = wrappedProducer;
		}
		
		@Override
		public EntityManagerFactory produce(CreationalContext<EntityManagerFactory> ctx) {
			EntityManagerFactory entityManagerFactory = wrappedProducer.produce(ctx);
			if (entityManagerFactory instanceof EntityManagerFactoryWrapper)
				return entityManagerFactory;
			
			return new EntityManagerFactoryWrapper(entityManagerFactory);
		}

		@Override
		public void dispose(EntityManagerFactory entityManagerFactory) {
			if (entityManagerFactory instanceof EntityManagerFactoryWrapper)
				entityManagerFactory = ((EntityManagerFactoryWrapper)entityManagerFactory).getWrappedEntityManagerFactory();
			
			wrappedProducer.dispose(entityManagerFactory);
		}
		
		@Override
		public Set<InjectionPoint> getInjectionPoints() {
			return wrappedProducer.getInjectionPoints();
		}
		
	}
	
}
