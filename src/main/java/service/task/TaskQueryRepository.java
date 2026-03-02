package service.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import model.task.TaskEntity;

@ApplicationScoped
public class TaskQueryRepository {

    @Inject
    EntityManager entityManager;

    public TaskQueryResult search(TaskQuery rawQuery) {
        TaskQuery query = rawQuery.normalized();

        StringBuilder fromClause = new StringBuilder(" FROM TaskEntity t");
        List<String> predicates = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();

        if (!query.tags().isEmpty()) {
            predicates.add("EXISTS (SELECT 1 FROM t.tags tag WHERE LOWER(tag.name) IN :tags)");
            parameters.put("tags", query.tags());
        }

        if (query.deleted()) {
            predicates.add("t.deletedAt IS NOT NULL");
        } else {
            predicates.add("t.deletedAt IS NULL");
        }

        if (query.status() != null) {
            predicates.add("t.status = :status");
            parameters.put("status", query.status());
        }

        if (query.keyword() != null && !query.keyword().isBlank()) {
            predicates.add("(LOWER(t.title) LIKE :keyword OR LOWER(COALESCE(t.description, '')) LIKE :keyword)");
            parameters.put("keyword", '%' + query.keyword().toLowerCase() + '%');
        }

        String whereClause = predicates.isEmpty() ? "" : " WHERE " + String.join(" AND ", predicates);
        String orderByClause = " ORDER BY " + sortExpression(query) + " " + query.order().name() + ", t.id ASC";

        TypedQuery<TaskEntity> selectQuery = entityManager.createQuery(
                "SELECT t" + fromClause + whereClause + orderByClause,
                TaskEntity.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(
                "SELECT COUNT(t.id)" + fromClause + whereClause,
                Long.class);

        parameters.forEach((name, value) -> {
            selectQuery.setParameter(name, value);
            countQuery.setParameter(name, value);
        });

        selectQuery.setFirstResult(query.offset());
        selectQuery.setMaxResults(query.size());

        List<TaskEntity> items = selectQuery.getResultList();
        long total = countQuery.getSingleResult();

        return new TaskQueryResult(items, total, query.page(), query.size());
    }

    private String sortExpression(TaskQuery query) {
        if (query.sort() == TaskSort.DUE_DATE) {
            return "t.dueDate";
        }
        return "t.createdAt";
    }
}
