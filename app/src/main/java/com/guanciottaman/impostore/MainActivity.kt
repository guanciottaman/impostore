// MainActivity.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.guanciottaman.impostore

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
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat



fun loadWordnet(context: Context): JSONObject {
    val inputStream: InputStream = context.assets.open("wordnet_it.json")
    val jsonString = inputStream.bufferedReader().use { it.readText() }
    return JSONObject(jsonString) // Parse the string into a JSONObject
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
        throw Exception("Nessun synset valido con almeno 2 parole!")
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
    val wordnet by remember { mutableStateOf(loadWordnet(context)) }

    var playerCount by remember { mutableIntStateOf(4) }
    var players by remember { mutableStateOf(List(playerCount) { "Player ${it + 1}" }) }
    var undercoverCount by remember { mutableIntStateOf(1) }

    var gameStarted by remember { mutableStateOf(false) }
    var wordsFinished by remember { mutableStateOf(false) }
    var currentPlayer by remember { mutableIntStateOf(0) }
    var roles by remember { mutableStateOf(emptyList<String>()) }
    var buttonsVisible by remember { mutableStateOf(players.map { true }.toMutableStateList()) }
    var remainingUndercover by remember { mutableIntStateOf(0) }
    var words by remember { mutableStateOf(emptyList<String>()) }
    var shownWord by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    val configuration = LocalContext.current.resources.configuration
    val currentLang = configuration.locales[0].language


    var flipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(targetValue = if (flipped) 180f else 0f)

    val BackgroundColor = Color(0xFF1E1E2C)
    val CardColor       = Color(0xFF2D2D44)
    val ButtonColor     = Color(0xFF6C63FF)
    val ButtonTextColor = Color.White
    val TextColor       = Color(0xFFE0E0E0)
    val HighlightColor  = Color(0xFFFFD700)


    fun startGame() {
        roles = assignRoles(undercoverCount, playerCount)
        val (uw, cw) = getTwoRandomWords(wordnet)
        words = roles.map { if (it == "Undercover") uw else cw }
        currentPlayer = 0
        flipped = false
        wordsFinished = false
        gameStarted = true
        buttonsVisible = players.map { true }.toMutableStateList()
        remainingUndercover = roles.count { it == "Undercover" }

        if (words.isNotEmpty()) {
            shownWord = words[0]
        } else {
            shownWord = ""
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
        buttonsVisible = players.map { true }.toMutableStateList()
    }

    fun setAppLanguage(languageCode: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }


    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Undercover Game") },
            colors = TopAppBarDefaults.mediumTopAppBarColors(
                containerColor = CardColor,
                titleContentColor = Color.White
            ),
            actions = {
                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint=Color.White
                    )
                }
            }
        )
    },
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundColor) { paddingValues ->
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

                if (showNamesPopup) {
                    val playerLabel = stringResource(R.string.player)
                    var tempNames by remember { mutableStateOf(List(playerCount) {
                        playerLabel + " " +  (it + 1)
                    }) }
                    AlertDialog(
                        onDismissRequest = { showNamesPopup = false },
                        title = { Text(stringResource(R.string.insert_player_names), color = TextColor) },
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
                                            tempNames = tempNames.toMutableList()
                                                .also { it[i] = newName }
                                        },
                                        label = { Text(stringResource(R.string.player) + (i + 1), color = TextColor) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextColor,
                                            unfocusedTextColor = TextColor,
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
                                    containerColor = ButtonColor
                                )
                            ) {
                                Text(stringResource(R.string.confirm))
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { showNamesPopup = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ButtonColor
                                )
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        },
                        containerColor = CardColor,
                    )
                }


                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.players) + ": ", fontSize = 20.sp, color = TextColor)
                    Button(
                        onClick = {
                            if (playerCount > 3) {
                                playerCount--
                                if (undercoverCount > (playerCount + 1) / 2 - 1) {
                                    undercoverCount--
                                }
                            }
                        }, colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonColor
                        )
                    ) {
                        Text("-")
                    }

                    Text(
                        text = playerCount.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )

                    Button(
                        onClick = { if (playerCount < 25) playerCount++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonColor
                        )
                    ) {
                        Text("+")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.undercovers) + ": ", fontSize = 20.sp, color = TextColor)
                    Button(
                        onClick = { if (undercoverCount > 1) undercoverCount-- },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonColor
                        )
                    ) {
                        Text("-")
                    }

                    Text(
                        text = undercoverCount.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )

                    Button(
                        onClick = { if (undercoverCount < ((playerCount + 1) / 2 - 1)) undercoverCount++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonColor
                        )
                    ) {
                        Text("+")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showNamesPopup = true }, colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor
                    )
                ) {
                    Text(stringResource(R.string.start), fontSize = 24.sp)
                }
            } else if (!wordsFinished) {
                Text(
                    stringResource(R.string.players) + ": " + players[currentPlayer],
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
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
                                .background(color = CardColor, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.tap_to_flip),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f }
                                .background(CardColor, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    stringResource(R.string.word) + ": $shownWord",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextColor
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
                            containerColor = ButtonColor
                        )
                    )
                    {
                        Text(stringResource(R.string.pass_phone))
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
                    stringResource(R.string.time_to_vote),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
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
                                    containerColor = ButtonColor
                                )
                            ) {
                                Text(player, color = Color.White, fontWeight = FontWeight.Medium)
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
                                title = { Text(stringResource(R.string.vote_result), color = TextColor) },
                                text = {
                                    Text(
                                        player + stringResource(R.string.was_the_undercover),
                                        fontSize = 20.sp,
                                        color = TextColor
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            resetGame()
                                        }, colors = ButtonDefaults.buttonColors(
                                            containerColor = ButtonColor
                                        )
                                    ) {
                                        Text(stringResource(R.string.ok))
                                    }
                                },
                                containerColor = CardColor
                            )
                        } else {
                            AlertDialog(
                                onDismissRequest = { showPopup = false },
                                title = { Text(stringResource(R.string.vote_result), color = TextColor) },
                                text = {
                                    Text(
                                        player + stringResource(R.string.was_the_undercover),
                                        fontSize = 20.sp,
                                        color = TextColor
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { showPopup = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ButtonColor
                                        )
                                    ) {
                                        Text(stringResource(R.string.ok))
                                    }
                                },
                                containerColor = CardColor
                            )
                        }
                    } else {
                        buttonsVisible[selectedPlayer] = false
                        AlertDialog(
                            onDismissRequest = {
                                showPopup = false
                            },
                            title = { Text(stringResource(R.string.vote_result), color = TextColor) },
                            text = {
                                Text(
                                    player + stringResource(R.string.was_not_undercover),
                                    fontSize = 20.sp,
                                    color = TextColor
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = { showPopup = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ButtonColor
                                    )
                                ) {
                                    Text(stringResource(R.string.ok))
                                }
                            },
                            containerColor = CardColor
                        )
                    }
                }

            }
        }
        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text(stringResource(R.string.settings), color = TextColor) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val configuration = LocalContext.current.resources.configuration
                        val currentLang = configuration.locales[0].language

                        var showLanguageMenu by remember { mutableStateOf(false) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text = stringResource(R.string.language),
                                fontSize = 20.sp,
                                color = ButtonTextColor
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Box {

                                TextButton(
                                    onClick = { showLanguageMenu = true }
                                ) {
                                    Text(
                                        text = if (currentLang == "it") "Italiano" else "English",
                                        color = TextColor
                                    )
                                }

                                DropdownMenu(
                                    expanded = showLanguageMenu,
                                    onDismissRequest = { showLanguageMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Italiano") },
                                        onClick = {
                                            setAppLanguage("it")
                                            showLanguageMenu = false
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("English") },
                                        onClick = {
                                            setAppLanguage("en")
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
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonColor)
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                },
                containerColor = CardColor
            )
        }
    }
}


fun assignRoles(undercovers: Int, totalPlayers: Int): List<String> {
    val roles = mutableListOf<String>()
    repeat(undercovers) { roles.add("Undercover") }
    while (roles.size < totalPlayers) roles.add("Cittadino")
    return roles.shuffled()
}
