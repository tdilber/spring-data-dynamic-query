package com.beyt.jdq.repository;

import com.beyt.jdq.deserializer.IDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import javax.persistence.EntityManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;


public class JpaDynamicQueryRepositoryFactoryBean<R extends JpaRepository<T, ID>, T, ID extends Serializable>
        extends JpaRepositoryFactoryBean<R, T, ID> {

  protected final IDeserializer deserializer;

  // Optional default components
  private EntityPathResolver entityPathResolver;
  private EscapeCharacter escapeCharacter;
  private JpaQueryMethodFactory queryMethodFactory;

  public JpaDynamicQueryRepositoryFactoryBean(Class<? extends R> repositoryInterface, IDeserializer deserializer) {
    super(repositoryInterface);
    this.deserializer = deserializer;
    this.escapeCharacter = EscapeCharacter.DEFAULT;
  }

  protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
    JpaRepositoryFactory jpaRepositoryFactory = new JpaDynamicQueryRepositoryFactory<T, ID>(entityManager, deserializer);
    jpaRepositoryFactory.setEntityPathResolver(this.entityPathResolver);
    jpaRepositoryFactory.setEscapeCharacter(this.escapeCharacter);
    if (this.queryMethodFactory != null) {
      jpaRepositoryFactory.setQueryMethodFactory(this.queryMethodFactory);
    }

    return jpaRepositoryFactory;
  }

  // Setters for default optional components
  @Autowired
  public void setEntityPathResolver(ObjectProvider<EntityPathResolver> resolver) {
    this.entityPathResolver = (EntityPathResolver)resolver.getIfAvailable(() -> SimpleEntityPathResolver.INSTANCE);
  }

  public void setEscapeCharacter(char escapeCharacter) {
    this.escapeCharacter = EscapeCharacter.of(escapeCharacter);
  }


  @Autowired
  public void setQueryMethodFactory(@Nullable JpaQueryMethodFactory factory) {
    if (factory != null) {
      this.queryMethodFactory = factory;
    }
  }


  private static class JpaDynamicQueryRepositoryFactory<T, ID extends Serializable>
          extends JpaRepositoryFactory {

    protected final EntityManager entityManager;
    protected final IDeserializer deserializer;

    public JpaDynamicQueryRepositoryFactory(EntityManager entityManager, IDeserializer deserializer) {
      super(entityManager);
      this.entityManager = entityManager;
      this.deserializer = deserializer;
    }

    protected JpaRepositoryImplementation<?, ?> getTargetRepository(RepositoryInformation information, EntityManager entityManager) {
      JpaEntityInformation<?, Serializable> entityInformation = this.getEntityInformation(information.getDomainType());
      Object repository = this.getTargetRepositoryViaReflection(information, new Object[]{entityInformation, entityManager, deserializer});
      Assert.isInstanceOf(JpaRepositoryImplementation.class, repository);
      return (JpaRepositoryImplementation)repository;
    }
  }
}
