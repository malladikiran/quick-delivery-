package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.ArrayDeque
import kotlin.math.cos
import kotlin.math.sin

// --- Domain Models ---
enum class OrderState(val label: String, val color: Color, val progressPct: Float) {
    PLACED("Placed", Color(0xFF94A3B8), 0.05f),
    CONFIRMED("Confirmed", NeonBlue, 0.15f),
    PREPARING("Preparing", NeonOrange, 0.30f),
    READY_FOR_PICKUP("Ready for Pickup", Color(0xFFF59E0B), 0.45f),
    RIDER_ASSIGNED("Rider Dispatched", NeonPurple, 0.60f),
    PICKED_UP("Order Picked Up", NeonCyan, 0.75f),
    OUT_FOR_DELIVERY("Out for Delivery", Color(0xFF38BDF8), 0.90f),
    DELIVERED("Delivered", NeonEmerald, 1.0f)
}

data class FoodDish(
    val id: Long,
    val name: String,
    val desc: String,
    val price: Double,
    @DrawableRes val imageRes: Int,
    val rating: String,
    val prepTime: String
)

data class CartItem(val dish: FoodDish, var quantity: Int)

data class RiderInfo(
    val id: Long,
    val name: String,
    val phone: String,
    val vehicle: String,
    var currentZone: String,
    var isAvailable: Boolean
)

data class DeliveryOrder(
    val id: Long,
    val restaurantName: String,
    val restaurantZone: String,
    val customerName: String,
    val customerZone: String,
    val items: List<CartItem>,
    val totalAmount: Double,
    val distanceKm: Double,
    var status: OrderState = OrderState.PLACED,
    var assignedRider: RiderInfo? = null,
    var predictedEtaMins: Int? = null,
    var bfsTrace: List<String> = emptyList(),
    var bfsHops: Int = 0
)

data class ZoneNode(
    val code: String,
    val name: String,
    val normX: Float,
    val normY: Float
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GlassQuickDeliverApp()
            }
        }
    }
}

// ================= GLASSMORPHIC CONTAINER HELPER =================
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    containerColor: Color = Color(0xFF1E293B).copy(alpha = 0.45f),
    borderColor: Color = Color.White.copy(alpha = 0.22f),
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor)
            .border(
                borderWidth,
                Brush.linearGradient(
                    listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.05f)
                    )
                ),
                RoundedCornerShape(cornerRadius)
            )
            .padding(1.dp),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassQuickDeliverApp() {
    var currentTab by remember { mutableIntStateOf(0) }
    var showPersonaDialog by remember { mutableStateOf(false) }

    // Active persona
    var selectedPersonaRole by remember { mutableStateOf("CUSTOMER") }
    var selectedPersonaName by remember { mutableStateOf("Rahul Sharma (Z4)") }

    // City Zones Topology G = (V, E)
    val zoneNodes = remember {
        listOf(
            ZoneNode("Z1", "Downtown Central", 0.30f, 0.20f),
            ZoneNode("Z2", "Tech Corridor", 0.72f, 0.20f),
            ZoneNode("Z3", "University Campus", 0.25f, 0.55f),
            ZoneNode("Z4", "South Hills", 0.75f, 0.55f),
            ZoneNode("Z5", "Market Square", 0.40f, 0.82f),
            ZoneNode("Z6", "Eastern Suburbs", 0.80f, 0.85f)
        )
    }

    val graphNeighbors = remember {
        mapOf(
            "Z1" to listOf("Z2", "Z3"),
            "Z2" to listOf("Z1", "Z4"),
            "Z3" to listOf("Z1", "Z4", "Z5"),
            "Z4" to listOf("Z2", "Z3", "Z5"),
            "Z5" to listOf("Z3", "Z4", "Z6"),
            "Z6" to listOf("Z5")
        )
    }

    val riders = remember {
        mutableStateListOf(
            RiderInfo(1, "Karan Verma", "9876543230", "Yamaha FZ (Bike)", "Z2", isAvailable = true),
            RiderInfo(2, "Anita Roy", "9876543231", "Honda Activa (Scooter)", "Z4", isAvailable = true),
            RiderInfo(3, "Vikram Singh", "9876543232", "Bajaj Pulsar (Bike)", "Z5", isAvailable = true)
        )
    }

    // Food dishes with generated image drawables
    val dishesSpiceKitchen = remember {
        listOf(
            FoodDish(
                id = 1,
                name = "Paneer Butter Masala",
                desc = "Cottage cheese cubes simmering in velvety spiced tomato cashew gravy",
                price = 280.0,
                imageRes = R.drawable.img_paneer_butter_masala,
                rating = "4.9 ★",
                prepTime = "18 mins"
            ),
            FoodDish(
                id = 2,
                name = "Butter Naan (2 pcs)",
                desc = "Clay oven tandoor baked flatbread brushed with farm-fresh butter",
                price = 90.0,
                imageRes = R.drawable.img_butter_naan,
                rating = "4.8 ★",
                prepTime = "10 mins"
            ),
            FoodDish(
                id = 3,
                name = "Hyderabadi Dum Biryani",
                desc = "Aromatic long basmati rice layered with saffron, vegetables and caramelized onions",
                price = 320.0,
                imageRes = R.drawable.img_biryani,
                rating = "4.9 ★",
                prepTime = "25 mins"
            ),
            FoodDish(
                id = 4,
                name = "Gulab Jamun (2 pcs)",
                desc = "Warm fried milk dumplings drenched in cardamom and saffron sugar syrup",
                price = 80.0,
                imageRes = R.drawable.img_gulab_jamun,
                rating = "5.0 ★",
                prepTime = "8 mins"
            )
        )
    }

    val cartItems = remember { mutableStateListOf<CartItem>() }

    val orders = remember {
        mutableStateListOf(
            DeliveryOrder(
                id = 1,
                restaurantName = "The Spice Kitchen",
                restaurantZone = "Z1",
                customerName = "Rahul Sharma",
                customerZone = "Z4",
                items = listOf(
                    CartItem(dishesSpiceKitchen[0], 1),
                    CartItem(dishesSpiceKitchen[1], 2)
                ),
                totalAmount = 460.0,
                distanceKm = 5.6,
                status = OrderState.READY_FOR_PICKUP,
                predictedEtaMins = 33
            )
        )
    }

    var trackingOrderId by remember { mutableLongStateOf(1L) }
    val bfsConsoleLogs = remember { mutableStateListOf<String>() }
    var selectedMapZone by remember { mutableStateOf<String?>("Z1") }

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Ambient Glassmorphism Mesh Gradient Background
                drawRect(Color(0xFF090D18))
                // Glow Orb 1 (Top Left - Cyan)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.10f),
                        radius = size.width * 0.65f
                    )
                )
                // Glow Orb 2 (Top Right - Electric Blue)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.25f),
                        radius = size.width * 0.70f
                    )
                )
                // Glow Orb 3 (Center Left - Purple)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.20f, size.height * 0.60f),
                        radius = size.width * 0.60f
                    )
                )
                // Glow Orb 4 (Bottom Right - Emerald)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF10B981).copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.80f, size.height * 0.85f),
                        radius = size.width * 0.65f
                    )
                )
            },
        containerColor = Color.Transparent,
        topBar = {
            // Glassmorphic Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                GlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    containerColor = Color(0xFF0F172A).copy(alpha = 0.65f),
                    borderColor = Color.White.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(NeonCyan, NeonBlue))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = "Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "QuickDeliver",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                                Text(
                                    "Glass • ADSA BFS & Live Map",
                                    fontSize = 11.sp,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Glass Persona Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.80f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.20f)
                            ),
                            modifier = Modifier.clickable { showPersonaDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NeonEmerald)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    selectedPersonaRole,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Floating Glass Dock Navigation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                GlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp,
                    containerColor = Color(0xFF0F172A).copy(alpha = 0.78f),
                    borderColor = Color.White.copy(alpha = 0.28f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val navTabs = listOf(
                            Triple("Menu", Icons.Default.RestaurantMenu, 0),
                            Triple("Kitchen", Icons.Default.SoupKitchen, 1),
                            Triple("Live Map", Icons.Default.Navigation, 2),
                            Triple("BFS Dispatch", Icons.Default.Hub, 3),
                            Triple("Rider", Icons.Default.TwoWheeler, 4)
                        )

                        navTabs.forEach { (label, icon, index) ->
                            val isSelected = currentTab == index
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { currentTab = index }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 36.dp else 28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color.Transparent
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = label,
                                        tint = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.55f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.50f)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> GlassCustomerScreen(
                    dishes = dishesSpiceKitchen,
                    cartItems = cartItems,
                    onAddToCart = { dish ->
                        val existing = cartItems.find { it.dish.id == dish.id }
                        if (existing != null) existing.quantity++
                        else cartItems.add(CartItem(dish, 1))
                    },
                    onRemoveFromCart = { dish ->
                        val existing = cartItems.find { it.dish.id == dish.id }
                        if (existing != null) {
                            if (existing.quantity > 1) existing.quantity--
                            else cartItems.remove(existing)
                        }
                    },
                    onPlaceOrder = {
                        val newId = (orders.maxOfOrNull { it.id } ?: 0L) + 1L
                        val newOrder = DeliveryOrder(
                            id = newId,
                            restaurantName = "The Spice Kitchen",
                            restaurantZone = "Z1",
                            customerName = "Rahul Sharma",
                            customerZone = "Z4",
                            items = cartItems.toList(),
                            totalAmount = cartItems.sumOf { it.dish.price * it.quantity },
                            distanceKm = 5.6,
                            status = OrderState.PLACED
                        )
                        orders.add(0, newOrder)
                        cartItems.clear()
                        trackingOrderId = newId
                        currentTab = 2 // Switch to Live Map & Track
                    },
                    orders = orders,
                    onTrackOrder = { id ->
                        trackingOrderId = id
                        currentTab = 2
                    }
                )
                1 -> GlassKitchenScreen(
                    orders = orders,
                    onConfirm = { ord -> ord.status = OrderState.CONFIRMED },
                    onPreparing = { ord -> ord.status = OrderState.PREPARING },
                    onReady = { ord ->
                        ord.status = OrderState.READY_FOR_PICKUP
                        // Multiple Linear Regression Formula matching Python service
                        val hour = 20
                        val trafficMult = if (hour in 19..22) 1.50 else 1.05
                        val etaMins = (10.0 + (ord.distanceKm * 2.4) * trafficMult).toInt()
                        ord.predictedEtaMins = etaMins
                    },
                    onTrack = { id ->
                        trackingOrderId = id
                        currentTab = 2
                    }
                )
                2 -> GlassLiveMapAndTrackScreen(
                    order = orders.find { it.id == trackingOrderId } ?: orders.firstOrNull(),
                    zoneNodes = zoneNodes,
                    graphNeighbors = graphNeighbors,
                    riders = riders,
                    radarPulse = radarPulse,
                    selectedZone = selectedMapZone,
                    onSelectZone = { selectedMapZone = it },
                    orders = orders,
                    onSelectOrder = { trackingOrderId = it }
                )
                3 -> GlassBfsDispatchScreen(
                    orders = orders.filter { it.status == OrderState.READY_FOR_PICKUP },
                    riders = riders,
                    zoneNodes = zoneNodes,
                    graphNeighbors = graphNeighbors,
                    consoleLogs = bfsConsoleLogs,
                    radarPulse = radarPulse,
                    onRunBfs = { order ->
                        // Authentic Breadth First Search (BFS)
                        bfsConsoleLogs.clear()
                        bfsConsoleLogs.add("==========================================")
                        bfsConsoleLogs.add("INITIATING BFS SEARCH FOR ORDER #${order.id}")
                        bfsConsoleLogs.add("Pickup Vertex (Restaurant Zone): ${order.restaurantZone}")

                        val startZone = order.restaurantZone
                        val queue = ArrayDeque<String>()
                        val visited = mutableSetOf<String>()
                        val hopDistances = mutableMapOf<String, Int>()
                        val traversalSteps = mutableListOf<String>()

                        queue.add(startZone)
                        visited.add(startZone)
                        hopDistances[startZone] = 0

                        var foundRider: RiderInfo? = null
                        var foundInZone: String? = null

                        while (queue.isNotEmpty()) {
                            val currentZone = queue.poll()
                            val hops = hopDistances[currentZone] ?: 0
                            val stepDesc = "$currentZone (hop $hops)"
                            traversalSteps.add(stepDesc)
                            bfsConsoleLogs.add("→ Dequeued: $stepDesc | Checking idle riders in $currentZone...")

                            val candidate = riders.firstOrNull { it.currentZone == currentZone && it.isAvailable }
                            if (candidate != null) {
                                foundRider = candidate
                                foundInZone = currentZone
                                bfsConsoleLogs.add("✓ MATCH DISCOVERED! Rider: ${candidate.name} in $currentZone (Hop: $hops)")
                                break
                            }

                            val neighbors = graphNeighbors[currentZone] ?: emptyList()
                            for (nbr in neighbors) {
                                if (nbr !in visited) {
                                    visited.add(nbr)
                                    hopDistances[nbr] = hops + 1
                                    queue.add(nbr)
                                    bfsConsoleLogs.add("  - Enqueued neighbor: $nbr at hop ${hops + 1}")
                                }
                            }
                        }

                        if (foundRider != null && foundInZone != null) {
                            foundRider.isAvailable = false
                            order.assignedRider = foundRider
                            order.status = OrderState.RIDER_ASSIGNED
                            order.bfsTrace = traversalSteps
                            order.bfsHops = hopDistances[foundInZone] ?: 0
                            bfsConsoleLogs.add("✓ Order #${order.id} status updated to RIDER_ASSIGNED")
                            bfsConsoleLogs.add("==========================================")
                        } else {
                            bfsConsoleLogs.add("✗ BFS exhausted. No available riders found in graph.")
                            bfsConsoleLogs.add("==========================================")
                        }
                    }
                )
                4 -> GlassRiderScreen(
                    riders = riders,
                    orders = orders,
                    onPickup = { ord -> ord.status = OrderState.PICKED_UP },
                    onOutForDelivery = { ord -> ord.status = OrderState.OUT_FOR_DELIVERY },
                    onDelivered = { ord ->
                        ord.status = OrderState.DELIVERED
                        ord.assignedRider?.let { r ->
                            r.isAvailable = true
                            r.currentZone = ord.customerZone
                        }
                    },
                    onTrack = { id ->
                        trackingOrderId = id
                        currentTab = 2
                    }
                )
            }
        }
    }

    if (showPersonaDialog) {
        AlertDialog(
            onDismissRequest = { showPersonaDialog = false },
            containerColor = Color(0xFF0F172A).copy(alpha = 0.95f),
            title = {
                Text(
                    "Switch User Persona",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Test role-specific workflows:",
                        fontSize = 12.sp,
                        color = GlassTextSecondary
                    )
                    listOf(
                        Triple("Customer (Rahul - Z4)", "CUSTOMER", 0),
                        Triple("Kitchen (The Spice Kitchen - Z1)", "RESTAURANT", 1),
                        Triple("Live Radar Map & Tracker", "CUSTOMER", 2),
                        Triple("Admin (BFS Dispatch Engine)", "ADMIN (BFS)", 3),
                        Triple("Rider (Anita Roy - Z4)", "RIDER", 4)
                    ).forEach { (lbl, role, tab) ->
                        Button(
                            onClick = {
                                selectedPersonaRole = role
                                currentTab = tab
                                showPersonaDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(lbl, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPersonaDialog = false }) {
                    Text("Close", color = NeonCyan)
                }
            }
        )
    }
}

// ================= TAB 0: GLASS CUSTOMER SCREEN =================
@Composable
fun GlassCustomerScreen(
    dishes: List<FoodDish>,
    cartItems: List<CartItem>,
    onAddToCart: (FoodDish) -> Unit,
    onRemoveFromCart: (FoodDish) -> Unit,
    onPlaceOrder: () -> Unit,
    orders: List<DeliveryOrder>,
    onTrackOrder: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Restaurant Hero Glass Card
            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "The Spice Kitchen",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Downtown Central • Zone Z1",
                                fontSize = 12.sp,
                                color = NeonCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeonEmerald.copy(alpha = 0.20f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.40f))
                        ) {
                            Text(
                                "OPEN NOW",
                                color = NeonEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Authentic North Indian specialties prepared fresh with heritage clay-oven tandoor recipes.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Signature Menu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    "4 Chef Specials",
                    fontSize = 12.sp,
                    color = GlassTextSecondary
                )
            }
        }

        // Food Items with generated photography
        items(dishes) { dish ->
            val inCart = cartItems.find { it.dish.id == dish.id }

            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                containerColor = Color(0xFF1E293B).copy(alpha = 0.55f),
                borderColor = Color.White.copy(alpha = 0.25f)
            ) {
                Column {
                    // Food Image with gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    ) {
                        Image(
                            painter = painterResource(id = dish.imageRes),
                            contentDescription = dish.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Gradient shadow at bottom of image
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color(0xFF0F172A).copy(alpha = 0.70f)
                                        )
                                    )
                                )
                        )
                        // Floating Badges on Image
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.60f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(dish.rating, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.60f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(dish.prepTime, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    // Food details & action
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    dish.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    dish.desc,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.70f),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Glass Price Pill
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.70f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    "₹${dish.price.toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonCyan,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }

                            // Stepper or Add button
                            if (inCart == null) {
                                Button(
                                    onClick = { onAddToCart(dish) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonBlue
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("add_item_${dish.id}")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add to Cart", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            } else {
                                GlassBox(
                                    cornerRadius = 14.dp,
                                    containerColor = Color(0xFF0F172A).copy(alpha = 0.80f),
                                    borderColor = NeonBlue.copy(alpha = 0.50f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { onRemoveFromCart(dish) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Minus", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            "${inCart.quantity}",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        )
                                        IconButton(
                                            onClick = { onAddToCart(dish) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Plus", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cart Drawer Glass Card
        if (cartItems.isNotEmpty()) {
            item {
                GlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    containerColor = Color(0xFF1E1B4B).copy(alpha = 0.65f),
                    borderColor = NeonPurple.copy(alpha = 0.40f)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Your Cart", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("${cartItems.sumOf { it.quantity }} items", fontSize = 12.sp, color = NeonCyan)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        cartItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${item.dish.name} × ${item.quantity}", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                                Text("₹${(item.dish.price * item.quantity).toInt()}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            }
                        }
                        Divider(
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total (Drop to Zone Z4):", color = Color.White, fontWeight = FontWeight.Medium)
                            Text(
                                "₹${cartItems.sumOf { it.dish.price * it.quantity }.toInt()}",
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonEmerald,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onPlaceOrder,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("place_order_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Place Order & Open Live Map", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Recent Orders
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Active & Past Orders", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
        }

        items(orders) { ord ->
            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Order #${ord.id} • ${ord.restaurantName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("₹${ord.totalAmount.toInt()} • Pickup: ${ord.restaurantZone} → Drop: ${ord.customerZone}", fontSize = 12.sp, color = GlassTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ord.status.color.copy(alpha = 0.20f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ord.status.color.copy(alpha = 0.40f))
                        ) {
                            Text(
                                ord.status.label,
                                color = ord.status.color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Button(
                        onClick = { onTrackOrder(ord.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Map", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ================= TAB 1: GLASS KITCHEN SCREEN =================
@Composable
fun GlassKitchenScreen(
    orders: List<DeliveryOrder>,
    onConfirm: (DeliveryOrder) -> Unit,
    onPreparing: (DeliveryOrder) -> Unit,
    onReady: (DeliveryOrder) -> Unit,
    onTrack: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassBox(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NeonOrange.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SoupKitchen, contentDescription = null, tint = NeonOrange, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Kitchen Display System", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.White)
                        Text("Live order preparation state machine", fontSize = 12.sp, color = GlassTextSecondary)
                    }
                }
            }
        }

        items(orders) { ord ->
            GlassBox(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Order #${ord.id}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ord.status.color.copy(alpha = 0.20f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ord.status.color.copy(alpha = 0.40f))
                        ) {
                            Text(
                                ord.status.label,
                                color = ord.status.color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Customer: ${ord.customerName} (Zone ${ord.customerZone})", fontSize = 13.sp, color = NeonCyan)
                    Text(
                        "Dishes: ${ord.items.joinToString { "${it.dish.name} × ${it.quantity}" }}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (ord.status) {
                            OrderState.PLACED -> {
                                Button(
                                    onClick = { onConfirm(ord) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Accept Order")
                                }
                            }
                            OrderState.CONFIRMED -> {
                                Button(
                                    onClick = { onPreparing(ord) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrange),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Start Preparing")
                                }
                            }
                            OrderState.PREPARING -> {
                                Button(
                                    onClick = { onReady(ord) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ready for Pickup (Run ML ETA)")
                                }
                            }
                            else -> {
                                Text("Kitchen processing complete", color = NeonEmerald, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = { onTrack(ord.id) },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text("View Map", color = Color.White)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ================= TAB 2: GLASS LIVE RADAR MAP & TRACKER =================
@Composable
fun GlassLiveMapAndTrackScreen(
    order: DeliveryOrder?,
    zoneNodes: List<ZoneNode>,
    graphNeighbors: Map<String, List<String>>,
    riders: List<RiderInfo>,
    radarPulse: Float,
    selectedZone: String?,
    onSelectZone: (String) -> Unit,
    orders: List<DeliveryOrder>,
    onSelectOrder: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Order selector pills if multiple
        if (orders.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(orders) { ord ->
                        val isSel = ord.id == order?.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) NeonBlue.copy(alpha = 0.35f) else Color(0xFF1E293B).copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonBlue else Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable { onSelectOrder(ord.id) }
                        ) {
                            Text(
                                "Order #${ord.id} (${ord.status.label})",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Live Delivery Radar Canvas Map
        item {
            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                containerColor = Color(0xFF0F172A).copy(alpha = 0.75f),
                borderColor = NeonCyan.copy(alpha = 0.35f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(NeonEmerald)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "LIVE CITY DELIVERY RADAR",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                        Text("Interactive 6-Zone Graph", fontSize = 11.sp, color = NeonCyan)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Map Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF060A14))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val w = size.width
                                    val h = size.height
                                    val tapped = zoneNodes.minByOrNull { z ->
                                        val zX = z.normX * w
                                        val zY = z.normY * h
                                        val dx = offset.x - zX
                                        val dy = offset.y - zY
                                        dx * dx + dy * dy
                                    }
                                    tapped?.let { onSelectZone(it.code) }
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // 1. Draw subtle background radar grid
                            val gridColor = Color(0x1A64748B)
                            for (i in 1..4) {
                                drawLine(gridColor, Offset(0f, h * i / 5), Offset(w, h * i / 5), strokeWidth = 1f)
                                drawLine(gridColor, Offset(w * i / 5, 0f), Offset(w * i / 5, h), strokeWidth = 1f)
                            }

                            // 2. Draw graph road edges
                            val drawnEdges = mutableSetOf<Pair<String, String>>()
                            graphNeighbors.forEach { (sourceCode, targets) ->
                                val srcNode = zoneNodes.first { it.code == sourceCode }
                                val srcPos = Offset(srcNode.normX * w, srcNode.normY * h)
                                targets.forEach { tgtCode ->
                                    val edgeKey = if (sourceCode < tgtCode) sourceCode to tgtCode else tgtCode to sourceCode
                                    if (!drawnEdges.contains(edgeKey)) {
                                        drawnEdges.add(edgeKey)
                                        val tgtNode = zoneNodes.first { it.code == tgtCode }
                                        val tgtPos = Offset(tgtNode.normX * w, tgtNode.normY * h)

                                        // Check if this edge is on active delivery path (e.g. Z1-Z2-Z4)
                                        val isPathEdge = (order != null &&
                                                ((sourceCode == order.restaurantZone && tgtCode == "Z2") ||
                                                        (sourceCode == "Z2" && tgtCode == order.customerZone) ||
                                                        (sourceCode == order.restaurantZone && tgtCode == order.customerZone)))

                                        if (isPathEdge) {
                                            // Glowing active route
                                            drawLine(
                                                brush = Brush.linearGradient(listOf(NeonOrange, NeonEmerald)),
                                                start = srcPos,
                                                end = tgtPos,
                                                strokeWidth = 6f,
                                                cap = StrokeCap.Round
                                            )
                                        } else {
                                            // Standard road network
                                            drawLine(
                                                color = Color(0x3394A3B8),
                                                start = srcPos,
                                                end = tgtPos,
                                                strokeWidth = 3f,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Draw Zone Nodes
                            zoneNodes.forEach { node ->
                                val pos = Offset(node.normX * w, node.normY * h)
                                val isPickup = order?.restaurantZone == node.code
                                val isDropoff = order?.customerZone == node.code
                                val isRiderZone = riders.any { it.currentZone == node.code && it.isAvailable }
                                val isSelected = selectedZone == node.code

                                // Concentric glow on selected / active nodes
                                if (isPickup || isDropoff || isSelected) {
                                    val pulseRadius = 24f + (radarPulse * 16f)
                                    val glowColor = when {
                                        isPickup -> NeonOrange.copy(alpha = (1f - radarPulse) * 0.45f)
                                        isDropoff -> NeonEmerald.copy(alpha = (1f - radarPulse) * 0.45f)
                                        else -> NeonCyan.copy(alpha = (1f - radarPulse) * 0.45f)
                                    }
                                    drawCircle(glowColor, radius = pulseRadius, center = pos)
                                }

                                // Zone base circle
                                val nodeBaseColor = when {
                                    isPickup -> Color(0xFFEA580C)
                                    isDropoff -> Color(0xFF16A34A)
                                    isRiderZone -> Color(0xFF0284C7)
                                    else -> Color(0xFF1E293B)
                                }
                                drawCircle(nodeBaseColor, radius = 16f, center = pos)
                                drawCircle(Color.White.copy(alpha = 0.85f), radius = 16f, center = pos, style = Stroke(width = 2f))

                                // Rider idle count badge
                                val idleCount = riders.count { it.currentZone == node.code && it.isAvailable }
                                if (idleCount > 0) {
                                    drawCircle(NeonCyan, radius = 6f, center = Offset(pos.x + 12f, pos.y - 12f))
                                }
                            }

                            // 4. Live Rider marker animation along route
                            if (order?.assignedRider != null) {
                                val pickupNode = zoneNodes.first { it.code == order.restaurantZone }
                                val dropNode = zoneNodes.first { it.code == order.customerZone }
                                val riderStartNode = zoneNodes.firstOrNull { it.code == (order.assignedRider?.currentZone ?: "Z2") } ?: pickupNode

                                val pRider = Offset(riderStartNode.normX * w, riderStartNode.normY * h)
                                val pRest = Offset(pickupNode.normX * w, pickupNode.normY * h)
                                val pCust = Offset(dropNode.normX * w, dropNode.normY * h)

                                // Interpolate vehicle position based on status
                                val currentPos = when (order.status) {
                                    OrderState.RIDER_ASSIGNED -> Offset(
                                        pRider.x + (pRest.x - pRider.x) * (0.3f + radarPulse * 0.4f),
                                        pRider.y + (pRest.y - pRider.y) * (0.3f + radarPulse * 0.4f)
                                    )
                                    OrderState.PICKED_UP -> pRest
                                    OrderState.OUT_FOR_DELIVERY -> Offset(
                                        pRest.x + (pCust.x - pRest.x) * (0.2f + radarPulse * 0.6f),
                                        pRest.y + (pCust.y - pRest.y) * (0.2f + radarPulse * 0.6f)
                                    )
                                    OrderState.DELIVERED -> pCust
                                    else -> pRest
                                }

                                // Pulsing radar wave around vehicle
                                drawCircle(NeonCyan.copy(alpha = (1f - radarPulse) * 0.60f), radius = 30f + (radarPulse * 20f), center = currentPos)
                                drawCircle(Color(0xFF0F172A), radius = 14f, center = currentPos)
                                drawCircle(NeonCyan, radius = 14f, center = currentPos, style = Stroke(width = 3f))
                                drawCircle(Color.White, radius = 6f, center = currentPos)
                            }
                        }

                        // Overlay Zone Labels on Map
                        zoneNodes.forEach { node ->
                            val isPickup = order?.restaurantZone == node.code
                            val isDropoff = order?.customerZone == node.code
                            val labelColor = when {
                                isPickup -> NeonOrange
                                isDropoff -> NeonEmerald
                                else -> Color.White
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        start = (node.normX * 280).dp,
                                        top = (node.normY * 230).dp
                                    )
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        node.code,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = labelColor,
                                        fontSize = 11.sp
                                    )
                                    if (isPickup) {
                                        Text("🍽️ PICKUP", fontSize = 8.sp, color = NeonOrange, fontWeight = FontWeight.Bold)
                                    } else if (isDropoff) {
                                        Text("🏠 DROPOFF", fontSize = 8.sp, color = NeonEmerald, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Map Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NeonOrange))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pickup (Z1)", fontSize = 11.sp, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NeonEmerald))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dropoff (Z4)", fontSize = 11.sp, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NeonCyan))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live Rider (🛵)", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Selected Zone Detail Pill
        selectedZone?.let { zCode ->
            val node = zoneNodes.first { it.code == zCode }
            val idleRiders = riders.filter { it.currentZone == zCode && it.isAvailable }
            item {
                GlassBox(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Zone $zCode – ${node.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Connections: ${graphNeighbors[zCode]?.joinToString() ?: ""}", fontSize = 12.sp, color = GlassTextSecondary)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (idleRiders.isNotEmpty()) NeonEmerald.copy(alpha = 0.2f) else Color(0xFF334155),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (idleRiders.isNotEmpty()) NeonEmerald else Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(
                                "${idleRiders.size} Idle Rider${if (idleRiders.size != 1) "s" else ""}",
                                color = if (idleRiders.isNotEmpty()) NeonEmerald else Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // ML Regression ETA Card
        if (order != null) {
            item {
                GlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    containerColor = Color(0xFF1E1B4B).copy(alpha = 0.70f),
                    borderColor = NeonCyan.copy(alpha = 0.40f)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "PYTHON ML REGRESSION ETA",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${order.predictedEtaMins ?: 33} Mins",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 34.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Transit: ${order.distanceKm} km • Peak Dinner Traffic Multiplier",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            // Order Progress Stepper
            item {
                GlassBox(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Order #${order.id} Live Progress", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(14.dp))

                        val states = OrderState.values()
                        val currentIdx = states.indexOf(order.status)

                        states.forEachIndexed { index, state ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (index <= currentIdx) state.color else Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (index < currentIdx) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                    } else {
                                        Text("${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    state.label,
                                    fontWeight = if (index == currentIdx) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (index == currentIdx) Color.White else Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            }
                            if (index < states.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 11.dp)
                                        .width(2.dp)
                                        .height(14.dp)
                                        .background(if (index < currentIdx) states[index + 1].color.copy(alpha = 0.8f) else Color(0xFF334155))
                                )
                            }
                        }
                    }
                }
            }

            // Assigned Rider Card
            order.assignedRider?.let { rider ->
                item {
                    GlassBox(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(rider.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                Text("${rider.vehicle} • Ph: ${rider.phone}", fontSize = 12.sp, color = GlassTextSecondary)
                                Text("Assigned via BFS (Discovered in Zone ${rider.currentZone}, Hops: ${order.bfsHops})", fontSize = 11.sp, color = NeonEmerald, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ================= TAB 3: GLASS BFS DISPATCH SCREEN =================
@Composable
fun GlassBfsDispatchScreen(
    orders: List<DeliveryOrder>,
    riders: List<RiderInfo>,
    zoneNodes: List<ZoneNode>,
    graphNeighbors: Map<String, List<String>>,
    consoleLogs: List<String>,
    radarPulse: Float,
    onRunBfs: (DeliveryOrder) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassBox(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Graph Topology G = (V, E) Dispatcher", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.White)
                    Text("Breadth-First Search radial discovery of nearest delivery partner", fontSize = 12.sp, color = NeonCyan)
                }
            }
        }

        // Zone Fleet Status Grid
        item {
            Text("Zone Rider Fleet Status", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    listOf("Z1", "Z2", "Z3"),
                    listOf("Z4", "Z5", "Z6")
                ).forEach { rowZones ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowZones.forEach { zCode ->
                            val zoneRiders = riders.filter { it.currentZone == zCode && it.isAvailable }
                            GlassBox(
                                modifier = Modifier.weight(1f),
                                cornerRadius = 16.dp,
                                containerColor = Color(0xFF1E293B).copy(alpha = 0.60f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(zCode, fontWeight = FontWeight.ExtraBold, color = NeonCyan, fontSize = 14.sp)
                                        Text("${zoneRiders.size} idle", fontSize = 11.sp, color = if (zoneRiders.isNotEmpty()) NeonEmerald else Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Adj: ${graphNeighbors[zCode]?.joinToString() ?: ""}", fontSize = 9.sp, color = GlassTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Orders Ready for BFS Dispatch", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        }

        if (orders.isEmpty()) {
            item {
                GlassBox(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                    Text("No orders waiting in queue. Accept and mark orders 'Ready' in the Kitchen tab.", modifier = Modifier.padding(16.dp), color = GlassTextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(orders) { ord ->
                GlassBox(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Order #${ord.id} • ${ord.restaurantName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Pickup: Zone ${ord.restaurantZone} → Drop: Zone ${ord.customerZone}", fontSize = 12.sp, color = NeonCyan)
                            Text("ETA: ${ord.predictedEtaMins ?: 30} mins (${ord.distanceKm} km)", fontSize = 11.sp, color = GlassTextSecondary)
                        }
                        Button(
                            onClick = { onRunBfs(ord) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("run_bfs_${ord.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run BFS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("BFS Execution Engine Terminal", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                containerColor = Color(0xFF030712).copy(alpha = 0.90f),
                borderColor = NeonCyan.copy(alpha = 0.30f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (consoleLogs.isEmpty()) {
                        Text(
                            "Queue initialized: FIFO. Click 'Run BFS' above to view level-by-level traversal.",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        consoleLogs.forEach { line ->
                            val lineCol = when {
                                line.startsWith("✓") -> NeonEmerald
                                line.startsWith("✗") -> NeonRose
                                line.startsWith("→") -> NeonCyan
                                line.startsWith("  -") -> Color(0xFF94A3B8)
                                else -> Color.White
                            }
                            Text(
                                line,
                                color = lineCol,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ================= TAB 4: GLASS RIDER SCREEN =================
@Composable
fun GlassRiderScreen(
    riders: List<RiderInfo>,
    orders: List<DeliveryOrder>,
    onPickup: (DeliveryOrder) -> Unit,
    onOutForDelivery: (DeliveryOrder) -> Unit,
    onDelivered: (DeliveryOrder) -> Unit,
    onTrack: (Long) -> Unit
) {
    var selectedRiderId by remember { mutableLongStateOf(1L) }
    val assignedOrders = orders.filter { it.assignedRider?.id == selectedRiderId }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassBox(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Active Rider Fleet", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(riders) { r ->
                            val isSel = selectedRiderId == r.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSel) NeonCyan.copy(alpha = 0.25f) else Color(0xFF1E293B).copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonCyan else Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier.clickable { selectedRiderId = r.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = if (isSel) NeonCyan else Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(r.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                        Text("Zone ${r.currentZone} • ${if (r.isAvailable) "Idle" else "Busy"}", fontSize = 10.sp, color = if (r.isAvailable) NeonEmerald else NeonOrange)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Deliveries Assigned to Current Rider", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        }

        if (assignedOrders.isEmpty()) {
            item {
                GlassBox(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                    Text("No deliveries assigned to this rider yet. Run BFS in the BFS Dispatch tab to match orders.", modifier = Modifier.padding(16.dp), color = GlassTextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(assignedOrders) { ord ->
                GlassBox(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Order #${ord.id}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ord.status.color.copy(alpha = 0.20f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ord.status.color.copy(alpha = 0.40f))
                            ) {
                                Text(
                                    ord.status.label,
                                    color = ord.status.color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Pickup from: ${ord.restaurantName} (Zone ${ord.restaurantZone})", fontSize = 13.sp, color = NeonOrange)
                        Text("Deliver to: ${ord.customerName} (Zone ${ord.customerZone})", fontSize = 13.sp, color = NeonEmerald)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (ord.status) {
                                OrderState.RIDER_ASSIGNED -> {
                                    Button(
                                        onClick = { onPickup(ord) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Mark Picked Up")
                                    }
                                }
                                OrderState.PICKED_UP -> {
                                    Button(
                                        onClick = { onOutForDelivery(ord) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Out for Delivery", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                                OrderState.OUT_FOR_DELIVERY -> {
                                    Button(
                                        onClick = { onDelivered(ord) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Mark Delivered", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                                OrderState.DELIVERED -> {
                                    Text("Delivered Successfully ✓", color = NeonEmerald, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                else -> {}
                            }
                            OutlinedButton(
                                onClick = { onTrack(ord.id) },
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Text("Radar Map", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
