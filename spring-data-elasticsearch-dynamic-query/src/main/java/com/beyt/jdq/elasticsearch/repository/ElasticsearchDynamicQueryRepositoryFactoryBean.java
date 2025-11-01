package com.beyt.jdq.elasticsearch.repository;

import com.beyt.jdq.core.deserializer.IDeserializer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.support.*;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.Serializable;

/**
 * Factory bean for creating Elasticsearch repositories with dynamic query support.
 * Similar to MongoDynamicQueryRepositoryFactoryBean but for Elasticsearch.
 */
public class ElasticsearchDynamicQueryRepositoryFactoryBean<R extends Repository<T, ID>, T, ID extends Serializable>
        extends ElasticsearchRepositoryFactoryBean<R, T, ID> {

    protected final IDeserializer deserializer;

    @Nullable
    private ElasticsearchOperations operations;

    public ElasticsearchDynamicQueryRepositoryFactoryBean(Class<? extends R> repositoryInterface, IDeserializer deserializer) {
        super(repositoryInterface);
        this.deserializer = deserializer;
    }

    public void setElasticsearchOperations(ElasticsearchOperations operations) {
        this.operations = operations;
        super.setElasticsearchOperations(operations);
    }

    protected RepositoryFactorySupport createRepositoryFactory() {
        return new ElasticsearchDynamicQueryRepositoryFactory(operations, deserializer);
    }

    /**
     * Custom factory that creates ElasticsearchDynamicQueryRepositoryImpl instances
     */
    private static class ElasticsearchDynamicQueryRepositoryFactory extends ElasticsearchRepositoryFactory {

        private final ElasticsearchOperations elasticsearchOperations;
        private final IDeserializer iDeserializer;

        public ElasticsearchDynamicQueryRepositoryFactory(
                ElasticsearchOperations elasticsearchOperations,
                IDeserializer iDeserializer) {
            super(elasticsearchOperations);
            this.elasticsearchOperations = elasticsearchOperations;
            this.iDeserializer = iDeserializer;
        }

        @Override
        protected Object getTargetRepository(RepositoryInformation information) {
            ElasticsearchEntityInformation<?, Serializable> entityInformation =
                    getEntityInformation(information.getDomainType());
            return getTargetRepositoryViaReflection(information, information.getDomainType(), entityInformation, elasticsearchOperations, iDeserializer);
        }

        @Override
        @NonNull
        protected Class<?> getRepositoryBaseClass(@NonNull RepositoryMetadata metadata) {
            return ElasticsearchDynamicQueryRepositoryImpl.class;
        }
    }
}

