package com.appmaker.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnCamera: Button
    private lateinit var btnGallery: Button
    private lateinit var btnAddNote: Button
    private lateinit var recyclerViewNotes: RecyclerView

    private val notesList = mutableListOf<NoteModel>()
    private lateinit var notesAdapter: NotesAdapter
    private var currentAttachedBitmap: Bitmap? = null

    data class NoteModel(val title: String, val content: String, val image: Bitmap?)

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            currentAttachedBitmap = imageBitmap
            Toast.makeText(this, "تم التقاط الصورة بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(this.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                    }
                    currentAttachedBitmap = bitmap
                    Toast.makeText(this, "تم استيراد الصورة بنجاح", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "تعذر تحميل الصورة", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etTitle = findViewById(R.id.etTitle)
        etContent = findViewById(R.id.etContent)
        btnCamera = findViewById(R.id.btnCamera)
        btnGallery = findViewById(R.id.btnGallery)
        btnAddNote = findViewById(R.id.btnAddNote)
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes)

        notesAdapter = NotesAdapter(notesList)
        recyclerViewNotes.layoutManager = LinearLayoutManager(this)
        recyclerViewNotes.adapter = notesAdapter

        btnCamera.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(takePictureIntent)
        }

        btnGallery.setOnClickListener {
            val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(pickPhotoIntent)
        }

        btnAddNote.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isNotEmpty() || content.isNotEmpty() || currentAttachedBitmap != null) {
                notesList.add(0, NoteModel(title, content, currentAttachedBitmap))
                notesAdapter.notifyItemInserted(0)
                recyclerViewNotes.scrollToPosition(0)

                etTitle.text.clear()
                etContent.text.clear()
                currentAttachedBitmap = null
                Toast.makeText(this, "تم حفظ الملاحظة", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "يرجى كتابة محتوى للملاحظة أولاً", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class NotesAdapter(private val notes: List<NoteModel>) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

        inner class NoteViewHolder(parent: ViewGroup) :
            RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)) {
            val titleView: TextView = itemView.findViewById(android.R.id.text1)
            val contentView: TextView = itemView.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            return NoteViewHolder(parent)
        }

        override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
            val note = notes[position]
            holder.titleView.text = if (note.title.isNotEmpty()) note.title else "بدون عنوان"
            holder.contentView.text = note.content
            holder.titleView.setTextColor(Color.parseColor("#BB86FC"))
            holder.contentView.setTextColor(Color.parseColor("#CCCCCC"))
        }

        override fun getItemCount(): Int = notes.size
    }
}