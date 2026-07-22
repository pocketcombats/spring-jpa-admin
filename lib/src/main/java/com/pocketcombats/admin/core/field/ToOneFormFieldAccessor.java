package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.EntityOptionMapper;
import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import com.pocketcombats.admin.core.search.SearchPredicateFactory;
import com.pocketcombats.admin.data.form.AdminSelectOption;
import com.pocketcombats.admin.data.form.AdminSelectOptionsResponse;
import com.pocketcombats.admin.util.ConversionUtils;
import com.pocketcombats.admin.widget.Option;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ToOneFormFieldAccessor extends AbstractFormFieldValueAccessor
        implements AdminFormFieldSingularValueAccessor, AutocompleteOptionsAccessor {

    private static final Logger LOG = LoggerFactory.getLogger(ToOneFormFieldAccessor.class);

    private static final String ID_PREFIX = "id";

    // Widget template-data keys. Templates reference these by string literal, so keep them in sync.
    private static final String ATTR_OPTIONS = "_options";
    private static final String ATTR_AUTOCOMPLETE = "_autocomplete";
    private static final String ATTR_CURRENT_ID = "_currentId";
    private static final String ATTR_CURRENT_LABEL = "_currentLabel";
    private static final String ATTR_TRUNCATED = "_truncated";
    private static final String ATTR_SHOWN_COUNT = "_shownCount";
    private static final String ATTR_TOTAL_APPROX = "_totalApprox";

    private final EntityManager em;
    private final ConversionService conversionService;
    private final boolean optional;
    // Structural capability to render as an autocomplete: a searchable, single-id target not pinned to
    // a custom template. Orthogonal to maxPreloadedOptions, which decides *when* to switch: an
    // incapable field never autocompletes, but its preload is still capped at the threshold.
    private final boolean autocompleteCapable;
    // Preload cap N. Target rows > N switch away from a full select: to autocomplete when capable,
    // otherwise to a select capped at N (plus the current selection) with a "more exist" note.
    // Integer.MAX_VALUE (or negative) opt out entirely: an uncapped classic select, no autocomplete.
    private final int maxPreloadedOptions;
    private final int maxCountedOptions;
    private final EntityOptionMapper optionMapper;
    // Search factory of the target's primary searchable admin model; null when the target is not a
    // searchable admin model (then queries fall back to id-only matching and the field never renders
    // as an autocomplete).
    private final @Nullable SearchPredicateFactory search;

    // Set once the truncated-preload warning has been logged, so a misconfigured field nags on first
    // render rather than on every form load.
    private volatile boolean truncationWarned;

    private final SingularAttribute<?, ?> attribute;
    private final Class<?> attributeIdType;
    private final @Nullable String targetIdAttributeName;
    // Scalar id path to project when counting rows: the single id where there is one, otherwise any
    // one @IdClass component, so probeTargetCount never materializes whole entities just to count.
    private final @Nullable String probeAttributeName;

    public ToOneFormFieldAccessor(
            EntityManager em,
            ConversionService conversionService,
            SingularAttribute<?, ?> attribute,
            boolean optional,
            boolean autocompleteCapable,
            int maxPreloadedOptions,
            int maxCountedOptions,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer,
            EntityOptionMapper optionMapper,
            @Nullable SearchPredicateFactory search
    ) {
        super(attribute.getName(), reader, writer);

        this.em = em;
        this.conversionService = conversionService;
        this.attribute = attribute;
        this.optional = optional;
        this.maxPreloadedOptions = maxPreloadedOptions;
        this.maxCountedOptions = maxCountedOptions;
        this.optionMapper = optionMapper;
        this.search = search;

        IdentifiableType<?> targetType = (IdentifiableType<?>) this.attribute.getType();
        this.attributeIdType = targetType.getIdType().getJavaType();
        this.targetIdAttributeName = resolveIdAttributeName(targetType, attributeIdType);
        this.probeAttributeName = resolveProbeAttributeName(targetType, attributeIdType);
        // Composite/@IdClass targets can't be id-searched (idPredicate) or stably paginated
        // (no single order attribute), which would leave the autocomplete widget browse-only
        // with unreliable infinite scroll.
        this.autocompleteCapable = autocompleteCapable && targetIdAttributeName != null;
    }

    private static @Nullable String resolveIdAttributeName(IdentifiableType<?> targetType, Class<?> idType) {
        // Composite / @IdClass identifier: options simply won't be explicitly ordered.
        return targetType.hasSingleIdAttribute() ? targetType.getId(idType).getName() : null;
    }

    private static @Nullable String resolveProbeAttributeName(IdentifiableType<?> targetType, Class<?> idType) {
        if (targetType.hasSingleIdAttribute()) {
            return targetType.getId(idType).getName();
        }
        // @IdClass: any id component projects to a scalar for counting
        return targetType.getIdClassAttributes().stream().map(Attribute::getName).findFirst().orElse(null);
    }

    @Override
    public String getDefaultTemplate() {
        return "admin/widget/toone";
    }

    @Override
    public String readValue(Object instance) {
        Object currentValue = getReader().getValue(instance);
        return getEntityStringId(currentValue);
    }

    /**
     * Picks the widget mode from the preload cap and the target-table size: a full preloaded select
     * when everything fits, the autocomplete marker when it doesn't and the field is capable, or a
     * select capped at the threshold (plus the current selection, plus a "more exist" note) when it
     * doesn't and the field can't autocomplete.
     */
    @Override
    public Map<String, Object> getModelAttributes(Object instance) {
        if (autocompleteDisabled()) {
            return preloadedAttributes(queryTargets(0));
        }
        if (autocompleteCapable && maxPreloadedOptions == 0) {
            return autocompleteAttributes(instance);
        }

        int probeLimit = Math.max(maxPreloadedOptions + 1, maxCountedOptions);
        int count = probeTargetCount(probeLimit);
        if (count <= maxPreloadedOptions) {
            // The whole table fits under the cap: preload it as a plain select
            return preloadedAttributes(queryTargets(0));
        }
        if (autocompleteCapable) {
            return autocompleteAttributes(instance);
        }
        warnTruncation(count, count >= probeLimit);
        return truncatedPreloadAttributes(instance, count, count >= probeLimit);
    }

    private boolean autocompleteDisabled() {
        return maxPreloadedOptions < 0 || maxPreloadedOptions == Integer.MAX_VALUE;
    }

    private Map<String, Object> autocompleteAttributes(Object instance) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ATTR_AUTOCOMPLETE, Boolean.TRUE);
        Object currentValue = getReader().getValue(instance);
        // Submit-encoded id of the current selection, "" when there is none
        attributes.put(ATTR_CURRENT_ID, currentValue == null ? "" : getEntityStringId(currentValue));
        if (currentValue != null) {
            attributes.put(ATTR_CURRENT_LABEL, currentSelectionLabel(currentValue));
        }
        return attributes;
    }

    private void warnTruncation(int total, boolean totalCapped) {
        if (truncationWarned) {
            return;
        }
        truncationWarned = true;
        LOG.warn(
                "To-one field '{}' preloads a capped option list ({} shown of {}{}): its target '{}' can't render as "
                        + "a searchable autocomplete (no searchFields on a registered admin model, a composite id, or "
                        + "a custom template). Add searchFields to the target model, set rawId, or raise max-preloaded-options.",
                getName(), maxPreloadedOptions, total, totalCapped ? "+" : "", attribute.getJavaType().getSimpleName()
        );
    }

    /**
     * Number of target rows, capped at {@code limit}, used to pick the widget mode and size the truncation
     * note. Projects a single id path, or one {@link jakarta.persistence.IdClass @IdClass} component, so
     * it counts index rows rather than materializing entities.
     */
    protected int probeTargetCount(int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object> probe = cb.createQuery();
        Root<?> root = probe.from(attribute.getJavaType());
        probe.select(probeAttributeName != null ? root.get(probeAttributeName) : root);
        return em.createQuery(probe)
                .setMaxResults(limit)
                .getResultList()
                .size();
    }

    private Map<String, Object> preloadedAttributes(List<?> targets) {
        List<Option> valueOptions = targets.stream().map(this::toOption).toList();
        return Map.of(ATTR_OPTIONS, withEmptySentinel(valueOptions));
    }

    private Option toOption(Object entity) {
        return new Option(getEntityStringId(entity), optionMapper.label(entity));
    }

    /**
     * Preloads the first N options, always keeping the current selection selectable even when it
     * falls past the cap: otherwise editing an entity whose relation is beyond the first N would
     * silently drop it on save.
     * <p>
     * Adds the {@value #ATTR_TRUNCATED}/{@value #ATTR_SHOWN_COUNT}/{@value #ATTR_TOTAL_APPROX}
     * contract the shared select widget renders a note from.
     */
    private Map<String, Object> truncatedPreloadAttributes(Object instance, int total, boolean totalCapped) {
        List<?> targets = maxPreloadedOptions == 0 ? List.of() : queryTargets(maxPreloadedOptions);
        List<Option> valueOptions = new ArrayList<>(targets.size() + 1);
        Set<String> seen = new HashSet<>();
        for (Object target : targets) {
            Option option = toOption(target);
            valueOptions.add(option);
            seen.add(option.id());
        }
        Object currentValue = getReader().getValue(instance);
        if (currentValue != null) {
            String currentId = getEntityStringId(currentValue);
            if (seen.add(currentId)) {
                valueOptions.add(new Option(currentId, currentSelectionLabel(currentValue)));
            }
        }
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ATTR_OPTIONS, withEmptySentinel(valueOptions));
        attributes.put(ATTR_TRUNCATED, Boolean.TRUE);
        attributes.put(ATTR_SHOWN_COUNT, valueOptions.size());
        attributes.put(ATTR_TOTAL_APPROX, totalCapped ? total + "+" : Integer.toString(total));
        return attributes;
    }

    private List<Option> withEmptySentinel(List<Option> valueOptions) {
        if (!optional) {
            return valueOptions;
        }
        List<Option> options = new ArrayList<>(valueOptions.size() + 1);
        options.add(Option.EMPTY);
        options.addAll(valueOptions);
        return options;
    }

    /**
     * Label of the already-read relation value. Falls back to {@code #<id>} when the relation can't
     * (or shouldn't) be formatted, so the widget always shows that a relation is set — never a
     * blank field over a non-empty hidden id.
     */
    private String currentSelectionLabel(Object currentValue) {
        if (em.contains(currentValue)
                || em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(currentValue)) {
            try {
                return optionMapper.label(currentValue);
            } catch (EntityNotFoundException e) {
                // Dangling reference: the relation points at a row that no longer exists. The raw id
                // is the best label available, and the form must render so the admin can repair the
                // entity rather than face an error page.
            }
        }
        return "#" + optionMapper.stringId(currentValue);
    }

    /**
     * Selectable target entities, up to {@code limit} of them (0 for all), ordered by id, so a capped
     * "first N" is stable.
     * Overridable for subclasses.
     */
    protected List<?> queryTargets(int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Class<?> attributeJavaType = attribute.getJavaType();
        CriteriaQuery<?> query = cb.createQuery(attributeJavaType);
        Root<?> root = query.from(attributeJavaType);
        if (targetIdAttributeName != null) {
            query.orderBy(cb.asc(root.get(targetIdAttributeName)));
        }
        TypedQuery<?> typedQuery = em.createQuery(query);
        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        return typedQuery.getResultList();
    }

    @Override
    public boolean autocompleteSupported() {
        // Consistent with the rendered widget: the options endpoint serves a field iff it can
        // actually render as autocomplete. A disabled threshold (negative / MAX_VALUE) never does,
        // so its endpoint 404s rather than serving options the widget will never request.
        return autocompleteCapable && !autocompleteDisabled();
    }

    @Override
    public AdminSelectOptionsResponse collectOptions(
            @Nullable String query,
            int page,
            int pageSize
    ) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        long firstResult = (long) (safePage - 1) * safePageSize;
        if (firstResult > Integer.MAX_VALUE - safePageSize) {
            // Page number far beyond any real dataset: skip the query rather than overflow setFirstResult.
            return new AdminSelectOptionsResponse(List.of(), false);
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        Class<?> targetType = attribute.getJavaType();
        CriteriaQuery<?> criteria = cb.createQuery(targetType);
        Root<?> root = criteria.from(targetType);

        Predicate searchPredicate = null;
        if (query != null && !query.isBlank()) {
            if (search != null) {
                searchPredicate = search.build(cb, criteria, root, query).orElse(null);
            }
            if (searchPredicate == null) {
                searchPredicate = idPredicate(cb, root, query);
            }
            if (searchPredicate == null) {
                // A query was typed but nothing can match it (no search fields configured and the text
                // isn't a valid id): return no options rather than a misleading unfiltered page.
                return new AdminSelectOptionsResponse(List.of(), false);
            }
        }
        if (searchPredicate != null) {
            criteria.where(searchPredicate);
        }
        if (targetIdAttributeName != null) {
            criteria.orderBy(cb.asc(root.get(targetIdAttributeName)));
        }

        // One extra row detects a next page
        int fetchSize = safePageSize == Integer.MAX_VALUE ? Integer.MAX_VALUE : safePageSize + 1;
        List<?> resultList = em.createQuery(criteria)
                .setFirstResult((int) firstResult)
                .setMaxResults(fetchSize)
                .getResultList();
        boolean hasMore = resultList.size() > safePageSize;
        List<AdminSelectOption> options = resultList.stream()
                .limit(safePageSize)
                .map(this::toSelectOption)
                .toList();
        return new AdminSelectOptionsResponse(options, hasMore);
    }

    @Override
    public @Nullable AdminSelectOption resolveOption(String value) {
        Object referenceId = parseReferenceId(value);
        if (referenceId == null) {
            return null;
        }
        Object entity = em.find(attribute.getJavaType(), referenceId);
        return entity == null ? null : toSelectOption(entity);
    }

    // Strips the ID_PREFIX and converts the remainder to the target's id type; null when the value
    // isn't a well-formed submit-ready id.
    private @Nullable Object parseReferenceId(String value) {
        if (!value.startsWith(ID_PREFIX)) {
            return null;
        }
        return ConversionUtils.tryConvert(conversionService, value.substring(ID_PREFIX.length()), attributeIdType);
    }

    private AdminSelectOption toSelectOption(Object entity) {
        return new AdminSelectOption(getEntityStringId(entity), optionMapper.label(entity));
    }

    private String getEntityStringId(@Nullable Object entity) {
        if (entity == null) {
            return Option.EMPTY.id();
        } else {
            return ID_PREFIX + optionMapper.stringId(entity);
        }
    }

    /**
     * Fallback search matching the target's id against the raw query text, used when the target model
     * exposes no {@link SearchPredicateFactory}.
     * Returns {@code null} when the query isn't a valid id or the target uses a composite identifier,
     * letting the caller report "no matches" instead of guessing.
     */
    private @Nullable Predicate idPredicate(CriteriaBuilder cb, Root<?> root, String query) {
        if (targetIdAttributeName == null) {
            return null;
        }
        Object id = ConversionUtils.tryConvert(conversionService, query.trim(), attributeIdType);
        return id == null ? null : cb.equal(root.get(targetIdAttributeName), id);
    }

    @Override
    public void setValue(Object instance, @Nullable String value, BindingResult bindingResult) {
        if (value == null || value.isBlank() || Option.EMPTY.id().equals(value)) {
            if (optional) {
                getWriter().setValue(instance, null);
            } else {
                // The classic select never submits blank for a required relation; the autocomplete
                // widget can, so reject it instead of writing null into a non-optional attribute.
                bindingResult.rejectValue(getName(), "jakarta.validation.constraints.NotNull.message");
            }
            return;
        }
        Object referenceId = parseReferenceId(value);
        if (referenceId == null) {
            bindingResult.rejectValue(getName(), "spring-jpa-admin.validation.constraints.ValidId.message");
            return;
        }
        Object reference = em.find(attribute.getJavaType(), referenceId);
        if (reference == null) {
            bindingResult.rejectValue(getName(), "spring-jpa-admin.validation.constraints.ValidId.message");
            return;
        }
        getWriter().setValue(instance, reference);
    }
}
