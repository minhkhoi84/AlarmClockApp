package io.github.hanihashemi.alarmclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hanihashemi.alarmclock.ui.theme.AnalogClockOuterBoxColor
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone

data class CountryTimeZone(
    val countryName: String,
    val timeZoneId: String,
    val displayName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen(
    navigateToCountry: (String) -> Unit,
    onBackPressed: () -> Unit
) {
    val vietnamTimeZone = CountryTimeZone("Vietnam", "Asia/Ho_Chi_Minh", "Hanoi, Vietnam")
    val usTimeZone = CountryTimeZone("United States", "America/New_York", "New York, US")
    val japanTimeZone = CountryTimeZone("Japan", "Asia/Tokyo", "Tokyo, Japan")
    
    var selectedCountries by remember { mutableStateOf(listOf(vietnamTimeZone, japanTimeZone)) }
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    
    // Update time every second
    LaunchedEffect(key1 = Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("World Clock") },
                navigationIcon = {
                    TextButton(onClick = onBackPressed) {
                        Text("Back", color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    // Add button as text
                    TextButton(onClick = {
                        // Add all available countries if not already added
                        val allTimeZones = listOf(vietnamTimeZone, usTimeZone, japanTimeZone)
                        val notAddedYet = allTimeZones.filter { country -> 
                            !selectedCountries.any { it.countryName == country.countryName } 
                        }
                        
                        if (notAddedYet.isNotEmpty()) {
                            selectedCountries = selectedCountries + notAddedYet.first()
                        }
                    }) {
                        Text("Add", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            items(selectedCountries) { country ->
                // Format time for this country
                val timezone = TimeZone.getTimeZone(country.timeZoneId)
                val calendar = Calendar.getInstance(timezone)
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                timeFormat.timeZone = timezone
                val formattedTime = timeFormat.format(calendar.time)
                
                // Row for each country with time
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { navigateToCountry(country.countryName) },
                    colors = CardDefaults.cardColors(containerColor = AnalogClockOuterBoxColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = country.displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Analog clock component (small)
                            Box(
                                modifier = Modifier.size(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val hour = calendar.get(Calendar.HOUR)
                                val minute = calendar.get(Calendar.MINUTE)
                                val second = calendar.get(Calendar.SECOND)
                                
                                AnalogClockComponent(
                                    hour = hour, 
                                    minute = minute, 
                                    second = second
                                )
                            }
                            
                            // Digital time display
                            Text(
                                text = formattedTime,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
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
fun CountryClockScreen(
    countryName: String,
    onBackPressed: () -> Unit
) {
    val timeZoneId = when (countryName) {
        "Vietnam" -> "Asia/Ho_Chi_Minh"
        "United States" -> "America/New_York"
        "Japan" -> "Asia/Tokyo"
        else -> "UTC"
    }
    
    val displayName = when (countryName) {
        "Vietnam" -> "Hanoi, Vietnam"
        "United States" -> "New York, US"
        "Japan" -> "Tokyo, Japan"
        else -> countryName
    }
    
    var currentTime by remember { mutableStateOf(Calendar.getInstance(TimeZone.getTimeZone(timeZoneId))) }
    
    // Update time every second
    LaunchedEffect(key1 = Unit) {
        while (true) {
            currentTime = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId))
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(countryName) },
                navigationIcon = {
                    TextButton(onClick = onBackPressed) {
                        Text("Back", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Clock",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Analog Clock
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val hour = currentTime.get(Calendar.HOUR)
                val minute = currentTime.get(Calendar.MINUTE)
                val second = currentTime.get(Calendar.SECOND)
                
                AnalogClockComponent(
                    hour = hour,
                    minute = minute,
                    second = second
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Digital time
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            timeFormat.timeZone = TimeZone.getTimeZone(timeZoneId)
            val formattedTime = timeFormat.format(currentTime.time)
            
            Text(
                text = formattedTime,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = displayName,
                fontSize = 18.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
} 