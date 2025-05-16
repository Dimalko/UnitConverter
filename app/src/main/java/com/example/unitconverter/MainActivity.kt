package com.example.unitconverter

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unitconverter.ui.theme.UnitConverterTheme
import com.google.android.gms.location.*
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UnitConverterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UnitConverterScreen()
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun UnitConverterScreen() {
    val context = LocalContext.current
    var userCountry by remember { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val fusedLocationClient: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    userCountry = address?.firstOrNull()?.countryCode ?: ""
                } else {
                    val locationRequest = LocationRequest.create().apply {
                        interval = 10000
                        fastestInterval = 5000
                        priority = Priority.PRIORITY_HIGH_ACCURACY
                        numUpdates = 1
                    }
                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val freshLocation = result.lastLocation
                            if (freshLocation != null) {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                val address = geocoder.getFromLocation(
                                    freshLocation.latitude,
                                    freshLocation.longitude,
                                    1
                                )
                                userCountry = address?.firstOrNull()?.countryCode ?: ""
                            }
                        }
                    }
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
            }
        } else {
            Toast.makeText(context, "Η άδεια τοποθεσίας δεν δόθηκε", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val countryInfo = when (userCountry) {
        "US" -> "Νόμισμα: USD | Σύστημα: Ίντσες, Λίβρες, Φαρενάιτ"
        "GR" -> "Νόμισμα: EUR | Σύστημα: Μέτρα, Κιλά, Κελσίου"
        else -> "Νόμισμα: EUR | Σύστημα: Μετρικό (προεπιλογή)"
    }

    val categories = listOf("Μήκος", "Βάρος", "Νομίσματα", "Θερμοκρασία")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    val allUnitsMap = mapOf(
        "Μήκος" to listOf("Μέτρα", "Χιλιόμετρα", "Πόδια", "Εκατοστά", "Ίντσες"),
        "Βάρος" to listOf("Κιλά", "Λίβρες"),
        "Νομίσματα" to listOf("EUR", "USD", "GBP", "YEN", "LEK"),
        "Θερμοκρασία" to listOf("Κελσίου", "Φαρενάιτ", "Κέλβιν")
    )

    val regionalUnitsMap = mapOf(
        "US" to mapOf(
            "Μήκος" to listOf("Πόδια", "Ίντσες"),
            "Βάρος" to listOf("Λίβρες"),
            "Θερμοκρασία" to listOf("Φαρενάιτ")
        ),
        "GR" to mapOf(
            "Μήκος" to listOf("Μέτρα", "Χιλιόμετρα", "Εκατοστά"),
            "Βάρος" to listOf("Κιλά"),
            "Θερμοκρασία" to listOf("Κελσίου")
        ),
        "default" to mapOf(
            "Μήκος" to listOf("Μέτρα", "Χιλιόμετρα", "Εκατοστά"),
            "Βάρος" to listOf("Κιλά"),
            "Θερμοκρασία" to listOf("Κελσίου")
        )
    )

    val units = allUnitsMap[selectedCategory] ?: emptyList()

    val defaultFromUnit = remember(userCountry, selectedCategory) {
        regionalUnitsMap[userCountry]?.get(selectedCategory)?.firstOrNull()
            ?: regionalUnitsMap["default"]?.get(selectedCategory)?.firstOrNull()
            ?: units.firstOrNull().orEmpty()
    }
    val defaultToUnit = remember(units) { units.lastOrNull().orEmpty() }

    var fromUnit by remember { mutableStateOf(defaultFromUnit) }
    var toUnit by remember { mutableStateOf(defaultToUnit) }
    var amount by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Μετατροπέας Μονάδων",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        if (userCountry.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Τοποθεσία: $userCountry", fontSize = 14.sp)
            Text(countryInfo, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Κατηγορία Μετατροπής:", fontSize = 16.sp)
                DropdownMenuBox(
                    items = categories,
                    selectedItem = selectedCategory,
                    onItemSelected = {
                        selectedCategory = it
                        fromUnit = regionalUnitsMap[userCountry]?.get(it)?.firstOrNull()
                            ?: regionalUnitsMap["default"]?.get(it)?.firstOrNull()
                                    ?: allUnitsMap[it]?.firstOrNull().orEmpty()
                        toUnit = allUnitsMap[it]?.lastOrNull().orEmpty()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = units.isNotEmpty()) {
                    Column {
                        Text("Από:", fontSize = 16.sp)
                        DropdownMenuBox(
                            items = units,
                            selectedItem = fromUnit,
                            onItemSelected = { fromUnit = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Σε:", fontSize = 16.sp)
                        DropdownMenuBox(
                            items = units,
                            selectedItem = toUnit,
                            onItemSelected = { toUnit = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Ποσό:", fontSize = 16.sp)
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            placeholder = { Text("Εισάγετε ποσό") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                result = convert(selectedCategory, fromUnit, toUnit, amount)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Μετατροπή")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = result,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownMenuBox(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedItem)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}


fun convert(category: String, from: String, to: String, amountStr: String): String {
    val amount = amountStr.toDoubleOrNull() ?: return "Μη έγκυρο ποσό"

    return when (category) {
        "Βάρος" -> {
            when {
                from == to -> "$amount $to"
                from == "Κιλά" && to == "Λίβρες" -> String.format("%.2f Λίβρες", amount * 2.20462)
                from == "Λίβρες" && to == "Κιλά" -> String.format("%.2f Κιλά", amount / 2.20462)
                else -> "Μετατροπή μη διαθέσιμη"
            }
        }

        "Μήκος" -> {
            val meters = when (from) {
                "Μέτρα" -> amount
                "Χιλιόμετρα" -> amount * 1000
                "Πόδια" -> amount * 0.3048
                "Εκατοστά" -> amount * 0.01
                "Ίντσες" -> amount * 0.0254
                else -> return "Μονάδα μη υποστηριζόμενη"
            }
            val converted = when (to) {
                "Μέτρα" -> meters
                "Χιλιόμετρα" -> meters / 1000
                "Πόδια" -> meters / 0.3048
                "Εκατοστά" -> meters / 0.01
                "Ίντσες" -> meters / 0.0254
                else -> return "Μονάδα μη υποστηριζόμενη"
            }
            return String.format("%.2f $to", converted)
        }

        "Θερμοκρασία" -> {
            val celsius = when (from) {
                "Κελσίου" -> amount
                "Φαρενάιτ" -> (amount - 32) * 5 / 9
                "Κέλβιν" -> amount - 273.15
                else -> return "Μονάδα μη υποστηριζόμενη"
            }
            val converted = when (to) {
                "Κελσίου" -> celsius
                "Φαρενάιτ" -> (celsius * 9 / 5) + 32
                "Κέλβιν" -> celsius + 273.15
                else -> return "Μονάδα μη υποστηριζόμενη"
            }
            return String.format("%.2f $to", converted)
        }

        "Νομίσματα" -> {
            val rates = mapOf(
                "EUR" to 1.0,
                "USD" to 1.1,
                "GBP" to 0.85,
                "YEN" to 130.0,
                "LEK" to 100.0
            )

            val fromRate = rates[from] ?: return "Άγνωστο νόμισμα"
            val toRate = rates[to] ?: return "Άγνωστο νόμισμα"

            val eurAmount = amount / fromRate
            val converted = eurAmount * toRate

            return String.format("%.2f $to", converted)
        }

        else -> "Η κατηγορία δεν υποστηρίζεται ακόμα"
    }
}
