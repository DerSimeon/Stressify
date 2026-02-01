package lol.simeon.stressify

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asDesktopBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.github.ajalt.mordant.rendering.VerticalAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import lol.simeon.stressify.bot.BotManager
import lol.simeon.stressify.generated.resources.Res
import lol.simeon.stressify.protocol.MinecraftAddress
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skia.Bitmap
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
fun main() = application()



private fun application() = application {
    val iconPainter: BitmapPainter = remember {
        val stream = object {}.javaClass.getResourceAsStream("/icon/icon.png")
            ?: error("Missing resource: /icon/icon.png")
        BitmapPainter(stream.readAllBytes().decodeToImageBitmap())
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Stressify | Minecraft Bot Tester",
        icon = iconPainter,
        state = rememberWindowState(placement = WindowPlacement.Maximized)
    ) {

        LaunchedEffect(Unit) {
            delay(50)
            window.toFront()
            window.requestFocus()

        }

        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(Modifier.fillMaxSize()) {
                BotUi()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BotUi() {
    val scope = rememberCoroutineScope()

    var hostPort by remember { mutableStateOf("localhost:25565") }
    var countText by remember { mutableStateOf("1") }

    var manager by remember { mutableStateOf<BotManager?>(null) }
    val connected = manager != null

    var selectedBot by remember { mutableStateOf<String?>(null) } // null = Global
    var message by remember { mutableStateOf("") }

    var log by remember { mutableStateOf(listOf<String>()) }
    fun pushLog(line: String) {
        log = (log + line).takeLast(300)
    }

    val nickGen = remember { NickGenerator() }

    val botStates by (manager?.botStates ?: remember { flowOf(emptyList()) })
        .collectAsState(initial = emptyList())

    val bots = remember(botStates) { botStates.map { it.name } }

    val applyGravity = remember { mutableStateOf(false) }

    LaunchedEffect(manager) {
        log = emptyList()
        val m = manager ?: return@LaunchedEffect
        m.logs.collectLatest { line ->
            log = (log + line).takeLast(300)
        }
    }

    LaunchedEffect(bots, connected) {
        if (!connected) {
            selectedBot = null
            return@LaunchedEffect
        }

        val sel = selectedBot
        if (sel != null && sel !in bots) selectedBot = null
    }

    fun parseAddress(input: String): MinecraftAddress {
        val trimmed = input.trim()
        val host: String
        val port: Int

        val idx = trimmed.lastIndexOf(':')
        if (idx > 0 && idx < trimmed.length - 1 && trimmed.substring(idx + 1).all { it.isDigit() }) {
            host = trimmed.substring(0, idx)
            port = trimmed.substring(idx + 1).toInt()
        } else {
            host = trimmed
            port = 25565
        }

        return MinecraftAddress(host, port)
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("mc-bots", style = MaterialTheme.typography.headlineSmall)

        Card {
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = hostPort,
                    onValueChange = { hostPort = it },
                    label = { Text("Host [:port]") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )



                // checkbox "should attempt to apply gravity"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it.filter(Char::isDigit).ifBlank { "" } },
                        label = { Text("Bot count") },
                        singleLine = true,
                        modifier = Modifier.widthIn(max = 220.dp)
                    )

                    Spacer(Modifier.weight(1f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Apply gravity to bots ", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = applyGravity.value,
                            onCheckedChange = { value -> applyGravity.value = value }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        enabled = !connected,
                        onClick = {
                            val cnt = countText.toIntOrNull()?.coerceIn(1, 10_000) ?: 1
                            val addr = runCatching { parseAddress(hostPort) }.getOrElse {
                                pushLog("Invalid address.")
                                return@Button
                            }

                            manager = BotManager(nickGen, addr, cnt, applyGravity.value)
                            pushLog("Launching $cnt bots -> ${addr.hostname}:${addr.port} and applyGravity=${applyGravity.value}...")

                            scope.launch(Dispatchers.IO) {
                                runCatching { manager!!.connectAll() }
                                    .onFailure { pushLog("Connect failed: ${it.message}") }
                            }
                        }
                    ) { Text("Connect") }

                    OutlinedButton(
                        enabled = connected,
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                runCatching {  manager?.disconnectAll("Disconnect") }
                                    .onFailure { pushLog("Disconnect failed: ${it.message}") }
                            }
                            selectedBot = null
                            pushLog("Disconnected.")
                            manager = null
                        }
                    ) { Text("Disconnect") }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            Card(Modifier.weight(0.42f)) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Bots", style = MaterialTheme.typography.titleMedium)

                    if (botStates.isEmpty()) {
                        Text(
                            "No bots.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
                            items(botStates) { bot ->
                                val isSelected = selectedBot == bot.name

                                val status = when (bot.status) {
                                    BotManager.BotState.Status.CONNECTING -> "connecting"
                                    BotManager.BotState.Status.ONLINE -> "online"
                                    BotManager.BotState.Status.DEAD -> "dead"
                                }

                                val extra = bot.lastReason?.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()

                                ListItem(
                                    headlineContent = { Text(bot.name) },
                                    supportingContent = {
                                        Text(
                                            status + extra,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ListItemDefaults.colors(
                                        containerColor = if (isSelected)
                                            MaterialTheme.colorScheme.secondaryContainer
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                                )
                                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                            }
                        }

                        ExposedDropdownMenuBoxRow(
                            bots = bots,
                            selected = selectedBot,
                            onSelect = { selectedBot = it }
                        )
                    }
                }
            }

            Card(Modifier.weight(0.58f)) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Send", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message or /command") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            enabled = connected && message.isNotBlank(),
                            onClick = {
                                val msg = message.trim()
                                val target = selectedBot

                                scope.launch(Dispatchers.IO) {
                                    if (target == null) {
                                        manager?.sendGlobal(msg)
                                        pushLog("@G $msg")
                                    } else {
                                        val ok = manager?.sendToBot(target, msg) ?: false
                                        pushLog(if (ok) "@$target $msg" else "No bot named '$target'")
                                    }
                                }
                                message = ""
                            }
                        ) { Text("Send") }
                    }

                    HorizontalDivider()

                    Text("Log", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(Modifier.fillMaxWidth().heightIn(min = 120.dp)) {
                        items(log) { line ->
                            Text(
                                line,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxRow(
    bots: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = selected ?: "Global"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Selected bot") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Global") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )

            bots.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(name)
                        expanded = false
                    }
                )
            }
        }
    }
}
