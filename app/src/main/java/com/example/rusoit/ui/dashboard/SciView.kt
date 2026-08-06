@file:OptIn(ExperimentalTvMaterial3Api::class)
package com.example.rusoit.ui.dashboard

import android.annotation.SuppressLint
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.example.rusoit.data.model.SCIInformation
import com.example.rusoit.ui.components.FocusTrappedModal
import com.example.rusoit.ui.theme.HudColors
import com.example.rusoit.utils.Resource
import com.example.rusoit.viewmodel.MonitoringViewModel
import java.util.Locale

/**
 * SCI TV — consulta espejo web (/system-C-I + detalle):
 * - GET /sci (activos) / lista completa para historial
 * - Modal de consulta con mapa OSM (como Leaflet en web)
 */
@Composable
fun SCIView(viewModel: MonitoringViewModel, onSCIClick: (SCIInformation) -> Unit) {
    val activeResource by viewModel.sciReports.collectAsState()
    val allResource by viewModel.sciAll.collectAsState()
    var tab by remember { mutableStateOf(0) } // 0 activos, 1 historial

    LaunchedEffect(Unit) {
        viewModel.loadSCIReports()
        viewModel.loadAllSCIReports()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "// SISTEMA DE COMANDO DE INCIDENTES",
            style = MaterialTheme.typography.labelSmall,
            color = HudColors.AccentSecondary,
            letterSpacing = 2.sp
        )
        Text(
            "Consulta de SCI",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SciTabChip("Activos", tab == 0) { tab = 0 }
            SciTabChip("Historial", tab == 1) { tab = 1 }
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                onClick = {
                    viewModel.loadSCIReports()
                    viewModel.loadAllSCIReports()
                },
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = HudColors.BgCard,
                    focusedContainerColor = HudColors.BgCardHover
                )
            ) {
                Text(
                    "ACTUALIZAR",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = HudColors.AccentPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val resource = if (tab == 0) activeResource else allResource
        when {
            resource is Resource.Loading || resource == null -> {
                LoadingSpinner(if (tab == 0) "Cargando SCI activos..." else "Cargando historial SCI...")
            }
            resource is Resource.Error -> {
                ErrorMessage(
                    resource?.message ?: "Error al cargar /sci",
                    onRetry = {
                        viewModel.loadSCIReports()
                        viewModel.loadAllSCIReports()
                    }
                )
            }
            else -> {
                val raw = resource?.data.orEmpty()
                val data = if (tab == 0) {
                    raw.filter { it.isActive() }.ifEmpty { raw }
                } else {
                    raw.filter { !it.isActive() }
                }
                if (data.isEmpty()) {
                    PlaceholderView(
                        if (tab == 0) "Sin incidentes SCI activos"
                        else "Sin SCI en historial"
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(data, key = { it.id }) { report ->
                            SciListCard(report = report, onClick = { onSCIClick(report) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SciTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) HudColors.AccentGlow else HudColors.BgCard,
            focusedContainerColor = HudColors.BgCardHover
        )
    ) {
        Text(
            label.uppercase(Locale.getDefault()),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (selected) HudColors.AccentPrimary else HudColors.TextMuted,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SciListCard(report: SCIInformation, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.colors(containerColor = HudColors.BgCard),
        modifier = Modifier.fillMaxWidth().height(190.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp).fillMaxSize()) {
            Box(
                modifier = Modifier
                    .background(
                        if (report.isActive()) HudColors.AccentPrimary else HudColors.TextMuted,
                        RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    if (report.isActive()) "EN CURSO" else (report.status?.uppercase(Locale.getDefault()) ?: "CERRADO"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                report.name ?: "Incidente SCI",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Inicio: ${report.date_to_start?.take(10) ?: "—"}",
                color = HudColors.TextMuted,
                fontSize = 13.sp
            )
            Text(
                report.ubication ?: "Sin coordenadas",
                color = HudColors.Amber,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "ABRIR MAPA Y DETALLE >",
                color = HudColors.AccentPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun SCIDetailOverlay(
    sci: SCIInformation,
    viewModel: MonitoringViewModel,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val detailResource by viewModel.sciDetail.collectAsState()
    var showMapFullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(sci.id) {
        viewModel.loadSCIDetail(sci.id)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.clearSCIDetail() }
    }

    val current = when (val r = detailResource) {
        is Resource.Success -> r.data ?: sci
        else -> sci
    }
    val coords = remember(current.ubication) { current.parseLatLng() }

    // Al volver del mapa a pantalla completa, recuperar foco en Cerrar
    LaunchedEffect(showMapFullscreen) {
        if (!showMapFullscreen) {
            focusRequester.requestFocus()
        }
    }

    FocusTrappedModal(
        scrimAlpha = 0.94f,
        initialFocusRequester = focusRequester
    ) {
        // Box (no Card clickable): evita que el contenedor robe el D-pad
        Box(
            modifier = Modifier
                .fillMaxSize(0.94f)
                .background(HudColors.BgCard, RoundedCornerShape(16.dp))
                .focusProperties { canFocus = false }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Panel consulta
                Column(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .padding(28.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "// CONSULTA SCI",
                        style = MaterialTheme.typography.labelSmall,
                        color = HudColors.AccentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        current.name ?: "Emergencia",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (detailResource is Resource.Loading) {
                        CircularProgressIndicator(color = HudColors.AccentPrimary, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Actualizando desde /sci/${sci.id}...", color = HudColors.TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    InfoRow(
                        "ESTADO:",
                        current.status?.uppercase(Locale.getDefault()) ?: "ACTIVO",
                        if (current.isActive()) HudColors.Amber else HudColors.Green
                    )
                    InfoRow("COORDENADAS:", current.ubication ?: "Sin ubicación")
                    InfoRow("INICIO:", current.date_to_start?.take(10) ?: "—")
                    current.time_to_start?.let { InfoRow("HORA:", it) }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("DESCRIPCIÓN / RESUMEN", style = MaterialTheme.typography.labelSmall, color = HudColors.TextMuted)
                    Text(
                        current.description ?: "Sin descripción registrada.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    if (coords != null) {
                        Button(
                            onClick = { showMapFullscreen = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.colors(containerColor = HudColors.Blue)
                        ) {
                            Icon(Icons.Default.Map, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ABRIR MAPA", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        colors = ButtonDefaults.colors(containerColor = HudColors.AccentPrimary)
                    ) {
                        Text("CERRAR", fontWeight = FontWeight.Bold)
                    }
                }

                // Vista previa: solo imagen (sin WebView) para no robar el foco del D-pad
                Box(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                        .background(HudColors.BgPrimary)
                        .focusProperties { canFocus = false }
                ) {
                    if (coords == null) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Map, null, tint = HudColors.AccentPrimary, modifier = Modifier.size(56.dp))
                            Text("Sin coordenadas válidas", color = HudColors.TextMuted, fontWeight = FontWeight.Bold)
                            Text(current.ubication ?: "", color = HudColors.Amber, fontSize = 12.sp)
                        }
                    } else {
                        SciOsmMap(
                            lat = coords.first,
                            lng = coords.second,
                            title = current.name ?: "SCI",
                            modifier = Modifier.fillMaxSize(),
                            allowWebView = true
                        )
                    }
                }
            }
        }
    }

    if (showMapFullscreen && coords != null) {
        SciMapFullscreenModal(
            title = current.name ?: "SCI",
            lat = coords.first,
            lng = coords.second,
            ubication = current.ubication,
            onDismiss = { showMapFullscreen = false }
        )
    }
}


@Composable
private fun SciMapFullscreenModal(
    title: String,
    lat: Double,
    lng: Double,
    ubication: String?,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    FocusTrappedModal(
        scrimAlpha = 0.96f,
        initialFocusRequester = focusRequester
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.96f)
                .background(HudColors.BgCard, RoundedCornerShape(16.dp))
                .focusProperties { canFocus = false }
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("// MAPA SCI", color = HudColors.AccentPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(ubication ?: "$lat,$lng", color = HudColors.Amber, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(focusRequester),
                        colors = ButtonDefaults.colors(containerColor = HudColors.AccentPrimary)
                    ) {
                        Text("CERRAR MAPA", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                SciOsmMap(
                    lat = lat,
                    lng = lng,
                    title = title,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(HudColors.BgPrimary, RoundedCornerShape(12.dp))
                        .focusProperties { canFocus = false },
                    allowWebView = true
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SciOsmMap(
    lat: Double,
    lng: Double,
    title: String,
    modifier: Modifier = Modifier,
    allowWebView: Boolean = true
) {
    var loading by remember(lat, lng) { mutableStateOf(true) }
    var loadFailed by remember(lat, lng) { mutableStateOf(false) }

    val safeTitle = remember(title) {
        title.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\"", "\\\"")
    }
    val html = remember(lat, lng, safeTitle) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0"/>
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>
            html, body, #map { margin:0; padding:0; height:100%; width:100%; background:#1e1e20; }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            var map = L.map('map', {
              zoomControl: false,
              dragging: false,
              scrollWheelZoom: false,
              doubleClickZoom: false,
              boxZoom: false,
              keyboard: false,
              tap: false
            }).setView([$lat, $lng], 15);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
              maxZoom: 19,
              attribution: 'OSM'
            }).addTo(map);
            L.marker([$lat, $lng]).addTo(map).bindPopup('$safeTitle');
            setTimeout(function(){ map.invalidateSize(true); }, 250);
            setTimeout(function(){ map.invalidateSize(true); }, 1000);
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    // Tile OSM de respaldo (servicio staticmap.de está discontinuado)
    val tileUrl = remember(lat, lng) {
        val zoom = 14
        val n = 1 shl zoom
        val x = (((lng + 180.0) / 360.0) * n).toInt().coerceIn(0, n - 1)
        val latRad = Math.toRadians(lat)
        val y = ((1.0 - kotlin.math.ln(kotlin.math.tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / Math.PI) / 2.0 * n)
            .toInt().coerceIn(0, n - 1)
        "https://tile.openstreetmap.org/$zoom/$x/$y.png"
    }

    Box(
        modifier = modifier
            .background(HudColors.BgPrimary)
            .focusProperties { canFocus = false },
        contentAlignment = Alignment.Center
    ) {
        if (!allowWebView || loadFailed) {
            AsyncImage(
                model = tileUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { loading = false },
                onError = { loading = false }
            )
        } else {
            key(html) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(0xFF1E1E20.toInt())
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = false
                            settings.displayZoomControls = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            isFocusable = false
                            isFocusableInTouchMode = false
                            descendantFocusability = FrameLayout.FOCUS_BLOCK_DESCENDANTS
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                    view?.evaluateJavascript(
                                        "if (typeof map !== 'undefined') { map.invalidateSize(true); }",
                                        null
                                    )
                                }
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        loadFailed = true
                                        loading = true
                                    }
                                }
                            }
                            loadDataWithBaseURL(
                                "https://unpkg.com/",
                                html,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    }
                )
            }
        }

        if (loading) {
            CircularProgressIndicator(color = HudColors.AccentPrimary, strokeWidth = 3.dp)
        }

        Text(
            title,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
