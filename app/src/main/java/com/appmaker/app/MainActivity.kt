package com.appmaker.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
    private lateinit var previewImageView: ImageView

    private val notesList = mutableListOf<NoteModel>()
    private lateinit var notesAdapter: NotesAdapter
    
    // حفظ مسار الصورة أو البيتماب بشكل آمن
    private var currentImageUri: Uri? = null
    private var currentBitmap: Bitmap? = null

    data class NoteModel(val title: String, val content: String, val imageUri: Uri?, val bitmap: Bitmap?)

    // طلب إذن الكاميرا
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "يرجى تفعيل صلاحية الكاميرا", Toast.LENGTH_SHORT).show()
        }
    }

    // التقاط صورة الكاميرا
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                currentBitmap = imageBitmap
                currentImageUri = null
                previewImageView.setImageBitmap(imageBitmap)
                previewImageView.visibility = View.VISIBLE
                Toast.makeText(this, "📸 تم التقاط الصورة وإرفاقها", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // اختيار صورة من المعرض
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                currentImageUri = uri
                currentBitmap = null
                previewImageView.setImageURI(uri)
                previewImageView.visibility = View.VISIBLE
                Toast.makeText(this, "🖼 تم تحديد الصورة بنجاح", Toast.LENGTH_SHORT).show()
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

        // إنشاء نافذة معاينة سريعة برمجياً تحت الأزرار
        val density = resources.displayMetrics.density
        previewImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (160 * density).toInt()
            ).apply {
                val pad = (10 * density).toInt()
                setMargins(pad, pad, pad, pad)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.parseColor("#2A2A3C"))
            visibility = View.GONE
        }

        // إضافة صورة المعاينة مباشرة فوق الـ RecyclerView
        val parentLayout = recyclerViewNotes.parent as? ViewGroup
        if (parentLayout != null) {
            val index = parentLayout.indexOfChild(recyclerViewNotes)
            parentLayout.addView(previewImageView, index)
        }

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

            if (title.isNotEmpty() || content.isNotEmpty() || currentImageUri != null || currentBitmap != null) {
                notesList.add(0, NoteModel(title, content, currentImageUri, currentBitmap))
                notesAdapter.notifyItemInserted(0)
                recyclerViewNotes.scrollToPosition(0)

                // تصفير الحقول ومعاينة الصورة
                etTitle.text.clear()
                etContent.text.clear()
                currentImageUri = null
                currentBitmap = null
                previewImageView.setImageDrawable(null)
                previewImageView.visibility = View.GONE

                Toast.makeText(this, "💾 تم حفظ الملاحظة وإدراجها بالأسفل", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "اكتب نصاً أو اختر صورة أولاً", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            cameraLauncher.launch(takePictureIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر تشغيل الكاميرا", Toast.LENGTH_SHORT).show()
        }
    }

    // بناء كرت الملاحظة في القائمة
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

            val cardLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                val pad = (12 * density).toInt()
                val bottomM = (12 * density).toInt()
                setPadding(pad, pad, pad, pad)
                setBackgroundColor(Color.parseColor("#1E1E2E"))
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = bottomM
                }
            }

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

            val tvTitle = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#BB86FC"))
            }

            val tvContent = TextView(context).apply {
                val topM = (4 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = topM
                }
                textSize = 14f
                setTextColor(Color.parseColor("#DDDDDD"))
            }

            cardLayout.addView(ivImage)
            cardLayout.addView(tvTitle)
            cardLayout.addView(tvContent)

            return NoteViewHolder(cardLayout, ivImage, tvTitle, tvContent)
        }

        override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
            val note = notes[position]
            holder.tvTitle.text = if (note.title.isNotEmpty()) note.title else "ملاحظة جديدة"
            holder.tvContent.text = note.content

            when {
                note.bitmap != null -> {
                    holder.ivImage.setImageBitmap(note.bitmap)
                    holder.ivImage.visibility = View.VISIBLE
                }
                note.imageUri != null -> {
                    holder.ivImage.setImageURI(note.imageUri)
                    holder.ivImage.visibility = View.VISIBLE
                }
                else -> {
                    holder.ivImage.visibility = View.GONE
                }
            }
        }

        override fun getItemCount(): Int = notes.size
    }
}
