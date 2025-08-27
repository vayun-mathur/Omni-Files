package com.vayun.files.parser

data class VCard(
    val version: String,
    val formattedName: String? = null,
    val name: VCardName? = null,
    val profile: String? = null,
    val kind: VCardKind? = null,
    val uid: String? = null,
    val notes: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val telephones: List<String> = emptyList(),
    val addresses: List<String> = emptyList(),
    val titles: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    val organizations: List<String> = emptyList(),
    val urls: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
    val birthdays: List<String> = emptyList(),
    val anniversaries: List<String> = emptyList(),
    val revisions: List<String> = emptyList(),
    val source: String? = null
)

data class VCardName(
    val familyName: String?,
    val givenName: String?,
    val middleName: String?,
    val prefixes: String?,
    val suffixes: String?
)

enum class VCardKind { INDIVIDUAL, GROUP, ORG, LOCATION }

data class VCardProperty(val name: String, val params: Map<String, String> = emptyMap(), val value: String)

object ContactsParser: Parser<List<VCard>>() {

    override fun parse(input: String): List<VCard> {
        val unfolded = unfoldLines(input)
        val lines = unfolded.split(Regex("\r?\n")).map { it.trimEnd() }.filter { it.isNotBlank() }
        val iterator = lines.iterator()
        val vcards = mutableListOf<VCard>()

        while (iterator.hasNext()) {
            val line = iterator.next()
            if (line.equals("BEGIN:VCARD", true)) {
                vcards.add(parseVCard(iterator))
            }
        }
        return vcards
    }

    private fun unfoldLines(input: String) = buildString {
        input.split(Regex("\r?\n")).forEach { line ->
            if (line.startsWith(" ") || line.startsWith("\t")) append(line.drop(1)) else {
                if (isNotEmpty()) append("\n")
                append(line)
            }
        }
    }

    private fun parseLine(line: String): VCardProperty {
        val (nameAndParams, value) = line.split(":", limit = 2)
        val parts = nameAndParams.split(";")
        val name = parts[0].uppercase()
        val params = parts.drop(1).associate {
            val (k, v) = it.split("=", limit = 2)
            k.uppercase() to v
        }
        return VCardProperty(name, params, value)
    }

    private fun parseVCard(iterator: Iterator<String>): VCard {
        val props = mutableListOf<VCardProperty>()

        while (iterator.hasNext()) {
            val line = iterator.next()
            if (line.equals("END:VCARD", true)) {
                return buildVCard(props)
            }
            props.add(parseLine(line))
        }
        error("Missing END:VCARD")
    }

    private fun buildVCard(props: List<VCardProperty>): VCard {
        return VCard(
            version = props.find { it.name == "VERSION" }?.value ?: error("VERSION required"),
            formattedName = props.find { it.name == "FN" }?.value,
            name = props.find { it.name == "N" }?.value?.split(";")?.let {
                VCardName(
                    familyName = it.getOrNull(0),
                    givenName = it.getOrNull(1),
                    middleName = it.getOrNull(2),
                    prefixes = it.getOrNull(3),
                    suffixes = it.getOrNull(4)
                )
            },
            profile = props.find { it.name == "PROFILE" }?.value,
            kind = props.find { it.name == "KIND" }?.value?.let { VCardKind.valueOf(it.uppercase()) },
            uid = props.find { it.name == "UID" }?.value,
            notes = props.filter { it.name == "NOTE" }.map { it.value },
            emails = props.filter { it.name == "EMAIL" }.map { it.value },
            telephones = props.filter { it.name == "TEL" }.map { it.value },
            addresses = props.filter { it.name == "ADR" }.map { it.value },
            titles = props.filter { it.name == "TITLE" }.map { it.value },
            roles = props.filter { it.name == "ROLE" }.map { it.value },
            organizations = props.filter { it.name == "ORG" }.map { it.value },
            urls = props.filter { it.name == "URL" }.map { it.value },
            photos = props.filter { it.name == "PHOTO" }.map { it.value },
            birthdays = props.filter { it.name == "BDAY" }.map { it.value },
            anniversaries = props.filter { it.name == "ANNIVERSARY" }.map { it.value },
            revisions = props.filter { it.name == "REV" }.map { it.value },
            source = props.find { it.name == "SOURCE" }?.value
        )
    }
}