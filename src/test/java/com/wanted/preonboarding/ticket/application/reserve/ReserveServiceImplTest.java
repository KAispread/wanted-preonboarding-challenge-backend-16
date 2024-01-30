package com.wanted.preonboarding.ticket.application.reserve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.wanted.preonboarding.ticket.domain.discount.Discount;
import com.wanted.preonboarding.ticket.domain.discount.DiscountRepository;
import com.wanted.preonboarding.ticket.domain.discount.model.DiscountType;
import com.wanted.preonboarding.ticket.domain.performance.Performance;
import com.wanted.preonboarding.ticket.domain.performance.PerformanceRepository;
import com.wanted.preonboarding.ticket.domain.performance.PerformanceSeatInfo;
import com.wanted.preonboarding.ticket.domain.performance.model.PerformanceType;
import com.wanted.preonboarding.ticket.dto.request.reservation.ReservationRequest;
import com.wanted.preonboarding.ticket.dto.response.reservation.ReservationInfo;
import com.wanted.preonboarding.ticket.exception.argument.InvalidArgumentException;
import com.wanted.preonboarding.ticket.exception.badrequest.BadRequestException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
class ReserveServiceImplTest {

    @Autowired
    ReserveServiceImpl reserveService;

    @Autowired
    DiscountRepository discountRepository;

    @Autowired
    PerformanceRepository performanceRepository;

    String performanceId;

    @BeforeEach
    void setUp() {
        List<Performance> all = performanceRepository.findAll();
        performanceId = all.get(0).getId().toString();
    }

    @DisplayName("예약에 성공한다.")
    @Test
    void reserve_success() {
        // given
        ReservationRequest request = ReservationRequest.builder()
            .name("KAI")
            .phone("01012345678")
            .age(30)
            .costAmount(100_000)
            .performanceId(performanceId)
            .round(1)
            .line("A")
            .seat(1)
            .build();
        LocalDateTime requestTime = LocalDateTime.of(2024, 1, 30, 12, 00);

        // when
        ReservationInfo reservationInfo = reserveService.reserve(request, requestTime);

        // then
        assertThat(reservationInfo).isNotNull()
            .extracting("performanceRound", "performanceId", "line", "seat", "name", "phone")
            .containsExactly(1, performanceId, "A", 1,"KAI","01012345678");
    }

    @DisplayName("이미 좌석에 예약한 사람이 있는 경우 예약에 실패한다")
    @Test
    void reserve_fail_already_exist() {
        // given
        ReservationRequest requestAlreadyExistSeat = ReservationRequest.builder()
            .name("KAI")
            .phone("01012345678")
            .age(30)
            .costAmount(10000)
            .performanceId(performanceId)
            .round(1)
            .line("A")
            .seat(4)
            .build();
        LocalDateTime requestTime = LocalDateTime.of(2024, 1, 30, 12, 00);

        // when & then
        assertThatThrownBy(() -> reserveService.reserve(requestAlreadyExistSeat, requestTime))
                .isInstanceOf(BadRequestException.class);
    }

    @DisplayName("존재하지 않는 좌석이라면 예약에 실패한다")
    @Test
    void reserve_fail_seat_not_exist() {
        // given
        ReservationRequest requestAlreadyExistSeat = ReservationRequest.builder()
            .name("KAI")
            .phone("01012345678")
            .age(30)
            .costAmount(10000)
            .performanceId(performanceId)
            .round(1)
            .line("B")
            .seat(1)
            .build();
        LocalDateTime requestTime = LocalDateTime.of(2024, 1, 30, 12, 00);

        // when & then
        assertThatThrownBy(() -> reserveService.reserve(requestAlreadyExistSeat, requestTime))
            .isInstanceOf(InvalidArgumentException.class);
    }

    @DisplayName("좌석이 비활성화된 상태라면 예약에 실패한다.")
    @Test
    void reserve_fail_seat_disable() {
        // given
        ReservationRequest requestAlreadyExistSeat = ReservationRequest.builder()
            .name("KAI")
            .phone("01012345678")
            .age(30)
            .costAmount(10000)
            .performanceId(performanceId)
            .round(1)
            .line("A")
            .seat(5)
            .build();
        LocalDateTime requestTime = LocalDateTime.of(2024, 1, 30, 12, 00);

        // when & then
        assertThatThrownBy(() -> reserveService.reserve(requestAlreadyExistSeat, requestTime))
            .isInstanceOf(BadRequestException.class);
    }

    // 공연 가격 100_000
    @DisplayName("잔고가 공연 가격보다 작다면 예약에 실패한다")
    @ValueSource(ints = {99_999, 1})
    @ParameterizedTest
    void reserve_fail_no_money(int amount) {
        // given
        ReservationRequest requestAlreadyExistSeat = ReservationRequest.builder()
            .name("KAI")
            .phone("01012345678")
            .age(30)
            .costAmount(amount)
            .performanceId(performanceId)
            .round(1)
            .line("A")
            .seat(1)
            .build();
        LocalDateTime requestTime = LocalDateTime.of(2024, 1, 30, 12, 00);

        // when & then
        assertThatThrownBy(() -> reserveService.reserve(requestAlreadyExistSeat, requestTime))
            .isInstanceOf(BadRequestException.class);
    }

    @DisplayName("할인 로직 적용 테스트")
    @Nested
    class DiscountTest {

        @DisplayName("공연에 할인이 적용되어 있다면 할인된 금액으로 예약한다")
        @Test
        void reserve_discount() {
            // given
            LocalDateTime endDate = LocalDateTime.of(2099, 12, 31, 11, 59);
            Discount discount = Discount.builder()
                .performanceId(UUID.fromString(performanceId))
                .name("오픈 할인")
                .type(DiscountType.PERCENT)
                .amount(10)
                .endDate(endDate)
                .build();
            discountRepository.save(discount);

            LocalDateTime requestTime = LocalDateTime.of(2024, 1, 30, 12, 00);
            ReservationRequest request = ReservationRequest.builder()
                .name("KAI")
                .phone("01012345678")
                .age(30)
                .costAmount(100_000)
                .performanceId(performanceId)
                .round(1)
                .line("A")
                .seat(1)
                .build();

            // when
            ReservationInfo reservationInfo = reserveService.reserve(request, requestTime);

            // then
            assertThat(reservationInfo).isNotNull();
            assertThat(reservationInfo.price()).isEqualTo(90_000);
        }

        @DisplayName("여러개의 할인 로직이 적용될 수 있다")
        @Test
        void reserve_with_several_discount() {
            // given
            LocalDateTime endDate = LocalDateTime.of(2099, 12, 31, 11, 59);
            Discount discount1 = Discount.builder()
                .performanceId(UUID.fromString(performanceId))
                .name("오픈 할인")
                .type(DiscountType.PERCENT)
                .amount(10)
                .endDate(endDate)
                .build();
            Discount discount2 = Discount.builder()
                .performanceId(UUID.fromString(performanceId))
                .name("가정의 달 할인")
                .type(DiscountType.AMOUNT)
                .amount(5000)
                .endDate(endDate)
                .build();
            discountRepository.saveAll(List.of(discount1, discount2));

            LocalDateTime requestTime = LocalDateTime.of(2024, 1, 30, 12, 00);
            ReservationRequest request = ReservationRequest.builder()
                .name("KAI")
                .phone("01012345678")
                .age(30)
                .costAmount(100_000)
                .performanceId(performanceId)
                .round(1)
                .line("A")
                .seat(1)
                .build();

            // when
            ReservationInfo reservationInfo = reserveService.reserve(request, requestTime);

            // then
            assertThat(reservationInfo).isNotNull();
            assertThat(reservationInfo.price()).isEqualTo(85_000);
        }

        @DisplayName("잔고가 공연 가격보다 작지만 할인된 금액으로 예약할 수 있는 경우 예약에 성공한다")
        @ValueSource(ints = {90_000, 99_999, 95_000})
        @ParameterizedTest
        void reserve_discounted_price_with_less_money(int amount) {
            // given
            LocalDateTime endDate = LocalDateTime.of(2099, 12, 31, 11, 59);
            Discount discount = Discount.builder()
                .performanceId(UUID.fromString(performanceId))
                .name("오픈 할인")
                .type(DiscountType.AMOUNT)
                .amount(10000)
                .endDate(endDate)
                .build();
            discountRepository.save(discount);

            LocalDateTime requestTime = LocalDateTime.of(2024, 1, 30, 12, 00);
            ReservationRequest request = ReservationRequest.builder()
                .name("KAI")
                .phone("01012345678")
                .age(30)
                .costAmount(amount)
                .performanceId(performanceId)
                .round(1)
                .line("A")
                .seat(1)
                .build();

            // when
            ReservationInfo reservationInfo = reserveService.reserve(request, requestTime);

            // then
            assertThat(reservationInfo).isNotNull()
                .extracting("performanceRound", "performanceId", "line", "seat", "name", "phone")
                .containsExactly(1, performanceId, "A", 1,"KAI","01012345678");
        }

    }

}