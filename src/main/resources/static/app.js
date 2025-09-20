// Order Management System JavaScript

class OrderManagementApp {
    constructor() {
        this.currentOrderId = null;
        this.orders = [];
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadOrders();
    }

    setupEventListeners() {
        // Navigation
        document.getElementById('view-orders-btn').addEventListener('click', () => this.showSection('orders'));
        document.getElementById('create-order-btn').addEventListener('click', () => this.showSection('create'));

        // Filters
        document.getElementById('apply-filters-btn').addEventListener('click', () => this.applyFilters());

        // Form handling
        document.getElementById('create-order-form').addEventListener('submit', (e) => this.handleCreateOrder(e));
        document.getElementById('add-item-btn').addEventListener('click', () => this.addOrderItem());

        // Modal handling
        document.querySelector('.modal-close').addEventListener('click', () => this.closeModal());
        document.getElementById('update-status-btn').addEventListener('click', () => this.updateOrderStatus());
        document.getElementById('cancel-order-btn').addEventListener('click', () => this.cancelOrder());

        // Close modal when clicking outside
        document.getElementById('order-modal').addEventListener('click', (e) => {
            if (e.target === e.currentTarget) {
                this.closeModal();
            }
        });
    }

    showSection(section) {
        // Update navigation
        document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
        document.getElementById(`${section === 'orders' ? 'view-orders' : 'create-order'}-btn`).classList.add('active');

        // Update sections
        document.querySelectorAll('.section').forEach(sec => sec.classList.remove('active'));
        document.getElementById(`${section === 'orders' ? 'orders' : 'create'}-section`).classList.add('active');

        if (section === 'orders') {
            this.loadOrders();
        }
    }

    async loadOrders() {
        try {
            this.showLoading('orders-list');
            const response = await fetch('/api/orders');
            if (!response.ok) throw new Error('Failed to load orders');
            
            this.orders = await response.json();
            this.displayOrders(this.orders);
        } catch (error) {
            this.showError('orders-list', 'Failed to load orders: ' + error.message);
        }
    }

    displayOrders(orders) {
        const container = document.getElementById('orders-list');
        
        if (orders.length === 0) {
            container.innerHTML = '<div class="loading">No orders found</div>';
            return;
        }

        container.innerHTML = orders.map(order => `
            <div class="order-card" onclick="app.showOrderDetails(${order.id})" data-testid="order-${order.id}">
                <div class="order-header">
                    <span class="order-id">Order #${order.id}</span>
                    <span class="order-status status-${order.status.toLowerCase()}">${order.status}</span>
                </div>
                <div class="order-details">
                    <div class="order-info">
                        <label>User ID:</label>
                        <span>${order.userId}</span>
                    </div>
                    <div class="order-info">
                        <label>Total Amount:</label>
                        <span>$${order.totalAmount}</span>
                    </div>
                    <div class="order-info">
                        <label>Order Date:</label>
                        <span>${this.formatDate(order.orderDate)}</span>
                    </div>
                    <div class="order-info">
                        <label>Items:</label>
                        <span>${order.orderItems ? order.orderItems.length : 0} items</span>
                    </div>
                </div>
            </div>
        `).join('');
    }

    applyFilters() {
        const userFilter = document.getElementById('user-filter').value.trim();
        const statusFilter = document.getElementById('status-filter').value;

        let filteredOrders = this.orders;

        if (userFilter) {
            filteredOrders = filteredOrders.filter(order => 
                order.userId.toString().includes(userFilter)
            );
        }

        if (statusFilter) {
            filteredOrders = filteredOrders.filter(order => 
                order.status === statusFilter
            );
        }

        this.displayOrders(filteredOrders);
    }

    async showOrderDetails(orderId) {
        try {
            this.currentOrderId = orderId;
            const response = await fetch(`/api/orders/${orderId}`);
            if (!response.ok) throw new Error('Failed to load order details');
            
            const order = await response.json();
            this.displayOrderDetails(order);
            this.openModal();
        } catch (error) {
            this.showError('order-details', 'Failed to load order details: ' + error.message);
        }
    }

    displayOrderDetails(order) {
        const container = document.getElementById('order-details');
        container.innerHTML = `
            <div class="order-details-grid">
                <div class="order-info">
                    <label>Order ID:</label>
                    <span>${order.id}</span>
                </div>
                <div class="order-info">
                    <label>User ID:</label>
                    <span>${order.userId}</span>
                </div>
                <div class="order-info">
                    <label>Status:</label>
                    <span class="order-status status-${order.status.toLowerCase()}">${order.status}</span>
                </div>
                <div class="order-info">
                    <label>Total Amount:</label>
                    <span>$${order.totalAmount}</span>
                </div>
                <div class="order-info">
                    <label>Order Date:</label>
                    <span>${this.formatDate(order.orderDate)}</span>
                </div>
                <div class="order-info">
                    <label>Shipping Address:</label>
                    <span>${order.shippingAddress}</span>
                </div>
            </div>
            
            <h4>Order Items:</h4>
            <div class="order-items-list">
                ${order.orderItems ? order.orderItems.map(item => `
                    <div class="order-item-detail">
                        <span><strong>${item.productName}</strong> (ID: ${item.productId})</span>
                        <span>Quantity: ${item.quantity}</span>
                        <span>Unit Price: $${item.unitPrice}</span>
                        <span>Total: $${item.totalPrice}</span>
                    </div>
                `).join('') : '<p>No items found</p>'}
            </div>
        `;
    }

    addOrderItem() {
        const container = document.getElementById('order-items');
        const newItem = document.createElement('div');
        newItem.className = 'order-item';
        newItem.innerHTML = `
            <input type="number" placeholder="Product ID" name="productId" required class="item-input">
            <input type="text" placeholder="Product Name" name="productName" required class="item-input">
            <input type="number" placeholder="Quantity" name="quantity" min="1" required class="item-input">
            <input type="number" placeholder="Unit Price" name="unitPrice" step="0.01" min="0" required class="item-input">
            <button type="button" class="btn-remove" onclick="removeItem(this)">Remove</button>
        `;
        container.appendChild(newItem);
    }

    async handleCreateOrder(event) {
        event.preventDefault();
        
        try {
            const formData = new FormData(event.target);
            const orderData = this.buildOrderData(formData);
            
            const response = await fetch('/api/orders', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(orderData)
            });

            if (!response.ok) {
                const error = await response.text();
                throw new Error(error || 'Failed to create order');
            }

            const newOrder = await response.json();
            this.showSuccess('Order created successfully! Order ID: ' + newOrder.id);
            event.target.reset();
            
            // Reset to single item
            const itemsContainer = document.getElementById('order-items');
            itemsContainer.innerHTML = `
                <div class="order-item">
                    <input type="number" placeholder="Product ID" name="productId" required class="item-input">
                    <input type="text" placeholder="Product Name" name="productName" required class="item-input">
                    <input type="number" placeholder="Quantity" name="quantity" min="1" required class="item-input">
                    <input type="number" placeholder="Unit Price" name="unitPrice" step="0.01" min="0" required class="item-input">
                    <button type="button" class="btn-remove" onclick="removeItem(this)">Remove</button>
                </div>
            `;
        } catch (error) {
            this.showError('create-section', 'Failed to create order: ' + error.message);
        }
    }

    buildOrderData(formData) {
        const orderData = {
            userId: parseInt(formData.get('userId')),
            shippingAddress: formData.get('shippingAddress'),
            orderItems: []
        };

        // Get all order items
        const items = document.querySelectorAll('.order-item');
        items.forEach(item => {
            const inputs = item.querySelectorAll('input');
            if (inputs[0].value) { // Only add if productId is filled
                orderData.orderItems.push({
                    productId: parseInt(inputs[0].value),
                    productName: inputs[1].value,
                    quantity: parseInt(inputs[2].value),
                    unitPrice: parseFloat(inputs[3].value)
                });
            }
        });

        return orderData;
    }

    async updateOrderStatus() {
        const newStatus = document.getElementById('status-update').value;
        if (!newStatus || !this.currentOrderId) return;

        try {
            const response = await fetch(`/api/orders/${this.currentOrderId}/status`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ status: newStatus })
            });

            if (!response.ok) throw new Error('Failed to update status');

            this.showSuccess('Order status updated successfully!');
            this.closeModal();
            this.loadOrders();
        } catch (error) {
            this.showError('order-details', 'Failed to update status: ' + error.message);
        }
    }

    async cancelOrder() {
        if (!this.currentOrderId) return;
        
        if (!confirm('Are you sure you want to cancel this order?')) return;

        try {
            const response = await fetch(`/api/orders/${this.currentOrderId}`, {
                method: 'DELETE'
            });

            if (!response.ok) throw new Error('Failed to cancel order');

            this.showSuccess('Order cancelled successfully!');
            this.closeModal();
            this.loadOrders();
        } catch (error) {
            this.showError('order-details', 'Failed to cancel order: ' + error.message);
        }
    }

    openModal() {
        document.getElementById('order-modal').classList.add('show');
    }

    closeModal() {
        document.getElementById('order-modal').classList.remove('show');
        this.currentOrderId = null;
    }

    formatDate(dateString) {
        return new Date(dateString).toLocaleString();
    }

    showLoading(containerId) {
        document.getElementById(containerId).innerHTML = '<div class="loading">Loading...</div>';
    }

    showError(containerId, message) {
        const container = typeof containerId === 'string' ? document.getElementById(containerId) : containerId;
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error';
        errorDiv.textContent = message;
        container.insertBefore(errorDiv, container.firstChild);
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (errorDiv.parentNode) {
                errorDiv.parentNode.removeChild(errorDiv);
            }
        }, 5000);
    }

    showSuccess(message) {
        const container = document.querySelector('.section.active');
        const successDiv = document.createElement('div');
        successDiv.className = 'success';
        successDiv.textContent = message;
        container.insertBefore(successDiv, container.firstChild);
        
        // Auto-remove after 3 seconds
        setTimeout(() => {
            if (successDiv.parentNode) {
                successDiv.parentNode.removeChild(successDiv);
            }
        }, 3000);
    }
}

// Global function for removing items (called from HTML)
function removeItem(button) {
    const item = button.parentElement;
    const container = item.parentElement;
    
    // Don't remove if it's the only item
    if (container.children.length > 1) {
        item.remove();
    } else {
        // Clear the values instead
        const inputs = item.querySelectorAll('input');
        inputs.forEach(input => input.value = '');
    }
}

// Initialize the application when DOM is loaded
let app;
document.addEventListener('DOMContentLoaded', () => {
    app = new OrderManagementApp();
});

// Export for testing if in Node.js environment
if (typeof module !== 'undefined' && module.exports) {
    module.exports = OrderManagementApp;
}