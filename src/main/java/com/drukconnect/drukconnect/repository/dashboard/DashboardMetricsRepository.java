package com.drukconnect.drukconnect.repository.dashboard;

import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.enums.feedback.FeedbackStatus;
import com.drukconnect.drukconnect.enums.listing.*;
import com.drukconnect.drukconnect.enums.moderation.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/** Aggregate queries only: dashboard requests never load complete histories into memory. */
@Repository
public class DashboardMetricsRepository {
    private final EntityManager entityManager;

    public DashboardMetricsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public record BuyerServiceStats(long services, long providers, long last30Days) {}
    public record BuyerReviewStats(long total, long approved, long pending, long rejected) {}
    public record UserStats(long total, long active, long newLast7Days) {}
    public record ListingStats(long total, long active, long newLast7Days) {}

    public BuyerServiceStats buyerServices(UUID buyerId, Instant now) {
        Object[] row = row("""
            select count(a), count(distinct a.listing.lister.id),
                   coalesce(sum(case when a.availedAt >= :since then 1 else 0 end), 0)
            from ListingServiceAvailment a
            where a.buyer.id = :buyerId and a.listing.status <> :deleted and a.availedAt <= :now
            """, Map.of("buyerId", buyerId, "deleted", ListingStatus.DELETED,
                    "since", now.minus(30, ChronoUnit.DAYS), "now", now));
        return new BuyerServiceStats(number(row[0]), number(row[1]), number(row[2]));
    }

    public BuyerReviewStats buyerReviews(UUID buyerId) {
        Object[] row = row("""
            select count(r),
                   coalesce(sum(case when r.status = :approved then 1 else 0 end), 0),
                   coalesce(sum(case when r.status = :pending then 1 else 0 end), 0),
                   coalesce(sum(case when r.status = :rejected then 1 else 0 end), 0)
            from ListingReview r
            where r.reviewer.id = :buyerId and r.listing.status <> :deleted
            """, Map.of("buyerId", buyerId, "deleted", ListingStatus.DELETED,
                    "approved", ReviewStatus.APPROVED, "pending", ReviewStatus.PENDING, "rejected", ReviewStatus.REJECTED));
        return new BuyerReviewStats(number(row[0]), number(row[1]), number(row[2]), number(row[3]));
    }

    public long pendingReviewInvitations(UUID buyerId, Instant now) {
        return count("""
            select count(i) from ListingReviewRequest i
            where i.buyer.id = :buyerId and i.status = 'PENDING'
              and i.emailSentAt is not null and i.expiresAt > :now
              and i.listing.status = :active
              and not exists (select r.id from ListingReview r
                  where r.listing.id = i.listing.id and r.reviewer.id = :buyerId
                    and r.status in (:pending, :approved))
            """, Map.of("buyerId", buyerId, "now", now, "active", ListingStatus.ACTIVE,
                    "pending", ReviewStatus.PENDING, "approved", ReviewStatus.APPROVED));
    }

    public long pendingVouchRequests(UUID buyerId) {
        return count("select count(v) from VouchRequestEntity v where v.target.id = :buyerId and v.status = :pending",
                Map.of("buyerId", buyerId, "pending", VouchRequestStatus.PENDING));
    }

    public long activeVouchesGiven(UUID buyerId) {
        return count("select count(v) from Vouch v where v.voucherUser.id = :buyerId and v.status = :active",
                Map.of("buyerId", buyerId, "active", VouchStatus.ACTIVE));
    }

    public UserStats users(Instant now) {
        Object[] row = row("""
            select count(u), coalesce(sum(case when u.status = :active then 1 else 0 end), 0),
                   coalesce(sum(case when u.createdAt >= :since and u.createdAt <= :now then 1 else 0 end), 0)
            from User u where u.status <> :deleted
            """, Map.of("active", UserStatus.ACTIVE, "deleted", UserStatus.DELETED,
                    "since", now.minus(7, ChronoUnit.DAYS), "now", now));
        return new UserStats(number(row[0]), number(row[1]), number(row[2]));
    }

    public ListingStats listings(Instant now) {
        Object[] row = row("""
            select count(l), coalesce(sum(case when l.status = :active then 1 else 0 end), 0),
                   coalesce(sum(case when l.createdAt >= :since and l.createdAt <= :now then 1 else 0 end), 0)
            from Listing l where l.status <> :deleted
            """, Map.of("active", ListingStatus.ACTIVE, "deleted", ListingStatus.DELETED,
                    "since", now.minus(7, ChronoUnit.DAYS), "now", now));
        return new ListingStats(number(row[0]), number(row[1]), number(row[2]));
    }

    // These counts follow the actual admin queues, including records for archived listings.
    public long pendingReviews() {
        return count("select count(r) from ListingReview r where r.status = :status", Map.of("status", ReviewStatus.PENDING));
    }
    public long pendingWithdrawals() {
        return count("select count(w) from VouchWithdrawalRequest w where w.status = :status", Map.of("status", VouchWithdrawalStatus.PENDING));
    }
    public long pendingReports() {
        return count("select count(r) from ListerScamReport r where r.status = :status", Map.of("status", ScamReportStatus.PENDING));
    }
    public long pendingAppeals() {
        return count("select count(a) from ListerScamAppeal a where a.status = :status", Map.of("status", ScamAppealStatus.PENDING));
    }
    public long unresolvedFeedback() {
        return count("select count(f) from PlatformFeedback f where f.status <> :status", Map.of("status", FeedbackStatus.RESOLVED));
    }

    private Object[] row(String jpql, Map<String, ?> parameters) {
        var query = entityManager.createQuery(jpql, Object[].class);
        parameters.forEach(query::setParameter);
        return query.getSingleResult();
    }
    private long count(String jpql, Map<String, ?> parameters) {
        var query = entityManager.createQuery(jpql, Long.class);
        parameters.forEach(query::setParameter);
        return query.getSingleResult();
    }
    private long number(Object value) { return ((Number) value).longValue(); }
}
