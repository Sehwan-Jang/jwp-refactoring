package kitchenpos.order.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import kitchenpos.menu.repository.MenuRepository;
import kitchenpos.order.domain.Order;
import kitchenpos.order.domain.OrderLineItem;
import kitchenpos.order.repository.OrderLineItemRepository;
import kitchenpos.order.repository.OrderRepository;
import kitchenpos.order.service.dto.ChangeOrderStatusRequest;
import kitchenpos.order.service.dto.OrderRequest;
import kitchenpos.order.service.dto.OrderResponse;
import kitchenpos.ordertable.domain.OrderTable;
import kitchenpos.ordertable.repository.OrderTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class OrderService {

    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final OrderLineItemRepository orderLineItemRepository;
    private final OrderTableRepository orderTableRepository;

    public OrderService(
        final MenuRepository menuRepository,
        final OrderRepository orderRepository,
        final OrderLineItemRepository orderLineItemDao,
        final OrderTableRepository orderTableRepository
    ) {
        this.menuRepository = menuRepository;
        this.orderRepository = orderRepository;
        this.orderLineItemRepository = orderLineItemDao;
        this.orderTableRepository = orderTableRepository;
    }

    public OrderResponse create(final OrderRequest request) {
        final List<OrderLineItem> orderLineItems = request.getOrderLineItems().stream()
            .map(item -> new OrderLineItem(
                    menuRepository.findById(item.getMenuId()).orElseThrow(NoSuchElementException::new), item.getQuantity()
                )
            )
            .collect(Collectors.toList());
        final OrderTable orderTable = orderTableRepository.findById(request.getOrderTableId())
            .orElseThrow(NoSuchElementException::new);
        validateCreation(orderTable, orderLineItems);

        final Order savedOrder = orderRepository.save(new Order(request.getOrderTableId(), orderLineItems));
        orderLineItemRepository.saveAll(orderLineItems);

        return OrderResponse.of(savedOrder);
    }

    private void validateCreation(OrderTable orderTable, List<OrderLineItem> orderLineItems) {
        if (orderLineItems.isEmpty()) {
            throw new IllegalStateException("주문 항목이 비어있습니다.");
        }

        if (orderTable.isEmpty()) {
            throw new IllegalStateException("빈 테이블입니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list() {
        final List<Order> orders = orderRepository.findAll();

        return orders.stream()
            .map(OrderResponse::of)
            .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse changeOrderStatus(final Long orderId, final ChangeOrderStatusRequest request) {
        final Order savedOrder = orderRepository.findById(orderId)
            .orElseThrow(NoSuchElementException::new);

        savedOrder.changeOrderStatus(request.getOrderStatus());

        return OrderResponse.of(orderRepository.save(savedOrder));
    }
}
