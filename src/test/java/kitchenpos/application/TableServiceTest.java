package kitchenpos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.domain.OrderTable;
import kitchenpos.fixtures.TableFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class TableServiceTest extends ServiceTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private OrderTableDao orderTableDao;

    @InjectMocks
    private TableService tableService;

    private OrderTable emptyTable;
    private OrderTable nonEmptyTable;

    @BeforeEach
    void setUp() {
        emptyTable = TableFixtures.createOrderTable(true);
        nonEmptyTable = TableFixtures.createOrderTable(false);
    }

    @Test
    void 주문_테이블을_생성한다() {
        assertDoesNotThrow(() -> tableService.create(emptyTable));
        verify(orderTableDao, times(1)).save(emptyTable);
    }

    @Test
    void 주문_테이블_리스트를_반환한다() {
        given(orderTableDao.findAll()).willReturn(TableFixtures.createOrderTables(true));

        List<OrderTable> orderTables = assertDoesNotThrow(() -> tableService.list());
        assertThat(orderTables).isNotEmpty();
    }

    @Test
    void 주문_테이블의_상태를_변경한다() {
        given(orderTableDao.findById(any())).willReturn(Optional.of(emptyTable));
        given(orderDao.existsByOrderTableIdAndOrderStatusIn(any(), any())).willReturn(false);

        assertDoesNotThrow(() -> tableService.changeEmpty(emptyTable.getId(), nonEmptyTable));
        verify(orderTableDao).save(ArgumentMatchers.refEq(nonEmptyTable));
    }

    @Test
    void 상태_변경_시_주문_테이블이_존재하지_않으면_예외를_반환한다() {
        given(orderTableDao.findById(any())).willReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> tableService.changeEmpty(emptyTable.getId(), nonEmptyTable));
    }

    @Test
    void 상태_변경_시_주문_테이블이_단체_지정되어_있으면_예외를_반환한다() {
        OrderTable groupedTable = TableFixtures.createOrderTable(1L, 1L, 10, true);
        given(orderTableDao.findById(any())).willReturn(Optional.of(groupedTable));

        assertThrows(IllegalArgumentException.class, () -> tableService.changeEmpty(emptyTable.getId(), nonEmptyTable));
    }

    @Test
    void 상태_변경_시_완료되지_않은_주문이_있으면_예외를_반환한다() {
        given(orderTableDao.findById(any())).willReturn(Optional.of(emptyTable));
        given(orderDao.existsByOrderTableIdAndOrderStatusIn(any(), any())).willReturn(true);

        assertThrows(IllegalArgumentException.class, () -> tableService.changeEmpty(emptyTable.getId(), nonEmptyTable));
    }

    @Test
    void 주문_테이블의_손님_수를_변경한다() {
        OrderTable crowdTable = TableFixtures.createOrderTable(1L, null, 3000, false);
        given(orderTableDao.findById(any())).willReturn(Optional.of(nonEmptyTable));

        assertDoesNotThrow(() -> tableService.changeNumberOfGuests(nonEmptyTable.getId(), crowdTable));
        verify(orderTableDao).save(ArgumentMatchers.refEq(crowdTable));
    }

    @Test
    void 손님_수_변경_시_손님_수가_음수이면_예외를_반환한다() {
        OrderTable invalidTable = TableFixtures.createOrderTable(1L, null, -1, false);

        assertThrows(
            IllegalArgumentException.class,
            () -> tableService.changeNumberOfGuests(nonEmptyTable.getId(), invalidTable)
        );
    }

    @Test
    void 손님_수_변경_시_주문_테이블이_존재하지_않으면_예외를_반환한다() {
        OrderTable crowdTable = TableFixtures.createOrderTable(1L, null, 3000, false);
        given(orderTableDao.findById(any())).willReturn(Optional.empty());

        assertThrows(
            IllegalArgumentException.class,
            () -> tableService.changeNumberOfGuests(nonEmptyTable.getId(), crowdTable)
        );
    }

    @Test
    void 손님_수_변경_시_빈_테이블이면_예외를_반환한다() {
        OrderTable crowdTable = TableFixtures.createOrderTable(1L, null, 3000, false);
        given(orderTableDao.findById(any())).willReturn(Optional.of(emptyTable));

        assertThrows(
            IllegalArgumentException.class,
            () -> tableService.changeNumberOfGuests(emptyTable.getId(), crowdTable)
        );
    }
}
