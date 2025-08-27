package com.vayun.files.fileview

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vayun.files.R
import com.vayun.files.ShareFAB
import java.io.File
import com.vayun.files.parser.VCard
import com.vayun.files.parser.ContactsParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPage(navController: NavController, path: String) {
    val contentResolver = LocalContext.current.contentResolver
    val currentFile = File(path)
    val context = LocalContext.current

    val contacts by remember {mutableStateOf(ContactsParser.parse(contentResolver.getText(path)))}

    val requestContactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            TODO("Add contacts import")
        } else {
            Toast.makeText(context, "Contacts permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    Scaffold(
        topBar = { TopAppBar({Text(currentFile.name)}) },
        floatingActionButton = {
            Column {
                ShareFAB(path)
                Spacer(Modifier.height(6.dp))
                FloatingActionButton({
                    requestContactsPermission.launch(Manifest.permission.WRITE_CONTACTS)
                }) {
                    Icon(painterResource(R.drawable.outline_download_24), null)
                }
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            LazyColumn {
                items(contacts) {
                    VCardContactCard(it)
                }
            }
        }
    }
}

@Composable
fun VCardContactCard(contact: VCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Full Name (FN) - The primary identifier
            Text(
                text = contact.formattedName ?: "No Name",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Optional: Show the structured name if available
            contact.name?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${it.givenName} ${it.familyName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Organization
            if (contact.organizations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🏢 ${contact.organizations.first()}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Emails
            if (contact.emails.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📧 Emails:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                contact.emails.forEach { email ->
                    Text(
                        text = "- $email",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Phone Numbers
            if (contact.telephones.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📞 Phones:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                contact.telephones.forEach { phone ->
                    Text(
                        text = "- $phone",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Addresses
            if (contact.addresses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🏠 Addresses:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                contact.addresses.forEach { address ->
                    Text(
                        text = "- $address",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Notes
            if (contact.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Notes:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                contact.notes.forEach { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // URLs
            if (contact.urls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🔗 URLs:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                contact.urls.forEach { url ->
                    Text(
                        text = "- $url",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}