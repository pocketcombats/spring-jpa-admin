package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.list.AdminEntityListEntry;
import com.pocketcombats.admin.data.list.AdminListColumn;
import com.pocketcombats.admin.data.list.AdminModelEntitiesList;
import com.pocketcombats.admin.data.list.ModelRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.ClassUtils;

import java.util.Collections;
import java.util.List;

public class AdminModelEntitiesListServiceImpl implements AdminModelEntitiesListService {

    private final AdminModelRegistry modelRegistry;
    private final EntityManager em;
    private final ConversionService conversionService;

    public AdminModelEntitiesListServiceImpl(
            AdminModelRegistry modelRegistry,
            EntityManager em,
            ConversionService conversionService
    ) {
        this.modelRegistry = modelRegistry;
        this.em = em;
        this.conversionService = conversionService;
    }

    @Override
    @Transactional
    public AdminModelEntitiesList listEntities(String modelName, ModelRequest query) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> paginationQuery = cb.createQuery(Long.class);
        Root<?> paginationRoot = paginationQuery.from(model.entityClass());
        paginationQuery.select(cb.count(paginationRoot));
        applyModelRequest(model, paginationQuery, paginationRoot, query);
        long totalCount = em.createQuery(paginationQuery).getSingleResult();

        int pageSize = model.pageSize();
        int pagesCount = (int) Math.ceil((totalCount / (double) pageSize));

        int page = query.getPage() == null ? 1 : query.getPage();
        if (query.getPage() != null) {
            page = Math.min(Math.max(query.getPage(), 1), pagesCount);
        }

        List<?> resultList;
        if (totalCount > 0) {
            CriteriaQuery<?> dataQuery = cb.createQuery(model.entityClass());
            Root<?> root = dataQuery.from(model.entityClass());
            applyModelRequest(model, dataQuery, root, query);
            if (query.getSort() != null) {
                // TODO: validate that this is existing field
                dataQuery.orderBy(cb.asc(root.get(query.getSort())));
            }
            resultList = em.createQuery(dataQuery)
                    .setFirstResult((page - 1) * pageSize)
                    .setMaxResults(pageSize)
                    .getResultList();
        } else {
            resultList = Collections.emptyList();
        }

        // To be part of admin model
        List<AdminListColumn> columns = model.listFields().stream()
                .map(listField -> new AdminListColumn(
                        listField.name(),
                        listField.label(),
                        listField.bool(),
                        listField.sortable()
                ))
                .toList();
        List<AdminEntityListEntry> entires = resultList.stream()
                .map(entity -> mapEntry(entity, model.listFields()))
                .toList();
        return new AdminModelEntitiesList(
                model.label(),
                model.modelName(),
                true,
                page,
                pagesCount,
                columns,
                entires
        );
    }

    private void applyModelRequest(AdminRegisteredModel model, CriteriaQuery<?> q, Root<?> root, ModelRequest query) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        if (!StringUtils.isEmpty(query.getSearch())) {
            q.where(
                    cb.and(
                            cb.or(model.searchPredicateFactory().build(cb, root, query.getSearch()).stream().toArray(Predicate[]::new))
                            // TODO: additional predicates from config
                    )
            );
        }
    }

    private String resolveId(Object entity) {
        Object identifier = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        return conversionService.convert(identifier, String.class);
    }

    private AdminEntityListEntry mapEntry(Object entity, List<AdminModelListField> fields) {
        String id = resolveId(entity);

        List<Object> attributes = fields.stream()
                .map(field -> {
                    Object value = field.valueAccessor().getValue(entity);
                    if (field.valueFormatter() == null) {
                        return value;
                    } else {
                        return field.valueFormatter().format(value);
                    }
                })
                .toList();

        return new AdminEntityListEntry(id, attributes);
    }
}
