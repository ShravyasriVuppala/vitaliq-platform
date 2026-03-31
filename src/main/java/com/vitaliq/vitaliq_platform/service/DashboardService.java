package com.vitaliq.vitaliq_platform.service;

import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import com.vitaliq.vitaliq_platform.document.WorkoutDocument;
import com.vitaliq.vitaliq_platform.dto.dashboard.DashboardSummaryResponse;
import com.vitaliq.vitaliq_platform.dto.dashboard.MuscleGroupCount;
import com.vitaliq.vitaliq_platform.dto.dashboard.VolumeDataPoint;
import com.vitaliq.vitaliq_platform.model.auth.User;
import com.vitaliq.vitaliq_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final UserRepository userRepository;

    // ─── Helper: extract authenticated user from JWT ───────────────────────

    private User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    // ─── Helper: safely cast aggregations container ────────────────────────

    private ElasticsearchAggregation getAggregation(SearchHits<?> hits, String name) {
        if (hits.getAggregations() == null) return null;
        ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();
        return (ElasticsearchAggregation) aggs.get(name);
    }

    // ── GET /api/dashboard/summary ───────────────────────────────────────────

    public DashboardSummaryResponse getSummary() {
        try {
            User user = getAuthenticatedUser();
            String userId = user.getId().toString();

            LocalDateTime weekStart = LocalDate.now()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay();
            LocalDateTime monthStart = LocalDate.now()
                    .withDayOfMonth(1)
                    .atStartOfDay();
            LocalDateTime now = LocalDateTime.now();

            SearchHits<WorkoutDocument> weekHits = queryWithSumAndAvg(userId, weekStart, now);
            SearchHits<WorkoutDocument> monthHits = queryWithSumAndAvg(userId, monthStart, now);

            return DashboardSummaryResponse.builder()
                    .workoutsThisWeek(weekHits.getTotalHits())
                    .totalVolumeLbsThisWeek(extractSum(weekHits, "total_volume"))
                    .totalDurationMinutesThisWeek(extractSumAsLong(weekHits, "total_duration"))
                    .avgDurationMinutesThisWeek(extractAvgAsLong(weekHits, "avg_duration"))
                    .workoutsThisMonth(monthHits.getTotalHits())
                    .totalVolumeLbsThisMonth(extractSum(monthHits, "total_volume"))
                    .totalDurationMinutesThisMonth(extractSumAsLong(monthHits, "total_duration"))
                    .avgDurationMinutesThisMonth(extractAvgAsLong(monthHits, "avg_duration"))
                    .build();

        } catch (Exception e) {
            log.error("getSummary failed — cause: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ── GET /api/dashboard/volume ────────────────────────────────────────────

    public List<VolumeDataPoint> getVolumeOverTime() {
        User user = getAuthenticatedUser();
        String userId = user.getId().toString();

        LocalDateTime from = LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m.term(t -> t
                                        .field("userId")
                                        .value(userId)))
                                .must(m -> m.range(r -> r
                                        .date(d -> d
                                                .field("startTime")
                                                .gte(from.toLocalDate().toString())
                                                .lte(to.toLocalDate().toString()))))
                        )
                )
                .withAggregation("volume_per_day",
                        co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                                .dateHistogram(dh -> dh
                                        .field("startTime")
                                        .calendarInterval(CalendarInterval.Day))
                                .aggregations("daily_volume",
                                        co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(sa -> sa
                                                .sum(s -> s.field("totalVolumeLbs"))))
                        )
                )
                .withMaxResults(0)
                .build();

        SearchHits<WorkoutDocument> hits = elasticsearchOperations.search(query, WorkoutDocument.class);

        List<VolumeDataPoint> dataPoints = new ArrayList<>();

        ElasticsearchAggregation agg = getAggregation(hits, "volume_per_day");
        if (agg != null) {
            agg.aggregation().getAggregate().dateHistogram().buckets().array()
                    .forEach(bucket -> {
                        double volume = bucket.aggregations()
                                .get("daily_volume").sum().value();
                        if (volume > 0) {
                            dataPoints.add(VolumeDataPoint.builder()
                                    .date(LocalDate.parse(bucket.keyAsString().substring(0, 10)))
                                    .totalVolumeLbs(volume)
                                    .build());
                        }
                    });
        }

        return dataPoints;
    }

    // ── GET /api/dashboard/muscle-groups ─────────────────────────────────────

    public List<MuscleGroupCount> getMuscleGroupBreakdown() {
        User user = getAuthenticatedUser();
        String userId = user.getId().toString();

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m.term(t -> t
                                        .field("userId")
                                        .value(userId)))
                                .must(m -> m.range(r -> r
                                        .date(d -> d
                                                .field("startTime")
                                                .gte(monthStart.toLocalDate().toString())
                                                .lte(now.toLocalDate().toString()))))
                        )
                )
                .withAggregation("by_muscle_group",
                        co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                                .terms(t -> t
                                        .field("muscleGroupsWorked")
                                        .size(20))
                        )
                )
                .withMaxResults(0)
                .build();

        SearchHits<WorkoutDocument> hits = elasticsearchOperations.search(query, WorkoutDocument.class);

        List<MuscleGroupCount> result = new ArrayList<>();

        ElasticsearchAggregation agg = getAggregation(hits, "by_muscle_group");
        if (agg != null) {
            List<StringTermsBucket> buckets = agg.aggregation().getAggregate()
                    .sterms().buckets().array();
            buckets.forEach(bucket ->
                    result.add(MuscleGroupCount.builder()
                            .muscleGroup(bucket.key().stringValue())
                            .count(bucket.docCount())
                            .build())
            );
        }

        return result;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private SearchHits<WorkoutDocument> queryWithSumAndAvg(
            String userId, LocalDateTime from, LocalDateTime to) {

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m.term(t -> t
                                        .field("userId")
                                        .value(userId)))
                                .must(m -> m.range(r -> r
                                        .date(d -> d
                                                .field("startTime")
                                                .gte(from.toLocalDate().toString())
                                                .lte(to.toLocalDate().toString()))))
                        )
                )
                .withAggregation("total_volume",
                        co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                                .sum(s -> s.field("totalVolumeLbs"))))
                .withAggregation("total_duration",
                        co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                                .sum(s -> s.field("durationMinutes"))))
                .withAggregation("avg_duration",
                        co.elastic.clients.elasticsearch._types.aggregations.Aggregation.of(a -> a
                                .avg(avg -> avg.field("durationMinutes"))))
                .withMaxResults(0)
                .build();

        return elasticsearchOperations.search(query, WorkoutDocument.class);
    }

    private Double extractSum(SearchHits<WorkoutDocument> hits, String aggName) {
        ElasticsearchAggregation agg = getAggregation(hits, aggName);
        if (agg == null) return null;
        double value = agg.aggregation().getAggregate().sum().value();
        return value > 0 ? value : null;
    }

    private Long extractSumAsLong(SearchHits<WorkoutDocument> hits, String aggName) {
        Double value = extractSum(hits, aggName);
        return value != null ? value.longValue() : null;
    }

    private Long extractAvgAsLong(SearchHits<WorkoutDocument> hits, String aggName) {
        ElasticsearchAggregation agg = getAggregation(hits, aggName);
        if (agg == null) return null;
        Double value = agg.aggregation().getAggregate().avg().value();
        // ES 9.x returns null when no documents match (older versions returned -Infinity)
        if (value == null || Double.isInfinite(value) || Double.isNaN(value)) return null;
        return value.longValue();
    }
}