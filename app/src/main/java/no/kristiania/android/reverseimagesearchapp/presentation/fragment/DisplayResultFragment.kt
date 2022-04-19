package no.kristiania.android.reverseimagesearchapp.presentation.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOverlay
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import no.kristiania.android.reverseimagesearchapp.R
import no.kristiania.android.reverseimagesearchapp.data.local.entity.ReverseImageSearchItem
import no.kristiania.android.reverseimagesearchapp.data.local.entity.UploadedImage
import no.kristiania.android.reverseimagesearchapp.databinding.FragmentDisplayResultsBinding
import no.kristiania.android.reverseimagesearchapp.presentation.MainActivity
import no.kristiania.android.reverseimagesearchapp.presentation.OnPhotoListener
import no.kristiania.android.reverseimagesearchapp.presentation.service.ResultImageService
import no.kristiania.android.reverseimagesearchapp.presentation.service.ThumbnailDownloader
import no.kristiania.android.reverseimagesearchapp.presentation.viewmodel.DisplayResultViewModel
import java.io.File

private const val PARENT_IMAGE_DATA = "parent_image_data"
private const val TAG = "DisplayResultImages"

@AndroidEntryPoint
class DisplayResultFragment : Fragment(R.layout.fragment_display_results), OnPhotoListener {

    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var binding: FragmentDisplayResultsBinding
    private lateinit var thumbnailDownloader: ThumbnailDownloader<PhotoHolder>
    private lateinit var mService: ResultImageService
    private var selectedImages = mutableListOf<ReverseImageSearchItem>()
    private lateinit var popupWindow: PopupWindow
    private var overlayImage:ImageView? = null

    private val viewModel by viewModels<DisplayResultViewModel>()
    private var parentImage: UploadedImage? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDisplayResultsBinding.bind(view)

        overlayImage?.findViewById<ImageView>(R.id.overlay_image)
        parentImage = arguments?.getParcelable(PARENT_IMAGE_DATA) as UploadedImage?
        mService = (activity as MainActivity).getService()

        if (parentImage != null) {
            val file = File(requireContext().cacheDir, parentImage!!.photoFileName)
            Log.i(TAG, "THIS IS FILE LENGTH ${file.length()}")
            val bitmap: Bitmap = BitmapFactory.decodeFile(file.path)

            binding.parentImageView.setImageBitmap(bitmap)
        }

        val responseHandler = Handler(Looper.getMainLooper())
        thumbnailDownloader =
            ThumbnailDownloader(responseHandler, mService) { photoHolder, bitmap ->
                val drawable = BitmapDrawable(resources, bitmap)
                photoHolder.bindDrawable(drawable)
            }

        lifecycle.addObserver(thumbnailDownloader.fragmentLifecycleObserver)

        photoRecyclerView = binding.rvList.also {
            it.apply {
                layoutManager = GridLayoutManager(context, 3)
            }
        }

        mService.resultItems.observe(
            viewLifecycleOwner, {
                photoRecyclerView.adapter = PhotoAdapter(it, this)
            }
        )
    }

    companion object {
        fun newInstance(image: UploadedImage?): DisplayResultFragment {
            val args = Bundle().apply {
                putParcelable(PARENT_IMAGE_DATA, image)
                Log.i(TAG, "${image?.urlOnServer}")
            }
            return DisplayResultFragment().apply {
                arguments = args
            }
        }
    }

    private inner class PhotoHolder(
        private val itemImageButton: ImageButton,
        private val onPhotoListener: OnPhotoListener,
    ) :
        RecyclerView.ViewHolder(itemImageButton), View.OnClickListener {
        init {
            itemImageButton.setOnClickListener(this)
        }

        val bindDrawable: (Drawable) -> Unit = itemImageButton::setImageDrawable

        override fun onClick(view: View) {
            onPhotoListener.onPhotoClick(layoutPosition, view)
        }
    }

    private inner class PhotoAdapter(
        private val resultItems: List<ReverseImageSearchItem>,
        private val onPhotoListener: OnPhotoListener,
    ) :
        RecyclerView.Adapter<PhotoHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            val view = layoutInflater.inflate(
                R.layout.list_results_gallery,
                parent,
                false
            ) as ImageButton
            return PhotoHolder(view, onPhotoListener)
        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val galleryItem = resultItems[position]
            val placeholder: Drawable = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_file_download,
            ) ?: ColorDrawable()
            holder.bindDrawable(placeholder)
            thumbnailDownloader.queueThumbnail(holder, galleryItem.link)
        }

        override fun getItemCount(): Int = resultItems.size
    }

    override fun onPhotoClick(position: Int, view: View) {
        val highlight = ResourcesCompat.getDrawable(resources, R.drawable.highlight, null)
        view.background = highlight

        mService.resultItems.value?.get(position)?.let { selectedImages.add(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        parentImage = null
    }

//    private fun startService(){
//        val serviceIntent = Intent(requireActivity(), ResultImageService::class.java)
//        requireActivity().startService(serviceIntent)
//        bindService()
//    }
//
//    private fun bindService(){
//        val serviceIntent = Intent(requireActivity(), ResultImageService::class.java)
//        requireActivity().bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
//    }
}