package com.example.quanlyfile

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.content.Intent
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class FileManagementActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var currentDirectory: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listView)
        registerForContextMenu(listView)

        currentDirectory = File("/sdcard")
        updateListView()

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = listView.getItemAtPosition(position) as String
            val selectedFile = File(currentDirectory, selectedItem)

            if (selectedFile.isDirectory) {
                currentDirectory = selectedFile
                updateListView()
            } else {
                openFile(selectedFile)
            }
        }
    }

    private fun updateListView() {
        val files = currentDirectory.listFiles()
        val fileNames = files?.map { it.name }?.toTypedArray() ?: arrayOf()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileNames)
        listView.adapter = adapter
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val selectedItem = listView.getItemAtPosition(info.position) as String
        val selectedFile = File(currentDirectory, selectedItem)

        return when (item.itemId) {
            R.id.menu_rename -> {
                showRenameDialog(selectedFile)
                true
            }
            R.id.menu_delete -> {
                showDeleteDialog(selectedFile)
                true
            }
            R.id.menu_copy -> {
                showCopyDialog(selectedFile)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_new_folder -> {
                showCreateFolderDialog()
                true
            }
            R.id.menu_new_text_file -> {
                showCreateTextFileDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openFile(file: File) {
        val extension = file.extension.toLowerCase()
        if (extension == "txt") {
            openTextFile(file)
        } else if (extension == "bmp" || extension == "jpg" || extension == "png") {
            openImageFile(file)
        } else {
            showToast("Unsupported file type")
        }
    }

    private fun openTextFile(file: File) {
        try {
            val inputStream = FileInputStream(file)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()

            val text = String(buffer)
            showFileContentDialog("Text File: ${file.name}", text)
        } catch (e: IOException) {
            showToast("Error opening text file")
        }
    }

    private fun openImageFile(file: File) {
        // Implement image file opening logic
        showToast("Opening Image File: ${file.name}")
    }

    private fun showFileContentDialog(title: String, content: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showRenameDialog(file: File) {
        val editText = EditText(this)
        editText.setText(file.name)
        AlertDialog.Builder(this)
            .setTitle("Rename")
            .setMessage("Enter new name:")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val newName = editText.text.toString()
                val newFile = File(file.parent, newName)
                if (file.renameTo(newFile)) {
                    updateListView()
                } else {
                    showToast("Rename failed")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("OK") { _, _ ->
                if (deleteFileOrDirectory(file)) {
                    updateListView()
                } else {
                    showToast("Delete failed")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCopyDialog(file: File) {
        val destinationEditText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Copy")
            .setMessage("Enter destination folder:")
            .setView(destinationEditText)
            .setPositiveButton("OK") { _, _ ->
                val destinationPath = destinationEditText.text.toString()
                val destinationDir = File(destinationPath)
                if (destinationDir.isDirectory) {
                    val newFile = File(destinationDir, file.name)
                    if (copyFile(file, newFile)) {
                        updateListView()
                    } else {
                        showToast("Copy failed")
                    }
                } else {
                    showToast("Invalid destination folder")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateFolderDialog() {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("New Folder")
            .setMessage("Enter folder name:")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val folderName = editText.text.toString()
                val newFolder = File(currentDirectory, folderName)
                if (newFolder.mkdir()) {
                    updateListView()
                } else {
                    showToast("Create folder failed")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateTextFileDialog() {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("New Text File")
            .setMessage("Enter file name:")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val fileName = editText.text.toString()
                val newFile = File(currentDirectory, fileName)
                try {
                    if (newFile.createNewFile()) {
                        updateListView()
                    } else {
                        showToast("Create text file failed")
                    }
                } catch (e: IOException) {
                    showToast("Create text file failed")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFileOrDirectory(file: File): Boolean {
        if (file.isDirectory) {
            val children = file.list()
            for (child in children) {
                val success = deleteFileOrDirectory(File(file, child))
                if (!success) {
                    return false
                }
            }
        }
        return file.delete()
    }

    private fun copyFile(sourceFile: File, dest
}