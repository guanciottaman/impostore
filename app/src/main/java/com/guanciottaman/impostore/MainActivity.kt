// MainActivity.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.guanciottaman.impostore

import java.util.Locale
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import android.content.Context
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.OutlinedTextField
import java.io.InputStream
import org.json.JSONObject
import android.content.res.Configuration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch


val Context.dataStore by preferencesDataStore(name = "settings")

val LANGUAGE_KEY = stringPreferencesKey("language")



suspend fun saveLanguage(context: Context, language: String) {
    context.dataStore.edit { prefs ->
        prefs[LANGUAGE_KEY] = language
    }
}

fun getLanguageFlow(context: Context): Flow<String> {
    return context.dataStore.data
        .map { prefs ->
            prefs[LANGUAGE_KEY] ?: "it"
        }
}

@Composable
fun localizedString(id: Int): String {
    val context = LocalContext.current

    val config = Configuration(context.resources.configuration)
    val localizedContext = context.createConfigurationContext(config)

    return localizedContext.resources.getString(id)
}


fun loadWordnet(context: Context, filename: String="wordnet_it.json"): JSONObject {
    val inputStream: InputStream = context.assets.open(filename)
    val jsonString = inputStream.bufferedReader().use { it.readText() }
    return JSONObject(jsonString)
}

fun getTwoRandomWords(wordnet: JSONObject): Pair<String, String> {

    val validSynsets = mutableListOf<String>()

    val keysIterator = wordnet.keys()
    while (keysIterator.hasNext()) {
        val key = keysIterator.next()
        val array = wordnet.getJSONArray(key)
        if (array.length() >= 2) {
            validSynsets.add(key)
        }
    }

    if (validSynsets.isEmpty()) {
        throw Exception("No valid synset with at least 2 words!")
    }

    val randomSynset = validSynsets.random()
    val wordsArray = wordnet.getJSONArray(randomSynset)

    val idx1 = Random.nextInt(wordsArray.length())
    var idx2 = Random.nextInt(wordsArray.length())
    while (idx2 == idx1) {
        idx2 = Random.nextInt(wordsArray.length())
    }

    val firstWord = wordsArray.getString(idx1)
    val secondWord = wordsArray.getString(idx2)

    return Pair(firstWord, secondWord)
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UndercoverApp()
        }
    }
}

@Composable
fun UndercoverApp() {
    val context = LocalContext.current
    val language by getLanguageFlow(context)
        .collectAsState(initial = "it")
    val wordnet = remember(language) {
        loadWordnet(context, if (language == "it") "wordnet_it.json" else "wordnet_en.json")
    }
    val scope = rememberCoroutineScope()

    var playerCount by remember { mutableIntStateOf(4) }
    val playerLabel = localizedString(R.string.player)
    var players by remember { mutableStateOf(List(playerCount) { "$playerLabel ${it + 1}" }) }
    var undercoverCount by remember { mutableIntStateOf(1) }

    var gameStarted by remember { mutableStateOf(false) }
    var wordsFinished by remember { mutableStateOf(false) }
    var currentPlayer by remember { mutableIntStateOf(0) }
    var roles by remember { mutableStateOf(emptyList<String>()) }
    val buttonsVisible = remember(players) { players.map { true }.toMutableStateList() }
    var remainingUndercover by remember { mutableIntStateOf(0) }
    var words by remember { mutableStateOf(emptyList<String>()) }
    var shownWord by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val localizedContext = remember(language) {
        val langTag = language
        val locale = Locale.Builder().setLanguage(langTag).build()
        val config = context.resources.configuration
        config.setLocales(android.os.LocaleList(locale))
        context.createConfigurationContext(config)
    }

    var flipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(targetValue = if (flipped) 180f else 0f)

    val backgroundColor = Color(0xFF1E1E2C)
    val cardColor       = Color(0xFF2D2D44)
    val buttonColor     = Color(0xFF6C63FF)
    val buttonTextColor = Color.White
    val textColor       = Color(0xFFE0E0E0)


    fun startGame() {
        roles = assignRoles(undercoverCount, playerCount)
        val (uw, cw) = getTwoRandomWords(wordnet)
        words = roles.map { if (it == "Undercover") uw else cw }
        currentPlayer = 0
        flipped = false
        wordsFinished = false
        gameStarted = true
        buttonsVisible.clear()
        buttonsVisible.addAll(players.map { true })

        remainingUndercover = roles.count { it == "Undercover" }

        shownWord = if (words.isNotEmpty()) {
            words[0]
        } else {
            ""
        }
    }

    fun resetGame() {
        gameStarted = false
        wordsFinished = false
        currentPlayer = 0
        flipped = false
        shownWord = ""
        words = emptyList()
        roles = emptyList()
        remainingUndercover = 0
        buttonsVisible.clear()
        buttonsVisible.addAll(players.map { true })

        buttonsVisible.clear()
        buttonsVisible.addAll(players.map { true })

    }
    fun localizedStringNonComposable(context: Context, id: Int): String {
        return context.getString(id)
    }

    CompositionLocalProvider(LocalContext provides localizedContext) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(localizedString(R.string.app_name)) },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = cardColor,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxSize(),
            containerColor = backgroundColor
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (!gameStarted) {
                    var showNamesPopup by remember { mutableStateOf(false) }
                    val tempNames = remember(playerCount) { MutableList(playerCount) {
                        index -> players.getOrNull(index) ?: ""
                    }.toMutableStateList() }

                    if (showNamesPopup) {

                        AlertDialog(
                            onDismissRequest = { showNamesPopup = false },
                            title = {
                                Text(
                                    localizedString(R.string.insert_player_names),
                                    color = textColor
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .heightIn(max = 500.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    for (i in 0 until playerCount) {
                                        OutlinedTextField(
                                            value = tempNames.getOrElse(i) { "" },
                                            onValueChange = { newName ->
                                                tempNames[i] = newName
                                            },
                                            label = {
                                                Text(
                                                    localizedString(R.string.player) + (i + 1),
                                                    color = textColor
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = textColor,
                                                unfocusedTextColor = textColor,
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        players = tempNames.toList()
                                        startGame()
                                        showNamesPopup = false
                                    }, colors = ButtonDefaults.buttonColors(
                                        containerColor = buttonColor
                                    )
                                ) {
                                    Text(localizedString(R.string.confirm))
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { showNamesPopup = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = buttonColor
                                    )
                                ) {
                                    Text(localizedString(R.string.cancel))
                                }
                            },
                            containerColor = cardColor,
                        )
                    }


                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            localizedString(R.string.players) + ": ",
                            fontSize = 20.sp,
                            color = textColor
                        )
                        Button(
                            onClick = {
                                if (playerCount > 3) {
                                    playerCount--
                                    players = players.take(playerCount).toMutableList()
                                    if (undercoverCount > (playerCount + 1) / 2 - 1) {
                                        undercoverCount--
                                    }
                                }
                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor
                            )
                        ) {
                            Text("-")
                        }

                        Text(
                            text = playerCount.toString(),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Button(
                            onClick = {
                                if (playerCount < 25) {
                                    playerCount++
                                    val playerBase = localizedStringNonComposable(context, R.string.player)
                                    players = players.toMutableList().apply { add("$playerBase $playerCount") }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor
                            )
                        ) {
                            Text("+")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            localizedString(R.string.undercovers) + ": ",
                            fontSize = 20.sp,
                            color = textColor
                        )
                        Button(
                            onClick = { if (undercoverCount > 1) undercoverCount-- },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor
                            )
                        ) {
                            Text("-")
                        }

                        Text(
                            text = undercoverCount.toString(),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Button(
                            onClick = { if (undercoverCount < ((playerCount + 1) / 2 - 1)) undercoverCount++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor
                            )
                        ) {
                            Text("+")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showNamesPopup = true }, colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor
                        )
                    ) {
                        Text(localizedString(R.string.start), fontSize = 24.sp)
                    }
                } else if (!wordsFinished) {
                    Text(
                        localizedString(R.string.players) + ": " + players[currentPlayer],
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier
                            .size(250.dp, 150.dp)
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12 * density
                            }
                            .clickable { flipped = !flipped },
                        contentAlignment = Alignment.Center
                    ) {
                        if (rotation <= 90f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color = cardColor, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    localizedString(R.string.tap_to_flip),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationY = 180f }
                                    .background(cardColor, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        localizedString(R.string.word) + ": $shownWord",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (flipped) {
                        Button(
                            onClick = {
                                flipped = false

                                if (currentPlayer == players.size - 1) {
                                    wordsFinished = true
                                } else {
                                    currentPlayer += 1
                                    shownWord = words[currentPlayer]
                                }
                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor
                            )
                        )
                        {
                            Text(localizedString(R.string.pass_phone))
                        }

                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        for (i in 0 until playerCount) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(
                                        if (i < currentPlayer) Color.Gray else Color.LightGray,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                        }
                    }

                } else {
                    Text(
                        localizedString(R.string.time_to_vote),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    var showPopup by remember { mutableStateOf(false) }
                    var selectedPlayer by remember { mutableIntStateOf(0) }
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        for ((i, player) in players.withIndex()) {
                            if (buttonsVisible[i]) {
                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .padding(vertical = 8.dp),
                                    onClick = {
                                        selectedPlayer = i

                                        if (roles[i] == "Undercover") {
                                            remainingUndercover--
                                        }

                                        buttonsVisible[i] = false
                                        showPopup = true
                                    }, colors = ButtonDefaults.buttonColors(
                                        containerColor = buttonColor
                                    )
                                ) {
                                    Text(
                                        player,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    if (showPopup) {
                        val player = players[selectedPlayer]
                        if (roles[selectedPlayer] == "Undercover") {
                            if (remainingUndercover == 0) {
                                AlertDialog(
                                    onDismissRequest = {
                                        resetGame()
                                    },
                                    title = {
                                        Text(
                                            localizedString(R.string.vote_result),
                                            color = textColor
                                        )
                                    },
                                    text = {
                                        Text(
                                            player + localizedString(R.string.was_the_undercover),
                                            fontSize = 20.sp,
                                            color = textColor
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                resetGame()
                                            }, colors = ButtonDefaults.buttonColors(
                                                containerColor = buttonColor
                                            )
                                        ) {
                                            Text(localizedString(R.string.ok))
                                        }
                                    },
                                    containerColor = cardColor
                                )
                            } else {
                                AlertDialog(
                                    onDismissRequest = { showPopup = false },
                                    title = {
                                        Text(
                                            localizedString(R.string.vote_result),
                                            color = textColor
                                        )
                                    },
                                    text = {
                                        Text(
                                            player + localizedString(R.string.was_the_undercover),
                                            fontSize = 20.sp,
                                            color = textColor
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = { showPopup = false },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = buttonColor
                                            )
                                        ) {
                                            Text(localizedString(R.string.ok))
                                        }
                                    },
                                    containerColor = cardColor
                                )
                            }
                        } else {
                            buttonsVisible[selectedPlayer] = false
                            AlertDialog(
                                onDismissRequest = {
                                    showPopup = false
                                },
                                title = {
                                    Text(
                                        localizedString(R.string.vote_result),
                                        color = textColor
                                    )
                                },
                                text = {
                                    Text(
                                        player + localizedString(R.string.was_not_undercover),
                                        fontSize = 20.sp,
                                        color = textColor
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { showPopup = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = buttonColor
                                        )
                                    ) {
                                        Text(localizedString(R.string.ok))
                                    }
                                },
                                containerColor = cardColor
                            )
                        }
                    }

                }
            }
            if (showSettings) {
                AlertDialog(
                    onDismissRequest = { showSettings = false },
                    title = { Text(localizedString(R.string.settings), color = textColor) },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    text = localizedString(R.string.language),
                                    fontSize = 20.sp,
                                    color = buttonTextColor
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Box {

                                    TextButton(
                                        onClick = { showLanguageMenu = true }
                                    ) {
                                        Text(
                                            text = if (language == "it") "Italiano" else "English",
                                            color = textColor
                                        )

                                    }

                                    DropdownMenu(
                                        expanded = showLanguageMenu,
                                        onDismissRequest = { showLanguageMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Italiano") },
                                            onClick = {
                                                scope.launch {
                                                    saveLanguage(context, "it")
                                                }
                                                showLanguageMenu = false

                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("English") },
                                            onClick = {
                                                scope.launch {
                                                    saveLanguage(context, "en")
                                                }
                                                showLanguageMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showSettings = false },
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                        ) {
                            Text(localizedString(R.string.ok))
                        }
                    },
                    containerColor = cardColor
                )
            }
        }
    }
}


fun assignRoles(undercoverCount: Int, totalPlayers: Int): List<String> {
    val roles = mutableListOf<String>()
    repeat(undercoverCount) { roles.add("Undercover") }
    while (roles.size < totalPlayers) roles.add("Citizen")
    return roles.shuffled()
}
