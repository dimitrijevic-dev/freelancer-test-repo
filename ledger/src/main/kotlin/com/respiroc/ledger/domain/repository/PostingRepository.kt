package com.respiroc.ledger.domain.repository

import com.respiroc.ledger.domain.model.Posting
import com.respiroc.util.repository.CustomJpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface PostingRepository : CustomJpaRepository<Posting, Long> {

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Posting p WHERE p.accountNumber = :accountNumber AND p.postingDate < :beforeDate")
    fun getAccountBalanceBeforeDate(
        @Param("accountNumber") accountNumber: String,
        @Param("beforeDate") beforeDate: LocalDate
    ): BigDecimal

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Posting p WHERE p.accountNumber = :accountNumber AND p.postingDate BETWEEN :startDate AND :endDate")
    fun getAccountMovementInPeriod(
        @Param("accountNumber") accountNumber: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): BigDecimal

    @Query("SELECT DISTINCT p.accountNumber FROM Posting p ORDER BY p.accountNumber")
    fun findDistinctAccountNumbers(): List<String>

    @Query(
        """
        SELECT p.accountNumber,
               COALESCE(SUM(CASE WHEN p.postingDate < :startDate THEN p.amount ELSE 0 END), 0) as openingBalance,
               COALESCE(SUM(CASE WHEN p.postingDate BETWEEN :startDate AND :endDate THEN p.amount ELSE 0 END), 0) as periodMovement,
               COUNT(CASE WHEN p.postingDate BETWEEN :startDate AND :endDate THEN 1 END) as transactionCount
        FROM Posting p 
        WHERE (:accountNumber IS NULL OR p.accountNumber = :accountNumber)
        GROUP BY p.accountNumber
        HAVING COALESCE(SUM(CASE WHEN p.postingDate < :startDate THEN p.amount ELSE 0 END), 0) != 0
            OR COALESCE(SUM(CASE WHEN p.postingDate BETWEEN :startDate AND :endDate THEN p.amount ELSE 0 END), 0) != 0
        ORDER BY p.accountNumber
    """
    )
    fun getGeneralLedgerSummary(
        @Param("accountNumber") accountNumber: String?,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Array<Any>>

    @Query(
        """
        SELECT p FROM Posting p 
        LEFT JOIN FETCH p.voucher v
        WHERE p.accountNumber = :accountNumber
        AND p.postingDate BETWEEN :startDate AND :endDate 
        ORDER BY p.postingDate, p.id
    """
    )
    fun findPostingsByAccountAndDateRange(
        @Param("accountNumber") accountNumber: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Posting>

    @Query(
        """
        SELECT 
            CASE 
                WHEN p.accountNumber LIKE '1%' THEN 'ASSET'
                WHEN p.accountNumber LIKE '3%' THEN 'REVENUE'
                ELSE 'EXPENSE'
            END AS accountType,
            p.accountNumber,
            SUM(p.amount) as totalAmount 
        FROM Posting p 
        WHERE (p.accountNumber >= '1' AND p.accountNumber < '2' OR p.accountNumber >= '3' AND p.accountNumber < '8')
          AND p.postingDate BETWEEN :startDate AND :endDate
        GROUP BY accountType, p.accountNumber
        ORDER BY accountType, p.accountNumber
    """
    )
    fun findProfitLossPostings(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Array<Any>>

    @Query(
        """
        SELECT 
            CASE 
                WHEN p.accountNumber LIKE '1%' THEN 'ASSET'
                WHEN p.accountNumber LIKE '20%' THEN 'EQUITY'
                WHEN p.accountNumber LIKE '2%' AND p.accountNumber NOT LIKE '20%' THEN 'LIABILITY'
                ELSE 'OTHER'
            END AS accountType,
            p.accountNumber,
            SUM(p.amount) as totalAmount 
        FROM Posting p 
        WHERE p.accountNumber >= '1' AND p.accountNumber < '3'
          AND p.postingDate BETWEEN :startDate AND :endDate 
        GROUP BY accountType, p.accountNumber
        ORDER BY accountType, p.accountNumber
    """
    )
    fun findBalanceSheetPostings(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Array<Any>>
}
