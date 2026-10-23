package com.appmaker.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    // طلب إذن الكاميرا عند التشغيل
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "يرجى منح صلاحية الكاميرا من إعدادات الهاتف", Toast.LENGTH_SHORT).show()
        }
    }

    // التقاط الصورة بالكاميرا
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            currentAttachedBitmap = imageBitmap
            Toast.makeText(this, "📸 تم التقاط الصورة وإرفاقها بالملاحظة", Toast.LENGTH_SHORT).show()
        }
    }

    // اختيار الصورة من المعرض
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
                    Toast.makeText(this, "🖼 تم استيراد الصورة وإرفاقها بالملاحظة", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "تعذر قراءة ملف الصورة", Toast.LENGTH_SHORT).show()
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
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
                Toast.makeText(this, "💾 تم حفظ الملاحظة بنجاح", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "يرجى كتابة نص أو التقاط صورة أولاً", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            cameraLauncher.launch(takePictureIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر تشغيل تطبيق الكاميرا", Toast.LENGTH_SHORT).show()
        }
    }

    // بناء واجهة بطاقة الملاحظة والصورة برمجياً 100% دون الحاجة لأي ملف XML
    inner class NotesAdapter(private val notes: List<NoteModel>) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

        inner class NoteViewHolder(
            val cardView: View,
            val ivImage: ImageView,
            val tvTitle: TextView,
            val tvContent: TextView
        ) : RecyclerView.ViewHolder(cardView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            val context = parent.context
            val density = context.resources.displayMetrics.density

            // حاوية البطاقة
            val cardLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                val pad = (14 * density).toInt()
                val bottomM = (10 * density).toInt()
                setPadding(pad, pad, pad, pad)
                setBackgroundColor(Color.parseColor("#1E1E2E"))
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = bottomM
                }
            }

            // عنصر عرض الصورة المرفقة
            val ivImage = ImageView(context).apply {
                val imgHeight = (180 * density).toInt()
                val imgBottomM = (8 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    imgHeight
                ).apply {
                    bottomMargin = imgBottomM
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                visibility = View.GONE
            }

            // عنوان الملاحظة
            val tvTitle = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#BB86FC"))
            }

            // محتوى الملاحظة
            val tvContent = TextView(context).apply {
                val topM = (4 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = topM
                }
                textSize = 14f
                setTextColor(Color.parseColor("#CCCCCC"))
            }

            cardLayout.addView(ivImage)
            cardLayout.addView(tvTitle)
            cardLayout.addView(tvContent)

            return NoteViewHolder(cardLayout, ivImage, tvTitle, tvContent)
        }

        override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
            val note = notes[position]
            holder.tvTitle.text = if (note.title.isNotEmpty()) note.title else "بدون عنوان"
            holder.tvContent.text = note.content

            if (note.image != null) {
                holder.ivImage.setImageBitmap(note.image)
                holder.ivImage.visibility = View.VISIBLE
            } else {
                holder.ivImage.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = notes.size
    }
}
