package com.beyt.jdq.elasticsearch.repository;

import com.beyt.jdq.elasticsearch.core.ElasticsearchSearchQueryTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.support.*;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Factory bean for creating Elasticsearch repositories with dynamic query support.
 * Similar to MongoDynamicQueryRepositoryFactoryBean but for Elasticsearch.
 */
public class ElasticsearchDynamicQueryRepositoryFactoryBean<R extends Repository<T, ID>, T, ID extends Serializable>
        extends ElasticsearchRepositoryFactoryBean<R, T, ID> {

    protected final ElasticsearchSearchQueryTemplate elasticsearchSearchQueryTemplate;

    @Nullable
    private ElasticsearchOperations operations;

    public ElasticsearchDynamicQueryRepositoryFactoryBean(Class<? extends R> repositoryInterface, ElasticsearchSearchQueryTemplate elasticsearchSearchQueryTemplate) {
        super(repositoryInterface);
        this.elasticsearchSearchQueryTemplate = elasticsearchSearchQueryTemplate;
    }

    public void setElasticsearchOperations(ElasticsearchOperations operations) {
        this.operations = operations;
        super.setElasticsearchOperations(operations);
    }

    protected RepositoryFactorySupport createRepositoryFactory() {
        return new ElasticsearchDynamicQueryRepositoryFactory(operations, elasticsearchSearchQueryTemplate);
    }

    /**
     * Custom factory that creates ElasticsearchDynamicQueryRepositoryImpl instances
     */
    private static class ElasticsearchDynamicQueryRepositoryFactory extends ElasticsearchRepositoryFactory {

        private final ElasticsearchOperations elasticsearchOperations;
        private final ElasticsearchSearchQueryTemplate elasticsearchSearchQueryTemplate;

        public ElasticsearchDynamicQueryRepositoryFactory(
                ElasticsearchOperations elasticsearchOperations,
                ElasticsearchSearchQueryTemplate elasticsearchSearchQueryTemplate) {
            super(elasticsearchOperations);
            this.elasticsearchOperations = elasticsearchOperations;
            this.elasticsearchSearchQueryTemplate = elasticsearchSearchQueryTemplate;
        }

        @Override
        protected Object getTargetRepository(RepositoryInformation information) {
            ElasticsearchEntityInformation<?, Serializable> entityInformation =
                    getEntityInformation(information.getDomainType());
            return getTargetRepositoryViaReflection(information, information.getDomainType(), entityInformation, elasticsearchOperations, elasticsearchSearchQueryTemplate);
        }

        @Override
        @NonNull
        protected Class<?> getRepositoryBaseClass(@NonNull RepositoryMetadata metadata) {
            return ElasticsearchDynamicQueryRepositoryImpl.class;
        }
    }
}

