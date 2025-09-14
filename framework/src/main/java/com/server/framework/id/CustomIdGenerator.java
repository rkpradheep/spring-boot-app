package com.server.framework.id;

import com.server.framework.entity.BatchTableEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;

import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class CustomIdGenerator implements IdentifierGenerator, ApplicationContextAware
{
	private static final Long BATCH_SIZE = 100L;
	private static final Long DEFAULT_START_ID = 1000000000000L;
	private Long currentId = null;
	private Long maxId = null;

	private static ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		context = applicationContext;
	}

	private EntityManagerFactory getEmf()
	{
		return context.getBean(EntityManagerFactory.class);
	}

	@Override
	synchronized public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException
	{
		if(Objects.nonNull(currentId) && currentId < maxId)
		{
			return currentId++;
		}
		EntityManagerFactory emf = getEmf();
		EntityManager em = emf.createEntityManager();
		try
		{
			em.getTransaction().begin();

			BatchTableEntity batchTableEntity = em.find(BatchTableEntity.class, 1L, LockModeType.PESSIMISTIC_WRITE);
			if(batchTableEntity == null)
			{
				batchTableEntity = new BatchTableEntity(1L, DEFAULT_START_ID);
				em.persist(batchTableEntity);
			}

			currentId = batchTableEntity.getBatchStart();
			maxId = currentId + BATCH_SIZE;
			batchTableEntity.setBatchStart(maxId);

			em.merge(batchTableEntity);
			em.getTransaction().commit();
		}
		catch(Exception e)
		{
			if(em.getTransaction().isActive())
			{
				em.getTransaction().rollback();
			}
			throw new HibernateException("Error generating ID", e);
		}
		finally
		{
			em.close();
		}
		return currentId++;
	}
}
