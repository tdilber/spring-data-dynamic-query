package com.beyt.jdq.mongodb.repository;

import com.beyt.jdq.mongodb.core.MongoSearchQueryTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Factory bean for creating MongoDynamicQueryRepository instances.
 * Similar to MongoDynamicQueryRepositoryFactoryBean for MongoDB.
 * 
 * To enable this factory, use:
 * @EnableMongoRepositories(repositoryFactoryBeanClass = MongoDynamicQueryRepositoryFactoryBean.class)
 */
public class MongoDynamicQueryRepositoryFactoryBean<R extends Repository<T, ID>, T, ID extends Serializable>
        extends MongoRepositoryFactoryBean<R, T, ID> {

    @Autowired
    private MongoSearchQueryTemplate mongoSearchQueryTemplate;

    public MongoDynamicQueryRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    @NonNull
    protected RepositoryFactorySupport getFactoryInstance(@NonNull MongoOperations operations) {
        return new MongoDynamicQueryRepositoryFactory(operations, mongoSearchQueryTemplate);
    }

    /**
     * Custom factory that creates MongoDynamicQueryRepositoryImpl instances
     */
    private static class MongoDynamicQueryRepositoryFactory extends MongoRepositoryFactory {

        private final MongoOperations mongoOperations;
        private final MongoSearchQueryTemplate mongoSearchQueryTemplate;

        public MongoDynamicQueryRepositoryFactory(
                MongoOperations mongoOperations,
                MongoSearchQueryTemplate mongoSearchQueryTemplate) {
            super(mongoOperations);
            this.mongoOperations = mongoOperations;
            this.mongoSearchQueryTemplate = mongoSearchQueryTemplate;
        }

        @Override
        protected Object getTargetRepository(RepositoryInformation information) {
            MongoEntityInformation<?, Serializable> entityInformation =
                    getEntityInformation(information.getDomainType());
            return getTargetRepositoryViaReflection(information, entityInformation, mongoOperations, mongoSearchQueryTemplate);
        }

        @Override
        @NonNull
        protected Class<?> getRepositoryBaseClass(@NonNull RepositoryMetadata metadata) {
            return MongoDynamicQueryRepositoryImpl.class;
        }
    }
}

