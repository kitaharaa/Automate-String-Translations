package com.kitaharaa

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object FilePaths {
    const val projectDirPath = "app/src/main/res"
    const val sourceFileDirPath = "/values/strings.xml"
    const val targerFileDirPath = "/values-de/strings.xml"
    const val attributeName = "name"
}

fun main() {
    println("AT: started")
    val projectDir = File(FilePaths.projectDirPath)
    val sourceFile = File(projectDir, FilePaths.sourceFileDirPath)
    val targetFile = File(projectDir, FilePaths.targerFileDirPath)
    // Load and parse both XML files
    val stringDocument = parseXmlFile(sourceFile)
    val stringDeDocument = parseXmlFile(targetFile)

    // Add missing strings to de-string.xml
    val addedAttributes = addMissingStrings(stringDocument, stringDeDocument)

    if (addedAttributes.isNotEmpty()) {
        saveXmlFile(targetFile, stringDeDocument)
        executeGitCommand("git", "add", FilePaths.projectDirPath + FilePaths.targerFileDirPath)
    }
    println("Added attributes count: ${addedAttributes.size}")
    println("Added attributes itself: $addedAttributes")
    println("AT: ended")
}

fun getLastCommitAttributes(filePath: String): Map<String, String> {
    // Get the first commit hash for the file
    val firstCommit = executeGitCommand("git", "rev-list", "--max-parents=0", "HEAD", "--", filePath)
        .inputStream.bufferedReader().readText().trim()
    // Get the last commit hash (latest commit)
    val lastCommit = executeGitCommand("git", "rev-parse", "HEAD")
        .inputStream.bufferedReader().readText().trim()

    println("fc = $firstCommit, lc = $lastCommit")

    // Get the diff between the first and last commit
    val diffProcess = executeGitCommand("git", "diff", firstCommit, lastCommit, "--", filePath)
    val output = diffProcess.inputStream.bufferedReader().readText()
    diffProcess.waitFor()

    println("Diff output: $output")

    // Extract the names of added strings (lines prefixed with '+')
    return output
        .lineSequence()
        .filter { it.startsWith("+") && !it.startsWith("++") } // Exclude lines starting with "++"
        .mapNotNull { line ->
            // Extract the "name" attribute from the added lines
            val name = Regex("""name="([^"]+)"""").find(line)?.groups?.get(1)?.value
            val content = Regex(""">(.+)<""").find(line)?.groups?.get(1)?.value
            if (name == null || content == null) return@mapNotNull null

            println("Add: name = $name, content = $content")
            name to content + "_modified"
        }.toMap()
}

fun parseXmlFile(file: File): Document {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    return builder.parse(file)
}

fun Document.toElements(): List<Element> {
    val nodeList = getElementsByTagName("string")
    return (0 until nodeList.length).map { index ->
        nodeList.item(index) as Element
    }
}

fun addMissingStrings(defaultLocaleDocument: Document, translatableLocaleDocument: Document): List<String> {
    val defaultLocaleElements = defaultLocaleDocument.toElements()
    val translatableLocaleElements = translatableLocaleDocument.toElements()
    val alreadyTranslatedAttributes =
        translatableLocaleElements.map { it.getAttribute(FilePaths.attributeName) }.toSet()

    val lastCommitAddedAttributes =
        getLastCommitAttributes(FilePaths.projectDirPath + FilePaths.sourceFileDirPath)
    if (lastCommitAddedAttributes.isEmpty()) return emptyList()

    val addedAttributes = mutableListOf<String>()
    // Iterate over source strings and add missing ones to the target
    defaultLocaleElements.forEach { item ->
        val itemAttribute = item.getAttribute(FilePaths.attributeName)

        if (itemAttribute !in alreadyTranslatedAttributes && itemAttribute in lastCommitAddedAttributes.keys) {
            val customNode = translatableLocaleDocument.createElement("string")
            customNode.setAttribute(FilePaths.attributeName, itemAttribute) // Set any attributes as needed
            // Optionally, you can also add some text content to the custom node
            val value = lastCommitAddedAttributes[itemAttribute]
            if (value.isNullOrBlank().not()) {
                customNode.textContent = value
                // Insert the custom node after the new string
                translatableLocaleDocument.documentElement.appendChild(customNode)
                addedAttributes.add(itemAttribute)
            }
        }
    }
    return addedAttributes
}

fun saveXmlFile(file: File, document: Document) {
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer().apply {
        setOutputProperty(OutputKeys.INDENT, "yes")
        setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    }
    val source = DOMSource(document)
    val result = StreamResult(file)
    transformer.transform(source, result)
}

fun executeGitCommand(vararg command: String): Process {
    val commands = command.toList()
    val process = ProcessBuilder(commands)
        .redirectErrorStream(true)
        .start()
    println("Command executed: $commands")
    return process
}