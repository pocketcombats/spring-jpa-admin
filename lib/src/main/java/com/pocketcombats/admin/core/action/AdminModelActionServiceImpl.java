package com.pocketcombats.admin.core.action;

import com.pocketcombats.admin.core.AdminModelListEntityMapper;
import com.pocketcombats.admin.core.AdminModelRegistry;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.RegisteredEntityDetails;
import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.data.action.ActionColumn;
import com.pocketcombats.admin.data.action.ActionPrompt;
import com.pocketcombats.admin.data.list.AdminEntityListEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class AdminModelActionServiceImpl implements AdminModelActionService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminModelActionServiceImpl.class);

    private final AdminModelRegistry modelRegistry;
    private final AdminModelListEntityMapper mapper;
    private final EntityManager em;
    private final ConversionService conversionService;

    public AdminModelActionServiceImpl(
            AdminModelRegistry modelRegistry,
            AdminModelListEntityMapper mapper,
            EntityManager em,
            ConversionService conversionService
    ) {
        this.modelRegistry = modelRegistry;
        this.mapper = mapper;
        this.em = em;
        this.conversionService = conversionService;
    }

    @Override
    @Transactional(readOnly = true)
    public ActionPrompt prompt(String modelName, String actionName, List<String> stringIds)
            throws UnknownModelException, UnknownActionException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);
        AdminModelAction action = getAction(model, actionName);

        List<ActionColumn> columns = collectColumns(model);
        List<?> entities = findEntities(model, stringIds);
        List<AdminEntityListEntry> entries = entities.stream()
                .map(entity -> mapper.mapEntry(entity, model.listFields()))
                .toList();
        return new ActionPrompt(
                action.getId(),
                action.getLabel(), action.getDescription(), action.isLocalized(),
                modelName, model.label(),
                columns, entries
        );
    }

    private AdminModelAction getAction(AdminRegisteredModel model, String actionName) throws UnknownActionException {
        AdminModelAction action = model.actions().get(actionName);
        if (action == null) {
            if (LOG.isInfoEnabled()) {
                LOG.info(
                        "Unknown action {} for model {}. Known actions are: {}",
                        actionName, model.modelName(), model.actions().keySet()
                );
            }
            throw new UnknownActionException(actionName);
        }
        return action;
    }

    private static List<ActionColumn> collectColumns(AdminRegisteredModel model) {
        return model.listFields().stream()
                .map(column -> new ActionColumn(column.label(), column.bool()))
                .toList();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<?> findEntities(
            AdminRegisteredModel model,
            List<String> stringIds
    ) {
        RegisteredEntityDetails entityDetails = model.entityDetails();
        List<?> ids = stringIds.stream()
                .map(stringId -> conversionService.convert(stringId, entityDetails.idAttribute().getJavaType()))
                .toList();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<?> query = cb.createQuery(entityDetails.entityClass());
        Root<?> root = query.from(entityDetails.entityClass());
        query.where(root.get((SingularAttribute) entityDetails.idAttribute()).in(ids));
        return em.createQuery(query).getResultList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void perform(String modelName, String actionName, List<String> stringIds)
            throws UnknownModelException, UnknownActionException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);
        AdminModelAction action = getAction(model, actionName);

        List<?> entities = findEntities(model, stringIds);
        action.run(em, model, entities);
    }
}
