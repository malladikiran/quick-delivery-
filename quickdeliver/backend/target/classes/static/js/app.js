// QuickDeliver Main JavaScript Controller
const API_BASE = '/api';

// Current active session state
let session = {
    username: 'rahul',
    role: 'CUSTOMER',
    entityId: 1,
    displayName: 'Rahul Sharma (Z4)',
    zoneCode: 'Z4'
};

// Cart state
let cart = []; // array of { foodItemId, name, price, quantity }
let restaurantsList = [];
let zonesList = [];

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
    updateSessionUI();
    loadInitialData();
});

function updateSessionUI() {
    document.getElementById('current-user-label').textContent = session.displayName;
    const badge = document.getElementById('current-role-badge');
    badge.textContent = session.role;
    badge.className = `role-badge ${session.role.toLowerCase()}`;
}

function showSwitchPersonaModal() {
    document.getElementById('persona-modal').style.display = 'flex';
}

function closeSwitchPersonaModal() {
    document.getElementById('persona-modal').style.display = 'none';
}

function setPersona(username, role, entityId, displayName, zoneCode) {
    session = { username, role, entityId, displayName, zoneCode };
    updateSessionUI();
    closeSwitchPersonaModal();

    // Auto switch to relevant tab
    if (role === 'CUSTOMER') switchTab('customer');
    else if (role === 'RESTAURANT') switchTab('restaurant');
    else if (role === 'ADMIN') switchTab('admin');
    else if (role === 'RIDER') switchTab('rider');
}

function switchTab(tabId) {
    document.querySelectorAll('.nav-tab').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.view-section').forEach(s => s.classList.remove('active'));

    const tabBtn = document.getElementById(`tab-${tabId}`);
    if (tabBtn) tabBtn.classList.add('active');

    const viewSec = document.getElementById(`view-${tabId}`);
    if (viewSec) viewSec.classList.add('active');

    if (tabId === 'customer') loadCustomerView();
    if (tabId === 'restaurant') loadRestaurantOrders();
    if (tabId === 'admin') loadAdminDashboard();
    if (tabId === 'rider') loadRiderOrders();
}

async function loadInitialData() {
    try {
        const [resZones, resRests] = await Promise.all([
            fetch(`${API_BASE}/customer/zones`),
            fetch(`${API_BASE}/customer/restaurants`)
        ]);
        zonesList = await resZones.json();
        restaurantsList = await resRests.json();

        // Populate Zone selects
        const zoneSelect = document.getElementById('cust-delivery-zone');
        if (zoneSelect) {
            zoneSelect.innerHTML = zonesList.map(z =>
                `<option value="${z.id}" ${z.zoneCode === session.zoneCode ? 'selected' : ''}>${z.zoneCode} – ${z.name}</option>`
            ).join('');
        }

        // Populate Restaurant selects
        const custRestSelect = document.getElementById('cust-restaurant-select');
        const restDashboardSelect = document.getElementById('rest-select');

        const optionsHtml = restaurantsList.map(r =>
            `<option value="${r.id}">${r.name} (${r.deliveryZone.zoneCode})</option>`
        ).join('');

        if (custRestSelect) {
            custRestSelect.innerHTML = optionsHtml;
            loadRestaurantMenu();
        }
        if (restDashboardSelect) {
            restDashboardSelect.innerHTML = optionsHtml;
        }

        // Load Riders
        const ridersRes = await fetch(`${API_BASE}/admin/dispatch/riders`);
        const ridersList = await ridersRes.json();
        const riderSelect = document.getElementById('rider-select');
        if (riderSelect) {
            riderSelect.innerHTML = ridersList.map(rd =>
                `<option value="${rd.id}">${rd.name} (${rd.currentZone.zoneCode} - ${rd.vehicleType})</option>`
            ).join('');
        }

        loadCustomerOrders();
    } catch (err) {
        console.error('Error loading initial data:', err);
    }
}

// ================= CUSTOMER MODULE =================
async function loadCustomerView() {
    loadCustomerOrders();
}

async function loadRestaurantMenu() {
    const restId = document.getElementById('cust-restaurant-select').value;
    if (!restId) return;

    try {
        const res = await fetch(`${API_BASE}/customer/restaurants/${restId}/menu`);
        const items = await res.json();
        const container = document.getElementById('cust-menu-container');

        if (items.length === 0) {
            container.innerHTML = `<p style="color: var(--text-muted); font-size: 0.9rem;">No menu items listed yet.</p>`;
            return;
        }

        container.innerHTML = items.map(item => `
            <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.75rem; border: 1px solid var(--border-color); border-radius: var(--radius-sm);">
                <div>
                    <div style="font-weight: 700; color: var(--text-navy);">${item.name}</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">${item.description || ''}</div>
                    <div style="font-weight: 700; color: var(--primary-blue); margin-top: 0.25rem;">₹${Number(item.price).toFixed(2)}</div>
                </div>
                <button class="btn btn-outline btn-sm" onclick="addToCart(${item.id}, '${item.name.replace(/'/g, "\\'")}', ${item.price})">
                    <i class="fa-solid fa-plus"></i> Add
                </button>
            </div>
        `).join('');
    } catch (err) {
        console.error('Error loading menu:', err);
    }
}

function addToCart(foodItemId, name, price) {
    const existing = cart.find(i => i.foodItemId === foodItemId);
    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({ foodItemId, name, price, quantity: 1 });
    }
    renderCart();
}

function removeFromCart(foodItemId) {
    const idx = cart.findIndex(i => i.foodItemId === foodItemId);
    if (idx !== -1) {
        if (cart[idx].quantity > 1) {
            cart[idx].quantity -= 1;
        } else {
            cart.splice(idx, 1);
        }
    }
    renderCart();
}

function renderCart() {
    const container = document.getElementById('cust-cart-items');
    const totalElem = document.getElementById('cust-cart-total');

    if (cart.length === 0) {
        container.innerHTML = `<p style="color: var(--text-muted); font-size: 0.9rem;">Cart is currently empty. Add dishes from the menu.</p>`;
        totalElem.textContent = '₹0.00';
        return;
    }

    let total = 0;
    container.innerHTML = cart.map(item => {
        const lineTotal = item.price * item.quantity;
        total += lineTotal;
        return `
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; font-size: 0.9rem;">
                <div>
                    <span style="font-weight: 600;">${item.name}</span>
                    <span style="color: var(--text-muted); margin-left: 0.5rem;">× ${item.quantity}</span>
                </div>
                <div style="display: flex; align-items: center; gap: 0.5rem;">
                    <span style="font-weight: 700;">₹${lineTotal.toFixed(2)}</span>
                    <button class="btn btn-outline btn-sm" style="padding: 0.15rem 0.45rem;" onclick="removeFromCart(${item.foodItemId})">-</button>
                    <button class="btn btn-outline btn-sm" style="padding: 0.15rem 0.45rem;" onclick="addToCart(${item.foodItemId}, '${item.name.replace(/'/g, "\\'")}', ${item.price})">+</button>
                </div>
            </div>
        `;
    }).join('');

    totalElem.textContent = `₹${total.toFixed(2)}`;
}

async function placeCustomerOrder() {
    if (cart.length === 0) {
        alert('Please add at least one item to your cart.');
        return;
    }

    const restId = Number(document.getElementById('cust-restaurant-select').value);
    const zoneId = Number(document.getElementById('cust-delivery-zone').value);

    const payload = {
        customerId: session.role === 'CUSTOMER' ? session.entityId : 1,
        restaurantId: restId,
        deliveryZoneId: zoneId,
        items: cart.map(i => ({ foodItemId: i.foodItemId, quantity: i.quantity }))
    };

    try {
        const res = await fetch(`${API_BASE}/customer/orders`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const err = await res.json();
            alert('Error: ' + (err.error || 'Failed to place order'));
            return;
        }

        const newOrder = await res.json();
        alert(`Order #${newOrder.id} placed successfully!`);
        cart = [];
        renderCart();
        loadCustomerOrders();
        trackOrder(newOrder.id);
    } catch (err) {
        console.error('Error placing order:', err);
    }
}

async function loadCustomerOrders() {
    const custId = session.role === 'CUSTOMER' ? session.entityId : 1;
    try {
        const res = await fetch(`${API_BASE}/customer/orders?customerId=${custId}`);
        const orders = await res.json();
        const container = document.getElementById('cust-orders-list');

        if (orders.length === 0) {
            container.innerHTML = `<p style="color: var(--text-muted); font-size: 0.85rem;">No past orders found.</p>`;
            return;
        }

        container.innerHTML = orders.map(ord => `
            <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.75rem; border: 1px solid var(--border-color); border-radius: var(--radius-sm);">
                <div>
                    <div style="font-weight: 700;">Order #${ord.id} – ${ord.restaurant.name}</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">Total: ₹${Number(ord.totalAmount).toFixed(2)} | Delivery: ${ord.deliveryZone.zoneCode}</div>
                    <div style="margin-top: 0.3rem;"><span class="status-pill status-${ord.status}">${ord.status.replace(/_/g, ' ')}</span></div>
                </div>
                <button class="btn btn-primary btn-sm" onclick="trackOrder(${ord.id})">
                    <i class="fa-solid fa-location-arrow"></i> Track Live
                </button>
            </div>
        `).join('');
    } catch (err) {
        console.error('Error loading customer orders:', err);
    }
}

// ================= RESTAURANT MODULE =================
async function loadRestaurantOrders() {
    const restSelect = document.getElementById('rest-select');
    const restId = restSelect ? restSelect.value : 1;
    if (!restId) return;

    try {
        const res = await fetch(`${API_BASE}/restaurant/${restId}/orders`);
        const orders = await res.json();
        const container = document.getElementById('rest-orders-table');

        if (orders.length === 0) {
            container.innerHTML = `<p style="color: var(--text-muted); padding: 1rem;">No orders assigned to this restaurant yet.</p>`;
            return;
        }

        container.innerHTML = orders.map(ord => {
            let actionBtn = '';
            if (ord.status === 'PLACED') {
                actionBtn = `<button class="btn btn-primary btn-sm" onclick="confirmRestOrder(${ord.id})"><i class="fa-solid fa-check"></i> Accept Order</button>`;
            } else if (ord.status === 'CONFIRMED') {
                actionBtn = `<button class="btn btn-warning btn-sm" onclick="startPreparingRestOrder(${ord.id})"><i class="fa-solid fa-fire"></i> Start Preparing</button>`;
            } else if (ord.status === 'PREPARING') {
                actionBtn = `<button class="btn btn-success btn-sm" onclick="markReadyRestOrder(${ord.id})"><i class="fa-solid fa-bell"></i> Ready for Pickup (Calc ETA)</button>`;
            } else {
                actionBtn = `<span style="font-size: 0.8rem; color: var(--text-muted); font-weight: 600;">Kitchen Done</span>`;
            }

            const itemsStr = (ord.items || []).map(i => `${i.foodItem.name} (×${i.quantity})`).join(', ');

            return `
                <div style="border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 1rem; display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap;">
                    <div>
                        <div style="display: flex; align-items: center; gap: 0.75rem;">
                            <span style="font-weight: 800; font-size: 1.05rem;">Order #${ord.id}</span>
                            <span class="status-pill status-${ord.status}">${ord.status.replace(/_/g, ' ')}</span>
                            <span style="font-size: 0.8rem; color: var(--text-muted);">Customer: ${ord.customer.name} (Zone ${ord.deliveryZone.zoneCode})</span>
                        </div>
                        <div style="font-size: 0.85rem; margin-top: 0.4rem; color: var(--text-navy);">
                            <strong>Items:</strong> ${itemsStr || 'N/A'}
                        </div>
                        <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem;">
                            Total: ₹${Number(ord.totalAmount).toFixed(2)} | Distance: ${ord.distanceKm} km
                        </div>
                    </div>
                    <div style="display: flex; gap: 0.5rem; align-items: center;">
                        ${actionBtn}
                        <button class="btn btn-outline btn-sm" onclick="trackOrder(${ord.id})"><i class="fa-solid fa-eye"></i></button>
                    </div>
                </div>
            `;
        }).join('');
    } catch (err) {
        console.error('Error loading restaurant orders:', err);
    }
}

async function confirmRestOrder(orderId) {
    await fetch(`${API_BASE}/restaurant/orders/${orderId}/confirm`, { method: 'POST' });
    loadRestaurantOrders();
}

async function startPreparingRestOrder(orderId) {
    await fetch(`${API_BASE}/restaurant/orders/${orderId}/preparing`, { method: 'POST' });
    loadRestaurantOrders();
}

async function markReadyRestOrder(orderId) {
    const res = await fetch(`${API_BASE}/restaurant/orders/${orderId}/ready`, { method: 'POST' });
    if (res.ok) {
        alert(`Order #${orderId} is now marked READY_FOR_PICKUP. ML ETA estimated and persisted!`);
    }
    loadRestaurantOrders();
}

// ================= ADMIN DISPATCH & BFS MODULE =================
async function loadAdminDashboard() {
    try {
        const [graphRes, readyRes] = await Promise.all([
            fetch(`${API_BASE}/admin/dispatch/zones-graph`),
            fetch(`${API_BASE}/admin/dispatch/ready-orders`)
        ]);

        const graph = await graphRes.json();
        const readyOrders = await readyRes.json();

        // Render Graph Nodes
        const grid = document.getElementById('admin-zone-grid');
        grid.innerHTML = graph.nodes.map(node => `
            <div class="zone-box" id="zone-box-${node.zoneCode}">
                <div class="zone-header">
                    <span class="zone-code-tag">${node.zoneCode}</span>
                    <span class="rider-count-tag ${node.availableRidersCount > 0 ? 'has-riders' : ''}">
                        <i class="fa-solid fa-motorcycle"></i> ${node.availableRidersCount} Available
                    </span>
                </div>
                <div style="font-size: 0.85rem; font-weight: 700;">${node.name}</div>
                <div style="font-size: 0.75rem; color: var(--text-muted); margin-top: 0.4rem;">
                    ${node.riderNames.length > 0 ? node.riderNames.join('<br>') : 'No idle riders in zone'}
                </div>
            </div>
        `).join('');

        // Render Ready Orders
        const readyContainer = document.getElementById('admin-ready-orders');
        if (readyOrders.length === 0) {
            readyContainer.innerHTML = `<p style="color: var(--text-muted); font-size: 0.9rem;">No orders currently waiting for dispatch. Mark orders as "Ready for Pickup" in Restaurant tab.</p>`;
            return;
        }

        readyContainer.innerHTML = readyOrders.map(ord => `
            <div style="border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 0.85rem; display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <div style="font-weight: 800;">Order #${ord.id} – ${ord.restaurant.name}</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">
                        Pickup: <strong>${ord.restaurant.deliveryZone.zoneCode}</strong> → Drop: <strong>${ord.deliveryZone.zoneCode}</strong>
                    </div>
                </div>
                <button class="btn btn-primary btn-sm" onclick="executeBfsForOrder(${ord.id}, '${ord.restaurant.deliveryZone.zoneCode}')">
                    <i class="fa-solid fa-network-wired"></i> Run BFS Assignment
                </button>
            </div>
        `).join('');
    } catch (err) {
        console.error('Error loading admin dispatch:', err);
    }
}

async function executeBfsForOrder(orderId, startZoneCode) {
    const consoleBox = document.getElementById('admin-bfs-console');
    consoleBox.innerHTML = `
        <div class="trace-line highlight">Initiating Breadth-First Search (BFS) for Order #${orderId}...</div>
        <div class="trace-line">Source Vertex (Restaurant Zone): ${startZoneCode}</div>
        <div class="trace-line">Initializing FIFO Queue Q = [ ${startZoneCode} ], Visited Set = { ${startZoneCode} }</div>
    `;

    // Visual pulse
    document.querySelectorAll('.zone-box').forEach(b => b.className = 'zone-box');
    const startBox = document.getElementById(`zone-box-${startZoneCode}`);
    if (startBox) startBox.classList.add('active-bfs');

    try {
        const res = await fetch(`${API_BASE}/admin/dispatch/${orderId}/run-bfs`, { method: 'POST' });
        const result = await res.json();

        if (result.success) {
            let traceHtml = consoleBox.innerHTML;
            (result.traversalPath || []).forEach(step => {
                traceHtml += `<div class="trace-line">→ Dequeued: <strong>${step}</strong> | Checking available riders in zone...</div>`;
            });

            traceHtml += `
                <div class="trace-line success">✓ MATCH FOUND! Nearest Available Rider: ${result.riderName} (${result.vehicleType})</div>
                <div class="trace-line success">✓ Located in Zone: ${result.foundZone} at Hop Distance: ${result.hopDistance}</div>
                <div class="trace-line highlight">✓ Order #${orderId} status advanced to RIDER_ASSIGNED.</div>
            `;
            consoleBox.innerHTML = traceHtml;

            // Highlight destination zone
            const foundBox = document.getElementById(`zone-box-${result.foundZone}`);
            if (foundBox) foundBox.classList.add('found-rider');

            alert(`BFS Completed!\nAssigned Rider: ${result.riderName}\nFound in Zone: ${result.foundZone} (Hop Distance: ${result.hopDistance})`);
            loadAdminDashboard();
        } else {
            consoleBox.innerHTML += `<div class="trace-line" style="color: #f87171;">✗ BFS Traversal Exhausted: ${result.message}</div>`;
            alert('BFS Result: ' + result.message);
        }
    } catch (err) {
        console.error('Error executing BFS:', err);
    }
}

// ================= RIDER MODULE =================
async function loadRiderOrders() {
    const riderSelect = document.getElementById('rider-select');
    const riderId = riderSelect ? riderSelect.value : 1;
    if (!riderId) return;

    try {
        const res = await fetch(`${API_BASE}/rider/${riderId}/assigned-orders`);
        const orders = await res.json();
        const container = document.getElementById('rider-orders-list');

        if (orders.length === 0) {
            container.innerHTML = `<p style="color: var(--text-muted); padding: 1rem;">No orders currently assigned to this rider. Run BFS in Admin tab to assign an order.</p>`;
            return;
        }

        container.innerHTML = orders.map(ord => {
            let actionBtn = '';
            if (ord.status === 'RIDER_ASSIGNED') {
                actionBtn = `<button class="btn btn-warning btn-sm" onclick="riderPickupOrder(${ord.id}, ${riderId})"><i class="fa-solid fa-box"></i> Mark Picked Up</button>`;
            } else if (ord.status === 'PICKED_UP') {
                actionBtn = `<button class="btn btn-primary btn-sm" onclick="riderOutForDelivery(${ord.id}, ${riderId})"><i class="fa-solid fa-bicycle"></i> Out for Delivery</button>`;
            } else if (ord.status === 'OUT_FOR_DELIVERY') {
                actionBtn = `<button class="btn btn-success btn-sm" onclick="riderDeliverOrder(${ord.id}, ${riderId})"><i class="fa-solid fa-circle-check"></i> Mark Delivered</button>`;
            } else if (ord.status === 'DELIVERED') {
                actionBtn = `<span style="font-weight: 700; color: var(--success-green); font-size: 0.85rem;"><i class="fa-solid fa-check-double"></i> Delivered</span>`;
            }

            return `
                <div style="border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 1rem; display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap;">
                    <div>
                        <div style="display: flex; align-items: center; gap: 0.75rem;">
                            <span style="font-weight: 800; font-size: 1.05rem;">Order #${ord.id}</span>
                            <span class="status-pill status-${ord.status}">${ord.status.replace(/_/g, ' ')}</span>
                        </div>
                        <div style="font-size: 0.85rem; margin-top: 0.35rem;">
                            <strong>Pickup From:</strong> ${ord.restaurant.name} (Zone ${ord.restaurant.deliveryZone.zoneCode}, ${ord.restaurant.address})
                        </div>
                        <div style="font-size: 0.85rem; margin-top: 0.2rem;">
                            <strong>Deliver To:</strong> ${ord.customer.name} (Zone ${ord.deliveryZone.zoneCode}, ${ord.customer.address})
                        </div>
                    </div>
                    <div style="display: flex; gap: 0.5rem; align-items: center;">
                        ${actionBtn}
                        <button class="btn btn-outline btn-sm" onclick="trackOrder(${ord.id})"><i class="fa-solid fa-eye"></i></button>
                    </div>
                </div>
            `;
        }).join('');
    } catch (err) {
        console.error('Error loading rider orders:', err);
    }
}

async function riderPickupOrder(orderId, riderId) {
    await fetch(`${API_BASE}/rider/orders/${orderId}/pickup?riderId=${riderId}`, { method: 'POST' });
    loadRiderOrders();
}

async function riderOutForDelivery(orderId, riderId) {
    await fetch(`${API_BASE}/rider/orders/${orderId}/out-for-delivery?riderId=${riderId}`, { method: 'POST' });
    loadRiderOrders();
}

async function riderDeliverOrder(orderId, riderId) {
    await fetch(`${API_BASE}/rider/orders/${orderId}/deliver?riderId=${riderId}`, { method: 'POST' });
    alert(`Order #${orderId} successfully delivered! Rider is now available for new orders.`);
    loadRiderOrders();
}

// ================= TRACKING MODULE =================
function trackOrderManual() {
    const id = document.getElementById('tracking-input-id').value;
    if (id) trackOrder(id);
}

async function trackOrder(orderId) {
    switchTab('tracking');
    document.getElementById('tracking-input-id').value = orderId;

    try {
        const res = await fetch(`${API_BASE}/tracking/${orderId}`);
        if (!res.ok) {
            document.getElementById('tracking-content').innerHTML = `
                <div class="card" style="text-align: center; padding: 2rem;">
                    <p style="color: var(--danger-red); font-weight: 700;">Order #${orderId} not found.</p>
                </div>
            `;
            return;
        }

        const data = await res.json();
        const ord = data.order;
        const eta = data.etaPrediction;

        // Steps array for Stepper
        const steps = [
            'PLACED', 'CONFIRMED', 'PREPARING', 'READY_FOR_PICKUP',
            'RIDER_ASSIGNED', 'PICKED_UP', 'OUT_FOR_DELIVERY', 'DELIVERED'
        ];
        const currentIdx = steps.indexOf(ord.status);

        const stepperHtml = `
            <div class="stepper">
                ${steps.map((st, i) => {
                    let cls = '';
                    if (i < currentIdx) cls = 'completed';
                    else if (i === currentIdx) cls = 'active';
                    return `
                        <div class="step-item ${cls}">
                            <div class="step-circle">${i < currentIdx ? '<i class="fa-solid fa-check"></i>' : (i + 1)}</div>
                            <span class="step-label">${st.replace(/_/g, ' ')}</span>
                        </div>
                    `;
                }).join('')}
            </div>
        `;

        const etaHtml = eta ? `
            <div class="eta-card">
                <div>
                    <div style="font-size: 0.85rem; text-transform: uppercase; letter-spacing: 0.5px; opacity: 0.85;">
                        <i class="fa-solid fa-brain"></i> Python ML Regression Prediction
                    </div>
                    <div class="eta-number">${eta.predictedMins} Mins</div>
                    <div style="font-size: 0.85rem; opacity: 0.9; margin-top: 0.25rem;">
                        Estimated Delivery Time (Distance: ${eta.distanceKm} km, Hour: ${eta.hourOfDay}:00)
                    </div>
                </div>
                <div style="text-align: right;">
                    <div style="font-size: 2.5rem; opacity: 0.4;"><i class="fa-solid fa-stopwatch"></i></div>
                    <span style="font-size: 0.75rem; background: rgba(255,255,255,0.2); padding: 0.2rem 0.5rem; border-radius: 12px;">
                        Multiple Linear Regression
                    </span>
                </div>
            </div>
        ` : '';

        const riderHtml = ord.rider ? `
            <div class="card" style="margin-top: 1rem;">
                <div class="card-title"><i class="fa-solid fa-id-badge"></i> Assigned Rider Details (Discovered via BFS)</div>
                <div style="display: flex; gap: 1.5rem; align-items: center; margin-top: 0.75rem;">
                    <div style="width: 50px; height: 50px; border-radius: 50%; background: var(--primary-blue-light); color: var(--primary-blue); display: flex; align-items: center; justify-content: center; font-size: 1.5rem;">
                        <i class="fa-solid fa-motorcycle"></i>
                    </div>
                    <div>
                        <div style="font-weight: 800; font-size: 1.1rem;">${ord.rider.name}</div>
                        <div style="font-size: 0.85rem; color: var(--text-muted);">Vehicle: ${ord.rider.vehicleType} | Phone: ${ord.rider.phone}</div>
                        <div style="font-size: 0.8rem; color: var(--primary-blue); font-weight: 600; margin-top: 0.2rem;">
                            Current Zone: ${ord.rider.currentZone.zoneCode} (${ord.rider.currentZone.name})
                        </div>
                    </div>
                </div>
            </div>
        ` : '';

        document.getElementById('tracking-content').innerHTML = `
            ${etaHtml}

            <div class="card" style="margin-bottom: 1rem;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                    <div>
                        <h3 style="font-size: 1.25rem; font-weight: 800;">Order #${ord.id}</h3>
                        <p style="font-size: 0.85rem; color: var(--text-muted);">From <strong>${ord.restaurant.name}</strong> to <strong>${ord.customer.name}</strong></p>
                    </div>
                    <span class="status-pill status-${ord.status}">${ord.status.replace(/_/g, ' ')}</span>
                </div>

                ${stepperHtml}

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; border-top: 1px solid var(--border-color); padding-top: 1rem; font-size: 0.9rem;">
                    <div>
                        <strong>Pickup:</strong> ${ord.restaurant.address} (Zone ${ord.restaurant.deliveryZone.zoneCode})<br>
                        <strong>Drop-off:</strong> ${ord.customer.address} (Zone ${ord.deliveryZone.zoneCode})
                    </div>
                    <div style="text-align: right;">
                        <strong>Total Bill:</strong> ₹${Number(ord.totalAmount).toFixed(2)}<br>
                        <strong>Transit Distance:</strong> ${ord.distanceKm} km
                    </div>
                </div>
            </div>

            ${riderHtml}
        `;
    } catch (err) {
        console.error('Error tracking order:', err);
    }
}
