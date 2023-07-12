package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.list.AdminEntityListEntry;
import com.pocketcombats.admin.data.list.AdminListColumn;
import com.pocketcombats.admin.data.list.AdminModelEntitiesList;
import com.pocketcombats.admin.data.list.ListAction;
import com.pocketcombats.admin.data.list.ListFilter;
import com.pocketcombats.admin.data.list.ListFilterOption;
import com.pocketcombats.admin.data.list.ModelRequest;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminModelEntitiesListServiceImpl implements AdminModelEntitiesListService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminModelEntitiesListServiceImpl.class);

    private final AdminModelRegistry modelRegistry;
    private final EntityManager em;
    private final AdminModelListEntityMapper mapper;

    public AdminModelEntitiesListServiceImpl(
            AdminModelRegistry modelRegistry,
            EntityManager em,
            AdminModelListEntityMapper mapper
    ) {
        this.modelRegistry = modelRegistry;
        this.em = em;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public AdminModelEntitiesList listEntities(
            String modelName,
            ModelRequest query,
            Map<String, String> filters
    ) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);
        Class<?> entityClass = model.entityDetails().entityClass();

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> paginationQuery = cb.createQuery(Long.class);
        Root<?> paginationRoot = paginationQuery.from(entityClass);
        paginationQuery.select(cb.count(paginationRoot));
        applyModelRequest(model, paginationQuery, paginationRoot, query);
        applyFilters(model, paginationQuery, paginationRoot, filters);
        long totalCount = em.createQuery(paginationQuery).getSingleResult();

        int pageSize = model.pageSize();
        int pagesCount = (int) Math.ceil((totalCount / (double) pageSize));

        int page = query.getPage() == null ? 1 : query.getPage();
        if (query.getPage() != null) {
            page = Math.min(Math.max(query.getPage(), 1), pagesCount);
        }

        List<?> resultList;
        if (totalCount > 0) {
            CriteriaQuery<?> dataQuery = cb.createQuery(entityClass);
            Root<?> root = dataQuery.from(entityClass);
            applyModelRequest(model, dataQuery, root, query);
            applyFilters(model, dataQuery, root, filters);
            applySorting(query.getSort(), model, dataQuery, root, cb);
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
                        listField.sortExpressionFactory() != null
                ))
                .toList();
        List<AdminEntityListEntry> entries = resultList.stream()
                .map(entity -> mapper.mapEntry(entity, model.listFields()))
                .toList();
        return new AdminModelEntitiesList(
                model.label(),
                model.modelName(),
                model.searchPredicateFactory() != null,
                page,
                pagesCount,
                collectListFilters(model),
                columns,
                collectActions(model),
                entries
        );
    }

    private void applyModelRequest(AdminRegisteredModel model, CriteriaQuery<?> q, Root<?> root, ModelRequest query) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        if (!StringUtils.isEmpty(query.getSearch()) && model.searchPredicateFactory() != null) {
            q.where(
                    cb.and(
                            cb.or(model.searchPredicateFactory().build(cb, root, query.getSearch()).stream().toArray(Predicate[]::new))
                            // TODO: additional predicates from config
                    )
            );
        }
    }

    private void applyFilters(
            AdminRegisteredModel model,
            CriteriaQuery<?> q,
            Root<?> root,
            Map<String, String> filters
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Predicate[] filterPredicates = model.filters().stream()
                .filter(modelFilter -> filters.containsKey(modelFilter.getName()))
                .map(modelFilter -> modelFilter.createPredicate(cb, root, filters.get(modelFilter.getName())))
                .toArray(Predicate[]::new);
        if (filterPredicates.length > 0) {
            LOG.debug("Applying {} filter(s)", filterPredicates.length);
            Predicate combinedFiltersPredicate = cb.and(filterPredicates);
            if (q.getRestriction() != null) {
                q.where(cb.and(q.getRestriction(), combinedFiltersPredicate));
            } else {
                q.where(combinedFiltersPredicate);
            }
        }
    }

    private void applySorting(
            @Nullable String sortString,
            AdminRegisteredModel model,
            CriteriaQuery<?> query,
            Root<?> root,
            CriteriaBuilder cb
    ) {
        if (!StringUtils.isEmpty(sortString)) {
            String sortFieldName;
            boolean asc;
            if (sortString.startsWith("-")) {
                sortFieldName = sortString.substring(1);
                asc = false;
            } else {
                sortFieldName = sortString;
                asc = true;
            }

            applySorting(model, query, root, cb, sortFieldName, asc);
        }
    }

    private void applySorting(
            AdminRegisteredModel model,
            CriteriaQuery<?> query,
            Root<?> root,
            CriteriaBuilder cb,
            String sortFieldName,
            boolean asc
    ) {
        model.listFields().stream()
                .filter(listField -> listField.name().equals(sortFieldName))
                .findAny()
                .flatMap(listField -> Optional.ofNullable(listField.sortExpressionFactory()))
                .map(sortExpressionFactory -> {
                    Expression<?> sortExpression = sortExpressionFactory.createExpression(root);
                    return asc ? cb.asc(sortExpression) : cb.desc(sortExpression);
                })
                .ifPresent(query::orderBy);
    }

    private static List<ListFilter> collectListFilters(AdminRegisteredModel model) {
        return model.filters().stream()
                .map(modelFilter -> new ListFilter(
                        modelFilter.getName(),
                        modelFilter.getLabel(),
                        modelFilter.collectOptions().stream()
                                .map(filterOption -> new ListFilterOption(
                                        filterOption.label(),
                                        filterOption.value(),
                                        filterOption.localize()
                                ))
                                .toList()
                ))
                // Remove meaningless filters
                .filter(filterOption -> filterOption.options().size() > 1)
                .toList();
    }

    private static List<ListAction> collectActions(AdminRegisteredModel model) {
        return model.actions().values().stream()
                .map(action -> new ListAction(action.getId(), action.getLabel(), action.isLocalized()))
                .toList();
    }
}
