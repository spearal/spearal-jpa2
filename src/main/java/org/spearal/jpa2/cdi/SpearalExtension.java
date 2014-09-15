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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.util.AnnotationLiteral;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

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
	
	private Map<String, AnnotatedMember<?>> injectedPersistenceContexts = new HashMap<String, AnnotatedMember<?>>();
	private Set<String> injectedPersistenceUnits = new HashSet<String>();
	
	public <X> void processInjectedPersistenceResources(@Observes ProcessAnnotatedType<X> event) {
		for (AnnotatedMethod<? super X> method : event.getAnnotatedType().getMethods())
			handleAnnotatedMember(method);
		
		for (AnnotatedField<? super X> field : event.getAnnotatedType().getFields())
			handleAnnotatedMember(field);
	}
	
	private void handleAnnotatedMember(AnnotatedMember<?> member) {
		if (member.isAnnotationPresent(PersistenceContext.class)) {
			PersistenceContext persistenceContext = member.getAnnotation(PersistenceContext.class);
			injectedPersistenceContexts.put(persistenceContext.unitName(), member);
		}
		if (member.isAnnotationPresent(PersistenceUnit.class)) {
			PersistenceUnit persistenceUnit = member.getAnnotation(PersistenceUnit.class);
			injectedPersistenceUnits.add(persistenceUnit.unitName());
		}
	}
	
	public void startup(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
		// Force startup of SpearalFactory and EntityManagerFactory
		for (Bean<?> bean : beanManager.getBeans(SpearalFactory.class))
			beanManager.getReference(bean, SpearalFactory.class, beanManager.createCreationalContext(bean)).toString();
		
		for (Bean<?> bean : beanManager.getBeans(EntityManagerFactory.class))
			beanManager.getReference(bean, EntityManagerFactory.class, beanManager.createCreationalContext(bean)).toString();
    }	
	
	public void produceMissingPersistenceUnits(@Observes AfterTypeDiscovery event, BeanManager beanManager) {
		for (String unitName : injectedPersistenceUnits)
			injectedPersistenceContexts.remove(unitName);
		
		for (AnnotatedMember<?> member : injectedPersistenceContexts.values()) {
			
			if (!member.isAnnotationPresent(SpearalEnabled.class))
				continue;
			
			final Set<Annotation> annotations = new HashSet<Annotation>(member.getAnnotations());
			Iterator<Annotation> ia = annotations.iterator();
			while (ia.hasNext()) {
				Annotation a = ia.next();
				if (a.annotationType().equals(PersistenceContext.class))
					ia.remove();
			}
			PersistenceContext persistenceContext = member.getAnnotation(PersistenceContext.class);
			PersistenceUnit persistenceUnit = new PersistenceUnitAnnotation(persistenceContext.name(), persistenceContext.unitName());
			
			annotations.add(persistenceUnit);
			
			try {
				final Field persistenceUnitField = PersistenceUnitProducer.class.getDeclaredField("persistenceUnit");
				final Constructor<PersistenceUnitProducer> persistenceUnitConstructor = PersistenceUnitProducer.class.getDeclaredConstructor();
				
				final Set<AnnotatedField<? super PersistenceUnitProducer>> annotatedFields = new HashSet<AnnotatedField<? super PersistenceUnitProducer>>();
				final Set<AnnotatedConstructor<PersistenceUnitProducer>> annotatedConstructors = new HashSet<AnnotatedConstructor<PersistenceUnitProducer>>();
				
				final AnnotatedType<PersistenceUnitProducer> annotatedPU = new AnnotatedType<SpearalExtension.PersistenceUnitProducer>() {
					@Override
					public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
						return null;
					}
	
					@Override
					public Set<Annotation> getAnnotations() {
						return Collections.emptySet();
					}
	
					@Override
					public Type getBaseType() {
						return PersistenceUnitProducer.class;
					}
	
					@Override
					public Set<Type> getTypeClosure() {
						return Collections.singleton((Type)PersistenceUnitProducer.class);
					}
					
					@Override
					public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
						return false;
					}
	
					@Override
					public Set<AnnotatedConstructor<PersistenceUnitProducer>> getConstructors() {
						return annotatedConstructors;
					}
	
					@Override
					public Set<AnnotatedField<? super PersistenceUnitProducer>> getFields() {
						return annotatedFields;
					}
	
					@Override
					public Class<PersistenceUnitProducer> getJavaClass() {
						return PersistenceUnitProducer.class;
					}
	
					@Override
					public Set<AnnotatedMethod<? super PersistenceUnitProducer>> getMethods() {
						return Collections.emptySet();
					}
				};
				
				AnnotatedConstructor<PersistenceUnitProducer> annotatedConstructor = new AnnotatedConstructor<SpearalExtension.PersistenceUnitProducer>() {

					@Override
					public List<AnnotatedParameter<PersistenceUnitProducer>> getParameters() {
						return Collections.emptyList();
					}

					@Override
					public AnnotatedType<PersistenceUnitProducer> getDeclaringType() {
						return annotatedPU;
					}

					@Override
					public boolean isStatic() {
						return false;
					}

					@Override
					public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
						return null;
					}

					@Override
					public Set<Annotation> getAnnotations() {
						return Collections.emptySet();
					}

					@Override
					public Type getBaseType() {
						return PersistenceUnitProducer.class;
					}

					@Override
					public Set<Type> getTypeClosure() {
						return Collections.singleton((Type)PersistenceUnitProducer.class);
					}

					@Override
					public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
						return false;
					}

					@Override
					public Constructor<PersistenceUnitProducer> getJavaMember() {
						return persistenceUnitConstructor;
					}
				};
				
				annotatedConstructors.add(annotatedConstructor);
				
				AnnotatedField<PersistenceUnitProducer> annotatedField = new AnnotatedField<SpearalExtension.PersistenceUnitProducer>() {
					@Override
					public AnnotatedType<PersistenceUnitProducer> getDeclaringType() {
						return annotatedPU;
					}
					
					@Override
					public boolean isStatic() {
						return false;
					}
					
					@SuppressWarnings("unchecked")
					@Override
					public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
						for (Annotation annotation : annotations) {
							if (annotation.annotationType().equals(annotationType))
								return (T)annotation;
						}
						return null;
					}
	
					@Override
					public Set<Annotation> getAnnotations() {
						return annotations;
					}
					
					@Override
					public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
						for (Annotation annotation : annotations) {
							if (annotation.annotationType().equals(annotationType))
								return true;
						}
						return false;
					}
					
					@Override
					public Field getJavaMember() {
						return persistenceUnitField;
					}
	
					@Override
					public Type getBaseType() {
						return EntityManagerFactory.class;
					}
					
					@Override
					public Set<Type> getTypeClosure() {
						return Collections.singleton((Type)EntityManagerFactory.class);
					}
				};
				
				annotatedFields.add(annotatedField);
				
				event.addAnnotatedType(annotatedPU, "javax.persistence.PersistenceUnit." + persistenceContext.unitName());
			}
			catch (Exception e) {
				
			}
		}
	}
	
	private static class PersistenceUnitProducer {
		
		@SuppressWarnings("unused")
		private EntityManagerFactory persistenceUnit;
	}
	
	@SuppressWarnings("all")
	private class PersistenceUnitAnnotation extends AnnotationLiteral<PersistenceUnit> implements PersistenceUnit {
		
		private static final long serialVersionUID = 1L;
		
		private String name;
		private String unitName;
		
		public PersistenceUnitAnnotation(String name, String unitName) {
			this.name = name;
			this.unitName = unitName;
		}

		public String name() {
			return name;
		}
		
		public String unitName() {
			return unitName;
		}		
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
